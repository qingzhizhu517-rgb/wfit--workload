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
    }
}
