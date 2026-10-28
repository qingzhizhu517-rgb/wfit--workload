package com.workload.system.integration;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionTemplate;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.*;
import com.workload.system.domain.dto.CalculationRunRequest;

/** Two independent physical JDBC connections, with the first transaction deliberately held open. */
class WorkloadConcurrencyIT {
    WorkloadMysqlFixture f;
    ExecutorService pool;
    @BeforeEach void setup() throws Exception { f = new WorkloadMysqlFixture(); pool = Executors.newFixedThreadPool(2); }
    @AfterEach void close() throws Exception { if(pool != null) { pool.shutdownNow(); assertThat(pool.awaitTermination(15, TimeUnit.SECONDS)).isTrue(); } if(f != null) f.close(); }
    @Test void absentSummaryStillSerializesFirstCalculationAndSourceWrite() throws Exception {
        CountDownLatch locked = new CountDownLatch(1), release = new CountDownLatch(1), started = new CountDownLatch(1);
        Future<?> first = pool.submit(() -> f.tx.execute(s -> {
            f.guard.lockDraftOrAbsent(1003L, f.SEM); locked.countDown(); await(release);
            f.calc.recalcAll(1003L, f.SEM); return null;
        }));
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        Future<?> second = pool.submit(() -> { started.countDown(); return f.tx.execute(s -> {
            BizTeachingTask task = new BizTeachingTask(); task.setUserId(1003L); task.setSemester(f.SEM); task.setCourseName("新增课程");
            return f.tasks.insertBizTeachingTask(task);
        }); });
        try { assertThat(started.await(5, TimeUnit.SECONDS)).isTrue(); assertWaitingOnDatabaseLock(second); }
        finally { release.countDown(); }
        first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
        assertThat(f.jdbc.queryForObject("SELECT needs_recalculation FROM biz_workload_summary", Integer.class)).isEqualTo(1);
        assertThat(f.jdbc.queryForObject("SELECT total_workload FROM biz_workload_summary", BigDecimal.class)).isEqualByComparingTo("32");
    }
    @Test void deleteWaitingForNewDetailMustNotLeaveAnOrphan() throws Exception {
        f.jdbc.update("DELETE FROM biz_wl_theory WHERE item_id=9");
        CountDownLatch locked = new CountDownLatch(1), release = new CountDownLatch(1), read = new CountDownLatch(1);
        Future<?> first = pool.submit(() -> f.tx.execute(s -> {
            f.guard.lockDraftOrAbsent(1003L, f.SEM);
            f.jdbc.update("INSERT INTO biz_wl_theory(item_id,J1,C1,K1,Q1,Q2,N) VALUES(9,32,1,1,1,1,1)");
            locked.countDown(); await(release); return null;
        }));
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        Future<?> second = pool.submit(() -> WorkloadMysqlFixture.authenticated(() -> f.tx.execute(s -> {
            assertThat(f.count("biz_wl_theory")).isZero(); // Establish an RR view before waiting for the period lock.
            read.countDown(); return f.items.deleteBizWorkloadItemById(9L);
        })));
        try { assertThat(read.await(5, TimeUnit.SECONDS)).isTrue(); assertWaitingOnDatabaseLock(second); }
        finally { release.countDown(); }
        first.get(10, TimeUnit.SECONDS);
        assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(ServiceException.class);
        assertThat(f.count("biz_workload_item")).isEqualTo(1);
        assertThat(f.count("biz_wl_theory")).isEqualTo(1);
    }
    @Test void repeatedCourseCountUsesCommittedRowsAfterWaitingForPeriodLock() throws Exception {
        CountDownLatch locked = new CountDownLatch(1), release = new CountDownLatch(1), read = new CountDownLatch(1);
        Future<?> first = pool.submit(() -> f.tx.execute(s -> {
            f.guard.lockDraftOrAbsent(1003L, f.SEM);
            f.jdbc.update("INSERT INTO biz_teaching_task(user_id,semester,course_name,education_level,class_name) VALUES(1003,?,'并发课程','本科','一班')", f.SEM);
            locked.countDown(); await(release); return null;
        }));
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        Future<Integer> second = pool.submit(() -> f.tx.execute(s -> {
            assertThat(f.count("biz_teaching_task")).isZero(); // Same RR snapshot established by import's user lookup.
            read.countDown(); f.guard.lockDraftOrAbsent(1003L, f.SEM);
            return f.mapper(com.workload.system.mapper.BizTeachingTaskMapper.class).countSameCourseTask(1003L, f.SEM, "并发课程", "本科");
        }));
        try { assertThat(read.await(5, TimeUnit.SECONDS)).isTrue(); assertWaitingOnDatabaseLock(second); }
        finally { release.countDown(); }
        first.get(10, TimeUnit.SECONDS);
        assertThat(second.get(10, TimeUnit.SECONDS)).as("第二班的自动次序必须是已提交计数1加1").isEqualTo(1);
    }
    @Test void duplicateReviewOfSameVersionHasExactlyOneWinner() throws Exception {
        f.calc.recalcAll(1003L, f.SEM);
        CountDownLatch go = new CountDownLatch(1);
        Callable<Boolean> review = () -> { await(go); try { WorkloadMysqlFixture.authenticated(() -> f.items.confirm(9L, 1L, new BigDecimal("32"))); return true; } catch(ServiceException expected) { return false; } };
        Future<Boolean> a = pool.submit(review), b = pool.submit(review); go.countDown();
        assertThat((a.get(10, TimeUnit.SECONDS) ? 1 : 0) + (b.get(10, TimeUnit.SECONDS) ? 1 : 0)).isEqualTo(1);
    }
    @Test void calculationWaitingForCompletionMustRejectLatestFrozenState() throws Exception {
        f.calc.recalcAll(1003L, f.SEM);
        f.jdbc.update("UPDATE biz_workload_item SET status=1 WHERE id=9");
        f.jdbc.update("UPDATE biz_workload_summary SET status=1");
        CountDownLatch changed = new CountDownLatch(1), release = new CountDownLatch(1);
        Future<?> first = pool.submit(() -> f.tx.execute(s -> {
            try { WorkloadMysqlFixture.authenticated(() -> { f.audit.approve(f.summaryId()); return null; }); }
            catch(Exception e) { throw new RuntimeException(e); }
            changed.countDown(); await(release); return null;
        }));
        assertThat(changed.await(5, TimeUnit.SECONDS)).isTrue();
        Future<?> second = pool.submit(() -> f.calc.run(new CalculationRunRequest(1003L, f.SEM, false)));
        try { assertWaitingOnDatabaseLock(second); }
        finally { release.countDown(); }
        first.get(10, TimeUnit.SECONDS);
        assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(ServiceException.class);
        assertThat(f.jdbc.queryForObject("SELECT status FROM biz_workload_summary", Integer.class)).isEqualTo(2);
        assertThat(f.count("biz_workload_calc_snapshot")).isEqualTo(1);
    }
    @Test void coefficientApprovalAndSubmitSerializeAndNeverSubmitOldTotals() throws Exception {
        f.calc.recalcAll(1003L, f.SEM);
        f.jdbc.update("INSERT INTO biz_coefficient_adjustment(id,user_id,semester,item_id,category,factor_code,old_value,requested_value,reason,status,base_calculation_version) VALUES(1,1003,?,9,'G1','C1',1,2,'test',0,1)",f.SEM);
        CountDownLatch changed = new CountDownLatch(1), release = new CountDownLatch(1);
        Future<?> first = pool.submit(() -> f.tx.execute(s -> {
            try { WorkloadMysqlFixture.authenticated(() -> { f.adjustments.approve(1L,"通过"); f.items.confirm(9L,2L,new BigDecimal("64")); return null; }); }
            catch(Exception e) { throw new RuntimeException(e); }
            changed.countDown(); await(release); return null;
        }));
        assertThat(changed.await(5, TimeUnit.SECONDS)).isTrue();
        Future<?> second = pool.submit(() -> WorkloadMysqlFixture.authenticated(() -> { f.audit.submit(f.summaryId()); return null; }));
        try { assertWaitingOnDatabaseLock(second); }
        finally { release.countDown(); }
        first.get(10, TimeUnit.SECONDS);
        assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(ServiceException.class);
        assertThat(f.jdbc.queryForObject("SELECT status FROM biz_workload_summary", Integer.class)).isZero();
    }
    private void assertWaitingOnDatabaseLock(Future<?> waiting) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        boolean observed = false;
        while (System.nanoTime() < deadline && !waiting.isDone()) {
            Integer count = f.admin.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l ON l.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID AND l.ENGINE=w.ENGINE WHERE l.OBJECT_SCHEMA=?", Integer.class, f.schema);
            if (count != null && count > 0) { observed = true; break; }
            Thread.sleep(20);
        }
        assertThat(observed).as("必须观察到隔离schema的真实数据库锁等待，不能以线程调度延迟代替").isTrue();
        assertThat(waiting.isDone()).isFalse();
    }
    static void await(CountDownLatch latch) { try { if(!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("transaction barrier timed out"); } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); } }
}
