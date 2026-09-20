package com.workload.system.service;

import com.workload.system.domain.dto.importing.ImportConfirmRequest;

/**
 * 导入预检批次状态机与暂存协调服务。
 *
 * <p>统一约束两阶段导入的批次状态转移，只允许：</p>
 * <ul>
 *   <li>{@code 0 解析中 → 1 待确认}（预检完成，{@link #markPrechecked}）</li>
 *   <li>{@code 1 待确认 → 2 已导入}（确认成功，{@link #markConfirmed}）</li>
 *   <li>{@code 1 待确认 → 4 失败}（确认失败，{@link #markFailed}）</li>
 * </ul>
 *
 * <p>确认前必须先通过 {@link #assertConfirmable}：批次须存在、处于待确认(1)、无阻断行、
 * 且确认令牌与存储哈希匹配。状态变更均通过条件 UPDATE（{@code where status=期望值}）实现，
 * 影响行数≠1 视为并发冲突。</p>
 *
 * @author wflg
 */
public interface ImportPreviewStore
{
    /**
     * 校验批次是否可确认。批次不存在、非待确认、存在阻断行或令牌不匹配均抛出 {@link com.workload.common.exception.ServiceException}。
     *
     * @param request 确认请求（含批次ID与明文确认令牌）
     */
    void assertConfirmable(ImportConfirmRequest request);

    /**
     * 预检完成，将批次从解析中(0)推进到待确认(1)。
     *
     * @param batchId 批次ID
     * @return 受影响行数（应为 1）
     */
    int markPrechecked(Long batchId);

    /**
     * 确认成功，将批次从待确认(1)固化到已导入(2)并落确认时间。
     *
     * @param batchId 批次ID
     * @return 受影响行数（应为 1，否则视为并发冲突抛异常）
     */
    int markConfirmed(Long batchId);

    /**
     * 确认失败，将批次从待确认(1)置为失败(4)。
     *
     * @param batchId 批次ID
     * @return 受影响行数（应为 1）
     */
    int markFailed(Long batchId);
}
