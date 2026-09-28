package com.workload.system.domain.dto;

import java.math.BigDecimal;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * 教师花名册字段白名单行 DTO（Task 6）。
 *
 * <p><b>安全边界：</b>只声明可参与业务的白名单列。<b>绝不定义</b>身份证号、住址、
 * 银行卡等敏感列——预检解析时只读取表头名用于敏感列风险提示，敏感列的<b>值</b>
 * 绝不读入任何字段、绝不写 {@code raw_json}、绝不落库或打印日志。</p>
 *
 * <p>本轮（2026-09-12 业务确认）花名册只做字段参考与异常报告，<b>不导入真实教师</b>，
 * 因此没有 confirm 写入流程；本 DTO 仅承载预检解析出的白名单字段。</p>
 *
 * <p>列名（按花名册 Sheet1 表头，顺序不敏感、按列名匹配）：
 * 系 / 教研室 / 员工姓名 / 职务 / 职务匹配工作量 / 岗位 / 职称 / 工号 / 联系方式。</p>
 *
 * @author wflg
 */
public class TeacherRosterImportDTO
{
    /** 系（院系名称，对应 sys_dept 参考） */
    @ExcelProperty("系")
    private String departmentName;

    /** 教研室（更细粒度的教学组织，仅作字段参考） */
    @ExcelProperty("教研室")
    private String teachingOffice;

    /** 员工姓名（对应 sys_user.nick_name） */
    @ExcelProperty("员工姓名")
    private String nickName;

    /** 职务（如科长/处长，仅展示，可空） */
    @ExcelProperty("职务")
    private String positionName;

    /** 职务匹配工作量（本学期岗位减免工作量参考值，可空） */
    @ExcelProperty("职务匹配工作量")
    private BigDecimal positionWorkload;

    /** 岗位（教师/银铃教师/教师兼行政/行政…，用于映射人员性质） */
    @ExcelProperty("岗位")
    private String employmentType;

    /** 职称（教授/副教授/讲师/助教/未定级；无/讲师待遇为待决值） */
    @ExcelProperty("职称")
    private String title;

    /** 工号（对应 sys_user.user_name，唯一） */
    @ExcelProperty("工号")
    private String userCode;

    /** 联系方式（手机号） */
    @ExcelProperty("联系方式")
    private String phonenumber;

    public String getDepartmentName()
    {
        return departmentName;
    }

    public void setDepartmentName(String departmentName)
    {
        this.departmentName = departmentName;
    }

    public String getTeachingOffice()
    {
        return teachingOffice;
    }

    public void setTeachingOffice(String teachingOffice)
    {
        this.teachingOffice = teachingOffice;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getPositionName()
    {
        return positionName;
    }

    public void setPositionName(String positionName)
    {
        this.positionName = positionName;
    }

    public BigDecimal getPositionWorkload()
    {
        return positionWorkload;
    }

    public void setPositionWorkload(BigDecimal positionWorkload)
    {
        this.positionWorkload = positionWorkload;
    }

    public String getEmploymentType()
    {
        return employmentType;
    }

    public void setEmploymentType(String employmentType)
    {
        this.employmentType = employmentType;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getUserCode()
    {
        return userCode;
    }

    public void setUserCode(String userCode)
    {
        this.userCode = userCode;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }
}
