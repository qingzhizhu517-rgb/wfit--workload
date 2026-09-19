package com.workload.system.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工作量计算公式及因子说明。
 */
public class FactorFormulaVo
{
    private String formulaType;
    private String expression;
    private BigDecimal result;
    private String description;
    private boolean reproducible;
    /** 旧数据无快照、只能读回子表时为 true：此时展示可信但无法逐因子复现历史核算 */
    private boolean legacy;
    private List<FactorVo> factors;

    public FactorFormulaVo(String formulaType, String expression, BigDecimal result,
            String description, List<FactorVo> factors)
    {
        this(formulaType, expression, result, description, true, factors);
    }

    public FactorFormulaVo(String formulaType, String expression, BigDecimal result,
            String description, boolean reproducible, List<FactorVo> factors)
    {
        this.formulaType = formulaType;
        this.expression = expression;
        this.result = result;
        this.description = description;
        this.reproducible = reproducible;
        this.legacy = false;
        this.factors = factors;
    }

    public String getFormulaType()
    {
        return formulaType;
    }

    public String getExpression()
    {
        return expression;
    }

    public BigDecimal getResult()
    {
        return result;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isReproducible()
    {
        return reproducible;
    }

    public boolean isLegacy()
    {
        return legacy;
    }

    public void setLegacy(boolean legacy)
    {
        this.legacy = legacy;
    }

    public List<FactorVo> getFactors()
    {
        return factors;
    }

    /** 单个公式因子。 */
    public static class FactorVo
    {
        private String key;
        private Object value;
        private String source;
        private String description;
        private String status;
        /** 因子取值所依据的规则键（如 COEF_REPEAT_1ST），无则为空 */
        private String ruleCode;
        /** 取值时的规则版本，历史快照据此复现 */
        private String ruleVersion;
        /** 来源引用：导入值/审批单/岗位来源批次等的可追溯标识 */
        private String sourceRef;

        public FactorVo(String key, Object value, String source, String description, String status)
        {
            this.key = key;
            this.value = value;
            this.source = source;
            this.description = description;
            this.status = status;
        }

        public String getKey()
        {
            return key;
        }

        public Object getValue()
        {
            return value;
        }

        public String getSource()
        {
            return source;
        }

        public String getDescription()
        {
            return description;
        }

        public String getStatus()
        {
            return status;
        }

        public String getRuleCode()
        {
            return ruleCode;
        }

        public void setRuleCode(String ruleCode)
        {
            this.ruleCode = ruleCode;
        }

        public String getRuleVersion()
        {
            return ruleVersion;
        }

        public void setRuleVersion(String ruleVersion)
        {
            this.ruleVersion = ruleVersion;
        }

        public String getSourceRef()
        {
            return sourceRef;
        }

        public void setSourceRef(String sourceRef)
        {
            this.sourceRef = sourceRef;
        }
    }
}
