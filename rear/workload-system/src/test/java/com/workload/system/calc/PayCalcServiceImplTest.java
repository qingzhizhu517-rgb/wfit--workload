package com.workload.system.calc;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizTeacherProfile;
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
