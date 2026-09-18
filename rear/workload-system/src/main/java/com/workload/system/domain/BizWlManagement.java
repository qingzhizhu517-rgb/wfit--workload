package com.workload.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * G11管理服务明细对象 biz_wl_management
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizWlManagement extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long itemId;

    /** FK biz_role_assignment */
    @Excel(name = "FK biz_role_assignment")
    private Long assignmentId;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String roleType;

    /** 计入 G11 的本学期岗位减免工作量（沿用 prorated_amount 列） */
    @Excel(name = "岗位减免工作量（本学期）")
    private BigDecimal proratedAmount;

    /** 计入 G11 说明（沿用 proration_basis 列） */
    @Excel(name = "计入 G11")
    private String prorationBasis;

    /** 来源批次 */
    @Excel(name = "来源批次")
    private String sourceBatchId;

    public void setItemId(Long itemId) 
    {
        this.itemId = itemId;
    }

    public Long getItemId() 
    {
        return itemId;
    }

    public void setAssignmentId(Long assignmentId) 
    {
        this.assignmentId = assignmentId;
    }

    public Long getAssignmentId() 
    {
        return assignmentId;
    }

    public void setRoleType(String roleType) 
    {
        this.roleType = roleType;
    }

    public String getRoleType() 
    {
        return roleType;
    }

    public void setProratedAmount(BigDecimal proratedAmount) 
    {
        this.proratedAmount = proratedAmount;
    }

    public BigDecimal getProratedAmount() 
    {
        return proratedAmount;
    }

    public void setProrationBasis(String prorationBasis) 
    {
        this.prorationBasis = prorationBasis;
    }

    public String getProrationBasis()
    {
        return prorationBasis;
    }

    public void setSourceBatchId(String sourceBatchId)
    {
        this.sourceBatchId = sourceBatchId;
    }

    public String getSourceBatchId()
    {
        return sourceBatchId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("assignmentId", getAssignmentId())
            .append("roleType", getRoleType())
            .append("proratedAmount", getProratedAmount())
            .append("prorationBasis", getProrationBasis())
            .append("sourceBatchId", getSourceBatchId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
