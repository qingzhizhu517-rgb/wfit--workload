package com.workload.system.calc;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import com.workload.system.service.ISysUserService;

@ExtendWith(MockitoExtension.class)
class WorkloadCalcServiceImplTest
{
    private static final Long ITEM_ID = 11L;
    private static final Long USER = 1L;
    private static final String SEMESTER = "2025-2026-1";

    @InjectMocks private WorkloadCalcServiceImpl service;
    @Mock private BizWorkloadItemMapper itemMapper;
    @Mock private BizTeacherProfileMapper teacherProfileMapper;
    @Mock private BizWorkloadSummaryMapper summaryMapper;
    @Mock private CalcStrategyFactory calcStrategyFactory;
    @Mock private SummaryCalcService summaryCalcService;
    @Mock private PayCalcService payCalcService;
    @Mock private ISysUserService sysUserService;

    @Test
    void draftSummaryAllowsItemModification()
    {
        givenItemAndSummary(WorkloadSummaryStatus.DRAFT);

        assertThatCode(() -> service.assertEditable(ITEM_ID)).doesNotThrowAnyException();
    }

    @Test
    void pendingAuditSummaryRejectsItemModification()
    {
        givenItemAndSummary(WorkloadSummaryStatus.PENDING_AUDIT);

        assertThatThrownBy(() -> service.assertEditable(ITEM_ID))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("禁止修改明细");
    }

    @Test
    void finishedSummaryStillRejectsItemModification()
    {
        givenItemAndSummary(WorkloadSummaryStatus.FINISHED);

        assertThatThrownBy(() -> service.assertEditable(ITEM_ID))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("禁止修改明细");
    }

    private void givenItemAndSummary(int summaryStatus)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(ITEM_ID);
        item.setUserId(USER);
        item.setSemester(SEMESTER);
        item.setStatus(0);
        when(itemMapper.selectBizWorkloadItemById(ITEM_ID)).thenReturn(item);

        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setUserId(USER);
        summary.setSemester(SEMESTER);
        summary.setStatus(summaryStatus);
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(summary));
    }
}
