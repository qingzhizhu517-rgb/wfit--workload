package com.workload.system.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 审批日志对象 biz_audit_log
 * <p>
 * 记录工作量汇总审批状态变更痕迹：谁在何时把哪条记录从什么状态改成什么状态、为什么驳回。
 * 与状态变更在同一事务内写入。
 *
 * @author wflg
 */
public class BizAuditLog implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 关联学期工作量汇总ID（biz_workload_summary.id） */
    private Long summaryId;

    /** 审批动作：submit/approve/reject/sign/unlock/teacherConfirm */
    private String action;

    /** 变更前状态（与 biz_workload_summary.status 同口径） */
    private Integer fromStatus;

    /** 变更后状态（与 biz_workload_summary.status 同口径） */
    private Integer toStatus;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人账户 */
    private String operatorName;

    /** 原因（驳回时填写） */
    private String reason;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setSummaryId(Long summaryId)
    {
        this.summaryId = summaryId;
    }

    public Long getSummaryId()
    {
        return summaryId;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getAction()
    {
        return action;
    }

    public void setFromStatus(Integer fromStatus)
    {
        this.fromStatus = fromStatus;
    }

    public Integer getFromStatus()
    {
        return fromStatus;
    }

    public void setToStatus(Integer toStatus)
    {
        this.toStatus = toStatus;
    }

    public Integer getToStatus()
    {
        return toStatus;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getReason()
    {
        return reason;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("summaryId", getSummaryId())
            .append("action", getAction())
            .append("fromStatus", getFromStatus())
            .append("toStatus", getToStatus())
            .append("operatorId", getOperatorId())
            .append("operatorName", getOperatorName())
            .append("reason", getReason())
            .append("createTime", getCreateTime())
            .toString();
    }
}
