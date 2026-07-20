package com.workload.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * 职称单位酬金费率对象 biz_pay_rate
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizPayRate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 职称 */
    @Excel(name = "职称")
    private String title;

    /** 单位工作量酬金(元) */
    @Excel(name = "单位工作量酬金(元)")
    private BigDecimal rate;

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

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setRate(BigDecimal rate) 
    {
        this.rate = rate;
    }

    public BigDecimal getRate() 
    {
        return rate;
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
            .append("title", getTitle())
            .append("rate", getRate())
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
