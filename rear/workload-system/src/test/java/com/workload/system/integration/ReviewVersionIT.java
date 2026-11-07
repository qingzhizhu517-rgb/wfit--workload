package com.workload.system.integration;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class ReviewVersionIT {
    WorkloadMysqlFixture f;
    @BeforeEach void open() throws Exception {f=new WorkloadMysqlFixture();}
    @AfterEach void close(){if(f!=null)f.close();}
    @Test void summaryAndIndependentLegacyPayEachAdvanceOnceAndPreviewDoesNotWrite() {
        var first=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);
        assertThat(first.getDataVersion()).isEqualTo(1);assertThat(first.getLastCalculatedAt()).isNotNull();
        var before=f.jdbc.queryForMap("SELECT * FROM biz_workload_summary");
        f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,false);
        assertThat(f.jdbc.queryForMap("SELECT * FROM biz_workload_summary")).isEqualTo(before);
        f.pay.recalcPay(1003L,WorkloadMysqlFixture.SEM);
        var next=f.mapper(com.workload.system.mapper.BizWorkloadSummaryMapper.class).selectBizWorkloadSummaryById(first.getId());
        assertThat(next.getDataVersion()).isEqualTo(2);assertThat(next.getLastCalculatedAt()).isEqualTo(first.getLastCalculatedAt());
        assertThat(f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true).getDataVersion()).isEqualTo(3);
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(longs={0,1})
    void independentPayCannotPromoteHistoricalSummaryWithoutTrueCalculationTime(long version){
        f.jdbc.update("INSERT INTO biz_workload_summary(id,user_id,semester,status,needs_recalculation,data_version,last_calculated_at,performance_pay) VALUES(99,1003,?,0,0,?,NULL,100)",WorkloadMysqlFixture.SEM,version);
        var before=f.jdbc.queryForMap("SELECT * FROM biz_workload_summary");
        assertThatThrownBy(()->f.pay.recalcPay(1003L,WorkloadMysqlFixture.SEM)).isInstanceOf(com.workload.common.exception.ServiceException.class).hasMessageContaining("汇总");
        assertThat(f.jdbc.queryForMap("SELECT * FROM biz_workload_summary")).isEqualTo(before);assertThat(f.count("biz_pay_record")).isZero();
    }
    @Test void failedPayWriteRollsBackSummaryVersionAndCalculationTime() {
        f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);var before=f.jdbc.queryForMap("SELECT * FROM biz_workload_summary");
        f.jdbc.execute("CREATE TRIGGER reject_pay BEFORE UPDATE ON biz_pay_record FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected pay failure'");
        assertThatThrownBy(()->f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true)).hasMessageContaining("injected pay failure");
        assertThat(f.jdbc.queryForMap("SELECT * FROM biz_workload_summary")).isEqualTo(before);
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(ints={1,2})
    void frozenSummaryRejectsEveryRecalculationWithConflictAndChangesNoRows(int status) {
        var summary=f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true);
        f.jdbc.update("UPDATE biz_workload_summary SET status=? WHERE id=?",status,summary.getId());
        var tables=java.util.List.of("biz_workload_summary","biz_pay_record","biz_workload_item","biz_wl_theory","biz_allowance_item","biz_workload_calc_snapshot","biz_workload_settlement_snapshot","biz_workload_review_batch","biz_workload_item_review","biz_audit_log");
        var before=new java.util.LinkedHashMap<String,java.util.List<java.util.Map<String,Object>>>();
        for(var table:tables)before.put(table,f.jdbc.queryForList("SELECT * FROM "+table));
        assertThatThrownBy(()->f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true))
                .isInstanceOf(com.workload.common.exception.ServiceException.class).extracting("code").isEqualTo(409);
        assertThatThrownBy(()->f.pay.recalcPay(1003L,WorkloadMysqlFixture.SEM))
                .isInstanceOf(com.workload.common.exception.ServiceException.class).extracting("code").isEqualTo(409);
        assertThatThrownBy(()->f.calc.recalcItem(9L))
                .isInstanceOf(com.workload.common.exception.ServiceException.class).extracting("code").isEqualTo(409);
        for(var table:tables)assertThat(f.jdbc.queryForList("SELECT * FROM "+table)).as(table+"冻结后保持原行").isEqualTo(before.get(table));
    }
}
