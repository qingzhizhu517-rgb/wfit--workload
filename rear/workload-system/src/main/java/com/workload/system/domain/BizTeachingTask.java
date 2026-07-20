package com.workload.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * 导入教学任务对象 biz_teaching_task
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizTeachingTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 教师ID */
    @Excel(name = "教师ID")
    private Long userId;

    /** 学年学期(如2025-2026-1) */
    @Excel(name = "学年学期(如2025-2026-1)")
    private String semester;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String academicYear;

    /** 课程名称 */
    @Excel(name = "课程名称")
    private String courseName;

    /** 课程代码 */
    @Excel(name = "课程代码")
    private String courseCode;

    /** 本科(含专升本)/专科 */
    @Excel(name = "本科(含专升本)/专科")
    private String educationLevel;

    /** 理工类/文史类/艺术类/其他 */
    @Excel(name = "理工类/文史类/艺术类/其他")
    private String majorCategory;

    /** 必修/选修 */
    @Excel(name = "必修/选修")
    private String courseNature;

    /** 省级一流/校级精品/其他 */
    @Excel(name = "省级一流/校级精品/其他")
    private String courseLevel;

    /** 主持人/团队前3/独立 */
    @Excel(name = "主持人/团队前3/独立")
    private String courseRole;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String className;

    /** 合堂人数 */
    @Excel(name = "合堂人数")
    private Long studentCount;

    /** 理论学时J1 */
    @Excel(name = "理论学时J1")
    private BigDecimal theoryHours;

    /** 实践学时J2 */
    @Excel(name = "实践学时J2")
    private BigDecimal practiceHours;

    /** 同名课第几次(1/2/3+ -&gt; C1 1.0/0.9/0.8) */
    @Excel(name = "同名课第几次(1/2/3+ -&gt; C1 1.0/0.9/0.8)")
    private Long repeatOrder;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String importSource;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String importBatch;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Date importTime;

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

    public void setCourseName(String courseName) 
    {
        this.courseName = courseName;
    }

    public String getCourseName() 
    {
        return courseName;
    }

    public void setCourseCode(String courseCode) 
    {
        this.courseCode = courseCode;
    }

    public String getCourseCode() 
    {
        return courseCode;
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

    public void setCourseNature(String courseNature) 
    {
        this.courseNature = courseNature;
    }

    public String getCourseNature() 
    {
        return courseNature;
    }

    public void setCourseLevel(String courseLevel) 
    {
        this.courseLevel = courseLevel;
    }

    public String getCourseLevel() 
    {
        return courseLevel;
    }

    public void setCourseRole(String courseRole) 
    {
        this.courseRole = courseRole;
    }

    public String getCourseRole() 
    {
        return courseRole;
    }

    public void setClassName(String className) 
    {
        this.className = className;
    }

    public String getClassName() 
    {
        return className;
    }

    public void setStudentCount(Long studentCount) 
    {
        this.studentCount = studentCount;
    }

    public Long getStudentCount() 
    {
        return studentCount;
    }

    public void setTheoryHours(BigDecimal theoryHours) 
    {
        this.theoryHours = theoryHours;
    }

    public BigDecimal getTheoryHours() 
    {
        return theoryHours;
    }

    public void setPracticeHours(BigDecimal practiceHours) 
    {
        this.practiceHours = practiceHours;
    }

    public BigDecimal getPracticeHours() 
    {
        return practiceHours;
    }

    public void setRepeatOrder(Long repeatOrder) 
    {
        this.repeatOrder = repeatOrder;
    }

    public Long getRepeatOrder() 
    {
        return repeatOrder;
    }

    public void setImportSource(String importSource) 
    {
        this.importSource = importSource;
    }

    public String getImportSource() 
    {
        return importSource;
    }

    public void setImportBatch(String importBatch) 
    {
        this.importBatch = importBatch;
    }

    public String getImportBatch() 
    {
        return importBatch;
    }

    public void setImportTime(Date importTime) 
    {
        this.importTime = importTime;
    }

    public Date getImportTime() 
    {
        return importTime;
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
            .append("courseName", getCourseName())
            .append("courseCode", getCourseCode())
            .append("educationLevel", getEducationLevel())
            .append("majorCategory", getMajorCategory())
            .append("courseNature", getCourseNature())
            .append("courseLevel", getCourseLevel())
            .append("courseRole", getCourseRole())
            .append("className", getClassName())
            .append("studentCount", getStudentCount())
            .append("theoryHours", getTheoryHours())
            .append("practiceHours", getPracticeHours())
            .append("repeatOrder", getRepeatOrder())
            .append("importSource", getImportSource())
            .append("importBatch", getImportBatch())
            .append("importTime", getImportTime())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
