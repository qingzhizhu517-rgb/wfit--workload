package com.workload.system.domain.dto;

import java.math.BigDecimal;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;

/**
 * 个人工作量明细导出 DTO（附件1：教师教育教学工作量统计表）
 * <p>
 * 除核算结果外一并导出各系数原值与「系数说明」，用于事后追溯
 * 「这条为什么只算 0.8」——重复次序、具体班级、超限封顶都在表内直读，
 * 不必再回查 biz_wl_* 明细表。
 * <p>
 * 字段声明顺序即 Excel 列顺序（EasyExcel 无 index 时按声明序写出），
 * 末尾 {@code @ExcelIgnore} 字段仅供 {@code coefRemark} 拼装，不落表。
 *
 * @author wflg
 */
public class PersonalWorkloadDetailExportDTO
{
    @ExcelProperty("类别")
    @ColumnWidth(8)
    private String itemType;

    @ExcelProperty("项目名称")
    @ColumnWidth(24)
    private String courseName;

    @ExcelProperty("班级")
    @ColumnWidth(16)
    private String className;

    @ExcelProperty("授课层次")
    @ColumnWidth(10)
    private String educationLevel;

    @ExcelProperty("专业大类")
    @ColumnWidth(10)
    private String majorCategory;

    /** 基数：G1 计划学时 / G2 实践学时 / G3 天数 / G4 学分 / G6 周数（G5、G11 无基数列） */
    @ExcelProperty("基数(学时/天/周/学分)")
    @ColumnWidth(20)
    private BigDecimal baseValue;

    /** 人数：G4 R4 / G5 R5 / G6 R6，教学任务类回落到选课人数 */
    @ExcelProperty("人数")
    @ColumnWidth(8)
    private Long studentCount;

    @ExcelProperty("重复次序")
    @ColumnWidth(10)
    private Long repeatOrder;

    /** 重复系数：G1 取 C1，G2 取 C2（恒 0.9），G3 取 K */
    @ExcelProperty("重复系数")
    @ColumnWidth(10)
    private BigDecimal repeatCoef;

    @ExcelProperty("课程类型K1")
    @ColumnWidth(12)
    private BigDecimal k1;

    @ExcelProperty("教学质量Q1")
    @ColumnWidth(12)
    private BigDecimal q1;

    @ExcelProperty("课程质量Q2")
    @ColumnWidth(12)
    private BigDecimal q2;

    @ExcelProperty("全外文Q3")
    @ColumnWidth(11)
    private BigDecimal q3;

    @ExcelProperty("合堂系数N")
    @ColumnWidth(11)
    private BigDecimal n;

    /** 其他系数：G3 取 D（学科天数系数），G5 取 K5（生均学时），G2 取 K（学科系数） */
    @ExcelProperty("其他系数(D/K5/K)")
    @ColumnWidth(16)
    private BigDecimal otherCoef;

    @ExcelProperty("核算工作量")
    @ColumnWidth(12)
    private BigDecimal calculatedWorkload;

    @ExcelProperty("系数说明")
    @ColumnWidth(38)
    private String coefRemark;

    @ExcelProperty("数据来源")
    @ColumnWidth(10)
    private String sourceType;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String status;

    // --- 以下字段仅用于拼装 coefRemark，不导出 ---

    @ExcelIgnore
    private Integer overLimit;

    @ExcelIgnore
    private String roleType;

    @ExcelIgnore
    private String prorationBasis;

    // --- Getters / Setters ---

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getMajorCategory() { return majorCategory; }
    public void setMajorCategory(String majorCategory) { this.majorCategory = majorCategory; }

    public BigDecimal getBaseValue() { return baseValue; }
    public void setBaseValue(BigDecimal baseValue) { this.baseValue = baseValue; }

    public Long getStudentCount() { return studentCount; }
    public void setStudentCount(Long studentCount) { this.studentCount = studentCount; }

    public Long getRepeatOrder() { return repeatOrder; }
    public void setRepeatOrder(Long repeatOrder) { this.repeatOrder = repeatOrder; }

    public BigDecimal getRepeatCoef() { return repeatCoef; }
    public void setRepeatCoef(BigDecimal repeatCoef) { this.repeatCoef = repeatCoef; }

    public BigDecimal getK1() { return k1; }
    public void setK1(BigDecimal k1) { this.k1 = k1; }

    public BigDecimal getQ1() { return q1; }
    public void setQ1(BigDecimal q1) { this.q1 = q1; }

    public BigDecimal getQ2() { return q2; }
    public void setQ2(BigDecimal q2) { this.q2 = q2; }

    public BigDecimal getQ3() { return q3; }
    public void setQ3(BigDecimal q3) { this.q3 = q3; }

    public BigDecimal getN() { return n; }
    public void setN(BigDecimal n) { this.n = n; }

    public BigDecimal getOtherCoef() { return otherCoef; }
    public void setOtherCoef(BigDecimal otherCoef) { this.otherCoef = otherCoef; }

    public BigDecimal getCalculatedWorkload() { return calculatedWorkload; }
    public void setCalculatedWorkload(BigDecimal calculatedWorkload) { this.calculatedWorkload = calculatedWorkload; }

    public String getCoefRemark() { return coefRemark; }
    public void setCoefRemark(String coefRemark) { this.coefRemark = coefRemark; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getOverLimit() { return overLimit; }
    public void setOverLimit(Integer overLimit) { this.overLimit = overLimit; }

    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }

    public String getProrationBasis() { return prorationBasis; }
    public void setProrationBasis(String prorationBasis) { this.prorationBasis = prorationBasis; }
}
