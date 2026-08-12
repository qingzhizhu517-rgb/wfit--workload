package com.workload.system.service;

import java.util.Map;

/**
 * 工作量审批领域服务
 * <p>
 * 审批流程状态机：0填报中 → 1教务助理待审 → 2院领导待签 → 3已完结。
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
     * 教务助理审核通过（待审 → 待签）
     *
     * @param id 汇总主键
     */
    void approve(Long id);

    /**
     * 教务助理驳回（待审 → 填报中）
     *
     * @param id 汇总主键
     * @param reason 驳回原因（写入审批日志，并兼容写入 remark）
     */
    void reject(Long id, String reason);

    /**
     * 院领导签字确认（待签 → 已完结）
     *
     * @param id 汇总主键
     */
    void sign(Long id);

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
