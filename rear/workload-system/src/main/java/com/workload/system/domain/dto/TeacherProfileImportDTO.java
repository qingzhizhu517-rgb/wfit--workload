package com.workload.system.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;

/**
 * 教师档案导入 Excel 行模型
 * <p>
 * Excel 表头顺序与字段顺序一致
 *
 * @author wflg
 */
public class TeacherProfileImportDTO
{
    @ExcelProperty("教师工号")
    @ColumnWidth(14)
    private String userCode;

    @ExcelProperty("教师姓名")
    @ColumnWidth(12)
    private String nickName;

    @ExcelProperty("院部名称")
    @ColumnWidth(20)
    private String deptName;

    @ExcelProperty("职称")
    @ColumnWidth(12)
    private String title;

    @ExcelProperty("人员性质")
    @ColumnWidth(12)
    private String teacherNature;

    @ExcelProperty("手机号")
    @ColumnWidth(14)
    private String phonenumber;

    @ExcelProperty("邮箱")
    @ColumnWidth(20)
    private String email;

    // --- Getters / Setters ---

    public String getUserCode()
    {
        return userCode;
    }

    public void setUserCode(String userCode)
    {
        this.userCode = userCode;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTeacherNature()
    {
        return teacherNature;
    }

    public void setTeacherNature(String teacherNature)
    {
        this.teacherNature = teacherNature;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    @Override
    public String toString()
    {
        return "TeacherProfileImportDTO{userCode='" + userCode + "', nickName='" + nickName
                + "', deptName='" + deptName + "', title='" + title + "'}";
    }
}
