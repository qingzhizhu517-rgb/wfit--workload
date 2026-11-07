package com.workload.system.integration;

import com.workload.system.review.*;
import com.workload.system.domain.*;
import com.workload.system.mapper.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class ReviewLifecycleIT {
    WorkloadMysqlFixture f;
    @BeforeEach void open() throws Exception {f=new WorkloadMysqlFixture();
        f.jdbc.update("UPDATE biz_workload_item SET source_type='MANUAL',description='历史人工录入' WHERE id=9");}
    @AfterEach void close(){if(f!=null)f.close();}
    BizWorkloadSummary ready() throws Exception {
        f.calc.recalcItem(9L);
        var s=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);
        var i=f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(9L);
        WorkloadMysqlFixture.authenticated(()->{f.items.confirm(9L,i.getCalculationVersion(),i.getCalculatedWorkload());f.audit.submit(s.getId());return null;});
        return f.mapper(BizWorkloadSummaryMapper.class).selectBizWorkloadSummaryById(s.getId());
    }
    ItemReviewService reviews(){return (ItemReviewService)f.beans.get(ItemReviewService.class);}
    @Test void submitCreatesFrozenBatchAndFinalApprovalNeedsAllPass() throws Exception {
        var s=ready();assertThat(s.getReviewBatchNo()).isNotBlank();assertThat(f.count("biz_workload_review_batch")).isEqualTo(1);assertThat(f.count("biz_workload_item_review")).isEqualTo(1);
        WorkloadMysqlFixture.authenticated(()->{assertThatThrownBy(()->f.audit.approve(s.getId())).hasMessageContaining("未通过");return null;});
        long id=f.jdbc.queryForObject("SELECT id FROM biz_workload_item_review",Long.class);
        WorkloadMysqlFixture.authenticated(()->{reviews().review(id,"PASS",new ReviewCommands.Review(0,null));f.audit.approve(s.getId());return null;});
        assertThat(f.jdbc.queryForObject("SELECT status FROM biz_workload_summary",Integer.class)).isEqualTo(2);
        assertThat(f.jdbc.queryForObject("SELECT version FROM biz_workload_item_review",Long.class)).isEqualTo(1);
    }
    @Test void rejectKeepsHistoryAndResubmitCreatesDifferentBatch() throws Exception {
        var s=ready();String old=s.getReviewBatchNo();long id=f.jdbc.queryForObject("SELECT id FROM biz_workload_item_review",Long.class);
        WorkloadMysqlFixture.authenticated(()->{reviews().review(id,"REJECT",new ReviewCommands.Review(0,"数值需核实"));f.audit.reject(s.getId(),"重新核算");return null;});
        f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);
        WorkloadMysqlFixture.authenticated(()->{var history=f.wire(new ReviewQueryService()).detail(s.getId());assertThat(history.get("reviewEvidenceStatus")).isEqualTo("HISTORY_ONLY");assertThat(((java.util.Map<?,?>)history.get("currentBatch")).get("versionMatches")).isEqualTo(false);return null;});
        WorkloadMysqlFixture.authenticated(()->{f.audit.submit(s.getId());assertThatThrownBy(()->reviews().review(id,"PASS",new ReviewCommands.Review(1,null))).hasMessageContaining("批次");return null;});
        assertThat(f.count("biz_workload_review_batch")).isEqualTo(2);assertThat(f.count("biz_workload_item_review")).isEqualTo(2);
        assertThat(f.mapper(BizWorkloadSummaryMapper.class).selectBizWorkloadSummaryById(s.getId()).getReviewBatchNo()).isNotEqualTo(old);
    }
    @Test void reviewAndSummaryRejectPreserveRawReasonAndRejectRaw501Characters() throws Exception {
        var s=ready();long id=f.jdbc.queryForObject("SELECT id FROM biz_workload_item_review",Long.class);String raw="  原文原因 \n",tooLong=" "+"理".repeat(499)+" ";
        WorkloadMysqlFixture.authenticated(()->{assertThatThrownBy(()->reviews().review(id,"REJECT",new ReviewCommands.Review(0,tooLong))).extracting("code").isEqualTo(400);reviews().review(id,"REJECT",new ReviewCommands.Review(0,raw));assertThatThrownBy(()->f.audit.reject(s.getId(),tooLong)).extracting("code").isEqualTo(400);f.audit.reject(s.getId(),raw);return null;});
        assertThat(f.jdbc.queryForObject("SELECT reason FROM biz_workload_item_review",String.class)).isEqualTo(raw);assertThat(f.jdbc.queryForObject("SELECT remark FROM biz_workload_summary",String.class)).isEqualTo(raw);assertThat(f.jdbc.queryForObject("SELECT reason FROM biz_audit_log WHERE action='reject'",String.class)).isEqualTo(raw);
    }
    @Test void repeatedVersionAndTamperedPayCannotPass() throws Exception {
        var s=ready();long id=f.jdbc.queryForObject("SELECT id FROM biz_workload_item_review",Long.class);
        WorkloadMysqlFixture.authenticated(()->{reviews().review(id,"PASS",new ReviewCommands.Review(0,null));assertThatThrownBy(()->reviews().review(id,"PASS",new ReviewCommands.Review(0,null))).isInstanceOf(com.workload.common.exception.ServiceException.class);return null;});
        f.jdbc.update("UPDATE biz_pay_record SET total_pay=total_pay+1");
        WorkloadMysqlFixture.authenticated(()->{assertThatThrownBy(()->f.audit.approve(s.getId())).hasMessageContaining("酬金");return null;});
    }
    @Test void detailHasFrozenSourceCountsAndNoUnboundedMembers() throws Exception {
        var s=ready();var queries=f.wire(new ReviewQueryService());
        WorkloadMysqlFixture.authenticated(()->{var detail=queries.detail(s.getId());assertThat(detail.get("sourceCounts")).isNotNull();var json=tools.jackson.databind.json.JsonMapper.builder().build();assertThat(json.readTree(json.writeValueAsString(detail)).path("summarySnapshot").path("g7").asText()).isEqualTo("32");assertThat(detail.get("currentBatch").toString()).doesNotContain("snapshotJson").doesNotContain("items=");return null;});
        f.jdbc.update("DELETE FROM biz_wl_theory");f.jdbc.update("DELETE FROM biz_workload_item");
        WorkloadMysqlFixture.authenticated(()->{assertThat(queries.reviews(s.getId(),s.getReviewBatchNo(),new ReviewCommands.Page(1,10)).getRows()).hasSize(1);return null;});
    }
    @Test void batchInsertFailureRollsBackPointerMembersAndStatus() throws Exception {
        f.calc.recalcItem(9L);var s=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);var i=f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(9L);
        WorkloadMysqlFixture.authenticated(()->{f.items.confirm(9L,i.getCalculationVersion(),i.getCalculatedWorkload());return null;});
        f.jdbc.execute("CREATE TRIGGER reject_review BEFORE INSERT ON biz_workload_item_review FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected review failure'");
        WorkloadMysqlFixture.authenticated(()->{assertThatThrownBy(()->f.audit.submit(s.getId())).hasMessageContaining("injected review failure");return null;});
        assertThat(f.count("biz_workload_review_batch")).isZero();assertThat(f.count("biz_workload_item_review")).isZero();assertThat(f.jdbc.queryForObject("SELECT status FROM biz_workload_summary",Integer.class)).isZero();assertThat(f.jdbc.queryForObject("SELECT review_batch_no FROM biz_workload_summary",String.class)).isNull();
    }
    @Test void teacherSignatureDoesNotRewriteBatchOrCalculationVersion() throws Exception {
        var s=ready();var before=f.jdbc.queryForMap("SELECT * FROM biz_workload_review_batch");
        try(var security=org.mockito.Mockito.mockStatic(com.workload.common.utils.SecurityUtils.class)){
            security.when(com.workload.common.utils.SecurityUtils::getUserId).thenReturn(1003L);security.when(com.workload.common.utils.SecurityUtils::getUsername).thenReturn("teacher");f.audit.teacherConfirm(s.getId());
        }
        assertThat(f.jdbc.queryForMap("SELECT * FROM biz_workload_review_batch")).isEqualTo(before);assertThat(f.jdbc.queryForObject("SELECT data_version FROM biz_workload_summary",Long.class)).isEqualTo(s.getDataVersion());
        long id=f.jdbc.queryForObject("SELECT id FROM biz_workload_item_review",Long.class);WorkloadMysqlFixture.authenticated(()->{reviews().review(id,"PASS",new ReviewCommands.Review(0,null));f.audit.approve(s.getId());return null;});
    }
    @Test void fiveHundredCharacterReviewReasonIsNotExpandedPastAuditColumn() throws Exception {
        ready();long id=f.jdbc.queryForObject("SELECT id FROM biz_workload_item_review",Long.class);
        WorkloadMysqlFixture.authenticated(()->{reviews().review(id,"REJECT",new ReviewCommands.Review(0,"理".repeat(500)));return null;});
        assertThat(f.jdbc.queryForObject("SELECT reason FROM biz_workload_item_review",String.class)).hasSize(500);
    }
    @Test void independentReviewPermissionCannotWaiveAndBatchKeepsPartialSuccess() throws Exception {
        var s=ready();long id=f.jdbc.queryForObject("SELECT id FROM biz_workload_item_review",Long.class);
        try(var security=org.mockito.Mockito.mockStatic(com.workload.common.utils.SecurityUtils.class);var scope=org.mockito.Mockito.mockStatic(com.workload.common.utils.DataScopeUtil.class)){
            security.when(com.workload.common.utils.SecurityUtils::getUserId).thenReturn(1004L);security.when(com.workload.common.utils.SecurityUtils::getUsername).thenReturn("assistant");security.when(()->com.workload.common.utils.SecurityUtils.hasPermi("system:auditRecord:review")).thenReturn(true);
            assertThatThrownBy(()->reviews().review(id,"WAIVE",new ReviewCommands.Review(0,"例外"))).extracting("code").isEqualTo(403);
        }
        WorkloadMysqlFixture.authenticated(()->{var receipt=new ReviewBatchCommandService(reviews()).review(java.util.List.of(new ReviewCommands.Entry(id,new ReviewCommands.Review(0,null)),new ReviewCommands.Entry(99999,new ReviewCommands.Review(0,null))),"PASS");assertThat(receipt.get("successCount")).isEqualTo(1);assertThat(receipt.get("failCount")).isEqualTo(1);return null;});
        assertThat(f.jdbc.queryForObject("SELECT result FROM biz_workload_item_review",String.class)).isEqualTo("PASS");
    }
    @Test void concurrentReviewCommandsHaveExactlyOneWinner() throws Exception {
        ready();long id=f.jdbc.queryForObject("SELECT id FROM biz_workload_item_review",Long.class);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var go=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> action=()->{go.await();try{return WorkloadMysqlFixture.authenticated(()->{reviews().review(id,"PASS",new ReviewCommands.Review(0,null));return true;});}catch(com.workload.common.exception.ServiceException conflict){assertThat(conflict.getCode()).isEqualTo(409);return false;}};
        try{var a=pool.submit(action);var b=pool.submit(action);go.countDown();assertThat((a.get(10,java.util.concurrent.TimeUnit.SECONDS)?1:0)+(b.get(10,java.util.concurrent.TimeUnit.SECONDS)?1:0)).isEqualTo(1);}finally{pool.shutdownNow();pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS);}
        assertThat(f.jdbc.queryForObject("SELECT version FROM biz_workload_item_review",Long.class)).isEqualTo(1);
    }
    @Test void reviewHistoryPreventsDeletionEvenAfterPointerCleared() throws Exception {
        var s=ready();WorkloadMysqlFixture.authenticated(()->{f.audit.reject(s.getId(),"保留历史");return null;});f.jdbc.update("UPDATE biz_workload_summary SET review_batch_no=NULL WHERE id=?",s.getId());
        var crud=f.proxy(f.wire(new com.workload.system.service.impl.BizWorkloadSummaryServiceImpl()));
        assertThatThrownBy(()->((com.workload.system.service.IBizWorkloadSummaryService)crud).deleteBizWorkloadSummaryById(s.getId())).hasMessageContaining("审核历史");assertThat(f.count("biz_workload_summary")).isEqualTo(1);
    }
    @Test void reviewAuditFailureRollsBackResultAndVersion() throws Exception {
        ready();long id=f.jdbc.queryForObject("SELECT id FROM biz_workload_item_review",Long.class);f.jdbc.execute("CREATE TRIGGER reject_review_audit BEFORE INSERT ON biz_audit_log FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected audit failure'");
        WorkloadMysqlFixture.authenticated(()->{assertThatThrownBy(()->reviews().review(id,"PASS",new ReviewCommands.Review(0,null))).hasMessageContaining("injected audit failure");return null;});
        assertThat(f.jdbc.queryForObject("SELECT result FROM biz_workload_item_review",String.class)).isEqualTo("PENDING");assertThat(f.jdbc.queryForObject("SELECT version FROM biz_workload_item_review",Long.class)).isZero();
    }
    @Test void emptyMemberSetAndUnexplainedSourceCannotSubmit() throws Exception {
        f.jdbc.update("DELETE FROM biz_wl_theory");f.jdbc.update("DELETE FROM biz_workload_item");var empty=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);
        WorkloadMysqlFixture.authenticated(()->{assertThatThrownBy(()->f.audit.submit(empty.getId())).hasMessageContaining("为空");return null;});assertThat(f.count("biz_workload_review_batch")).isZero();
    }
}
