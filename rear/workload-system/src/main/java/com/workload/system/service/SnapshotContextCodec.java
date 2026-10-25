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
            boolean reproducible, SourceTaskVo sourceTask, List<FactorContext> factors) {}

    public static String encode(FactorFormulaVo formula)
    {
        List<FactorContext> factors = formula.getFactors() == null ? List.of()
                : formula.getFactors().stream().filter(f -> f != null && f.getKey() != null)
                    .map(f -> new FactorContext(f.getKey(), f.getDescription(), f.getStatus(),
                            f.getRuleCode(), f.getRuleVersion(), f.getSourceRef())).toList();
        try
        {
            return JSON.writeValueAsString(new Context(1, formula.getFormulaType(), formula.getDescription(),
                    formula.isReproducible(), formula.getSourceTask(), factors));
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("序列化快照上下文失败：" + e.getMessage());
        }
    }

    public static FactorFormulaVo restore(String json, FactorFormulaVo persisted)
    {
        try
        {
            Context context = JSON.readValue(json, Context.class);
            if (context == null || context.formatVersion() != 1 || context.formulaType() == null
                    || context.factors() == null)
            {
                throw new IllegalArgumentException("不支持或不完整的上下文格式");
            }
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
            return restored;
        }
        catch (Exception e)
        {
            throw new ServiceException("读取快照上下文失败：" + e.getMessage());
        }
    }
}
