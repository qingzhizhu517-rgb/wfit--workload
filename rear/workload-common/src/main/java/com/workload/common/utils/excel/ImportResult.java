package com.workload.common.utils.excel;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入统一返回结果
 *
 * @author wflg
 */
public class ImportResult
{
    /** 成功行数 */
    private int successCount = 0;

    /** 失败行数 */
    private int failCount = 0;

    /** 跳过行数（重复等） */
    private int skipCount = 0;

    /** 错误详情 */
    private final List<ErrorRow> errors = new ArrayList<>();

    /** 导入批次 ID（可选） */
    private Long batchId;

    public void addSuccess()
    {
        this.successCount++;
    }

    public void addSkip()
    {
        this.skipCount++;
    }

    public void addError(int rowIndex, String message)
    {
        this.failCount++;
        this.errors.add(new ErrorRow(rowIndex, message));
    }

    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }

    public int getTotalCount()
    {
        return successCount + failCount + skipCount;
    }

    // --- Getters / Setters ---

    public int getSuccessCount()
    {
        return successCount;
    }

    public void setSuccessCount(int successCount)
    {
        this.successCount = successCount;
    }

    public int getFailCount()
    {
        return failCount;
    }

    public void setFailCount(int failCount)
    {
        this.failCount = failCount;
    }

    public int getSkipCount()
    {
        return skipCount;
    }

    public void setSkipCount(int skipCount)
    {
        this.skipCount = skipCount;
    }

    public List<ErrorRow> getErrors()
    {
        return errors;
    }

    public Long getBatchId()
    {
        return batchId;
    }

    public void setBatchId(Long batchId)
    {
        this.batchId = batchId;
    }

    /**
     * 错误行信息
     */
    public static class ErrorRow
    {
        private final int rowIndex;
        private final String message;

        public ErrorRow(int rowIndex, String message)
        {
            this.rowIndex = rowIndex;
            this.message = message;
        }

        public int getRowIndex()
        {
            return rowIndex;
        }

        public String getMessage()
        {
            return message;
        }

        @Override
        public String toString()
        {
            return "第 " + rowIndex + " 行: " + message;
        }
    }

    @Override
    public String toString()
    {
        return "ImportResult{success=" + successCount
                + ", fail=" + failCount
                + ", skip=" + skipCount
                + ", errors=" + errors.size() + "}";
    }
}
