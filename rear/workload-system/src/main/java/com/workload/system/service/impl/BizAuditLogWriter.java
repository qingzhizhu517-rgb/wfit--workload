package com.workload.system.service.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.workload.common.utils.SecurityUtils;
import com.workload.system.domain.BizAuditLog;
import com.workload.system.mapper.BizAuditLogMapper;

/**
 * 审批审计日志独立事务写入器
 * <p>
 * 并发冲突（action=conflict）审计日志必须在外层事务回滚后依然落库，
 * 因此以独立 Bean + {@link Propagation#REQUIRES_NEW} 新事务写入：
 * 外层事务回滚不影响本日志的持久化。
 * <p>
 * 注意：REQUIRES_NEW 依赖 Spring AOP 代理，必须由外部 Bean 跨 Bean 调用，
 * 不能在 {@link BizAuditServiceImpl} 内自调用私有方法实现。
 *
 * @author wflg
 */
@Component
public class BizAuditLogWriter
{
    /** 并发冲突痕迹动作（原子更新影响行数 != 1 时写入） */
    public static final String ACTION_CONFLICT = "conflict";

    @Autowired
    private BizAuditLogMapper bizAuditLogMapper;

    /**
     * 独立新事务写入一条 action=conflict 的审计日志。
     * <p>
     * 即使调用方随后抛异常导致外层事务回滚，本日志也已在独立事务中提交。
     *
     * @param summaryId 汇总记录ID
     * @param fromStatus 冲突前记录的当前状态
     * @param toStatus 冲突前记录的当前状态（冲突不变更状态，与 fromStatus 一致）
     * @param reason 冲突原因描述
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void writeConflictLog(Long summaryId, Integer fromStatus, Integer toStatus, String reason)
    {
        BizAuditLog auditLog = new BizAuditLog();
        auditLog.setSummaryId(summaryId);
        auditLog.setAction(ACTION_CONFLICT);
        auditLog.setFromStatus(fromStatus);
        auditLog.setToStatus(toStatus);
        auditLog.setOperatorId(SecurityUtils.getUserId());
        auditLog.setOperatorName(SecurityUtils.getUsername());
        auditLog.setReason(reason);
        auditLog.setCreateTime(new Date());
        bizAuditLogMapper.insertBizAuditLog(auditLog);
    }
}
