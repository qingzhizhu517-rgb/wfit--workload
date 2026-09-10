package com.workload.system.domain.dto;

import java.math.BigDecimal;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;

/**
 * 绩效酬金汇总导出 DTO（附件2：教师教育教学工作量绩效酬金统计表）
 * <p>
 * 数据源为 biz_workload_summary LEFT JOIN biz_pay_record：
 * 绩效酬金（course_hour_pay）、其他酬金（other_pay_total）、总金额（total_pay）
 * 一律取酬金表的落库值，与「酬金记录」页、教师端看到的金额同源，避免两处口径不一致。
 * 未核算酬金的教师仍会出现在表内，酬金三列为空——表示「待核算」而不是 0 元。
 * <p>
 * 「超额工作量」是 总工作量−额定，未封顶；触顶（>200%）时它并不是计酬基数，
 * 故另出「计酬超额工作量」= min(总工作量, CAP_200PCT)−额定，两列并排避免
 * 「超额×单位酬金 ≠ 绩效酬金」被当成算错。金额为 0 的原因（非专任/未核算/触顶）
 * 一律写进「备注」列，回答「这个人为什么没钱」。
 *
 * @author wflg
 */
public class PaySummaryExportDTO
{
    @ExcelProperty("工号")
    @ColumnWidth(14)
    private String userCode;

    @ExcelProperty("姓名")
    @ColumnWidth(12)
    private String userName;

    @ExcelProperty("院部")
    @ColumnWidth(18)
    private String deptName;

    @ExcelProperty("学年学期")
    @ColumnWidth(14)
    private String semester;

    @ExcelProperty("职称")
    @ColumnWidth(10)
    private String title;

    /** 人员性质：仅「专任」（或档案缺失）计发绩效酬金，外聘/校企/银龄等不计 */
    @ExcelProperty("人员性质")
    @ColumnWidth(12)
    private String teacherNature;

    @ExcelProperty("总工作量")
    @ColumnWidth(10)
    private BigDecimal totalWorkload;

    @ExcelProperty("额定工作量")
    @ColumnWidth(12)
    private BigDecimal ratedWorkload;

    @ExcelProperty("超额工作量")
    @ColumnWidth(12)
    private BigDecimal excessWorkload;

    @ExcelProperty("是否触顶")
    @ColumnWidth(10)
    private String cappedLabel;

    /** 计酬超额工作量：min(总工作量, 200%上限) − 额定，下限 0，即绩效酬金的实际计算基数 */
    @ExcelProperty("计酬超额工作量")
    @ColumnWidth(15)
    private BigDecimal payableExcess;

    @ExcelProperty("单位酬金(元)")
    @ColumnWidth(13)
    private BigDecimal payRate;

    /** 绩效酬金：取 biz_pay_record.course_hour_pay，未核算酬金时为空 */
    @ExcelProperty("绩效酬金(元)")
    @ColumnWidth(13)
    private BigDecimal coursePay;

    /** 其他酬金合计：A~G 各档之和，取 biz_pay_record.other_pay_total */
    @ExcelProperty("其他酬金(元)")
    @ColumnWidth(13)
    private BigDecimal otherPayTotal;

    /** 总金额：绩效 + 其他，四舍五入取整，取 biz_pay_record.total_pay */
    @ExcelProperty("总金额(元)")
    @ColumnWidth(12)
    private Long totalPay;

    @ExcelProperty("审批状态")
    @ColumnWidth(14)
    private String statusLabel;

    /** 备注：金额为 0 或与超额不匹配时的原因说明，由 Controller 拼装 */
    @ExcelProperty("备注")
    @ColumnWidth(40)
    private String remark;

    /** 是否触顶原始值（0/1），仅用于拼备注与「是否触顶」列，不单独导出 */
    @ExcelIgnore
    private Integer capped;

    // --- Getters / Setters ---

    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getTotalWorkload() { return totalWorkload; }
    public void setTotalWorkload(BigDecimal totalWorkload) { this.totalWorkload = totalWorkload; }

    public BigDecimal getRatedWorkload() { return ratedWorkload; }
    public void setRatedWorkload(BigDecimal ratedWorkload) { this.ratedWorkload = ratedWorkload; }

    public BigDecimal getExcessWorkload() { return excessWorkload; }
    public void setExcessWorkload(BigDecimal excessWorkload) { this.excessWorkload = excessWorkload; }

    public BigDecimal getPayRate() { return payRate; }
    public void setPayRate(BigDecimal payRate) { this.payRate = payRate; }

    public BigDecimal getCoursePay() { return coursePay; }
    public void setCoursePay(BigDecimal coursePay) { this.coursePay = coursePay; }

    public BigDecimal getOtherPayTotal() { return otherPayTotal; }
    public void setOtherPayTotal(BigDecimal otherPayTotal) { this.otherPayTotal = otherPayTotal; }

    public Long getTotalPay() { return totalPay; }
    public void setTotalPay(Long totalPay) { this.totalPay = totalPay; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }

    public String getTeacherNature() { return teacherNature; }
    public void setTeacherNature(String teacherNature) { this.teacherNature = teacherNature; }

    public String getCappedLabel() { return cappedLabel; }
    public void setCappedLabel(String cappedLabel) { this.cappedLabel = cappedLabel; }

    public BigDecimal getPayableExcess() { return payableExcess; }
    public void setPayableExcess(BigDecimal payableExcess) { this.payableExcess = payableExcess; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Integer getCapped() { return capped; }
    public void setCapped(Integer capped) { this.capped = capped; }
}
