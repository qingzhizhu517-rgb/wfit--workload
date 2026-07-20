package com.workload.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * 学期工作量汇总对象 biz_workload_summary
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizWorkloadSummary extends BaseEntity
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

    /** 第一课堂=G1+G2+G3+G4+G5+G6 */
    @Excel(name = "第一课堂=G1+G2+G3+G4+G5+G6")
    private BigDecimal G7;

    /** 第二课堂 */
    @Excel(name = "第二课堂")
    private BigDecimal G8;

    /** 其他 */
    @Excel(name = "其他")
    private BigDecimal G9;

    /** 教学合计=G7+G8+G9 */
    @Excel(name = "教学合计=G7+G8+G9")
    private BigDecimal G10;

    /** 管理服务 */
    @Excel(name = "管理服务")
    private BigDecimal G11;

    /** 总工作量=G10+G11 */
    @Excel(name = "总工作量=G10+G11")
    private BigDecimal totalWorkload;

    /** 额定(统一180) */
    @Excel(name = "额定(统一180)")
    private BigDecimal ratedWorkload;

    /** 超额定=max(0,total-rated) */
    @Excel(name = "超额定=max(0,total-rated)")
    private BigDecimal excessWorkload;

    /** 职称快照 */
    @Excel(name = "职称快照")
    private String title;

    /** 单位酬金快照 */
    @Excel(name = "单位酬金快照")
    private BigDecimal payRate;

    /** 绩效酬金=(min(total,540)-180)*rate */
    @Excel(name = "绩效酬金=(min(total,540)-180)*rate")
    private BigDecimal performancePay;

    /** 触200%封顶 */
    @Excel(name = "触200%封顶")
    private Integer isCapped;

    /** 第五条达标标准/学期 */
    @Excel(name = "第五条达标标准/学期")
    private BigDecimal basicTeachingStandard;

    /** 达标G10&gt;=standard */
    @Excel(name = "达标G10&gt;=standard")
    private Integer basicTeachingMet;

    /** 0草稿/1已公示/2已审核/3已锁定 */
    @Excel(name = "0草稿/1已公示/2已审核/3已锁定")
    private Integer status;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String teacherSign;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Date teacherSignTime;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String deptLeaderSign;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Date deptLeaderSignTime;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String academicAssistantSign;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Date academicAssistantSignTime;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Date lockTime;

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

    public void setG7(BigDecimal G7) 
    {
        this.G7 = G7;
    }

    public BigDecimal getG7() 
    {
        return G7;
    }

    public void setG8(BigDecimal G8) 
    {
        this.G8 = G8;
    }

    public BigDecimal getG8() 
    {
        return G8;
    }

    public void setG9(BigDecimal G9) 
    {
        this.G9 = G9;
    }

    public BigDecimal getG9() 
    {
        return G9;
    }

    public void setG10(BigDecimal G10) 
    {
        this.G10 = G10;
    }

    public BigDecimal getG10() 
    {
        return G10;
    }

    public void setG11(BigDecimal G11) 
    {
        this.G11 = G11;
    }

    public BigDecimal getG11() 
    {
        return G11;
    }

    public void setTotalWorkload(BigDecimal totalWorkload) 
    {
        this.totalWorkload = totalWorkload;
    }

    public BigDecimal getTotalWorkload() 
    {
        return totalWorkload;
    }

    public void setRatedWorkload(BigDecimal ratedWorkload) 
    {
        this.ratedWorkload = ratedWorkload;
    }

    public BigDecimal getRatedWorkload() 
    {
        return ratedWorkload;
    }

    public void setExcessWorkload(BigDecimal excessWorkload) 
    {
        this.excessWorkload = excessWorkload;
    }

    public BigDecimal getExcessWorkload() 
    {
        return excessWorkload;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setPayRate(BigDecimal payRate) 
    {
        this.payRate = payRate;
    }

    public BigDecimal getPayRate() 
    {
        return payRate;
    }

    public void setPerformancePay(BigDecimal performancePay) 
    {
        this.performancePay = performancePay;
    }

    public BigDecimal getPerformancePay() 
    {
        return performancePay;
    }

    public void setIsCapped(Integer isCapped) 
    {
        this.isCapped = isCapped;
    }

    public Integer getIsCapped() 
    {
        return isCapped;
    }

    public void setBasicTeachingStandard(BigDecimal basicTeachingStandard) 
    {
        this.basicTeachingStandard = basicTeachingStandard;
    }

    public BigDecimal getBasicTeachingStandard() 
    {
        return basicTeachingStandard;
    }

    public void setBasicTeachingMet(Integer basicTeachingMet) 
    {
        this.basicTeachingMet = basicTeachingMet;
    }

    public Integer getBasicTeachingMet() 
    {
        return basicTeachingMet;
    }

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    public void setTeacherSign(String teacherSign) 
    {
        this.teacherSign = teacherSign;
    }

    public String getTeacherSign() 
    {
        return teacherSign;
    }

    public void setTeacherSignTime(Date teacherSignTime) 
    {
        this.teacherSignTime = teacherSignTime;
    }

    public Date getTeacherSignTime() 
    {
        return teacherSignTime;
    }

    public void setDeptLeaderSign(String deptLeaderSign) 
    {
        this.deptLeaderSign = deptLeaderSign;
    }

    public String getDeptLeaderSign() 
    {
        return deptLeaderSign;
    }

    public void setDeptLeaderSignTime(Date deptLeaderSignTime) 
    {
        this.deptLeaderSignTime = deptLeaderSignTime;
    }

    public Date getDeptLeaderSignTime() 
    {
        return deptLeaderSignTime;
    }

    public void setAcademicAssistantSign(String academicAssistantSign) 
    {
        this.academicAssistantSign = academicAssistantSign;
    }

    public String getAcademicAssistantSign() 
    {
        return academicAssistantSign;
    }

    public void setAcademicAssistantSignTime(Date academicAssistantSignTime) 
    {
        this.academicAssistantSignTime = academicAssistantSignTime;
    }

    public Date getAcademicAssistantSignTime() 
    {
        return academicAssistantSignTime;
    }

    public void setLockTime(Date lockTime) 
    {
        this.lockTime = lockTime;
    }

    public Date getLockTime() 
    {
        return lockTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("semester", getSemester())
            .append("academicYear", getAcademicYear())
            .append("G7", getG7())
            .append("G8", getG8())
            .append("G9", getG9())
            .append("G10", getG10())
            .append("G11", getG11())
            .append("totalWorkload", getTotalWorkload())
            .append("ratedWorkload", getRatedWorkload())
            .append("excessWorkload", getExcessWorkload())
            .append("title", getTitle())
            .append("payRate", getPayRate())
            .append("performancePay", getPerformancePay())
            .append("isCapped", getIsCapped())
            .append("basicTeachingStandard", getBasicTeachingStandard())
            .append("basicTeachingMet", getBasicTeachingMet())
            .append("status", getStatus())
            .append("teacherSign", getTeacherSign())
            .append("teacherSignTime", getTeacherSignTime())
            .append("deptLeaderSign", getDeptLeaderSign())
            .append("deptLeaderSignTime", getDeptLeaderSignTime())
            .append("academicAssistantSign", getAcademicAssistantSign())
            .append("academicAssistantSignTime", getAcademicAssistantSignTime())
            .append("lockTime", getLockTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
