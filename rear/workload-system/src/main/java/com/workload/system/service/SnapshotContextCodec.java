package com.workload.system.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.domain.vo.FactorFormulaVo.FactorEvidence;
import com.workload.system.domain.vo.FactorFormulaVo.DecisionEvidence;
import com.workload.system.calc.rule.RuleResolution;
import com.workload.system.domain.vo.FactorFormulaVo.FactorVo;
import com.workload.system.domain.vo.FactorFormulaVo.SourceTaskVo;

/** 历史解释上下文的版本化格式；不从当前规则或源表补历史证据。 */
public final class SnapshotContextCodec
{
    private static final ObjectMapper JSON = new ObjectMapper();

    private SnapshotContextCodec() {}

    public record FactorContext(String key, String description, String status,
            String ruleCode, String ruleVersion, String sourceRef) {}

    public record Context(int formatVersion, String formulaType, String description,
            boolean reproducible, SourceTaskVo sourceTask, List<FactorContext> factors,
            List<RuleResolution> policySources, List<FactorEvidence> factorEvidence,
            List<DecisionEvidence> decisionEvidence, String evidenceStatus, List<String> missingEvidenceReasons, String asOfDate, List<com.workload.system.calc.rule.FactorProvenance.Proof> persistedFactorSources) {}

    public static String encode(FactorFormulaVo formula)
    {
        if (formula.getFactors() != null && formula.getFactors().stream()
                .anyMatch(f -> f == null || f.getKey() == null || f.getKey().isBlank()))
            throw SnapshotIntegrity.failure("缺失因子或因子标识，无法固化上下文");
        List<FactorContext> factors = formula.getFactors() == null ? List.of()
                : formula.getFactors().stream()
                    .map(f -> new FactorContext(f.getKey(), f.getDescription(), f.getStatus(),
                            f.getRuleCode(), f.getRuleVersion(), f.getSourceRef())).toList();
        try
        {
            if (formula.getPolicySources() != null)
            {
                Context context=new Context(formula.getPersistedFactorSources()==null ? 3 : 4, formula.getFormulaType(), formula.getDescription(),
                        formula.isReproducible(), formula.getSourceTask(), factors, formula.getPolicySources(),
                        formula.getFactorEvidence(), formula.getDecisionEvidence(), formula.getEvidenceStatus(), formula.getMissingEvidenceReasons(), formula.getAsOfDate(),formula.getPersistedFactorSources());
                com.fasterxml.jackson.databind.node.ObjectNode tree=JSON.valueToTree(context);
                if(context.formatVersion()==3) tree.remove("persistedFactorSources");
                return JSON.writeValueAsString(tree);
            }
            // Keep the original v2 fields and insertion order byte-for-byte, including historical v1 fixtures.
            Map<String,Object> legacy = new java.util.LinkedHashMap<>();
            legacy.put("formatVersion",2); legacy.put("formulaType",formula.getFormulaType());
            legacy.put("description",formula.getDescription()); legacy.put("reproducible",formula.isReproducible());
            legacy.put("sourceTask",formula.getSourceTask()); legacy.put("factors",factors);
            return JSON.writeValueAsString(legacy);
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("序列化快照上下文失败：" + e.getMessage());
        }
    }

    public static Context decode(String json)
    {
        try
        {
            com.fasterxml.jackson.databind.JsonNode node = SnapshotIntegrity.json().readTree(json);
            com.fasterxml.jackson.databind.JsonNode version = node == null ? null : node.get("formatVersion");
            if (version == null || !version.isIntegralNumber() || !version.canConvertToInt())
                throw new IllegalArgumentException("格式版本必须是显式整数");
            if (version.intValue() < 3 && java.util.List.of("policySources", "factorEvidence", "decisionEvidence", "evidenceStatus", "missingEvidenceReasons", "asOfDate")
                    .stream().anyMatch(node::has))
                throw new IllegalArgumentException("历史格式不允许政策来源扩展字段");
            if(version.intValue()<4 && node.has("persistedFactorSources")) throw new IllegalArgumentException("旧格式不允许持久化来源树");
            Context context = SnapshotIntegrity.json().treeToValue(node, Context.class);
            if (context == null || (context.formatVersion() != 1 && context.formatVersion() != 2 && context.formatVersion() != 3 && context.formatVersion() != 4)
                    || context.formulaType() == null || context.formulaType().isBlank() || context.factors() == null)
                throw new IllegalArgumentException("不支持或不完整的上下文格式");
            java.util.Set<String> keys = new java.util.HashSet<>();
            for (FactorContext factor : context.factors())
                if (factor == null || factor.key() == null || factor.key().isBlank() || !keys.add(factor.key()))
                    throw new IllegalArgumentException("重复或缺失上下文因子");
            return context;
        }
        catch (Exception e)
        {
            throw SnapshotIntegrity.failure("读取快照上下文失败：" + e.getMessage());
        }
    }

    public static FactorFormulaVo restore(String json, FactorFormulaVo persisted)
    {
        try
        {
            Context context = decode(json);
            Map<String, FactorVo> values = persisted.getFactors().stream()
                    .collect(Collectors.toMap(FactorVo::getKey, Function.identity()));
            List<FactorVo> factors = new ArrayList<>();
            for (FactorContext meta : context.factors())
            {
                FactorVo value = meta == null ? null : values.remove(meta.key());
                if (value == null) throw new IllegalArgumentException("上下文因子与数值记录不一致");
                FactorVo factor = new FactorVo(meta.key(), value.getValue(), value.getSource(),
                        meta.description(), meta.status());
                factor.setRuleCode(meta.ruleCode());
                factor.setRuleVersion(meta.ruleVersion());
                factor.setSourceRef(meta.sourceRef());
                factors.add(factor);
            }
            if (!values.isEmpty()) throw new IllegalArgumentException("上下文遗漏因子");
            FactorFormulaVo restored = new FactorFormulaVo(context.formulaType(), persisted.getExpression(),
                    persisted.getResult(), context.description(),
                    context.reproducible() && persisted.isReproducible(), factors);
            restored.setSourceTask(context.sourceTask());
            if (context.formatVersion() >= 3) {
                if(context.formatVersion()==4) restored.setPersistedFactorSources(context.persistedFactorSources());
                restored.setAsOfDate(context.asOfDate());
                restored.setPolicySources(context.policySources()); restored.setFactorEvidence(context.factorEvidence());
                restored.setDecisionEvidence(context.decisionEvidence()); restored.setEvidenceStatus(context.evidenceStatus());
                restored.setMissingEvidenceReasons(context.missingEvidenceReasons());
            } // v1/v2 retain explicit PARTIAL / MISSING_HISTORICAL_SOURCE defaults; never consult current rules.
            return restored;
        }
        catch (Exception e)
        {
            throw SnapshotIntegrity.failure("读取快照上下文失败：" + e.getMessage());
        }
    }
}
