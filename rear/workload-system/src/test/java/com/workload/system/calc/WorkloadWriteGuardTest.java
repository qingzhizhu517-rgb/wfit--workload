package com.workload.system.calc;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

@ExtendWith(MockitoExtension.class)
class WorkloadWriteGuardTest
{
    private static final Long USER = 1L;
    private static final String SEMESTER = "2025-2026-1";

    @Mock private BizWorkloadSummaryMapper mapper;
    @InjectMocks private WorkloadWriteGuard guard;

    @Test
    void absentSummaryAllowsSourceCreation()
    {
        when(mapper.selectByUserSemesterForUpdate(USER, SEMESTER)).thenReturn(null);
        assertThatCode(() -> guard.lockDraftOrAbsent(USER, SEMESTER)).doesNotThrowAnyException();
    }

    @Test
    void draftSummaryAllowsSourceCreation()
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setStatus(0);
        when(mapper.selectByUserSemesterForUpdate(USER, SEMESTER)).thenReturn(summary);
        assertThatCode(() -> guard.lockDraftOrAbsent(USER, SEMESTER)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void submittedSummaryRejectsEveryWrite(int status)
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setStatus(status);
        when(mapper.selectByUserSemesterForUpdate(USER, SEMESTER)).thenReturn(summary);
        assertThatThrownBy(() -> guard.lockDraftOrAbsent(USER, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已进入审批流程");
    }

    @Test
    void missingOwnershipRejectsValidation()
    {
        assertThatThrownBy(() -> guard.lockDraftOrAbsent(null, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("缺少教师或学期");
    }
}
