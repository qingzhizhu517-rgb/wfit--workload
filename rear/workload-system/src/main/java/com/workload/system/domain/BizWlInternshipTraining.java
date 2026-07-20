package com.workload.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * G3教学实习实训明细对象 biz_wl_internship_training
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizWlInternshipTraining extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long itemId;

    /** 实际天数(1天=8学时) */
    @Excel(name = "实际天数(1天=8学时)")
    private BigDecimal T;

    /** 指导系数理工4/艺术3/文史2/单位2 */
    @Excel(name = "指导系数理工4/艺术3/文史2/单位2")
    private BigDecimal D;

    /** 重复系数第一轮1/第二轮0.9 */
    @Excel(name = "重复系数第一轮1/第二轮0.9")
    private BigDecimal K;

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

    public void setT(BigDecimal T) 
    {
        this.T = T;
    }

    public BigDecimal getT() 
    {
        return T;
    }

    public void setD(BigDecimal D) 
    {
        this.D = D;
    }

    public BigDecimal getD() 
    {
        return D;
    }

    public void setK(BigDecimal K) 
    {
        this.K = K;
    }

    public BigDecimal getK() 
    {
        return K;
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
            .append("T", getT())
            .append("D", getD())
            .append("K", getK())
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
