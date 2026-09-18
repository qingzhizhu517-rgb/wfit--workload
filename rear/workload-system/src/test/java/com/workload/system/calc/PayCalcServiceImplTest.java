package com.workload.system.calc;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.BizPayRecord;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.mapper.BizAllowanceItemMapper;
import com.workload.system.mapper.BizPayRecordMapper;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

@ExtendWith(MockitoExtension.class)
class PayCalcServiceImplTest
{
    private static final Long USER = 1L;
    private static final String SEMESTER = "2025-2026-1";

    @InjectMocks private PayCalcServiceImpl service;
    @Mock private BizWorkloadSummaryMapper summaryMapper;
    @Mock private BizAllowanceItemMapper allowanceMapper;
    @Mock private BizPayRecordMapper payRecordMapper;
    @Mock private BizTeacherProfileMapper teacherProfileMapper;
    @Mock private WorkloadWriteGuard writeGuard;

    @Test
    void concurrentFreezeRejectsExistingPayUpdate()
    {
        givenDraftPay(false);
        assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
                .isInstanceOf(ServiceException.class).hasMessageContaining("酬金未保存");
        verify(payRecordMapper, never()).updateBizPayRecord(any());
    }

    @Test
    void duplicateInsertCannotFallBackToUnconditionalUpdate()
    {
        givenDraftPay(true);
        assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
                .isInstanceOf(ServiceException.class).hasMessageContaining("酬金未保存");
        verify(payRecordMapper, never()).updateBizPayRecord(any());
    }

    @Test
    void payCalculationLocksBeforeReadingSummaryInputs()
    {
        givenDraftPay(false);
        when(payRecordMapper.updateIfSummaryDraft(any())).thenReturn(1);
        service.recalcPay(USER, SEMESTER);
        var order = inOrder(writeGuard, summaryMapper);
        order.verify(writeGuard).lockDraftOrAbsent(USER, SEMESTER);
        order.verify(summaryMapper).selectBizWorkloadSummaryList(any());
    }

    @Test
    void duplicateInsertUpdatesAmountsAndPreservesRecordStatus()
    {
        givenDraftPay(true);
        when(payRecordMapper.updateIfSummaryDraft(any())).thenReturn(1);

        BizPayRecord record = service.recalcPay(USER, SEMESTER);

        assertThat(record.getId()).isEqualTo(20L);
        assertThat(record.getStatus()).isEqualTo(1);
        assertThat(record.getCourseHourPay()).isEqualByComparingTo("100.50");
        assertThat(record.getTotalPay()).isEqualTo(101L);
        verify(payRecordMapper, never()).updateBizPayRecord(any());
    }

    private void givenDraftPay(boolean duplicate)
    {
        BizWorkloadSummary summary = summaryWithStatus(0);
        summary.setPerformancePay(new BigDecimal("100.50"));
        when(summaryMapper.selectBizWorkloadSummaryList(any())).thenReturn(List.of(summary));
        BizPayRecord record = new BizPayRecord();
        record.setId(20L);record.setUserId(USER);record.setSemester(SEMESTER);record.setStatus(1);
        if (duplicate)
        {
            when(payRecordMapper.selectBizPayRecordList(any())).thenReturn(List.of(), List.of(record));
            when(payRecordMapper.insertBizPayRecord(any())).thenThrow(new DuplicateKeyException("duplicate"));
        }
        else when(payRecordMapper.selectBizPayRecordList(any())).thenReturn(List.of(record));
    }

    @BeforeEach
    void setUp()
    {
        lenient().when(teacherProfileMapper.selectBizTeacherProfileByUserId(USER))
                .thenReturn(new BizTeacherProfile());
    }

    @Test
    void draftSummaryAllowsPayRecalculation()
    {
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(summaryWithStatus(WorkloadSummaryStatus.DRAFT)));
        when(allowanceMapper.selectBizAllowanceItemList(any())).thenReturn(Collections.emptyList());
        when(payRecordMapper.selectBizPayRecordList(any())).thenReturn(Collections.emptyList());

        assertThatCode(() -> service.recalcPay(USER, SEMESTER)).doesNotThrowAnyException();
    }

    @Test
    void pendingAuditSummaryCannotHavePayRecalculated()
    {
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(summaryWithStatus(WorkloadSummaryStatus.PENDING_AUDIT)));

        assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("酬金已冻结");
    }

    @Test
    void finishedSummaryCannotHavePayRecalculated()
    {
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(summaryWithStatus(WorkloadSummaryStatus.FINISHED)));

        assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("酬金已冻结");
    }

    @Test
    void draftSummaryAllowsAllowancesEdited()
    {
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(summaryWithStatus(WorkloadSummaryStatus.DRAFT)));

        assertThatCode(() -> service.assertAllowanceEditable(USER, SEMESTER)).doesNotThrowAnyException();
    }

    @Test
    void pendingAuditSummaryCannotHaveAllowancesEdited()
    {
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(summaryWithStatus(WorkloadSummaryStatus.PENDING_AUDIT)));

        assertThatThrownBy(() -> service.assertAllowanceEditable(USER, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("禁止修改");
    }

    @Test
    void finishedSummaryCannotHaveAllowancesEdited()
    {
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(summaryWithStatus(WorkloadSummaryStatus.FINISHED)));

        assertThatThrownBy(() -> service.assertAllowanceEditable(USER, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("禁止修改");
    }

    private BizWorkloadSummary summaryWithStatus(int status)
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setId(10L);
        summary.setUserId(USER);
        summary.setSemester(SEMESTER);
        summary.setStatus(status);
        return summary;
    }
}
