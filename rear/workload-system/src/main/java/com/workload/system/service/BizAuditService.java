package com.workload.system.service;

import java.util.Map;

/**
 * 工作量审批领域服务
 * <p>
 * 审批流程状态机（2026-09-10 简化为两级）：0 填报中 → 1 教务处待审 → 2 已完结(锁定)。
 * 驳回从 1 回到 0。院领导签字环节已移除。
 * 所有状态变更均为带事务的原子条件更新（update ... where id=? and status=?），
 * 并在同一事务内写入 biz_audit_log 审批日志。
 *
 * @author wflg
 */
public interface BizAuditService
{
    /**
     * 提交审核（填报中 → 待审）
     *
     * @param id 汇总主键
     */
    void submit(Long id);

    /**
     * 教务处审核通过（待审 → 已完结，写 academic_assistant_sign 与 lock_time）
     *
     * @param id 汇总主键
     */
    void approve(Long id);

    /**
     * 教务处驳回（待审 → 填报中）
     *
     * @param id 汇总主键
     * @param reason 驳回原因（写入审批日志，并兼容写入 remark）
     */
    void reject(Long id, String reason);

    /**
     * 解锁（已完结 → 填报中，清空 lock_time）
     *
     * @param id 汇总主键
     */
    void unlock(Long id);

    /**
     * 教师本人确认汇总（写 teacher_sign/teacher_sign_time，不变更状态）
     *
     * @param id 汇总主键
     */
    void teacherConfirm(Long id);

    /**
     * 批量提交审核（逐条独立事务，收集成功/失败明细）
     *
     * @param ids 汇总主键数组
     * @return 回执 Map：successCount / failCount / failDetails(id+reason 明细列表)
     */
    Map<String, Object> batchSubmit(Long[] ids);
}
