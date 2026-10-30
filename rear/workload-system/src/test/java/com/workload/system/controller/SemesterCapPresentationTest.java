package com.workload.system.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.github.pagehelper.PageHelper;
import com.workload.common.utils.DataScopeUtil;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.service.*;

class SemesterCapPresentationTest {
    @AfterEach void cleanup() {RequestContextHolder.resetRequestAttributes();PageHelper.clearPage();}
    @Test void unfilteredSummaryListUsesEachRowsOwnSemester() {
        var controller=new BizWorkloadSummaryController();var summaries=mock(IBizWorkloadSummaryService.class);var rules=mock(RuleParamService.class);
        var autumn=summary("2025-2026-1");var spring=summary("2025-2026-2");
        when(summaries.selectBizWorkloadSummaryList(any())).thenReturn(List.of(autumn,spring));
        autumn.setWorkloadCap(new BigDecimal("500"));
        spring.setWorkloadCap(new BigDecimal("600"));
        ReflectionTestUtils.setField(controller,"bizWorkloadSummaryService",summaries);ReflectionTestUtils.setField(controller,"ruleParamService",rules);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        try(var scope=mockStatic(DataScopeUtil.class)) {controller.list(new BizWorkloadSummary());}
        assertThat(autumn.getWorkloadCap()).isEqualByComparingTo("500");assertThat(spring.getWorkloadCap()).isEqualByComparingTo("600");
        verify(rules,never()).get(anyString(),any(BigDecimal.class));
    }
    @Test void dashboardUsesReturnedSummarySemesterWhenQueryIsEmpty() {
        var rules=mock(RuleParamService.class);var summaries=mock(IBizWorkloadSummaryService.class);var controller=dashboard(rules,summaries);
        var saved=summary("2025-2026-2");saved.setWorkloadCap(new BigDecimal("600"));
        when(summaries.selectBizWorkloadSummaryList(any())).thenReturn(List.of(saved));
        var result=controller.teacherStats(null);
        Map<?,?> data=(Map<?,?>)result.get("data");assertThat((BigDecimal)data.get("workloadCap")).isEqualByComparingTo("600");
        verifyNoInteractions(rules);
    }
    @Test void dashboardWithoutSummaryDoesNotParseNullSemesterOrReadCurrentRule() {
        var rules=mock(RuleParamService.class);var summaries=mock(IBizWorkloadSummaryService.class);var controller=dashboard(rules,summaries);
        when(summaries.selectBizWorkloadSummaryList(any())).thenReturn(List.of());
        var result=controller.teacherStats(null);Map<?,?> data=(Map<?,?>)result.get("data");
        assertThat(data.get("hasSummary")).isEqualTo(false);assertThat(data.get("workloadCap")).isNull();verifyNoInteractions(rules);
    }
    private BizDashboardController dashboard(RuleParamService rules,IBizWorkloadSummaryService summaries) {
        var controller=new BizDashboardController() { @Override public Long getUserId() {return 1L;} };
        var tasks=mock(IBizTeachingTaskService.class);var items=mock(IBizWorkloadItemService.class);
        when(tasks.selectBizTeachingTaskList(any())).thenReturn(List.of());when(items.selectBizWorkloadItemList(any())).thenReturn(List.of());
        ReflectionTestUtils.setField(controller,"bizTeachingTaskService",tasks);ReflectionTestUtils.setField(controller,"bizWorkloadItemService",items);
        ReflectionTestUtils.setField(controller,"bizWorkloadSummaryService",summaries);ReflectionTestUtils.setField(controller,"ruleParamService",rules);return controller;
    }
    private BizWorkloadSummary summary(String semester) {var s=new BizWorkloadSummary();s.setSemester(semester);return s;}
}
