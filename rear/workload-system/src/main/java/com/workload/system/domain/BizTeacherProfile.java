package com.workload.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * 教师业务档案对象 biz_teacher_profile
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizTeacherProfile extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 教师ID(关联sys_user.user_id) */
    private Long userId;

    /** 职称(教授/副教授/讲师/助教/未定级) */
    @Excel(name = "职称(教授/副教授/讲师/助教/未定级)")
    private String title;

    /** 人员性质(专任/外聘/校企/银龄/青州外聘) */
    @Excel(name = "人员性质(专任/外聘/校企/银龄/青州外聘)")
    private String teacherNature;

    /** 特殊状态(正常/产假/在职读博/访学) */
    @Excel(name = "特殊状态(正常/产假/在职读博/访学)")
    private String specialStatus;

    /** 特殊状态起 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "特殊状态起", width = 30, dateFormat = "yyyy-MM-dd")
    private Date specialStatusStart;

    /** 特殊状态止 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "特殊状态止", width = 30, dateFormat = "yyyy-MM-dd")
    private Date specialStatusEnd;

    /** 校企考核结果(优秀/合格/不合格) */
    @Excel(name = "校企考核结果(优秀/合格/不合格)")
    private String enterpriseEvalResult;

    /** 院部(sys_dept.dept_id) */
    @Excel(name = "院部(sys_dept.dept_id)")
    private Long deptId;

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setTeacherNature(String teacherNature) 
    {
        this.teacherNature = teacherNature;
    }

    public String getTeacherNature() 
    {
        return teacherNature;
    }

    public void setSpecialStatus(String specialStatus) 
    {
        this.specialStatus = specialStatus;
    }

    public String getSpecialStatus() 
    {
        return specialStatus;
    }

    public void setSpecialStatusStart(Date specialStatusStart) 
    {
        this.specialStatusStart = specialStatusStart;
    }

    public Date getSpecialStatusStart() 
    {
        return specialStatusStart;
    }

    public void setSpecialStatusEnd(Date specialStatusEnd) 
    {
        this.specialStatusEnd = specialStatusEnd;
    }

    public Date getSpecialStatusEnd() 
    {
        return specialStatusEnd;
    }

    public void setEnterpriseEvalResult(String enterpriseEvalResult) 
    {
        this.enterpriseEvalResult = enterpriseEvalResult;
    }

    public String getEnterpriseEvalResult() 
    {
        return enterpriseEvalResult;
    }

    public void setDeptId(Long deptId) 
    {
        this.deptId = deptId;
    }

    public Long getDeptId() 
    {
        return deptId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("title", getTitle())
            .append("teacherNature", getTeacherNature())
            .append("specialStatus", getSpecialStatus())
            .append("specialStatusStart", getSpecialStatusStart())
            .append("specialStatusEnd", getSpecialStatusEnd())
            .append("enterpriseEvalResult", getEnterpriseEvalResult())
            .append("deptId", getDeptId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
