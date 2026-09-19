package com.workload.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * G1/G2 系数调整申请对象 biz_coefficient_adjustment
 *
 * <p>教师/教务对某条工作量明细（{@link BizWorkloadItem}）的单个系数发起复核申请。
 * 状态机：0 待审(PENDING) → 1 通过(APPROVED) / 2 驳回(REJECTED)；3 撤销(CANCELLED)。
 * 通过后由 Task 9 负责原子应用系数并重算，本模型只负责申请与审批状态流转。</p>
 *
 * @author wflg
 * @date 2026-09-12
 */
public class BizCoefficientAdjustment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 待审 */
    public static final int STATUS_PENDING = 0;
    /** 已通过 */
    public static final int STATUS_APPROVED = 1;
    /** 已驳回 */
    public static final int STATUS_REJECTED = 2;
    /** 已撤销 */
    public static final int STATUS_CANCELLED = 3;

    /** 主键 */
    private Long id;

    /** 申请所属教师（服务端由明细带出，不接受客户端传入） */
    @Excel(name = "教师ID")
    private Long userId;

    /** 学年学期（服务端由明细带出） */
    @Excel(name = "学年学期")
    private String semester;

    /** 关联教学任务 FK biz_teaching_task（可空） */
    private Long taskId;

    /** 关联工作量明细 FK biz_workload_item */
    @Excel(name = "工作量明细ID")
    private Long itemId;

    /** 工作量类别：仅 G1/G2 */
    @Excel(name = "类别")
    private String category;

    /** 系数编码：G1 的 C1/K1/Q1/Q2/N，G2 的 K/C2/Q1/Q2 */
    @Excel(name = "系数编码")
    private String factorCode;

    /** 申请前的系数值（服务端从当前值读取，客户端传入被忽略） */
    @Excel(name = "原系数值")
    private BigDecimal oldValue;

    /** 申请调整为的系数值 */
    @Excel(name = "申请系数值")
    private BigDecimal requestedValue;

    /** 申请理由 */
    @Excel(name = "申请理由")
    private String reason;

    /** 佐证材料地址 */
    private String attachmentUrl;

    /** 状态：0待审/1通过/2驳回/3撤销 */
    @Excel(name = "状态", readConverterExp = "0=待审,1=通过,2=驳回,3=撤销")
    private Integer status;

    /** 审核人 */
    private Long reviewerId;

    /** 审核意见 */
    @Excel(name = "审核意见")
    private String reviewReason;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewedAt;

    /** 提交申请时明细的计算版本，用于乐观并发（与 biz_workload_item.calculation_version 对齐） */
    private Long baseCalculationVersion;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setSemester(String semester)
    {
        this.semester = semester;
    }

    public String getSemester()
    {
        return semester;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public Long getItemId()
    {
        return itemId;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getCategory()
    {
        return category;
    }

    public void setFactorCode(String factorCode)
    {
        this.factorCode = factorCode;
    }

    public String getFactorCode()
    {
        return factorCode;
    }

    public void setOldValue(BigDecimal oldValue)
    {
        this.oldValue = oldValue;
    }

    public BigDecimal getOldValue()
    {
        return oldValue;
    }

    public void setRequestedValue(BigDecimal requestedValue)
    {
        this.requestedValue = requestedValue;
    }

    public BigDecimal getRequestedValue()
    {
        return requestedValue;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getReason()
    {
        return reason;
    }

    public void setAttachmentUrl(String attachmentUrl)
    {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentUrl()
    {
        return attachmentUrl;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setReviewerId(Long reviewerId)
    {
        this.reviewerId = reviewerId;
    }

    public Long getReviewerId()
    {
        return reviewerId;
    }

    public void setReviewReason(String reviewReason)
    {
        this.reviewReason = reviewReason;
    }

    public String getReviewReason()
    {
        return reviewReason;
    }

    public void setReviewedAt(Date reviewedAt)
    {
        this.reviewedAt = reviewedAt;
    }

    public Date getReviewedAt()
    {
        return reviewedAt;
    }

    public void setBaseCalculationVersion(Long baseCalculationVersion)
    {
        this.baseCalculationVersion = baseCalculationVersion;
    }

    public Long getBaseCalculationVersion()
    {
        return baseCalculationVersion;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("userId", getUserId())
                .append("semester", getSemester())
                .append("taskId", getTaskId())
                .append("itemId", getItemId())
                .append("category", getCategory())
                .append("factorCode", getFactorCode())
                .append("oldValue", getOldValue())
                .append("requestedValue", getRequestedValue())
                .append("reason", getReason())
                .append("attachmentUrl", getAttachmentUrl())
                .append("status", getStatus())
                .append("reviewerId", getReviewerId())
                .append("reviewReason", getReviewReason())
                .append("reviewedAt", getReviewedAt())
                .append("baseCalculationVersion", getBaseCalculationVersion())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
