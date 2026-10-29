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
    /** Real resolver + G4 strategy/callback/formula + transactional recalc, not a synthetic source fixture. */
    private BizWorkloadCalcSnapshot captureActualG4Policy() throws Exception {
        String ruleDdl=f.admin.queryForObject("SHOW CREATE TABLE biz_workload_rule", (rs,n) -> rs.getString(2));
        f.jdbc.execute(ruleDdl);
        String resource="mapper/system/BizWorkloadRuleMapper.xml";
        try(var in=getClass().getClassLoader().getResourceAsStream(resource)) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(in,f.sessions.getConfiguration(),resource,f.sessions.getConfiguration().getSqlFragments()).parse();
        }
        f.jdbc.update("INSERT INTO biz_workload_rule(rule_code,rule_value,rule_desc,effective_from,status) VALUES('CONST_COURSE_DESIGN',0.4,'原始常量政策','2025-01-01',1),('CAP_R4_MAX',60,'原始告警政策','2025-01-01',1)");
        var resolver=new com.workload.system.calc.rule.RuleParamServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(resolver,"bizWorkloadRuleMapper",f.mapper(BizWorkloadRuleMapper.class));
        f.beans.put(com.workload.system.calc.rule.RuleParamService.class,resolver);
        var strategy=f.wire(new com.workload.system.calc.strategy.CourseDesignCalcStrategy());
        var factory=(com.workload.system.calc.strategy.CalcStrategyFactory)f.beans.get(com.workload.system.calc.strategy.CalcStrategyFactory.class);
        org.springframework.test.util.ReflectionTestUtils.setField(factory,"strategyMap",java.util.Map.of("courseDesignCalcStrategy",strategy));
        Object target=((org.springframework.aop.framework.Advised)f.calc).getTargetSource().getTarget();
        org.springframework.test.util.ReflectionTestUtils.setField(target,"ruleParamService",resolver);
        f.jdbc.update("INSERT INTO biz_workload_category_dict(type_code,type_name,parent_group,calc_strategy) VALUES('G4','课程设计','G7','courseDesignCalcStrategy')");
        f.jdbc.update("UPDATE biz_workload_item SET item_type='G4' WHERE id=9");
        f.jdbc.update("INSERT INTO biz_wl_course_design(item_id,J4,R4) VALUES(9,2,70)");
        assertThat(f.calc.recalcItem(9L)).isEqualByComparingTo("56");
        return f.mapper(BizWorkloadCalcSnapshotMapper.class).selectLatestByItemId(9L);
    }

    @ParameterizedTest @ValueSource(strings={"rename","modify","delete"})
    void v3PolicyRemainsIndependentAfterRealSourceMutation(String mutation) throws Exception {
        BizWorkloadCalcSnapshot before=captureActualG4Policy();
        assertThat(before.getRuleVersion()).matches("[0-9a-f]{64}");
        assertThat(before.getContextJson()).contains("\"formatVersion\":3");
        switch(mutation) {
            case "rename" -> f.jdbc.update("UPDATE biz_workload_rule SET rule_code=CONCAT('RENAMED_',rule_code),rule_desc='新说明'");
            case "modify" -> f.jdbc.update("UPDATE biz_workload_rule SET rule_value=999,effective_from='2099-01-01'");
            case "delete" -> f.jdbc.update("DELETE FROM biz_workload_rule");
        }
        FactorFormulaVo restored=f.formulas.build(item());
        assertThat(restored.getEvidenceStatus()).isEqualTo("COMPLETE");
        assertThat(restored.isReproducible()).isTrue();
        assertThat(restored.getPolicySources()).hasSize(2).allMatch(r -> r.sourceKind().equals("DB_RULE"));
        assertThat(restored.getPolicySources()).extracting(r -> r.content().description()).containsExactly("原始告警政策","原始常量政策");
        assertThat(restored.getDecisionEvidence()).hasSize(1).allMatch(d -> d.result());
        assertThat(restored.getSnapshotHash()).isEqualTo(before.getSnapshotHash());
    }

    @ParameterizedTest @ValueSource(strings={"content","reference","value"})
    void v3InnerEvidenceRejectsRealSqlTamperingEvenWithNewOuterHash(String mutation) throws Exception {
        BizWorkloadCalcSnapshot saved=captureActualG4Policy();
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var context=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(saved.getContextJson());
        if (mutation.equals("content")) ((com.fasterxml.jackson.databind.node.ObjectNode)context.path("policySources").get(1).path("content")).put("description","伪造政策");
        else if (mutation.equals("reference")) ((com.fasterxml.jackson.databind.node.ObjectNode)context.path("factorEvidence").get(2)).put("ruleRef","a".repeat(64));
        else saved.setFactorJson(saved.getFactorJson().replace("0.4000","0.9").replace("0.4","0.9"));
        saved.setContextJson(context.toString());saved.setSnapshotHash(SnapshotIntegrity.hashV3(saved));
        f.jdbc.update("UPDATE biz_workload_calc_snapshot SET context_json=?,factor_json=?,snapshot_hash=? WHERE id=?",saved.getContextJson(),saved.getFactorJson(),saved.getSnapshotHash(),saved.getId());
        assertThatThrownBy(() -> f.formulas.build(item())).isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException)e).getCode()).isEqualTo(50017));
    }

}
