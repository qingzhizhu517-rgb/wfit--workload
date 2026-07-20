package com.workload.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * G4课程设计明细对象 biz_wl_course_design
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizWlCourseDesign extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long itemId;

    /** 课程设计学分 */
    @Excel(name = "课程设计学分")
    private BigDecimal J4;

    /** 指导人数(&lt;=20) */
    @Excel(name = "指导人数(&lt;=20)")
    private Long R4;

    public void setItemId(Long itemId) 
    {
        this.itemId = itemId;
    }

    public Long getItemId() 
    {
        return itemId;
    }

    public void setJ4(BigDecimal J4) 
    {
        this.J4 = J4;
    }

    public BigDecimal getJ4() 
    {
        return J4;
    }

    public void setR4(Long R4) 
    {
        this.R4 = R4;
    }

    public Long getR4() 
    {
        return R4;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("J4", getJ4())
            .append("R4", getR4())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
