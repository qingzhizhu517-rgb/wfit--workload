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
     * 逐行流式读取指定名称的 Sheet：仅解析名为 {@code sheetName} 的工作表，每行独立回调
     * （数据, 物理行号）。用于两阶段导入只读「导入数据」页、绝不解析「示例与说明」页。
     * <p>
     * sheetName 为空时回退到第一个 Sheet（兼容旧单 Sheet 文件）。行处理器与单元格解析异常
     * 共用同一 ImportResult 与物理行号口径。
     *
     * @param inputStream 输入流
     * @param clazz       行数据 DTO 类型
     * @param sheetName   目标 Sheet 名（空则读第一个 Sheet）
     * @param rowConsumer 行处理器（数据, 物理行号 0基含表头）
     * @param <T>         DTO 类型
     * @return 导入结果
     */
    public static <T> ImportResult readEachRowOfSheet(InputStream inputStream, Class<T> clazz,
                                                      String sheetName, BiConsumer<T, Integer> rowConsumer)
    {
        ExcelImportListener<T> listener = new ExcelImportListener<>(rowConsumer);
        ExcelReaderBuilder builder = EasyExcel.read(inputStream, clazz, listener);
        if (sheetName != null && !sheetName.isBlank())
        {
            builder.sheet(sheetName).doRead();
        }
        else
        {
            builder.sheet().doRead();
        }
        return listener.getResult();
    }

    /**
     * 逐行读取指定名称 Sheet，找不到该名时回退到第一个 Sheet（兼容旧单 Sheet 文件）。
     * <p>
     * 入参为字节数组而非流：命中失败需以全新流重试第一个 Sheet，字节数组可重复消费。
     * 用于 /importExcel 默认走 V2 却收到旧单 Sheet 文件（无「导入数据」页）时不再整批失败。
     *
     * @param bytes       Excel 文件字节
     * @param clazz       行数据 DTO 类型
     * @param sheetName   优先解析的 Sheet 名（空则直接读第一个）
     * @param rowConsumer 行处理器（数据, 物理行号 0基含表头）
     * @param <T>         DTO 类型
     * @return 导入结果
     */
    public static <T> ImportResult readEachRowOfSheetOrFirst(byte[] bytes, Class<T> clazz,
                                                             String sheetName, BiConsumer<T, Integer> rowConsumer)
    {
        if (sheetName == null || sheetName.isBlank())
        {
            return readEachRow(new java.io.ByteArrayInputStream(bytes), clazz, rowConsumer);
        }
        // 先按名解析；命中不到（EasyExcel 抛异常）用新流回退到第一个 Sheet
        boolean named = false;
        try (com.alibaba.excel.ExcelReader reader = EasyExcel.read(new java.io.ByteArrayInputStream(bytes)).build())
        {
            named = reader.excelExecutor().sheetList().stream()
                    .anyMatch(s -> sheetName.equals(s.getSheetName()));
        }
        catch (Exception ignored)
        {
            // 读取 Sheet 列表失败：退回第一个 Sheet
        }
        return named
                ? readEachRowOfSheet(new java.io.ByteArrayInputStream(bytes), clazz, sheetName, rowConsumer)
                : readEachRow(new java.io.ByteArrayInputStream(bytes), clazz, rowConsumer);
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
