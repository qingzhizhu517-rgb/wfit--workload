package com.workload.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.core.domain.BaseEntity;

/**
 * 导入逐行暂存对象 biz_import_row
 *
 * <p>两阶段导入的暂存载体：预检阶段把源文件数据页逐行落此表，确认阶段按 {@code row_no}
 * 顺序回放写入业务表。{@code rawJson} 仅保存 DTO 白名单序列化内容，绝不包含未映射的敏感列。</p>
 *
 * @author wflg
 * @date 2026-09-12
 */
public class BizImportRow extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 所属批次ID */
    private Long batchId;

    /** 源文件物理行号 */
    private Integer rowNo;

    /** 原始行 JSON（DTO 白名单序列化，无敏感列） */
    private String rawJson;

    /** 规范化后行 JSON */
    private String normalizedJson;

    /** 行状态 READY/BLOCKED/IMPORTED/SKIPPED/FAILED */
    private String status;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMessage;

    /** 写入后的业务目标记录ID */
    private Long targetId;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setBatchId(Long batchId)
    {
        this.batchId = batchId;
    }

    public Long getBatchId()
    {
        return batchId;
    }

    public void setRowNo(Integer rowNo)
    {
        this.rowNo = rowNo;
    }

    public Integer getRowNo()
    {
        return rowNo;
    }

    public void setRawJson(String rawJson)
    {
        this.rawJson = rawJson;
    }

    public String getRawJson()
    {
        return rawJson;
    }

    public void setNormalizedJson(String normalizedJson)
    {
        this.normalizedJson = normalizedJson;
    }

    public String getNormalizedJson()
    {
        return normalizedJson;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setErrorCode(String errorCode)
    {
        this.errorCode = errorCode;
    }

    public String getErrorCode()
    {
        return errorCode;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setTargetId(Long targetId)
    {
        this.targetId = targetId;
    }

    public Long getTargetId()
    {
        return targetId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("batchId", getBatchId())
            .append("rowNo", getRowNo())
            .append("rawJson", getRawJson())
            .append("normalizedJson", getNormalizedJson())
            .append("status", getStatus())
            .append("errorCode", getErrorCode())
            .append("errorMessage", getErrorMessage())
            .append("targetId", getTargetId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
