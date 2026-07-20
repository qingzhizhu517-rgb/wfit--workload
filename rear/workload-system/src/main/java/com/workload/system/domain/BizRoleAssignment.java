package com.workload.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * 岗位任职对象 biz_role_assignment
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizRoleAssignment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long userId;

    /** 班主任/系主任/教研室主任/专业负责人/俱乐部经理/实验人员/督导/中层副职/心理中心 */
    @Excel(name = "班主任/系主任/教研室主任/专业负责人/俱乐部经理/实验人员/督导/中层副职/心理中心")
    private String roleType;

    /** 目标班级或范围 */
    @Excel(name = "目标班级或范围")
    private String target;

    /** 任职起 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "任职起", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 任职止(NULL=至今) */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "任职止(NULL=至今)", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String semester;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String academicYear;

    /** 该岗位标准学时/学年 */
    @Excel(name = "该岗位标准学时/学年")
    private BigDecimal allowanceRate;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
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

    public void setRoleType(String roleType) 
    {
        this.roleType = roleType;
    }

    public String getRoleType() 
    {
        return roleType;
    }

    public void setTarget(String target) 
    {
        this.target = target;
    }

    public String getTarget() 
    {
        return target;
    }

    public void setStartDate(Date startDate) 
    {
        this.startDate = startDate;
    }

    public Date getStartDate() 
    {
        return startDate;
    }

    public void setEndDate(Date endDate) 
    {
        this.endDate = endDate;
    }

    public Date getEndDate() 
    {
        return endDate;
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

    public void setAllowanceRate(BigDecimal allowanceRate) 
    {
        this.allowanceRate = allowanceRate;
    }

    public BigDecimal getAllowanceRate() 
    {
        return allowanceRate;
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
            .append("roleType", getRoleType())
            .append("target", getTarget())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("semester", getSemester())
            .append("academicYear", getAcademicYear())
            .append("allowanceRate", getAllowanceRate())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
