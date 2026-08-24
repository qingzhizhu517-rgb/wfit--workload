package com.workload.common.utils.excel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
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

    /** 批处理器（每满 batchSize 行调用一次）；与 rowConsumer 二选一 */
    private final Consumer<List<T>> batchProcessor;

    /**
     * 行处理器（逐行回调，入参为 数据 + 物理行号(0基,含表头)）。
     * 非空时走逐行模式：不缓冲、不分批，抛异常即记为该行错误，与解析异常(onException)
     * 使用同一 ImportResult 和同一物理行号口径，避免行号错位与错误丢失。
     */
    private final BiConsumer<T, Integer> rowConsumer;

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
        this.rowConsumer = null;
    }

    /**
     * 构造（逐行模式）：每行独立回调，抛异常即记为该行错误。
     *
     * @param rowConsumer 行处理器（数据, 物理行号）
     */
    public ExcelImportListener(BiConsumer<T, Integer> rowConsumer)
    {
        this.batchProcessor = null;
        this.rowConsumer = rowConsumer;
        this.batchSize = DEFAULT_BATCH_SIZE;
    }

    /**
     * 逐行读取回调
     */
    @Override
    public void invoke(T data, AnalysisContext context)
    {
        currentRow++;
        if (rowConsumer != null)
        {
            int physicalRow = context.readRowHolder().getRowIndex();
            try
            {
                rowConsumer.accept(data, physicalRow);
                result.addSuccess();
            }
            catch (Exception e)
            {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.warn("第 {} 行处理失败: {}", physicalRow, msg);
                result.addError(physicalRow, msg);
            }
            return;
        }

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
        if (rowConsumer == null && !batch.isEmpty())
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
