package com.workload.system.domain.dto;

import java.math.BigDecimal;
import java.util.Date;
import com.workload.common.annotation.Excel;

/**
 * 教学任务导出数据传输对象
 *
 * @author wflg
 */
public class TeachingTaskExportDTO
{
    @Excel(name = "教师工号", sort = 1)
    private String userCode;

    @Excel(name = "教师姓名", sort = 2)
    private String userName;

    @Excel(name = "学年学期", sort = 3)
    private String semester;

    @Excel(name = "学年", sort = 4)
    private String academicYear;

    @Excel(name = "课程名称", sort = 5)
    private String courseName;

    @Excel(name = "课程代码", sort = 6)
    private String courseCode;

    @Excel(name = "授课层次", sort = 7)
    private String educationLevel;

    @Excel(name = "专业大类", sort = 8)
    private String majorCategory;

    @Excel(name = "课程性质", sort = 9)
    private String courseNature;

    @Excel(name = "课程级别", sort = 10)
    private String courseLevel;

    @Excel(name = "课程角色", sort = 11)
    private String courseRole;

    @Excel(name = "班级", sort = 12)
    private String className;

    @Excel(name = "合堂人数", sort = 13)
    private Long studentCount;

    @Excel(name = "理论学时J1", sort = 14)
    private BigDecimal theoryHours;

    @Excel(name = "实践学时J2", sort = 15)
    private BigDecimal practiceHours;

    @Excel(name = "同名课第几次(1/2/3+ -> C1 1.0/0.9/0.8)", sort = 16)
    private Long repeatOrder;

    @Excel(name = "导入来源", readConverterExp = "EXCEL=Excel导入,SEED=种子数据", sort = 17)
    private String importSource;

    @Excel(name = "导入批次", sort = 18)
    private String importBatch;

    @Excel(name = "导入时间", dateFormat = "yyyy-MM-dd HH:mm:ss", width = 20, sort = 19)
    private Date importTime;

    @Excel(name = "状态", readConverterExp = "0=停用,1=正常", sort = 20)
    private Integer status;

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

    public String getSemester()
    {
        return semester;
    }

    public void setSemester(String semester)
    {
        this.semester = semester;
    }

    public String getAcademicYear()
    {
        return academicYear;
    }

    public void setAcademicYear(String academicYear)
    {
        this.academicYear = academicYear;
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

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public Long getStudentCount()
    {
        return studentCount;
    }

    public void setStudentCount(Long studentCount)
    {
        this.studentCount = studentCount;
    }

    public BigDecimal getTheoryHours()
    {
        return theoryHours;
    }

    public void setTheoryHours(BigDecimal theoryHours)
    {
        this.theoryHours = theoryHours;
    }

    public BigDecimal getPracticeHours()
    {
        return practiceHours;
    }

    public void setPracticeHours(BigDecimal practiceHours)
    {
        this.practiceHours = practiceHours;
    }

    public Long getRepeatOrder()
    {
        return repeatOrder;
    }

    public void setRepeatOrder(Long repeatOrder)
    {
        this.repeatOrder = repeatOrder;
    }

    public String getImportSource()
    {
        return importSource;
    }

    public void setImportSource(String importSource)
    {
        this.importSource = importSource;
    }

    public String getImportBatch()
    {
        return importBatch;
    }

    public void setImportBatch(String importBatch)
    {
        this.importBatch = importBatch;
    }

    public Date getImportTime()
    {
        return importTime;
    }

    public void setImportTime(Date importTime)
    {
        this.importTime = importTime;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }
}