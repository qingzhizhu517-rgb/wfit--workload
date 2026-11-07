package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.mapper.BizAuditLogMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

@ExtendWith(MockitoExtension.class)
class BizAuditServiceImplTest
{
    @org.mockito.Mock private com.workload.system.calc.WorkloadWriteGuard writeGuard;

    @Mock private com.workload.system.review.ReviewBatchService reviewBatches;
    @InjectMocks private BizAuditServiceImpl service;
    @Mock private BizWorkloadSummaryMapper summaryMapper;
    @Mock private BizAuditLogMapper auditLogMapper;
    @Mock private BizAuditLogWriter auditLogWriter;
    @Mock private com.workload.system.mapper.BizWorkloadItemMapper itemMapper;
    @Mock private com.workload.system.mapper.BizCoefficientAdjustmentMapper adjustmentMapper;

    @Test
    void historicalPendingWithoutReviewBatchCannotBeApproved()
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setId(7L); summary.setUserId(1003L); summary.setSemester("2025-2026-1"); summary.setStatus(1);
        when(summaryMapper.selectBizWorkloadSummaryById(7L)).thenReturn(summary);
        when(summaryMapper.selectByUserSemesterForUpdate(1003L, "2025-2026-1")).thenReturn(summary);
        org.mockito.Mockito.lenient().when(summaryMapper.approveSummary(7L, 1, 2, "qa", "qa")).thenReturn(1);
        try (var security = org.mockito.Mockito.mockStatic(com.workload.common.utils.SecurityUtils.class))
        {
            security.when(com.workload.common.utils.SecurityUtils::getUsername).thenReturn("qa");
            assertThatThrownBy(() -> service.approve(7L)).isInstanceOf(ServiceException.class)
                    .hasMessageContaining("重新核算").extracting("code").isEqualTo(409);
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1})
    void pendingCoefficientBlocksSubmissionAndFinalApproval(int status)
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setId(7L); summary.setUserId(1003L); summary.setSemester("2025-2026-1"); summary.setStatus(status);summary.setDataVersion(1L);summary.setReviewBatchNo("batch-qa");
        when(summaryMapper.selectBizWorkloadSummaryById(7L)).thenReturn(summary);
        when(summaryMapper.selectByUserSemesterForUpdate(1003L, "2025-2026-1")).thenReturn(summary);
        when(adjustmentMapper.selectPendingIdsForUpdate(1003L, "2025-2026-1")).thenReturn(java.util.List.of(81L));
        try (var scope = org.mockito.Mockito.mockStatic(com.workload.common.utils.DataScopeUtil.class);
             var security = org.mockito.Mockito.mockStatic(com.workload.common.utils.SecurityUtils.class))
        {
            assertThatThrownBy(() -> { if (status == 0) service.submit(7L); else service.approve(7L); })
                    .isInstanceOf(ServiceException.class).hasMessageContaining("待审系数");
        }
        var order = org.mockito.Mockito.inOrder(summaryMapper, adjustmentMapper);
        order.verify(summaryMapper).selectByUserSemesterForUpdate(1003L, "2025-2026-1");
        order.verify(adjustmentMapper).selectPendingIdsForUpdate(1003L, "2025-2026-1");
        org.mockito.Mockito.verifyNoInteractions(auditLogMapper, auditLogWriter);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1})
    void noPendingCoefficientAllowsExistingTransition(int status)
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setId(7L); summary.setUserId(1003L); summary.setSemester("2025-2026-1"); summary.setStatus(status);summary.setDataVersion(1L);summary.setReviewBatchNo("batch-qa");
        when(summaryMapper.selectBizWorkloadSummaryById(7L)).thenReturn(summary);
        when(summaryMapper.selectByUserSemesterForUpdate(1003L, "2025-2026-1")).thenReturn(summary);
        when(adjustmentMapper.selectPendingIdsForUpdate(1003L, "2025-2026-1")).thenReturn(java.util.List.of());
        if (status == 0) when(summaryMapper.updateStatusIf(7L, 0, 1, "qa")).thenReturn(1);
        else when(summaryMapper.approveSummary(7L, 1, 2, "qa", "qa")).thenReturn(1);
        try (var scope = org.mockito.Mockito.mockStatic(com.workload.common.utils.DataScopeUtil.class);
             var security = org.mockito.Mockito.mockStatic(com.workload.common.utils.SecurityUtils.class))
        {
            security.when(com.workload.common.utils.SecurityUtils::getUsername).thenReturn("qa");
            if (status == 0) service.submit(7L); else service.approve(7L);
        }
        if (status == 0) {verify(reviewBatches).create(summary);verify(summaryMapper).updateStatusIf(7L, 0, 1, "qa");}
        else {verify(reviewBatches).assertCurrent(summary,true);verify(summaryMapper).approveSummary(7L, 1, 2, "qa", "qa");}
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1})
    void unreviewedItemsBlockSubmissionAndFinalApproval(int status)
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setId(7L); summary.setUserId(1003L); summary.setSemester("2025-2026-1"); summary.setStatus(status);summary.setDataVersion(1L);summary.setReviewBatchNo("batch-qa");
        when(summaryMapper.selectBizWorkloadSummaryById(7L)).thenReturn(summary);
        when(summaryMapper.selectByUserSemesterForUpdate(1003L, "2025-2026-1")).thenReturn(summary);
        when(itemMapper.selectUnreviewedIdsForUpdate(1003L, "2025-2026-1"))
                .thenReturn(java.util.List.of(9L));
        try (var scope = org.mockito.Mockito.mockStatic(com.workload.common.utils.DataScopeUtil.class);
             var security = org.mockito.Mockito.mockStatic(com.workload.common.utils.SecurityUtils.class))
        {
            assertThatThrownBy(() -> { if (status == 0) service.submit(7L); else service.approve(7L); })
                    .isInstanceOf(ServiceException.class).hasMessageContaining("未核对");
        }
        org.mockito.Mockito.verifyNoInteractions(auditLogMapper, auditLogWriter);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1})
    void dirtyFlagCommittedBeforeLockBlocksSubmissionAndApproval(int status)
    {
        BizWorkloadSummary before = new BizWorkloadSummary();
        before.setId(7L); before.setUserId(1003L); before.setSemester("2025-2026-1"); before.setStatus(status);
        before.setNeedsRecalculation(0);
        BizWorkloadSummary current = new BizWorkloadSummary();
        current.setId(7L); current.setUserId(1003L); current.setSemester("2025-2026-1"); current.setStatus(status);
        current.setNeedsRecalculation(1);
        when(summaryMapper.selectBizWorkloadSummaryById(7L)).thenReturn(before);
        when(summaryMapper.selectByUserSemesterForUpdate(1003L, "2025-2026-1")).thenReturn(current);
        try (var scope = org.mockito.Mockito.mockStatic(com.workload.common.utils.DataScopeUtil.class);
             var security = org.mockito.Mockito.mockStatic(com.workload.common.utils.SecurityUtils.class))
        {
            assertThatThrownBy(() -> { if (status == 0) service.submit(7L); else service.approve(7L); })
                    .hasMessageContaining("重新核算");
        }
        org.mockito.Mockito.verifyNoInteractions(auditLogMapper, auditLogWriter);
        verify(summaryMapper, never()).updateStatusIf(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(summaryMapper, never()).approveSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void finishedSummaryCannotBeUnlocked()
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setId(7L);
        summary.setStatus(WorkloadSummaryStatus.FINISHED);
        when(summaryMapper.selectBizWorkloadSummaryById(7L)).thenReturn(summary);

        assertThatThrownBy(() -> service.unlock(7L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("永久锁定");
        verify(summaryMapper, never()).unlockById(7L, null);
    }
}
