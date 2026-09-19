package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.common.utils.SecurityUtils;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.domain.BizCoefficientAdjustment;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.mapper.BizCoefficientAdjustmentMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import com.workload.system.service.ICoefficientAdjustmentService.CoefficientAdjustmentRequest;

@ExtendWith(MockitoExtension.class)
class CoefficientAdjustmentServiceImplTest
{
    @Mock private BizCoefficientAdjustmentMapper adjustmentMapper;
    @Mock private BizWorkloadItemMapper itemMapper;
    private final BizWorkloadSummaryMapper summaryMapper = mock(BizWorkloadSummaryMapper.class);
    @Spy private WorkloadWriteGuard workloadWriteGuard = new WorkloadWriteGuard(summaryMapper);
    @InjectMocks private CoefficientAdjustmentServiceImpl service;

    // ---- 白名单：G1 的 C1/K1/Q1/Q2/N 与 G2 的 K/C2/Q1/Q2 共 9 个可提交 ----
    @ParameterizedTest
    @CsvSource({"G1,C1", "G1,K1", "G1,Q1", "G1,Q2", "G1,N", "G2,K", "G2,C2", "G2,Q1", "G2,Q2"})
    void allowedFactorCanBeSubmitted(String category, String factorCode)
    {
        when(itemMapper.selectBizWorkloadItemById(9L)).thenReturn(item(category));

        Long id = service.submit(request(category, factorCode, new BigDecimal("1.20"), "教务复核后上调"));

        ArgumentCaptor<BizCoefficientAdjustment> captor = ArgumentCaptor.forClass(BizCoefficientAdjustment.class);
        verify(adjustmentMapper).insertCoefficientAdjustment(captor.capture());
        BizCoefficientAdjustment saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(BizCoefficientAdjustment.STATUS_PENDING);
        assertThat(saved.getCategory()).isEqualTo(category);
        assertThat(saved.getFactorCode()).isEqualTo(factorCode);
        assertThat(saved.getBaseCalculationVersion()).isEqualTo(3L);
    }

    @Test
    void nonWhitelistFactorIsRejected()
    {
        // C2 属 G2，不属 G1
        assertThatThrownBy(() -> service.submit(request("G1", "C2", new BigDecimal("1.0"), "理由")))
                .isInstanceOf(ServiceException.class);
        // G3 及其它类别一律拒绝
        assertThatThrownBy(() -> service.submit(request("G3", "Q1", new BigDecimal("1.0"), "理由")))
                .isInstanceOf(ServiceException.class);
        verify(itemMapper, never()).selectBizWorkloadItemById(anyLong());
        verify(adjustmentMapper, never()).insertCoefficientAdjustment(any());
    }

    @Test
    void nonPositiveRequestedValueIsRejected()
    {
        assertThatThrownBy(() -> service.submit(request("G1", "C1", BigDecimal.ZERO, "理由")))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.submit(request("G1", "C1", new BigDecimal("-1"), "理由")))
                .isInstanceOf(ServiceException.class);
        verify(adjustmentMapper, never()).insertCoefficientAdjustment(any());
    }

    @Test
    void blankReasonIsRejected()
    {
        assertThatThrownBy(() -> service.submit(request("G1", "C1", new BigDecimal("1.1"), "   ")))
                .isInstanceOf(ServiceException.class);
        verify(adjustmentMapper, never()).insertCoefficientAdjustment(any());
    }

    @Test
    void duplicatePendingIsRejected()
    {
        when(itemMapper.selectBizWorkloadItemById(9L)).thenReturn(item("G1"));
        when(adjustmentMapper.countPendingByItemFactor(9L, "C1")).thenReturn(1);

        assertThatThrownBy(() -> service.submit(request("G1", "C1", new BigDecimal("1.1"), "理由")))
                .isInstanceOf(ServiceException.class);
        verify(adjustmentMapper, never()).insertCoefficientAdjustment(any());
    }

    @Test
    void frozenSummaryRejectsApplicationWithApprovalMessage()
    {
        when(itemMapper.selectBizWorkloadItemById(9L)).thenReturn(item("G1"));
        BizWorkloadSummary frozen = new BizWorkloadSummary();
        frozen.setStatus(1);
        when(summaryMapper.selectByUserSemesterForUpdate(2002L, "2026-2027-1")).thenReturn(frozen);

        assertThatThrownBy(() -> service.submit(request("G1", "C1", new BigDecimal("1.1"), "理由")))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("审批流程");
        verify(adjustmentMapper, never()).insertCoefficientAdjustment(any());
    }

    @Test
    void oldValueIsReadFromServerIgnoringClientSuppliedValue()
    {
        when(itemMapper.selectBizWorkloadItemById(9L)).thenReturn(item("G1"));
        when(adjustmentMapper.selectCurrentFactorValue("G1", "C1", 9L)).thenReturn(new BigDecimal("1.10"));

        CoefficientAdjustmentRequest req = request("G1", "C1", new BigDecimal("1.20"), "理由");
        req.setOldValue(new BigDecimal("9.99")); // 客户端伪造的旧值必须被忽略

        service.submit(req);

        ArgumentCaptor<BizCoefficientAdjustment> captor = ArgumentCaptor.forClass(BizCoefficientAdjustment.class);
        verify(adjustmentMapper).insertCoefficientAdjustment(captor.capture());
        assertThat(captor.getValue().getOldValue()).isEqualByComparingTo("1.10");
        assertThat(captor.getValue().getRequestedValue()).isEqualByComparingTo("1.20");
    }

    @Test
    void approveThrowsConflictWhenNoPendingRowUpdated()
    {
        BizCoefficientAdjustment adj = new BizCoefficientAdjustment();
        adj.setId(5L);
        adj.setStatus(BizCoefficientAdjustment.STATUS_PENDING);
        adj.setBaseCalculationVersion(3L);
        when(adjustmentMapper.selectCoefficientAdjustmentById(5L)).thenReturn(adj);
        when(adjustmentMapper.markApprovedIfPending(anyLong(), anyLong(), anyString(), anyLong())).thenReturn(0);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUserId).thenReturn(1004L);
            assertThatThrownBy(() -> service.approve(5L, "材料有效"))
                    .isInstanceOf(ServiceException.class);
        }
        verify(adjustmentMapper).markApprovedIfPending(eq(5L), eq(1004L), eq("材料有效"), eq(3L));
    }

    @Test
    void rejectRequiresReviewReason()
    {
        assertThatThrownBy(() -> service.reject(5L, "  "))
                .isInstanceOf(ServiceException.class);
        verify(adjustmentMapper, never()).markRejectedIfPending(anyLong(), anyLong(), anyString(), anyLong());
    }

    private BizWorkloadItem item(String category)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(9L);
        item.setUserId(2002L);
        item.setSemester("2026-2027-1");
        item.setItemType(category);
        item.setTaskId(555L);
        item.setCalculationVersion(3L);
        return item;
    }

    private CoefficientAdjustmentRequest request(String category, String factorCode,
            BigDecimal requestedValue, String reason)
    {
        CoefficientAdjustmentRequest req = new CoefficientAdjustmentRequest();
        req.setItemId(9L);
        req.setCategory(category);
        req.setFactorCode(factorCode);
        req.setRequestedValue(requestedValue);
        req.setReason(reason);
        return req;
    }
}
