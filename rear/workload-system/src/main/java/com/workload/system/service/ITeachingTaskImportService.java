package com.workload.system.service;

import java.io.InputStream;
import java.math.BigDecimal;

import com.workload.common.utils.excel.ImportResult;
import com.workload.system.domain.dto.TeachingTaskImportDTO;
import com.workload.system.domain.dto.importing.ImportConfirmRequest;
import com.workload.system.domain.dto.importing.ImportPreviewResponse;

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

    /**
     * 两阶段导入 · 预检：只解析「导入数据」Sheet（V2）或旧单 Sheet（V1 转换），逐行校验后
     * 落 {@code biz_import_row} 暂存，创建 {@code biz_import_batch} 并推进到待确认(1)。
     * <p>
     * 预检阶段<b>绝不写</b>教学任务/工作量主表/子表；说明页示例绝不入库、不计入 readyCount。
     * V1 文件用 {@link com.workload.system.domain.dto.TeachingTaskImportV1DTO} 读旧表头，
     * 把 {@code baseValue} 按类别转成 v2 语义字段（G1/G2→计划学时，G3→天数，G6→周数，G4→学分，
     * G5 用选课人数）后走同一校验，避免旧文件全行被拒。
     *
     * @param input           源文件输入流
     * @param fileName        原始文件名
     * @param templateVersion 模板版本 V1/V2（空按 V2）
     * @return 预检结果（含 batchId、readyCount/blockedCount、逐行视图与一次性确认令牌）
     */
    ImportPreviewResponse preview(InputStream input, String fileName, String templateVersion);

    /**
     * 两阶段导入 · 预检（分类模板版）：在通用预检基础上，按 {@code templateType} 锁定类别。
     * <p>
     * {@code templateType=ALL} 放行 G1~G6；{@code G1/G2/G3} 时，数据页中类别不符的行判
     * {@code BLOCKED}（错误信息含「模板类别 Gx」），不计入 readyCount。批次落
     * {@code template_category}，供确认阶段二次闸门读取。{@code null}/空按 {@code ALL}。
     *
     * @param input           源文件输入流
     * @param fileName        原始文件名
     * @param templateVersion 模板版本 V1/V2（空按 V2）
     * @param templateType    模板类别 ALL/G1/G2/G3（空按 ALL）
     * @return 预检结果
     */
    ImportPreviewResponse preview(InputStream input, String fileName, String templateVersion, String templateType);

    /**
     * 两阶段导入 · 确认：校验批次可确认（待确认+令牌匹配+0 阻断）后，按 {@code row_no} 顺序
     * 回放 {@link #processSingleRow}（templateType 取批次 {@code template_category}，
     * 保留冻结 guard/快照/afterCalculated；即便预检漏判，确认阶段仍按批次类别拦错类行）。
     * <p>
     * 幂等键为（工号,学期,课程代码,班级,类别）：命中则该行 SKIPPED（不静默覆盖），
     * 成功 IMPORTED、异常 FAILED；最后把批次固化到已导入(2)。
     *
     * @param request 确认请求（批次ID + 确认令牌 + 是否允许更新）
     * @return 导入结果（成功/跳过/失败计数与错误明细）
     */
    ImportResult confirm(ImportConfirmRequest request);
}
