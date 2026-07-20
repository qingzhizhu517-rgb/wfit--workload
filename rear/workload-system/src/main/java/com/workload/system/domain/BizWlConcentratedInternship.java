package com.workload.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * G6集中实习明细对象 biz_wl_concentrated_internship
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizWlConcentratedInternship extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long itemId;

    /** 实习周数 */
    @Excel(name = "实习周数")
    private BigDecimal W;

    /** 指导人数(&lt;=20) */
    @Excel(name = "指导人数(&lt;=20)")
    private Long R6;

    public void setItemId(Long itemId) 
    {
        this.itemId = itemId;
    }

    public Long getItemId() 
    {
        return itemId;
    }

    public void setW(BigDecimal W) 
    {
        this.W = W;
    }

    public BigDecimal getW() 
    {
        return W;
    }

    public void setR6(Long R6) 
    {
        this.R6 = R6;
    }

    public Long getR6() 
    {
        return R6;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("W", getW())
            .append("R6", getR6())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
