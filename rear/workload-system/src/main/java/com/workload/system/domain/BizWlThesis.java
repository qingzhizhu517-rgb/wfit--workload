package com.workload.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * G5毕业论文明细对象 biz_wl_thesis
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizWlThesis extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long itemId;

    /** 指导人数(本&lt;=10,专&lt;=15) */
    @Excel(name = "指导人数(本&lt;=10,专&lt;=15)")
    private Long R5;

    /** 系数理工本9/专5,文史本6/专4 */
    @Excel(name = "系数理工本9/专5,文史本6/专4")
    private BigDecimal K5;

    /** 本科/专科 */
    @Excel(name = "本科/专科")
    private String educationLevel;

    /** 理工类/文史类 */
    @Excel(name = "理工类/文史类")
    private String major;

    public void setItemId(Long itemId) 
    {
        this.itemId = itemId;
    }

    public Long getItemId() 
    {
        return itemId;
    }

    public void setR5(Long R5) 
    {
        this.R5 = R5;
    }

    public Long getR5() 
    {
        return R5;
    }

    public void setK5(BigDecimal K5) 
    {
        this.K5 = K5;
    }

    public BigDecimal getK5() 
    {
        return K5;
    }

    public void setEducationLevel(String educationLevel) 
    {
        this.educationLevel = educationLevel;
    }

    public String getEducationLevel() 
    {
        return educationLevel;
    }

    public void setMajor(String major) 
    {
        this.major = major;
    }

    public String getMajor() 
    {
        return major;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("R5", getR5())
            .append("K5", getK5())
            .append("educationLevel", getEducationLevel())
            .append("major", getMajor())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
