package com.workload.common.utils.excel;

import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;

/**
 * EasyExcel 读取工具类
 *
 * @author wflg
 */
public class ExcelReadUtil
{
    /**
     * 读取 Excel 第一个 Sheet
     *
     * @param inputStream   输入流
     * @param clazz         行数据 DTO 类型
     * @param batchProcessor 批处理器
     * @param <T>           DTO 类型
     * @return 导入结果
     */
    public static <T> ImportResult read(InputStream inputStream, Class<T> clazz, Consumer<List<T>> batchProcessor)
    {
        return read(inputStream, clazz, batchProcessor, 100);
    }

    /**
     * 读取 Excel 第一个 Sheet（自定义批次大小）
     */
    public static <T> ImportResult read(InputStream inputStream, Class<T> clazz,
                                         Consumer<List<T>> batchProcessor, int batchSize)
    {
        ExcelImportListener<T> listener = new ExcelImportListener<>(batchProcessor, batchSize);
        EasyExcel.read(inputStream, clazz, listener).sheet().doRead();
        return listener.getResult();
    }

    /**
     * 读取 Excel（跳过表头行数）
     *
     * @param inputStream    输入流
     * @param clazz          行数据 DTO 类型
     * @param batchProcessor 批处理器
     * @param headRowNumber  表头行数（默认 1）
     * @param <T>            DTO 类型
     * @return 导入结果
     */
    public static <T> ImportResult read(InputStream inputStream, Class<T> clazz,
                                         Consumer<List<T>> batchProcessor, int batchSize, int headRowNumber)
    {
        ExcelImportListener<T> listener = new ExcelImportListener<>(batchProcessor, batchSize);
        EasyExcel.read(inputStream, clazz, listener)
                .headRowNumber(headRowNumber)
                .sheet()
                .doRead();
        return listener.getResult();
    }

    /**
     * 逐行流式读取：每行独立回调（数据, 物理行号），不缓冲不分批。
     * 行处理器抛异常即记为该行错误；单元格解析异常也记入同一 ImportResult，
     * 与业务错误共用物理行号口径，避免错误丢失与行号错位。
     *
     * @param inputStream 输入流
     * @param clazz       行数据 DTO 类型
     * @param rowConsumer 行处理器（数据, 物理行号 0基含表头）
     * @param <T>         DTO 类型
     * @return 导入结果（成功/失败计数 + 错误明细）
     */
    public static <T> ImportResult readEachRow(InputStream inputStream, Class<T> clazz,
                                               BiConsumer<T, Integer> rowConsumer)
    {
        ExcelImportListener<T> listener = new ExcelImportListener<>(rowConsumer);
        EasyExcel.read(inputStream, clazz, listener).sheet().doRead();
        return listener.getResult();
    }

    /**
     * 获取 ExcelReaderBuilder（用于 doReadSync 等灵活配置）
     *
     * @param inputStream 输入流
     * @return ExcelReaderBuilder
     */
    public static ExcelReaderBuilder builder(InputStream inputStream)
    {
        return EasyExcel.read(inputStream);
    }
}
