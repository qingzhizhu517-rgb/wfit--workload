package com.workload.system.domain.dto;

import java.math.BigDecimal;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;

/**
 * 绩效酬金汇总导出 DTO（附件2：教师教育教学工作量绩效酬金统计表）
 *
 * @author wflg
 */
public class PaySummaryExportDTO
{
    @ExcelProperty("教师ID")
    @ColumnWidth(10)
    private Long userId;

    @ExcelProperty("学年学期")
    @ColumnWidth(14)
    private String semester;

    @ExcelProperty("职称")
    @ColumnWidth(8)
    private String title;

    @ExcelProperty("总工作量")
    @ColumnWidth(10)
    private BigDecimal totalWorkload;

    @ExcelProperty("额定工作量")
    @ColumnWidth(10)
    private BigDecimal ratedWorkload;

    @ExcelProperty("超额工作量")
    @ColumnWidth(10)
    private BigDecimal excessWorkload;

    @ExcelProperty("单位酬金(元)")
    @ColumnWidth(12)
    private BigDecimal payRate;

    @ExcelProperty("绩效酬金(元)")
    @ColumnWidth(12)
    private BigDecimal performancePay;

    // --- Getters / Setters ---

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

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

    public BigDecimal getPerformancePay() { return performancePay; }
    public void setPerformancePay(BigDecimal performancePay) { this.performancePay = performancePay; }
}
