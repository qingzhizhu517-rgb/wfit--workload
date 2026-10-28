package com.workload.system.integration;
import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.*;
import com.workload.system.domain.dto.CalculationRunRequest;
import com.workload.system.mapper.*;

class SourceChangeConsistencyIT {
    WorkloadMysqlFixture f;
    @BeforeEach void setup() throws Exception { f = new WorkloadMysqlFixture(); }
    @AfterEach void close() { if (f != null) f.close(); }
    @Test void deletedDetailCannotBeConfirmedOrExplainedUsingItsOldSnapshot() throws Exception {
        f.calc.recalcAll(1003L, f.SEM);
        f.theory.deleteBizWlTheoryByItemId(9L);
        BizWorkloadItem item = f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(9L);
        assertThat(f.formulas.build(item)).as("删除子表后不能展示旧快照为当前公式").isNull();
        assertThatThrownBy(() -> WorkloadMysqlFixture.authenticated(() -> f.items.confirm(9L, item.getCalculationVersion(), BigDecimal.ZERO))).isInstanceOf(ServiceException.class);
    }
    @Test void changedManualWorkloadCannotSubmitOldSummary() throws Exception {
        f.jdbc.update("UPDATE biz_workload_item SET item_type='G8' WHERE id=9");
        f.calc.recalcAll(1003L, f.SEM);
        WorkloadMysqlFixture.authenticated(() -> {
            BizWorkloadItem edit = new BizWorkloadItem(); edit.setId(9L); edit.setCalculatedWorkload(new BigDecimal("48"));
            f.items.updateBizWorkloadItem(edit); f.items.confirm(9L, 0L, new BigDecimal("48")); return null;
        });
        assertThatThrownBy(() -> WorkloadMysqlFixture.authenticated(() -> { f.audit.submit(f.summaryId()); return null; }))
            .isInstanceOf(ServiceException.class).hasMessageContaining("重新核算");
    }
    @Test void linkedTeachingTaskEditCannotSilentlyDivergeFromItsDetail() {
        f.jdbc.update("INSERT INTO biz_teaching_task(id,user_id,semester,course_name,theory_hours) VALUES(7,1003,?,'数学',32)", f.SEM);
        f.jdbc.update("UPDATE biz_workload_item SET task_id=7 WHERE id=9");
        BizTeachingTask edit = new BizTeachingTask(); edit.setId(7L); edit.setTheoryHours(new BigDecimal("64"));
        assertThatThrownBy(() -> f.tasks.updateBizTeachingTask(edit)).isInstanceOf(ServiceException.class).hasMessageContaining("明细");
        assertThat(f.jdbc.queryForObject("SELECT theory_hours FROM biz_teaching_task WHERE id=7", BigDecimal.class)).isEqualByComparingTo("32");
    }
    @Test void importedCourseInvalidatesExistingTotalsBeforeItCanBeSubmitted() throws Exception {
        f.calc.recalcAll(1003L, f.SEM);
        var users = org.mockito.Mockito.mock(com.workload.system.service.ISysUserService.class);
        var user = new com.workload.common.core.domain.entity.SysUser(); user.setUserId(1003L);
        org.mockito.Mockito.when(users.selectUserByUserName("import_teacher")).thenReturn(user);
        f.beans.put(com.workload.system.service.ISysUserService.class, users);
        var importer = (com.workload.system.service.ITeachingTaskImportService) f.proxy(f.wire(new com.workload.system.service.impl.TeachingTaskImportServiceImpl()));
        var row = new com.workload.system.domain.dto.TeachingTaskImportDTO();
        row.setSemester(f.SEM); row.setUserCode("import_teacher"); row.setCourseName("导入新课"); row.setWorkloadType("G1"); row.setEducationLevel("本科"); row.setPlannedHours(new BigDecimal("16")); row.setStudentCount(30);
        importer.processSingleRow(row,"T03-IMPORT","G1");
        assertThat(f.jdbc.queryForObject("SELECT needs_recalculation FROM biz_workload_summary", Integer.class)).isEqualTo(1);
        assertThat(f.count("biz_workload_calc_snapshot")).isEqualTo(2);
        f.jdbc.update("UPDATE biz_workload_item SET status=1");
        assertThatThrownBy(() -> WorkloadMysqlFixture.authenticated(() -> { f.audit.submit(f.summaryId()); return null; })).hasMessageContaining("重新核算");
    }
    @Test void deletedPayRequiresRecalculationBeforeApproval() throws Exception {
        f.calc.recalcAll(1003L, f.SEM);
        f.jdbc.update("UPDATE biz_workload_item SET status=1");
        var payRecords = (com.workload.system.service.IBizPayRecordService) f.proxy(f.wire(new com.workload.system.service.impl.BizPayRecordServiceImpl()));
        Long id = f.jdbc.queryForObject("SELECT id FROM biz_pay_record", Long.class);
        payRecords.deleteBizPayRecordById(id);
        assertThatThrownBy(() -> WorkloadMysqlFixture.authenticated(() -> { f.audit.submit(f.summaryId()); return null; })).hasMessageContaining("重新核算");
        f.summary.recalcSummary(1003L, f.SEM, true);
        assertThat(f.count("biz_pay_record")).isEqualTo(1);
    }
    @Test void frozenAllowanceInBatchRollsBackEarlierDeleteAndDirtyFlag() {
        f.calc.recalcAll(1003L, f.SEM);
        f.jdbc.update("INSERT INTO biz_workload_summary(user_id,semester,status,needs_recalculation) VALUES(1004,?,2,0)", f.SEM);
        f.jdbc.update("INSERT INTO biz_allowance_item(id,user_id,semester,fee_type,amount,status) VALUES(71,1003,?,'A',45,1),(72,1004,?,'A',60,1)", f.SEM, f.SEM);
        var allowances = (com.workload.system.service.IBizAllowanceItemService) f.proxy(f.wire(new com.workload.system.service.impl.BizAllowanceItemServiceImpl()));
        assertThatThrownBy(() -> allowances.deleteBizAllowanceItemByIds(new Long[]{71L,72L})).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        assertThat(f.count("biz_allowance_item")).isEqualTo(2);
        assertThat(f.jdbc.queryForObject("SELECT needs_recalculation FROM biz_workload_summary WHERE user_id=1003", Integer.class)).isZero();
    }
    @Test void frozenPayInBatchRollsBackEarlierDirtyFlag() {
        f.calc.recalcAll(1003L, f.SEM);
        f.jdbc.update("INSERT INTO biz_workload_summary(user_id,semester,status,needs_recalculation) VALUES(1004,?,2,0)", f.SEM);
        f.jdbc.update("INSERT INTO biz_pay_record(user_id,semester,status) VALUES(1004,?,0)", f.SEM);
        var payRecords = (com.workload.system.service.IBizPayRecordService) f.proxy(f.wire(new com.workload.system.service.impl.BizPayRecordServiceImpl()));
        Long[] ids = f.jdbc.queryForList("SELECT id FROM biz_pay_record ORDER BY user_id", Long.class).toArray(Long[]::new);
        assertThatThrownBy(() -> payRecords.deleteBizPayRecordByIds(ids)).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        assertThat(f.count("biz_pay_record")).isEqualTo(2);
        assertThat(f.jdbc.queryForObject("SELECT needs_recalculation FROM biz_workload_summary WHERE user_id=1003", Integer.class)).isZero();
    }
    @Test void newSourceInvalidatesSummaryAndCannotBeSilentlyOmitted() {
        f.calc.recalcAll(1003L, f.SEM);
        BizTeachingTask task = new BizTeachingTask(); task.setUserId(1003L); task.setSemester(f.SEM); task.setCourseName("待生成课程"); task.setStatus(1);
        f.tasks.insertBizTeachingTask(task);
        assertThat(f.jdbc.queryForObject("SELECT needs_recalculation FROM biz_workload_summary", Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> f.summary.recalcSummary(1003L, f.SEM, true)).isInstanceOf(ServiceException.class).hasMessageContaining("教学任务");
        assertThat(f.jdbc.queryForObject("SELECT needs_recalculation FROM biz_workload_summary", Integer.class)).isEqualTo(1);
    }
    @Test void unsynchronizedPositionCannotBeOmittedAndLinkedPositionCannotDiverge() {
        f.calc.recalcAll(1003L, f.SEM);
        var roles = (com.workload.system.service.IBizRoleAssignmentService) f.proxy(f.wire(new com.workload.system.service.impl.BizRoleAssignmentServiceImpl()));
        BizRoleAssignment role = new BizRoleAssignment(); role.setUserId(1003L); role.setSemester(f.SEM); role.setRoleType("教学管理"); role.setStartDate(new java.util.Date()); role.setAllowanceRate(new BigDecimal("20")); role.setStatus(1);
        roles.insertBizRoleAssignment(role);
        assertThat(f.jdbc.queryForObject("SELECT needs_recalculation FROM biz_workload_summary", Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> f.summary.recalcSummary(1003L, f.SEM, true)).isInstanceOf(ServiceException.class).hasMessageContaining("岗位");
        f.jdbc.update("INSERT INTO biz_workload_item(user_id,semester,item_type,source_type,assignment_id,calculated_workload,status) VALUES(1003,?,'G11','AUTO',?,20,0)", f.SEM, role.getId());
        BizRoleAssignment edit = new BizRoleAssignment(); edit.setId(role.getId()); edit.setAllowanceRate(new BigDecimal("40"));
        assertThatThrownBy(() -> roles.updateBizRoleAssignment(edit)).isInstanceOf(ServiceException.class).hasMessageContaining("明细");
        assertThatThrownBy(() -> roles.deleteBizRoleAssignmentById(role.getId())).isInstanceOf(ServiceException.class).hasMessageContaining("明细");
    }
    @Test void summaryAndPayRefreshAtomicallyAndFailedRefreshKeepsDirtyAmounts() throws Exception {
        f.jdbc.update("UPDATE biz_workload_item SET item_type='G8',calculated_workload=200 WHERE id=9");
        f.calc.recalcAll(1003L, f.SEM);
        f.jdbc.update("INSERT INTO biz_allowance_item(user_id,semester,fee_type,amount,status) VALUES(1003,?,'A',45,1)", f.SEM);
        f.tx.execute(s -> { f.guard.lockDraftOrAbsent(1003L,f.SEM); f.guard.markChanged(1003L,f.SEM); return null; });
        assertThatThrownBy(() -> f.pay.recalcPay(1003L,f.SEM)).isInstanceOf(ServiceException.class).hasMessageContaining("重新核算");
        f.summary.recalcSummary(1003L, f.SEM, true);
        assertThat(f.jdbc.queryForObject("SELECT other_pay_total FROM biz_pay_record", BigDecimal.class)).isEqualByComparingTo("45");
        f.jdbc.update("UPDATE biz_workload_summary SET needs_recalculation=1");
        f.jdbc.update("UPDATE biz_workload_item SET calculated_workload=220 WHERE id=9");
        f.jdbc.execute("CREATE TRIGGER fail_pay_update BEFORE UPDATE ON biz_pay_record FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='late update failure'");
        assertThatThrownBy(() -> f.summary.recalcSummary(1003L, f.SEM, true)).hasMessageContaining("late update failure");
        assertThat(f.jdbc.queryForObject("SELECT needs_recalculation FROM biz_workload_summary", Integer.class)).isEqualTo(1);
        assertThat(f.jdbc.queryForObject("SELECT total_workload FROM biz_workload_summary", BigDecimal.class)).isEqualByComparingTo("200");
    }
    @Test void realRunRollsBackCalculatedItemSnapshotSummaryAndPayOnLateFailure() {
        // SQL trigger fires inside the real pay mapper, after real strategy, snapshot and summary writes.
        f.jdbc.execute("CREATE TRIGGER fail_pay BEFORE INSERT ON biz_pay_record FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected late pay failure'");
        assertThatThrownBy(() -> f.calc.run(new CalculationRunRequest(1003L, f.SEM, false))).hasMessageContaining("injected late pay failure");
        assertThat(f.count("biz_workload_calc_snapshot")).isZero(); assertThat(f.count("biz_workload_summary")).isZero(); assertThat(f.count("biz_pay_record")).isZero();
        assertThat(f.jdbc.queryForObject("SELECT calculation_version FROM biz_workload_item WHERE id=9", Long.class)).isZero();
    }
}
