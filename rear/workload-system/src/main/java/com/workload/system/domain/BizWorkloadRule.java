package com.workload.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * 全局核算规则参数对象 biz_workload_rule
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizWorkloadRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 参数键名 */
    @Excel(name = "参数键名")
    private String ruleCode;

    /** 参数数值 */
    @Excel(name = "参数数值")
    private BigDecimal ruleValue;

    /** 参数说明 */
    @Excel(name = "参数说明")
    private String ruleDesc;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Date effectiveFrom;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Date effectiveTo;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Integer status;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setRuleCode(String ruleCode) 
    {
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() 
    {
        return ruleCode;
    }

    public void setRuleValue(BigDecimal ruleValue) 
    {
        this.ruleValue = ruleValue;
    }

    public BigDecimal getRuleValue() 
    {
        return ruleValue;
    }

    public void setRuleDesc(String ruleDesc) 
    {
        this.ruleDesc = ruleDesc;
    }

    public String getRuleDesc() 
    {
        return ruleDesc;
    }

    public void setEffectiveFrom(Date effectiveFrom) 
    {
        this.effectiveFrom = effectiveFrom;
    }

    public Date getEffectiveFrom() 
    {
        return effectiveFrom;
    }

    public void setEffectiveTo(Date effectiveTo) 
    {
        this.effectiveTo = effectiveTo;
    }

    public Date getEffectiveTo() 
    {
        return effectiveTo;
    }

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("ruleCode", getRuleCode())
            .append("ruleValue", getRuleValue())
            .append("ruleDesc", getRuleDesc())
            .append("effectiveFrom", getEffectiveFrom())
            .append("effectiveTo", getEffectiveTo())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
