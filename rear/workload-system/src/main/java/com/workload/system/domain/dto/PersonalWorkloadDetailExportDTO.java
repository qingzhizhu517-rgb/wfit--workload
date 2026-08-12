package com.workload.system.domain.dto;

import java.math.BigDecimal;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;

/**
 * 个人工作量明细导出 DTO（附件1：教师教育教学工作量统计表）
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

    @ExcelProperty("核算工作量")
    @ColumnWidth(12)
    private BigDecimal calculatedWorkload;

    @ExcelProperty("数据来源")
    @ColumnWidth(10)
    private String sourceType;

    @ExcelProperty("状态")
    @ColumnWidth(8)
    private Integer status;

    // --- Getters / Setters ---

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public BigDecimal getCalculatedWorkload() { return calculatedWorkload; }
    public void setCalculatedWorkload(BigDecimal calculatedWorkload) { this.calculatedWorkload = calculatedWorkload; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
