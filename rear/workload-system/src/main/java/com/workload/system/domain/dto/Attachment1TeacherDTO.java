package com.workload.system.domain.dto;

import java.math.BigDecimal;

/**
 * 附件1 教师级信息（首行 A/B 列与 AC~AM 列 + 标题行所需的学年/学院）。
 * <p>
 * 与任务行 {@link Attachment1RowDTO} 分开：教师级列只在首条数据行出现，
 * 其余行留空——与教务处参考件填法一致。
 */
public class Attachment1TeacherDTO
{
    /** 标题/首行 A 列：学院（sys_dept.dept_name） */
    private String collegeName;

    /** 首行 B 列：教师姓名（sys_user.nick_name） */
    private String teacherName;

    /** 标题行用：学年（如 2026-2027），semester 形如 2026-2027-1 */
    private String academicYear;

    /** AC 第一课堂教学工作量 G7 */
    private BigDecimal g7;

    /** AD 第二课堂工作量 G8 */
    private BigDecimal g8;

    /** AE 其他工作量 G9 */
    private BigDecimal g9;

    /** AF 其他工作量说明（summary.g8_remark） */
    private String g8Remark;

    /** AG 教学工作量合计 G10 */
    private BigDecimal g10;

    /** AH 管理服务工作量 G11 */
    private BigDecimal g11;

    /** AI 管理服务工作量说明（summary.g11_remark） */
    private String g11Remark;

    /** AJ 总工作量 */
    private BigDecimal totalWorkload;

    /** AK 额定工作量 */
    private BigDecimal ratedWorkload;

    /** AL 超额定工作量（保留1位小数） */
    private BigDecimal excessWorkload;

    /** AM 教师签字（线上签署时非空；线下签为 null 留空） */
    private String teacherSign;

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public BigDecimal getG7() { return g7; }
    public void setG7(BigDecimal g7) { this.g7 = g7; }
    public BigDecimal getG8() { return g8; }
    public void setG8(BigDecimal g8) { this.g8 = g8; }
    public BigDecimal getG9() { return g9; }
    public void setG9(BigDecimal g9) { this.g9 = g9; }
    public String getG8Remark() { return g8Remark; }
    public void setG8Remark(String g8Remark) { this.g8Remark = g8Remark; }
    public BigDecimal getG10() { return g10; }
    public void setG10(BigDecimal g10) { this.g10 = g10; }
    public BigDecimal getG11() { return g11; }
    public void setG11(BigDecimal g11) { this.g11 = g11; }
    public String getG11Remark() { return g11Remark; }
    public void setG11Remark(String g11Remark) { this.g11Remark = g11Remark; }
    public BigDecimal getTotalWorkload() { return totalWorkload; }
    public void setTotalWorkload(BigDecimal totalWorkload) { this.totalWorkload = totalWorkload; }
    public BigDecimal getRatedWorkload() { return ratedWorkload; }
    public void setRatedWorkload(BigDecimal ratedWorkload) { this.ratedWorkload = ratedWorkload; }
    public BigDecimal getExcessWorkload() { return excessWorkload; }
    public void setExcessWorkload(BigDecimal excessWorkload) { this.excessWorkload = excessWorkload; }
    public String getTeacherSign() { return teacherSign; }
    public void setTeacherSign(String teacherSign) { this.teacherSign = teacherSign; }
}
