package com.workload.system.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workload.common.constant.HttpStatus;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DataScopeUtil;
import com.workload.common.utils.SecurityUtils;
import com.workload.system.domain.BizAuditLog;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.mapper.BizAuditLogMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import com.workload.system.service.BizAuditService;

/**
 * 工作量审批领域服务实现
 * <p>
 * 审批状态机（2026-09-10 简化为两级）：0 填报中 → 1 教务处待审 → 2 已完结(锁定)。
 * 驳回从 1 回到 0。院领导签字环节已移除：{@code dept_leader_sign} 列保留但不再写入
 * （保留列便于将来恢复）。
 * <p>
 * 每个动作：前置校验 → 带前置状态的原子条件更新（按影响行数判定并发冲突）
 * → 同事务写入 biz_audit_log 审批日志。
 * <p>
 * 错误码约定：记录不存在 → {@link HttpStatus#NOT_FOUND}；状态冲突（前置校验失败或原子更新影响行数 != 1）
 * → {@link HttpStatus#CONFLICT}，且冲突分支经 {@link BizAuditLogWriter}（REQUIRES_NEW 独立事务）
 * 追加一条 action=conflict 的审计日志，保证外层事务回滚后日志仍落库。
 *
 * @author wflg
 */
@Service
public class BizAuditServiceImpl implements BizAuditService
{
    private static final Logger log = LoggerFactory.getLogger(BizAuditServiceImpl.class);

    /** 审批动作枚举（biz_audit_log.action 列取值） */
    private static final String ACTION_SUBMIT = "submit";
    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    /** 历史日志值：2026-09-10 简化为两级审批前存在院领导签字环节，存量日志中仍有此 action，
     *  保留常量以免后人误以为可清理；新流程不再产生 */
    private static final String ACTION_SIGN = "sign";
    private static final String ACTION_UNLOCK = "unlock";
    private static final String ACTION_TEACHER_CONFIRM = "teacherConfirm";

    /** 错误码：记录不存在 */
    private static final int CODE_NOT_FOUND = HttpStatus.NOT_FOUND;

    /** 错误码：审批状态冲突 */
    private static final int CODE_STATUS_CONFLICT = HttpStatus.CONFLICT;

    @Autowired
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    @Autowired
    private BizAuditLogMapper bizAuditLogMapper;

    /** 独立事务审计日志写入器（跨 Bean 调用使 REQUIRES_NEW 生效，冲突日志不随外层事务回滚） */
    @Autowired
    private BizAuditLogWriter bizAuditLogWriter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id)
    {
        BizWorkloadSummary summary = requireSummary(id);
        assertStatus(summary, WorkloadSummaryStatus.DRAFT, "只有填报中状态才能提交审核");
        // 提交是「本人动作」：教师角色仅可提交本人的汇总，防止横向越权提交他人记录
        // （与 teacherConfirm 的归属校验对齐；教务/管理角色由 assertOwnOrAdmin 放行）
        DataScopeUtil.assertOwnOrAdmin(summary.getUserId());

        String username = SecurityUtils.getUsername();
        int rows = bizWorkloadSummaryMapper.updateStatusIf(id, WorkloadSummaryStatus.DRAFT, WorkloadSummaryStatus.PENDING_AUDIT, username);
        assertUpdated(rows, id, WorkloadSummaryStatus.DRAFT, WorkloadSummaryStatus.PENDING_AUDIT);
        writeAuditLog(id, ACTION_SUBMIT, WorkloadSummaryStatus.DRAFT, WorkloadSummaryStatus.PENDING_AUDIT, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id)
    {
        BizWorkloadSummary summary = requireSummary(id);
        assertStatus(summary, WorkloadSummaryStatus.PENDING_AUDIT, "只有待审状态才能审核");

        String username = SecurityUtils.getUsername();
        // 教务处审核通过即终态：置已完结(2)，同时写 academic_assistant_sign 与 lock_time
        int rows = bizWorkloadSummaryMapper.approveSummary(id, WorkloadSummaryStatus.PENDING_AUDIT, WorkloadSummaryStatus.FINISHED, username, username);
        assertUpdated(rows, id, WorkloadSummaryStatus.PENDING_AUDIT, WorkloadSummaryStatus.FINISHED);
        writeAuditLog(id, ACTION_APPROVE, WorkloadSummaryStatus.PENDING_AUDIT, WorkloadSummaryStatus.FINISHED, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reason)
    {
        BizWorkloadSummary summary = requireSummary(id);
        // 简化两级审批后只剩一个审批环节（教务处待审 1），故驳回只支持 1 → 0
        Integer current = summary.getStatus();
        if (current == null || current != WorkloadSummaryStatus.PENDING_AUDIT)
        {
            throw new ServiceException(
                    "只有待审状态才能驳回（当前状态: " + current + "）", CODE_STATUS_CONFLICT);
        }

        String username = SecurityUtils.getUsername();
        // 驳回原因写入审批日志；同时兼容写入 remark（reason 为空时保持原 remark，维持现有行为）
        String remark = reason != null ? reason : summary.getRemark();
        int rows = bizWorkloadSummaryMapper.rejectSummary(id, current, WorkloadSummaryStatus.DRAFT, remark, username);
        assertUpdated(rows, id, current, WorkloadSummaryStatus.DRAFT);
        writeAuditLog(id, ACTION_REJECT, current, WorkloadSummaryStatus.DRAFT, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlock(Long id)
    {
        BizWorkloadSummary summary = requireSummary(id);
        assertStatus(summary, WorkloadSummaryStatus.FINISHED, "只有已完结状态才能解锁");

        String username = SecurityUtils.getUsername();
        int rows = bizWorkloadSummaryMapper.unlockById(id, username);
        assertUpdated(rows, id, WorkloadSummaryStatus.FINISHED, WorkloadSummaryStatus.DRAFT);
        writeAuditLog(id, ACTION_UNLOCK, WorkloadSummaryStatus.FINISHED, WorkloadSummaryStatus.DRAFT, null);
        log.info("管理员 {} 解锁了汇总 id={}", username, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void teacherConfirm(Long id)
    {
        BizWorkloadSummary summary = requireSummary(id);
        if (summary.getStatus() == null || summary.getStatus() != WorkloadSummaryStatus.PENDING_AUDIT)
        {
            throw new ServiceException("只有待审状态才能教师确认（当前状态: " + summary.getStatus() + "）", CODE_STATUS_CONFLICT);
        }

        // SecurityUtils 无 getNickName 方法，按约定取登录账户
        String teacherSign = SecurityUtils.getUsername();
        Long userId = SecurityUtils.getUserId();
        int rows = bizWorkloadSummaryMapper.teacherConfirmById(id, userId, teacherSign);
        if (rows != 1)
        {
            // 并发冲突：经独立事务（REQUIRES_NEW）写入 conflict 审计日志后再抛带冲突错误码的异常，
            // 避免外层事务回滚导致日志丢失
            bizAuditLogWriter.writeConflictLog(id, summary.getStatus(), summary.getStatus(),
                    "并发冲突：教师确认未生效，记录状态已变化或记录不属于当前用户（影响行数 " + rows + "）");
            throw new ServiceException("记录状态已变化或记录不属于当前用户，请刷新后重试", CODE_STATUS_CONFLICT);
        }
        // 教师确认不变更状态，from_status/to_status 记录确认时的状态
        writeAuditLog(id, ACTION_TEACHER_CONFIRM, summary.getStatus(), summary.getStatus(), null);
    }

    @Override
    public Map<String, Object> batchSubmit(Long[] ids)
    {
        // 逐条独立事务（经代理调用 submit，每条成功即提交、失败即回滚），
        // 因此本方法不开启整体事务，避免一条失败导致整批回滚
        BizAuditService proxy = (BizAuditService) AopContext.currentProxy();
        int successCount = 0;
        List<Map<String, Object>> failDetails = new ArrayList<>();
        for (Long id : ids)
        {
            try
            {
                proxy.submit(id);
                successCount++;
            }
            catch (Exception e)
            {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", id);
                detail.put("reason", e.getMessage());
                failDetails.add(detail);
            }
        }
        Map<String, Object> receipt = new HashMap<>();
        receipt.put("successCount", successCount);
        receipt.put("failCount", failDetails.size());
        receipt.put("failDetails", failDetails);
        return receipt;
    }

    /**
     * 查询汇总记录，不存在则抛业务异常
     */
    private BizWorkloadSummary requireSummary(Long id)
    {
        BizWorkloadSummary summary = bizWorkloadSummaryMapper.selectBizWorkloadSummaryById(id);
        if (summary == null)
        {
            throw new ServiceException("汇总记录不存在", CODE_NOT_FOUND);
        }
        return summary;
    }

    /**
     * 前置状态校验（友好提示；并发最终防线是原子条件更新的影响行数判定）
     */
    private void assertStatus(BizWorkloadSummary summary, int expectedStatus, String message)
    {
        if (!Integer.valueOf(expectedStatus).equals(summary.getStatus()))
        {
            throw new ServiceException(message + "（当前状态: " + summary.getStatus() + "）", CODE_STATUS_CONFLICT);
        }
    }

    /**
     * 原子条件更新影响行数 != 1：记录状态已被并发变更。
     * 经 {@link BizAuditLogWriter} 独立事务（REQUIRES_NEW）写入一条 action=conflict 的审计日志
     * （含预期 from/to 状态与操作人），再抛带状态冲突错误码的业务异常；
     * 外层事务回滚不影响冲突日志落库。
     */
    private void assertUpdated(int rows, Long summaryId, int fromStatus, int toStatus)
    {
        if (rows != 1)
        {
            bizAuditLogWriter.writeConflictLog(summaryId, fromStatus, toStatus,
                    "并发冲突：预期 " + fromStatus + " → " + toStatus + " 未生效（影响行数 " + rows + "）");
            throw new ServiceException("记录状态已变化，请刷新后重试", CODE_STATUS_CONFLICT);
        }
    }

    /**
     * 写入审批日志（与状态变更同事务）
     */
    private void writeAuditLog(Long summaryId, String action, Integer fromStatus, Integer toStatus, String reason)
    {
        BizAuditLog auditLog = new BizAuditLog();
        auditLog.setSummaryId(summaryId);
        auditLog.setAction(action);
        auditLog.setFromStatus(fromStatus);
        auditLog.setToStatus(toStatus);
        auditLog.setOperatorId(SecurityUtils.getUserId());
        auditLog.setOperatorName(SecurityUtils.getUsername());
        auditLog.setReason(reason);
        auditLog.setCreateTime(new Date());
        bizAuditLogMapper.insertBizAuditLog(auditLog);
    }
}
