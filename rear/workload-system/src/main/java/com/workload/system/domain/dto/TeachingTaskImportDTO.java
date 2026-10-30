package com.workload.system.domain.dto;

import java.math.BigDecimal;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;

/**
 * 教学任务导入 Excel 行模型
 * <p>
 * Excel 表头顺序与字段顺序一致
 *
 * @author wflg
 */
public class TeachingTaskImportDTO
{
    @com.alibaba.excel.annotation.ExcelIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore private String rawImportJson;
    @com.alibaba.excel.annotation.ExcelIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore private Long importRowId;
    @com.alibaba.excel.annotation.ExcelIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore private String importSourceHash;
    @com.alibaba.excel.annotation.ExcelIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore private String importTemplateVersion;
    public String getImportTemplateVersion() { return importTemplateVersion; }
    public void setImportTemplateVersion(String v) { importTemplateVersion=v; }
    public String getRawImportJson() { return rawImportJson; }
    public void setRawImportJson(String v) { rawImportJson=v; }
    public Long getImportRowId() { return importRowId; }
    public void setImportRowId(Long v) { importRowId=v; }
    public String getImportSourceHash() { return importSourceHash; }
    public void setImportSourceHash(String v) { importSourceHash=v; }

    @ExcelProperty("学年学期")
    @ColumnWidth(18)
    private String semester;

    @ExcelProperty("教师工号")
    @ColumnWidth(12)
    private String userCode;

    @ExcelProperty("教师姓名")
    @ColumnWidth(10)
    private String userName;

    @ExcelProperty("课程名称")
    @ColumnWidth(24)
    private String courseName;

    @ExcelProperty("课程代码")
    @ColumnWidth(14)
    private String courseCode;

    @ExcelProperty("工作量类别")
    @ColumnWidth(14)
    private String workloadType;

    @ExcelProperty("授课层次")
    @ColumnWidth(10)
    private String educationLevel;

    @ExcelProperty("专业大类")
    @ColumnWidth(10)
    private String majorCategory;

    @ExcelProperty("课程性质")
    @ColumnWidth(10)
    private String courseNature;

    @ExcelProperty("课程级别")
    @ColumnWidth(12)
    private String courseLevel;

    @ExcelProperty("课程角色")
    @ColumnWidth(12)
    private String courseRole;

    @ExcelProperty("教学评价")
    @ColumnWidth(10)
    private String teachingEval;

    @ExcelProperty("选课/指导人数")
    @ColumnWidth(12)
    private Integer studentCount;

    /** G1/G2 计划学时（理论/实践学时）。v2 数量列按类别语义拆分，不再共用单一「基数」列。 */
    @ExcelProperty("计划学时")
    @ColumnWidth(12)
    private BigDecimal plannedHours;

    /** G3 实习实训天数。 */
    @ExcelProperty("天数")
    @ColumnWidth(10)
    private BigDecimal days;

    /** G6 集中实习周数。 */
    @ExcelProperty("周数")
    @ColumnWidth(10)
    private BigDecimal weeks;

    /** G4 课程设计学分。 */
    @ExcelProperty("课程设计学分")
    @ColumnWidth(12)
    private BigDecimal credits;

    @ExcelProperty("课程系数")
    @ColumnWidth(10)
    private BigDecimal courseCoefficient;

    /** 班级：用于区分同一门课的不同班次，落库到 biz_teaching_task.class_name，
     *  参与重复系数分组的可追溯展示（附件1「系数说明」列显示"第2次(自动化2402)"）。 */
    @ExcelProperty("班级")
    @ColumnWidth(16)
    private String className;

    /** 重复次序：同名课在本学期是第几次开课（1/2/3+）。
     *  留空则由导入按同组已入库条数自动补位，见 TeachingTaskImportServiceImpl#resolveRepeatOrder。 */
    @ExcelProperty("重复次序")
    @ColumnWidth(10)
    private Integer repeatOrder;

    // --- Getters / Setters ---

    public String getSemester()
    {
        return semester;
    }

    public void setSemester(String semester)
    {
        this.semester = semester;
    }

    public String getUserCode()
    {
        return userCode;
    }

    public void setUserCode(String userCode)
    {
        this.userCode = userCode;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getCourseName()
    {
        return courseName;
    }

    public void setCourseName(String courseName)
    {
        this.courseName = courseName;
    }

    public String getCourseCode()
    {
        return courseCode;
    }

    public void setCourseCode(String courseCode)
    {
        this.courseCode = courseCode;
    }

    public String getWorkloadType()
    {
        return workloadType;
    }

    public void setWorkloadType(String workloadType)
    {
        this.workloadType = workloadType;
    }

    public String getEducationLevel()
    {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel)
    {
        this.educationLevel = educationLevel;
    }

    public String getMajorCategory()
    {
        return majorCategory;
    }

    public void setMajorCategory(String majorCategory)
    {
        this.majorCategory = majorCategory;
    }

    public String getCourseNature()
    {
        return courseNature;
    }

    public void setCourseNature(String courseNature)
    {
        this.courseNature = courseNature;
    }

    public String getCourseLevel()
    {
        return courseLevel;
    }

    public void setCourseLevel(String courseLevel)
    {
        this.courseLevel = courseLevel;
    }

    public String getCourseRole()
    {
        return courseRole;
    }

    public void setCourseRole(String courseRole)
    {
        this.courseRole = courseRole;
    }

    public String getTeachingEval()
    {
        return teachingEval;
    }

    public void setTeachingEval(String teachingEval)
    {
        this.teachingEval = teachingEval;
    }

    public Integer getStudentCount()
    {
        return studentCount;
    }

    public void setStudentCount(Integer studentCount)
    {
        this.studentCount = studentCount;
    }

    public BigDecimal getPlannedHours()
    {
        return plannedHours;
    }

    public void setPlannedHours(BigDecimal plannedHours)
    {
        this.plannedHours = plannedHours;
    }

    public BigDecimal getDays()
    {
        return days;
    }

    public void setDays(BigDecimal days)
    {
        this.days = days;
    }

    public BigDecimal getWeeks()
    {
        return weeks;
    }

    public void setWeeks(BigDecimal weeks)
    {
        this.weeks = weeks;
    }

    public BigDecimal getCredits()
    {
        return credits;
    }

    public void setCredits(BigDecimal credits)
    {
        this.credits = credits;
    }

    public BigDecimal getCourseCoefficient()
    {
        return courseCoefficient;
    }

    public void setCourseCoefficient(BigDecimal courseCoefficient)
    {
        this.courseCoefficient = courseCoefficient;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public Integer getRepeatOrder()
    {
        return repeatOrder;
    }

    public void setRepeatOrder(Integer repeatOrder)
    {
        this.repeatOrder = repeatOrder;
    }

    @Override
    public String toString()
    {
        return "TeachingTaskImportDTO{semester='" + semester + "', userCode='" + userCode
                + "', courseName='" + courseName + "', className='" + className
                + "', workloadType='" + workloadType + "'}";
    }
}
