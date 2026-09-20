package com.workload.system.domain.dto.importing;

/**
 * 导入确认请求。
 *
 * @param batchId       待确认批次主键
 * @param confirmToken  预检返回的一次性确认令牌（明文，服务端与批次存储的哈希比对）
 * @param updateSupport 是否允许对既有目标记录执行更新（否则重复行按 SKIPPED 处理）
 *
 * @author wflg
 */
public record ImportConfirmRequest(Long batchId, String confirmToken, boolean updateSupport)
{
}
