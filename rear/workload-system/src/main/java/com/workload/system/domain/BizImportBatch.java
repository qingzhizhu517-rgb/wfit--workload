package com.workload.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * 导入批次记录对象 biz_import_batch
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizImportBatch extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 批次号 */
    @Excel(name = "批次号")
    private String batchNo;

    /** 教学任务/教师信息/岗位任职 */
    @Excel(name = "教学任务/教师信息/岗位任职")
    private String importType;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String fileName;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String fileUrl;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long totalCount;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long successCount;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long failCount;

    /** 0解析中/1待确认/2已导入/3已驳回/4失败 */
    @Excel(name = "0解析中/1待确认/2已导入/3已驳回/4失败")
    private Integer status;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String errorSummary;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setBatchNo(String batchNo) 
    {
        this.batchNo = batchNo;
    }

    public String getBatchNo() 
    {
        return batchNo;
    }

    public void setImportType(String importType) 
    {
        this.importType = importType;
    }

    public String getImportType() 
    {
        return importType;
    }

    public void setFileName(String fileName) 
    {
        this.fileName = fileName;
    }

    public String getFileName() 
    {
        return fileName;
    }

    public void setFileUrl(String fileUrl) 
    {
        this.fileUrl = fileUrl;
    }

    public String getFileUrl() 
    {
        return fileUrl;
    }

    public void setTotalCount(Long totalCount) 
    {
        this.totalCount = totalCount;
    }

    public Long getTotalCount() 
    {
        return totalCount;
    }

    public void setSuccessCount(Long successCount) 
    {
        this.successCount = successCount;
    }

    public Long getSuccessCount() 
    {
        return successCount;
    }

    public void setFailCount(Long failCount) 
    {
        this.failCount = failCount;
    }

    public Long getFailCount() 
    {
        return failCount;
    }

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    public void setErrorSummary(String errorSummary) 
    {
        this.errorSummary = errorSummary;
    }

    public String getErrorSummary() 
    {
        return errorSummary;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("batchNo", getBatchNo())
            .append("importType", getImportType())
            .append("fileName", getFileName())
            .append("fileUrl", getFileUrl())
            .append("totalCount", getTotalCount())
            .append("successCount", getSuccessCount())
            .append("failCount", getFailCount())
            .append("status", getStatus())
            .append("errorSummary", getErrorSummary())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
