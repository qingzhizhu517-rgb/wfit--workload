package com.workload.system.integration;

import com.workload.system.review.*;
import com.workload.system.mapper.*;
import com.workload.system.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class ReviewQueryAndNoteIT {
    WorkloadMysqlFixture f;
    @BeforeEach void open() throws Exception {f=new WorkloadMysqlFixture();}
    @AfterEach void close(){if(f!=null)f.close();}
    OfflineAdjustmentNoteService notes(){return f.proxy(f.wire(new OfflineAdjustmentNoteService()));}
    ReviewQueryService queries(){return f.wire(new ReviewQueryService());}
    ReviewCommands.Note command(String key,String reason){return new ReviewCommands.Note(key,new BigDecimal("-12.30"),reason,LocalDate.of(2026,11,6),"线下凭证1",null,"备注");}
    @Test void finishedNoteIsAppendOnlyIdempotentAndDoesNotChangeOriginalRows() throws Exception {
        var s=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);f.jdbc.update("UPDATE biz_workload_summary SET status=2,teacher_sign='original',academic_assistant_sign='original' WHERE id=?",s.getId());
        var originalSummary=f.jdbc.queryForMap("SELECT * FROM biz_workload_summary");var originalPay=f.jdbc.queryForMap("SELECT * FROM biz_pay_record");
        WorkloadMysqlFixture.authenticated(()->{var first=notes().append(s.getId(),command("request-1","补差说明"));var same=notes().append(s.getId(),command("request-1","补差说明"));assertThat(same.getId()).isEqualTo(first.getId());assertThatThrownBy(()->notes().append(s.getId(),command("request-1","异内容"))).hasMessageContaining("幂等");return null;});
        assertThat(f.count("biz_offline_adjustment_note")).isEqualTo(1);assertThat(f.jdbc.queryForMap("SELECT * FROM biz_workload_summary")).isEqualTo(originalSummary);assertThat(f.jdbc.queryForMap("SELECT * FROM biz_pay_record")).isEqualTo(originalPay);
        assertThat(f.jdbc.queryForObject("SELECT handled_by_id FROM biz_offline_adjustment_note",Long.class)).isEqualTo(1004);assertThat(f.jdbc.queryForObject("SELECT settlement_snapshot_id FROM biz_offline_adjustment_note",Long.class)).isNull();
    }
    @Test void fullFiveHundredCharacterReasonStillSavesNoteAndAudit() throws Exception {
        var s=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);f.jdbc.update("UPDATE biz_workload_summary SET status=2 WHERE id=?",s.getId());
        WorkloadMysqlFixture.authenticated(()->{notes().append(s.getId(),command("long-reason","理".repeat(500)));return null;});
        assertThat(f.jdbc.queryForObject("SELECT reason FROM biz_offline_adjustment_note",String.class)).hasSize(500);assertThat(f.count("biz_audit_log")).isEqualTo(1);
    }
    @Test void offlineReasonAndNoteTextPreserveRawWhitespaceAndRaw501IsRejected() throws Exception {
        var s=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);f.jdbc.update("UPDATE biz_workload_summary SET status=2 WHERE id=?",s.getId());String reason="  原文原因 \n",text="  原文备注 \n";
        WorkloadMysqlFixture.authenticated(()->{assertThatThrownBy(()->notes().append(s.getId(),command("raw-501"," "+"理".repeat(499)+" "))).extracting("code").isEqualTo(400);notes().append(s.getId(),new ReviewCommands.Note("raw",new BigDecimal("1.00"),reason,LocalDate.of(2026,11,6),null,null,text));return null;});
        assertThat(f.jdbc.queryForObject("SELECT reason FROM biz_offline_adjustment_note",String.class)).isEqualTo(reason);assertThat(f.jdbc.queryForObject("SELECT note_text FROM biz_offline_adjustment_note",String.class)).isEqualTo(text);
    }
    @Test void draftCannotReceiveOfflineNoteAndTeacherCannotWrite() throws Exception {
        var s=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);
        WorkloadMysqlFixture.authenticated(()->{assertThatThrownBy(()->notes().append(s.getId(),command("req","理由"))).hasMessageContaining("完结");return null;});assertThat(f.count("biz_offline_adjustment_note")).isZero();
    }
    @Test void requestKeyCannotBeReusedByDifferentTrustedActor() throws Exception {
        var s=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);f.jdbc.update("UPDATE biz_workload_summary SET status=2 WHERE id=?",s.getId());
        WorkloadMysqlFixture.authenticated(()->{notes().append(s.getId(),command("actor-bound","理由"));return null;});
        try(var security=org.mockito.Mockito.mockStatic(com.workload.common.utils.SecurityUtils.class);var scope=org.mockito.Mockito.mockStatic(com.workload.common.utils.DataScopeUtil.class)){
            security.when(com.workload.common.utils.SecurityUtils::getUserId).thenReturn(777L);security.when(com.workload.common.utils.SecurityUtils::getUsername).thenReturn("different");security.when(()->com.workload.common.utils.SecurityUtils.hasPermi("system:offlineAdjustmentNote:add")).thenReturn(true);
            assertThatThrownBy(()->notes().append(s.getId(),command("actor-bound","理由"))).extracting("code").isEqualTo(409);
        }assertThat(f.count("biz_offline_adjustment_note")).isEqualTo(1);
    }
    @Test void actualTeacherContextReadsOnlyOwnSummaryAndNeverWritesEvenIfGrantedAdd() throws Exception {
        var s=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);f.jdbc.update("INSERT INTO biz_workload_summary(id,user_id,semester,status) VALUES(9999,2003,?,2)",WorkloadMysqlFixture.SEM);
        var user=new com.workload.common.core.domain.entity.SysUser();user.setUserId(1003L);user.setUserName("teacher");var role=new com.workload.common.core.domain.entity.SysRole();role.setRoleKey("teacher");user.setRoles(java.util.List.of(role));
        var login=new com.workload.common.core.domain.model.LoginUser(1003L,1L,user,java.util.Set.of("system:workloadSummary:query","system:offlineAdjustmentNote:add"));var context=org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login,null,java.util.List.of()));org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        try {assertThat(queries().detail(s.getId()).get("summary")).isNotNull();
            assertThatThrownBy(()->queries().detail(9999L)).hasMessageContaining("无权").extracting("code").isEqualTo(403);assertThatThrownBy(()->queries().reviews(9999L,null,new ReviewCommands.Page(1,10))).hasMessageContaining("无权").extracting("code").isEqualTo(403);assertThatThrownBy(()->queries().logs(9999L,new ReviewCommands.Page(1,10))).hasMessageContaining("无权").extracting("code").isEqualTo(403);assertThatThrownBy(()->queries().notes(9999L,new ReviewCommands.Page(1,10))).hasMessageContaining("无权").extracting("code").isEqualTo(403);assertThatThrownBy(()->notes().append(s.getId(),command("teacher-write","理由"))).extracting("code").isEqualTo(403);
        }finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}
        assertThat(f.count("biz_offline_adjustment_note")).isZero();
    }
    @Test void noteAuditFailureRollsBackAppendWithoutChangingOriginalSummary() throws Exception {
        var s=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);f.jdbc.update("UPDATE biz_workload_summary SET status=2 WHERE id=?",s.getId());var before=f.jdbc.queryForMap("SELECT * FROM biz_workload_summary");f.jdbc.execute("CREATE TRIGGER reject_note_audit BEFORE INSERT ON biz_audit_log FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected audit failure'");
        WorkloadMysqlFixture.authenticated(()->{assertThatThrownBy(()->notes().append(s.getId(),command("audit-rollback","理由"))).hasMessageContaining("injected audit failure");return null;});
        assertThat(f.count("biz_offline_adjustment_note")).isZero();assertThat(f.jdbc.queryForMap("SELECT * FROM biz_workload_summary")).isEqualTo(before);
    }
    @Test void historicalDetailReportsMissingAndReadPagesAreBounded() throws Exception {
        var s=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);
        WorkloadMysqlFixture.authenticated(()->{var detail=queries().detail(s.getId());assertThat(detail.get("reviewEvidenceStatus")).isEqualTo("NOT_SUBMITTED");assertThat(queries().reviews(s.getId(),null,new ReviewCommands.Page(1,10)).getRows()).isEmpty();f.jdbc.update("UPDATE biz_workload_summary SET status=2 WHERE id=?",s.getId());assertThat(queries().detail(s.getId()).get("reviewEvidenceStatus")).isEqualTo("HISTORICAL_MISSING");return null;});
    }
}
