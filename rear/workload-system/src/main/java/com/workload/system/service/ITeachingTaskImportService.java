package com.workload.system.service;

import java.io.InputStream;
import java.math.BigDecimal;

import com.workload.common.utils.excel.ImportResult;
import com.workload.system.domain.dto.TeachingTaskImportDTO;

/**
 * 教学任务 Excel 导入服务
 *
 * @author wflg
 */
public interface ITeachingTaskImportService
{
    /**
     * 流式导入教学任务 Excel（避免大文件全量入内存），支持分类模板锁定类别。
     * <p>
     * 边读边入库：EasyExcel 逐行回调，每行独立事务处理。单行失败仅回滚该行并记入错误，
     * 单元格解析异常与业务异常共用同一结果与物理行号，不影响其他行。
     *
     * @param inputStream  Excel 输入流
     * @param fileName     原始文件名
     * @param templateType 模板类别：{@code ALL} 放行 G1~G6；{@code G1/G2/G3} 仅接受对应类别行，
     *                     其余行在写库前拒绝。{@code null}/空按 {@code ALL} 处理（向后兼容）。
     * @return 导入结果
     */
    ImportResult importTeachingTasksStreaming(InputStream inputStream, String fileName, String templateType);

    /**
     * 为单行创建教学任务 + 工作量明细 + 计算（通用模板，等价于 {@code templateType=ALL}）
     *
     * @param dto      行数据
     * @param batchNo  批次号
     * @return 计算后的工作量值
     */
    BigDecimal processSingleRow(TeachingTaskImportDTO dto, String batchNo);

    /**
     * 为单行创建教学任务 + 工作量明细 + 计算，并按模板类别在写库前校验行类别。
     *
     * @param dto          行数据
     * @param batchNo      批次号
     * @param templateType 模板类别（ALL/G1/G2/G3）
     * @return 计算后的工作量值
     */
    BigDecimal processSingleRow(TeachingTaskImportDTO dto, String batchNo, String templateType);
}
