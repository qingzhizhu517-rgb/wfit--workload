package com.workload.system.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workload.system.domain.*;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.domain.vo.FactorFormulaVo.FactorVo;
import com.workload.system.mapper.BizWorkloadCalcSnapshotMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SnapshotIntegrityTest
{
    private final ObjectMapper json = new ObjectMapper();
    private BizWorkloadCalcSnapshot fresh()
    {
        BizWorkloadItem item = new BizWorkloadItem(); item.setId(7L);
        FactorVo factor = new FactorVo("X", new BigDecimal("2.00"), "IMPORT_VALUE", "历史因子", "NORMAL");
        factor.setSourceRef("approval:17");
        return new WorkloadSnapshotService(mock(BizWorkloadCalcSnapshotMapper.class)).capture(item,
                new FactorFormulaVo("G1", "X", new BigDecimal("2.0"), "history", List.of(factor)), "v1");
    }
    @Test void newSnapshotSurvivesJsonStorageNormalizationAndDecimalScale() throws Exception
    {
        BizWorkloadCalcSnapshot s = fresh();
        s.setFactorJson("{ \"X\" : \"2.00\" }"); s.setSourceJson("{ \"X\" : \"IMPORT_VALUE\" }");
        s.setContextJson(json.writerWithDefaultPrettyPrinter().writeValueAsString(json.readTree(s.getContextJson())));
        s.setResult(new BigDecimal("2.00"));
        assertThat(SnapshotIntegrity.verify(s).values()).containsEntry("X", "2.00");
    }
    @ParameterizedTest @ValueSource(strings={"context", "source", "result", "formula", "value", "rule", "item", "version", "missing", "duplicate", "unknown", "broken", "emptyContext"})
    void mutatedSnapshotFails(String mutation)
    {
        BizWorkloadCalcSnapshot s = fresh();
        switch(mutation) {
            case "context" -> s.setContextJson(s.getContextJson().replace("approval:17", "approval:18"));
            case "source" -> s.setSourceJson("{\"X\":\"FORGED\"}");
            case "result" -> s.setResult(new BigDecimal("2.01"));
            case "formula" -> s.setFormulaExpression("X × 2");
            case "value" -> s.setFactorJson("{\"X\":\"3\"}");
            case "rule" -> s.setRuleVersion("v2");
            case "item" -> s.setItemId(8L);
            case "version" -> s.setCalculationVersion(99L);
            case "missing" -> s.setFactorJson("{}");
            case "duplicate" -> s.setFactorJson("{\"X\":\"2.00\",\"X\":\"2.00\"}");
            case "unknown" -> s.setContextJson(s.getContextJson().replace("\"formatVersion\":2", "\"formatVersion\":99"));
            case "broken" -> s.setContextJson("{");
            case "emptyContext" -> s.setContextJson("");
        }
        assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).isInstanceOf(com.workload.common.exception.ServiceException.class);
    }
    @ParameterizedTest @ValueSource(strings={"J1,C1,K1,Q1,Q2,N,Q3", "J1,C1,K1,Q1,Q2,N", "J2,K,C2,Q1,Q2,Q3", "T,D,K,Q1,Q2,Q3", "J4,R4,CONST", "R5,K5", "W,R6,R6_EFFECTIVE,CONST", "岗位减免"})
    void realLegacyFivePartHashSurvivesMysqlOrderAndScale(String keys) throws Exception
    {
        BizWorkloadCalcSnapshot s = legacy(keys, false);
        String hash = s.getSnapshotHash(); mysqlNormalize(s);
        assertThat(SnapshotIntegrity.verify(s).values().keySet()).containsExactly(keys.split(","));
        assertThat(s.getSnapshotHash()).isEqualTo(hash);
    }
    @ParameterizedTest @ValueSource(strings={"2.1", "\"2\"", "null"})
    void formatVersionMustBeAnExplicitKnownInteger(String version)
    {
        BizWorkloadCalcSnapshot s = fresh();
        s.setContextJson(s.getContextJson().replace("\"formatVersion\":2", "\"formatVersion\":" + version));
        assertThatThrownBy(() -> SnapshotContextCodec.decode(s.getContextJson())).hasMessageContaining("快照上下文");
    }

    @Test void realLegacySixPartHashRestoresOrderFromContext() throws Exception
    {
        BizWorkloadCalcSnapshot s = legacy("Z,A", true); mysqlNormalize(s);
        assertThat(SnapshotIntegrity.verify(s).values().keySet()).containsExactly("Z", "A");
    }
    @Test void unknownLegacyOrderDoesNotGuessPermutations() throws Exception
    {
        BizWorkloadCalcSnapshot s = legacy("Z,A", false);
        assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).hasMessageContaining("历史证据");
    }
    @Test void missingOrUnknownContextCannotUseValidFivePartHash() throws Exception
    {
        BizWorkloadCalcSnapshot s = legacy("R5,K5", false); s.setContextJson("{\"formatVersion\":0}");
        assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).hasMessageContaining("快照上下文");
    }
    @Test void legacyResultRecoveryNeverGuessesRoundedOriginal() throws Exception
    {
        BizWorkloadCalcSnapshot s = legacy("R5,K5", false);
        s.setResult(new BigDecimal("2.001")); s.setSnapshotHash(SnapshotHashTestFixture.legacyHash(s)); s.setResult(new BigDecimal("2.00"));
        assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).hasMessageContaining("哈希不符");
    }
    private BizWorkloadCalcSnapshot legacy(String keys, boolean context) throws Exception
    {
        BizWorkloadCalcSnapshot s = new BizWorkloadCalcSnapshot();
        Map<String,String> values = new LinkedHashMap<>(), sources = new LinkedHashMap<>(); List<FactorVo> factors = new ArrayList<>();
        for (String key : keys.split(",")) { values.put(key,"1.0"); sources.put(key,"IMPORT_VALUE"); factors.add(new FactorVo(key,"1.0","IMPORT_VALUE",key,"NORMAL")); }
        s.setItemId(7L); s.setCalculationVersion(3L); s.setFormulaExpression(keys); s.setRuleVersion("old"); s.setResult(new BigDecimal("2"));
        s.setFactorJson(json.writeValueAsString(values)); s.setSourceJson(json.writeValueAsString(sources));
        if (context) s.setContextJson(SnapshotContextCodec.encode(new FactorFormulaVo("G1", keys, s.getResult(),"old",factors)).replace("\"formatVersion\":2", "\"formatVersion\":1"));
        s.setSnapshotHash(SnapshotHashTestFixture.legacyHash(s)); return s;
    }
    private void mysqlNormalize(BizWorkloadCalcSnapshot s) throws Exception
    {
        s.setFactorJson(json.writerWithDefaultPrettyPrinter().writeValueAsString(new TreeMap<>(json.readValue(s.getFactorJson(), Map.class))));
        s.setSourceJson(json.writerWithDefaultPrettyPrinter().writeValueAsString(new TreeMap<>(json.readValue(s.getSourceJson(), Map.class))));
        s.setResult(new BigDecimal("2.00"));
    }
}
