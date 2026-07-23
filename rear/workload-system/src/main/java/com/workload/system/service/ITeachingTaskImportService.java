package com.workload.system.service;

import java.math.BigDecimal;
import java.util.List;

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
     * 导入教学任务 Excel
     * <p>
     * 流程：解析行 → 查找教师 → 创建教学任务 → 创建工作量明细 → 调用策略计算
     *
     * @param rows   Excel 行数据
     * @param fileName 原始文件名
     * @return 导入结果
     */
    ImportResult importTeachingTasks(List<TeachingTaskImportDTO> rows, String fileName);

    /**
     * 为单行创建教学任务 + 工作量明细 + 计算
     *
     * @param dto      行数据
     * @param batchNo  批次号
     * @return 计算后的工作量值
     */
    BigDecimal processSingleRow(TeachingTaskImportDTO dto, String batchNo);
}
