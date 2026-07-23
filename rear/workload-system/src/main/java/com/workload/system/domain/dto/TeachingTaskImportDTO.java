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

    @ExcelProperty("选课人数")
    @ColumnWidth(10)
    private Integer studentCount;

    @ExcelProperty("计划学时/天数/周数")
    @ColumnWidth(18)
    private BigDecimal baseValue;

    @ExcelProperty("课程系数")
    @ColumnWidth(10)
    private BigDecimal courseCoefficient;

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

    public BigDecimal getBaseValue()
    {
        return baseValue;
    }

    public void setBaseValue(BigDecimal baseValue)
    {
        this.baseValue = baseValue;
    }

    public BigDecimal getCourseCoefficient()
    {
        return courseCoefficient;
    }

    public void setCourseCoefficient(BigDecimal courseCoefficient)
    {
        this.courseCoefficient = courseCoefficient;
    }

    @Override
    public String toString()
    {
        return "TeachingTaskImportDTO{semester='" + semester + "', userCode='" + userCode
                + "', courseName='" + courseName + "', workloadType='" + workloadType + "'}";
    }
}
