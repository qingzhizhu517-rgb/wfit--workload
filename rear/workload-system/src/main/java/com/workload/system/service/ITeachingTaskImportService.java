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
     * 流式导入教学任务 Excel（避免大文件全量入内存）
     * <p>
     * 边读边入库：EasyExcel 逐行回调，每行独立事务处理。单行失败仅回滚该行并记入错误，
     * 单元格解析异常与业务异常共用同一结果与物理行号，不影响其他行。
     *
     * @param inputStream Excel 输入流
     * @param fileName    原始文件名
     * @return 导入结果
     */
    ImportResult importTeachingTasksStreaming(InputStream inputStream, String fileName);

    /**
     * 为单行创建教学任务 + 工作量明细 + 计算
     *
     * @param dto      行数据
     * @param batchNo  批次号
     * @return 计算后的工作量值
     */
    BigDecimal processSingleRow(TeachingTaskImportDTO dto, String batchNo);
}
