package com.workload.system.calc;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.workload.common.constant.HttpStatus;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

@Component
public class WorkloadWriteGuard
{
    private final BizWorkloadSummaryMapper summaryMapper;

    public WorkloadWriteGuard(BizWorkloadSummaryMapper summaryMapper)
    {
        this.summaryMapper = summaryMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockDraftOrAbsent(Long userId, String semester)
    {
        if (userId == null || !StringUtils.hasText(semester))
        {
            throw new ServiceException("缺少教师或学期，无法校验冻结状态");
        }
        lockPeriod(userId, semester);
        BizWorkloadSummary summary = summaryMapper.selectByUserSemesterForUpdate(userId, semester);
        if (summary != null && WorkloadSummaryStatus.isWriteFrozen(summary.getStatus()))
        {
            throw new ServiceException("学期汇总已进入审批流程，数据已冻结", HttpStatus.CONFLICT);
        }
    }
    /** All writers and audit actions use period -> summary -> item -> adjustment order. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockPeriod(Long userId, String semester)
    {
        if (userId == null || !StringUtils.hasText(semester))
            throw new ServiceException("缺少教师或学期，无法加锁");
        summaryMapper.lockPeriod(userId, semester);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markChanged(Long userId, String semester)
    {
        summaryMapper.markNeedsRecalculation(userId, semester);
    }
}
