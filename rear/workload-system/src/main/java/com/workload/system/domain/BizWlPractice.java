package com.workload.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * G2课内实践明细对象 biz_wl_practice
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizWlPractice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long itemId;

    /** 实践学时 */
    @Excel(name = "实践学时")
    private BigDecimal J2;

    /** 理工1.0/其他0.9 */
    @Excel(name = "理工1.0/其他0.9")
    private BigDecimal K;

    /** 实践重复第一次1.0/第二次起0.9 */
    @Excel(name = "实践重复第一次1.0/第二次起0.9")
    private BigDecimal C2;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private BigDecimal Q1;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private BigDecimal Q2;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private BigDecimal Q3;

    public void setItemId(Long itemId) 
    {
        this.itemId = itemId;
    }

    public Long getItemId() 
    {
        return itemId;
    }

    public void setJ2(BigDecimal J2) 
    {
        this.J2 = J2;
    }

    public BigDecimal getJ2() 
    {
        return J2;
    }

    public void setK(BigDecimal K) 
    {
        this.K = K;
    }

    public BigDecimal getK() 
    {
        return K;
    }

    public void setC2(BigDecimal C2) 
    {
        this.C2 = C2;
    }

    public BigDecimal getC2() 
    {
        return C2;
    }

    public void setQ1(BigDecimal Q1) 
    {
        this.Q1 = Q1;
    }

    public BigDecimal getQ1() 
    {
        return Q1;
    }

    public void setQ2(BigDecimal Q2) 
    {
        this.Q2 = Q2;
    }

    public BigDecimal getQ2() 
    {
        return Q2;
    }

    public void setQ3(BigDecimal Q3) 
    {
        this.Q3 = Q3;
    }

    public BigDecimal getQ3() 
    {
        return Q3;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("J2", getJ2())
            .append("K", getK())
            .append("C2", getC2())
            .append("Q1", getQ1())
            .append("Q2", getQ2())
            .append("Q3", getQ3())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
