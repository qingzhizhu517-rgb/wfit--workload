package com.workload.system.calc;

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
    void finishedSummaryCannotHavePayRecalculated()
    {
        BizWorkloadSummary finished = finishedSummary();
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(finished));

        assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已锁定");
    }

    @Test
    void finishedSummaryCannotHaveAllowancesEdited()
    {
        BizWorkloadSummary finished = finishedSummary();
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(finished));

        assertThatThrownBy(() -> service.assertAllowanceEditable(USER, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("禁止修改");
    }

    private BizWorkloadSummary finishedSummary()
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setUserId(USER);
        summary.setSemester(SEMESTER);
        summary.setStatus(2);
        return summary;
    }
}
