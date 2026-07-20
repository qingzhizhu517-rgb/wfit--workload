package com.workload.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * G1理论课明细对象 biz_wl_theory
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizWlTheory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** FK biz_workload_item.id */
    private Long itemId;

    /** 理论学时 */
    @Excel(name = "理论学时")
    private BigDecimal J1;

    /** 重复系数1.0/0.9/0.8 */
    @Excel(name = "重复系数1.0/0.9/0.8")
    private BigDecimal C1;

    /** 课程类型必修1.1/选修1.0 */
    @Excel(name = "课程类型必修1.1/选修1.0")
    private BigDecimal K1;

    /** 教学质量1.0/不合格0.8 */
    @Excel(name = "教学质量1.0/不合格0.8")
    private BigDecimal Q1;

    /** 课程质量 */
    @Excel(name = "课程质量")
    private BigDecimal Q2;

    /** 全外文系数 */
    @Excel(name = "全外文系数")
    private BigDecimal Q3;

    /** 合堂1.1/1.2 */
    @Excel(name = "合堂1.1/1.2")
    private BigDecimal N;

    public void setItemId(Long itemId) 
    {
        this.itemId = itemId;
    }

    public Long getItemId() 
    {
        return itemId;
    }

    public void setJ1(BigDecimal J1) 
    {
        this.J1 = J1;
    }

    public BigDecimal getJ1() 
    {
        return J1;
    }

    public void setC1(BigDecimal C1) 
    {
        this.C1 = C1;
    }

    public BigDecimal getC1() 
    {
        return C1;
    }

    public void setK1(BigDecimal K1) 
    {
        this.K1 = K1;
    }

    public BigDecimal getK1() 
    {
        return K1;
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

    public void setN(BigDecimal N) 
    {
        this.N = N;
    }

    public BigDecimal getN() 
    {
        return N;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("J1", getJ1())
            .append("C1", getC1())
            .append("K1", getK1())
            .append("Q1", getQ1())
            .append("Q2", getQ2())
            .append("Q3", getQ3())
            .append("N", getN())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
