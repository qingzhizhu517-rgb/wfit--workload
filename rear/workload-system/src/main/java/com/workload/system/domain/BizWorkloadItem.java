package com.workload.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * 工作量明细主表对象 biz_workload_item
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizWorkloadItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long userId;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String semester;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String academicYear;

    /** G1..G9,G11 */
    @Excel(name = "G1..G9,G11")
    private String itemType;

    /** IMPORT/MANUAL */
    @Excel(name = "IMPORT/MANUAL")
    private String sourceType;

    /** FK biz_teaching_task */
    @Excel(name = "FK biz_teaching_task")
    private Long taskId;

    /** FK biz_role_assignment(G11) */
    @Excel(name = "FK biz_role_assignment(G11)")
    private Long assignmentId;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String courseName;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String educationLevel;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String majorCategory;

    /** 最终核算工作量 */
    @Excel(name = "最终核算工作量")
    private BigDecimal calculatedWorkload;

    /** G8/G9说明 */
    @Excel(name = "G8/G9说明")
    private String description;

    /** 指导人数超标(G5/G6) */
    @Excel(name = "指导人数超标(G5/G6)")
    private Integer isOverLimit;

    /** 0未审批/1通过/2驳回 */
    @Excel(name = "0未审批/1通过/2驳回")
    private Integer deanApprovalStatus;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String deanApprovalBy;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Date deanApprovalTime;

    /** 0无/1申诉中/2已处理/3已驳回 */
    @Excel(name = "0无/1申诉中/2已处理/3已驳回")
    private Integer appealStatus;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String appealReason;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String appealReply;

    /** 0草稿/1已核对/2有异议/3已驳回 */
    @Excel(name = "0草稿/1已核对/2有异议/3已驳回")
    private Integer status;

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

    public void setAcademicYear(String academicYear) 
    {
        this.academicYear = academicYear;
    }

    public String getAcademicYear() 
    {
        return academicYear;
    }

    public void setItemType(String itemType) 
    {
        this.itemType = itemType;
    }

    public String getItemType() 
    {
        return itemType;
    }

    public void setSourceType(String sourceType) 
    {
        this.sourceType = sourceType;
    }

    public String getSourceType() 
    {
        return sourceType;
    }

    public void setTaskId(Long taskId) 
    {
        this.taskId = taskId;
    }

    public Long getTaskId() 
    {
        return taskId;
    }

    public void setAssignmentId(Long assignmentId) 
    {
        this.assignmentId = assignmentId;
    }

    public Long getAssignmentId() 
    {
        return assignmentId;
    }

    public void setCourseName(String courseName) 
    {
        this.courseName = courseName;
    }

    public String getCourseName() 
    {
        return courseName;
    }

    public void setEducationLevel(String educationLevel) 
    {
        this.educationLevel = educationLevel;
    }

    public String getEducationLevel() 
    {
        return educationLevel;
    }

    public void setMajorCategory(String majorCategory) 
    {
        this.majorCategory = majorCategory;
    }

    public String getMajorCategory() 
    {
        return majorCategory;
    }

    public void setCalculatedWorkload(BigDecimal calculatedWorkload) 
    {
        this.calculatedWorkload = calculatedWorkload;
    }

    public BigDecimal getCalculatedWorkload() 
    {
        return calculatedWorkload;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setIsOverLimit(Integer isOverLimit) 
    {
        this.isOverLimit = isOverLimit;
    }

    public Integer getIsOverLimit() 
    {
        return isOverLimit;
    }

    public void setDeanApprovalStatus(Integer deanApprovalStatus) 
    {
        this.deanApprovalStatus = deanApprovalStatus;
    }

    public Integer getDeanApprovalStatus() 
    {
        return deanApprovalStatus;
    }

    public void setDeanApprovalBy(String deanApprovalBy) 
    {
        this.deanApprovalBy = deanApprovalBy;
    }

    public String getDeanApprovalBy() 
    {
        return deanApprovalBy;
    }

    public void setDeanApprovalTime(Date deanApprovalTime) 
    {
        this.deanApprovalTime = deanApprovalTime;
    }

    public Date getDeanApprovalTime() 
    {
        return deanApprovalTime;
    }

    public void setAppealStatus(Integer appealStatus) 
    {
        this.appealStatus = appealStatus;
    }

    public Integer getAppealStatus() 
    {
        return appealStatus;
    }

    public void setAppealReason(String appealReason) 
    {
        this.appealReason = appealReason;
    }

    public String getAppealReason() 
    {
        return appealReason;
    }

    public void setAppealReply(String appealReply) 
    {
        this.appealReply = appealReply;
    }

    public String getAppealReply() 
    {
        return appealReply;
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
            .append("userId", getUserId())
            .append("semester", getSemester())
            .append("academicYear", getAcademicYear())
            .append("itemType", getItemType())
            .append("sourceType", getSourceType())
            .append("taskId", getTaskId())
            .append("assignmentId", getAssignmentId())
            .append("courseName", getCourseName())
            .append("educationLevel", getEducationLevel())
            .append("majorCategory", getMajorCategory())
            .append("calculatedWorkload", getCalculatedWorkload())
            .append("description", getDescription())
            .append("isOverLimit", getIsOverLimit())
            .append("deanApprovalStatus", getDeanApprovalStatus())
            .append("deanApprovalBy", getDeanApprovalBy())
            .append("deanApprovalTime", getDeanApprovalTime())
            .append("appealStatus", getAppealStatus())
            .append("appealReason", getAppealReason())
            .append("appealReply", getAppealReply())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
