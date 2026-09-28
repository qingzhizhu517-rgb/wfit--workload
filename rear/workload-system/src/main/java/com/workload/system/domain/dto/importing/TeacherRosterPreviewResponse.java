package com.workload.system.domain.dto.importing;

import java.util.List;
import java.util.Map;

/**
 * 教师花名册字段白名单预检结果（Task 6，preview-only）。
 *
 * <p>与统一 {@link ImportPreviewResponse} 不同：本响应<b>不携带 batchId/confirmToken</b>，
 * 因为花名册本轮只做字段参考与异常报告，<b>不开放确认写入真实人员</b>，也不落 {@code biz_import_batch}/
 * {@code biz_import_row} 暂存。它承载字段映射统计、异常队列与脱敏后的逐行视图。</p>
 *
 * <p><b>脱敏保证：</b>{@code rows} 与所有统计均只含白名单字段；身份证/住址等敏感列
 * 只有其<b>表头名</b>出现在 {@link #sensitiveHeaders()} 中作风险提示，敏感<b>值</b>绝不出现在本响应任何位置。</p>
 *
 * @param totalRows             数据行总数
 * @param readyCount            可导入（映射齐全）行数
 * @param blockedCount          阻断行数（重复工号/未知职称/待决人员性质/缺必填）
 * @param uniqueUserCodes       去重后的非空工号数量
 * @param duplicateCodes        重复出现的工号清单
 * @param deptCounts            按「系」聚合的行数
 * @param employmentTypeCounts  按「岗位」聚合的行数
 * @param unresolvedTitles      未能自动映射的职称清单（如 无 / 讲师待遇）
 * @param unresolvedNatures     未能自动映射人员性质的岗位清单（如 教师兼行政 / 行政）
 * @param sensitiveHeaders      检测到的敏感列表头名（仅名不含值）
 * @param rows                  脱敏逐行视图（READY/BLOCKED + 错误信息）
 * @param warnings              非阻断性提示（含「本次不导入真实教师」声明与重复工号定位）
 *
 * @author wflg
 */
public record TeacherRosterPreviewResponse(int totalRows, int readyCount, int blockedCount,
        int uniqueUserCodes, List<String> duplicateCodes, Map<String, Integer> deptCounts,
        Map<String, Integer> employmentTypeCounts, List<String> unresolvedTitles,
        List<String> unresolvedNatures, List<String> sensitiveHeaders, List<ImportRowView> rows,
        List<String> warnings)
{
}
