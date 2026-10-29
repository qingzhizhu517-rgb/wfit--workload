package com.workload.system.integration;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.*;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.domain.vo.FactorFormulaVo.FactorVo;
import com.workload.system.mapper.*;
import com.workload.system.service.*;

/** Real MySQL JSON object normalization + DECIMAL(10,2), random disposable schema only. */
class SnapshotIntegrityIT
{
    WorkloadMysqlFixture f;
    @BeforeEach void setup() throws Exception { f = new WorkloadMysqlFixture(); }
    @AfterEach void close() { if (f != null) f.close(); }
    private BizWorkloadItem item() { return f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(9L); }
    private BizWorkloadCalcSnapshot capture()
    {
        return f.tx.execute(status -> new WorkloadSnapshotService(f.mapper(BizWorkloadCalcSnapshotMapper.class)).capture(item(),
                new FactorFormulaVo("G1", "Z × A", new BigDecimal("2"), "captured evidence", List.of(
                    new FactorVo("Z", new BigDecimal("2.00"), "IMPORT_VALUE", "last", "NORMAL"),
                    new FactorVo("A", new BigDecimal("1.0"), "RULE_DEFAULT", "first", "NORMAL"))), "v1"));
    }
    @Test void capturesAndRestoresAfterRealMysqlJsonReorderingAndDecimalScale()
    {
        BizWorkloadCalcSnapshot before = capture();
        BizWorkloadCalcSnapshot after = f.mapper(BizWorkloadCalcSnapshotMapper.class).selectLatestByItemId(9L);
        assertThat(after.getFactorJson()).isNotEqualTo(before.getFactorJson());
        assertThat(after.getResult().scale()).isEqualTo(2);
        BizWorkloadItem item = item(); item.setCalculationVersion(after.getCalculationVersion());
        FactorFormulaVo restored = f.formulas.build(item);
        assertThat(restored.getFactors()).extracting(FactorVo::getKey).containsExactly("Z", "A");
        assertThat(restored.getDescription()).isEqualTo("captured evidence");
        assertThat(restored.getSnapshotHash()).isEqualTo(before.getSnapshotHash());
    }
    @ParameterizedTest @ValueSource(ints={0,1})
    void validatesRealLegacyAlgorithmAfterMysqlNormalization(int version)
    {
        BizWorkloadCalcSnapshot s = new BizWorkloadCalcSnapshot();
        s.setItemId(9L); s.setCalculationVersion(1L); s.setFormulaExpression("R5 × K5");
        s.setResult(new BigDecimal("2")); s.setRuleVersion("historical"); s.setCreatedAt(new java.util.Date());
        s.setFactorJson("{\"R5\":\"2.0\",\"K5\":\"1.0\"}");
        s.setSourceJson("{\"R5\":\"IMPORT_VALUE\",\"K5\":\"RULE_DEFAULT\"}");
        if (version == 1) s.setContextJson(SnapshotContextCodec.encode(new FactorFormulaVo("G5", "R5 × K5", s.getResult(), "legacy", List.of(
                new FactorVo("R5", "2.0", "IMPORT_VALUE", "count", "NORMAL"),
                new FactorVo("K5", "1.0", "RULE_DEFAULT", "rate", "NORMAL")))).replace("\"formatVersion\":2", "\"formatVersion\":1"));
        s.setSnapshotHash(SnapshotHashTestFixture.legacyHash(s));
        f.tx.executeWithoutResult(status -> f.mapper(BizWorkloadCalcSnapshotMapper.class).insertBizWorkloadCalcSnapshot(s));
        BizWorkloadCalcSnapshot persisted = f.mapper(BizWorkloadCalcSnapshotMapper.class).selectLatestByItemId(9L);
        assertThat(persisted.getFactorJson()).isNotEqualTo(s.getFactorJson());
        assertThat(SnapshotIntegrity.verify(persisted).values().keySet()).containsExactly("R5", "K5");
        assertThat(persisted.getSnapshotHash()).isEqualTo(s.getSnapshotHash());
    }
    @ParameterizedTest @ValueSource(strings={"result", "formula_expression", "source_json", "context_json", "unknown", "broken"})
    void historicalReadRejectsPersistedTampering(String field)
    {
        BizWorkloadCalcSnapshot s = capture();
        switch(field) {
            case "result" -> f.jdbc.update("UPDATE biz_workload_calc_snapshot SET result=3 WHERE id=?", s.getId());
            case "formula_expression" -> f.jdbc.update("UPDATE biz_workload_calc_snapshot SET formula_expression='forged' WHERE id=?", s.getId());
            case "source_json" -> f.jdbc.update("UPDATE biz_workload_calc_snapshot SET source_json=JSON_OBJECT('A','FORGED','Z','IMPORT_VALUE') WHERE id=?", s.getId());
            case "context_json" -> f.jdbc.update("UPDATE biz_workload_calc_snapshot SET context_json=REPLACE(context_json,'captured evidence','forged') WHERE id=?", s.getId());
            case "unknown" -> f.jdbc.update("UPDATE biz_workload_calc_snapshot SET context_json=REPLACE(context_json,'\"formatVersion\":2','\"formatVersion\":99') WHERE id=?", s.getId());
            case "broken" -> f.jdbc.update("UPDATE biz_workload_calc_snapshot SET context_json='{' WHERE id=?", s.getId());
        }
        BizWorkloadItem item = item(); item.setCalculationVersion(s.getCalculationVersion());
        assertThatThrownBy(() -> f.formulas.build(item)).isInstanceOf(ServiceException.class).hasMessageContaining("完整性");
    }
}
