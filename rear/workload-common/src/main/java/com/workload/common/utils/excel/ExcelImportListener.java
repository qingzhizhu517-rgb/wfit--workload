package com.workload.common.utils.excel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;

/**
 * EasyExcel 通用导入监听器基类
 * <p>
 * 用法：
 * <pre>
 * ExcelImportListener&lt;MyDto&gt; listener = new ExcelImportListener&lt;&gt;(batch -&gt; {
 *     myService.batchInsert(batch);
 * });
 * EasyExcel.read(file.getInputStream(), MyDto.class, listener).sheet().doRead();
 * ImportResult result = listener.getResult();
 * </pre>
 *
 * @param <T> 行数据 DTO 类型
 * @author wflg
 */
public class ExcelImportListener<T> extends AnalysisEventListener<T>
{
    private static final Logger log = LoggerFactory.getLogger(ExcelImportListener.class);

    /** 默认批次大小 */
    private static final int DEFAULT_BATCH_SIZE = 100;

    /** 批次大小 */
    private final int batchSize;

    /** 当前批次缓冲 */
    private List<T> batch = new ArrayList<>();

    /** 批处理器（每满 batchSize 行调用一次） */
    private final Consumer<List<T>> batchProcessor;

    /** 导入结果 */
    private final ImportResult result = new ImportResult();

    /** 当前行号（从 1 开始，含表头） */
    private int currentRow = 0;

    /**
     * 构造（默认 100 行一批）
     *
     * @param batchProcessor 批处理器
     */
    public ExcelImportListener(Consumer<List<T>> batchProcessor)
    {
        this(batchProcessor, DEFAULT_BATCH_SIZE);
    }

    /**
     * 构造
     *
     * @param batchProcessor 批处理器
     * @param batchSize      批次大小
     */
    public ExcelImportListener(Consumer<List<T>> batchProcessor, int batchSize)
    {
        this.batchProcessor = batchProcessor;
        this.batchSize = batchSize;
    }

    /**
     * 逐行读取回调
     */
    @Override
    public void invoke(T data, AnalysisContext context)
    {
        currentRow++;
        batch.add(data);

        if (batch.size() >= batchSize)
        {
            processBatch();
        }
    }

    /**
     * 读完所有行后的收尾
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context)
    {
        if (!batch.isEmpty())
        {
            processBatch();
        }
        log.info("Excel 导入完成: {}", result);
    }

    /**
     * 数据转换异常（单元格格式错误等）
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception
    {
        if (exception instanceof ExcelDataConvertException)
        {
            ExcelDataConvertException ex = (ExcelDataConvertException) exception;
            int rowIndex = ex.getRowIndex();
            int colIndex = ex.getColumnIndex();
            String msg = "第 " + rowIndex + " 行第 " + colIndex + " 列数据格式错误: " + exception.getMessage();
            log.warn(msg);
            result.addError(rowIndex, msg);
        }
        else
        {
            throw exception;
        }
    }

    /**
     * 处理当前批次
     */
    private void processBatch()
    {
        try
        {
            batchProcessor.accept(batch);
            result.setSuccessCount(result.getSuccessCount() + batch.size());
        }
        catch (Exception e)
        {
            log.error("批次处理异常: {}", e.getMessage(), e);
            // 批量失败时逐行标记
            for (int i = 0; i < batch.size(); i++)
            {
                result.addError(currentRow - batch.size() + i + 1, e.getMessage());
            }
        }
        finally
        {
            batch = new ArrayList<>();
        }
    }

    /**
     * 获取导入结果
     */
    public ImportResult getResult()
    {
        return result;
    }

    /**
     * 获取当前已读行数
     */
    public int getCurrentRow()
    {
        return currentRow;
    }
}
