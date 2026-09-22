package com.workload.system.service;

import java.io.InputStream;

import com.workload.common.utils.excel.ImportResult;
import com.workload.system.domain.dto.PositionWorkloadImportDTO;
import com.workload.system.domain.dto.importing.ImportConfirmRequest;
import com.workload.system.domain.dto.importing.ImportPreviewResponse;

/**
 * 本学期岗位减免工作量导入服务（两阶段：预检 / 确认）。
 *
 * <p>沿用统一导入框架：预检只解析数据页、零业务写入、逐行落 {@code biz_import_row}；
 * 确认按 {@code row_no} 顺序回放写 {@code biz_role_assignment}（{@code allowance_rate} 存本学期值）。
 * 不除以 2、不查岗位目录、不按日期折算。</p>
 *
 * @author wflg
 */
public interface IPositionWorkloadImportService
{
    /**
     * 预检：解析岗位减免工作簿（优先「导入数据」页，回退第一个 Sheet），逐行校验后落
     * {@code biz_import_row}，创建 {@code biz_import_batch} 并推进到待确认(1)。
     * <p>预检阶段绝不写 {@code biz_role_assignment}。</p>
     *
     * @param input           源文件输入流
     * @param fileName        原始文件名
     * @param templateVersion 模板版本（空按 V1）
     * @return 预检结果（含 batchId、readyCount/blockedCount、逐行视图与一次性确认令牌）
     */
    ImportPreviewResponse preview(InputStream input, String fileName, String templateVersion);

    /**
     * 确认：校验批次可确认（待确认+令牌匹配+0 阻断）后，按 {@code row_no} 顺序回放。
     * <p>幂等键为（工号,学期）：命中则该行 SKIPPED；成功 IMPORTED、异常 FAILED；
     * 写每条前经 {@link com.workload.system.calc.WorkloadWriteGuard} 校验冻结状态，
     * 汇总处于审批中/已完结(1/2)则该行失败，绝不写入。</p>
     *
     * @param request 确认请求（批次ID + 确认令牌 + 是否允许更新）
     * @return 导入结果（成功/跳过/失败计数与错误明细）
     */
    ImportResult confirm(ImportConfirmRequest request);

    /**
     * 单行写入：查教师 → 校验冻结 → 写 {@code biz_role_assignment}（status=1 有效）。
     * 每行独立事务，经 AOP 代理调用以使 {@code @Transactional} 与冻结锁生效。
     *
     * @param dto     规范化行
     * @param batchNo 来源批次号（落 source_batch_id）
     */
    void processSingleRow(PositionWorkloadImportDTO dto, String batchNo);
}
