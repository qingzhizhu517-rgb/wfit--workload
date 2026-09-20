package com.workload.system.domain.dto.importing;

import java.util.List;

/**
 * 导入预检结果。
 *
 * <p>供前端向导展示可导入/被阻断的行数与逐行明细，并携带一次性 {@code confirmToken}
 * 供确认阶段回传。仅在 {@code blockedCount == 0} 时允许确认。</p>
 *
 * @param batchId         暂存批次主键
 * @param importType      导入类型
 * @param templateVersion 模板版本 V1/V2/ROSTER_V1/ROLE_V1
 * @param totalCount      数据页总行数
 * @param readyCount      可导入行数
 * @param blockedCount    阻断行数
 * @param rows            逐行视图（白名单字段）
 * @param warnings        非阻断性提示
 * @param confirmToken    一次性确认令牌明文（仅本次响应返回，服务端只存其哈希）
 *
 * @author wflg
 */
public record ImportPreviewResponse(Long batchId, String importType, String templateVersion,
        int totalCount, int readyCount, int blockedCount, List<ImportRowView> rows,
        List<String> warnings, String confirmToken)
{
}
