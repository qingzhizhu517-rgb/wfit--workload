package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.mapper.BizRoleAssignmentMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

@ExtendWith(MockitoExtension.class)
class BizRoleAssignmentServiceImplTest
{
    @Mock private BizRoleAssignmentMapper mapper;
    private final BizWorkloadSummaryMapper summaryMapper = mock(BizWorkloadSummaryMapper.class);
    @Spy private WorkloadWriteGuard workloadWriteGuard = new WorkloadWriteGuard(summaryMapper);
    @InjectMocks private BizRoleAssignmentServiceImpl service;

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void frozenSemesterRejectsSourceInsert(int status)
    {
        freeze(status);
        assertThatThrownBy(() -> service.insertBizRoleAssignment(assignment()))
                .isInstanceOf(ServiceException.class);
        verify(mapper, never()).insertBizRoleAssignment(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void frozenSemesterRejectsSourceUpdateUsingStoredOwner(int status)
    {
        when(mapper.selectBizRoleAssignmentById(7L)).thenReturn(assignment());
        freeze(status);
        BizRoleAssignment change = assignment();
        change.setUserId(99L);
        change.setSemester("2026-2027-1");
        assertThatThrownBy(() -> service.updateBizRoleAssignment(change))
                .isInstanceOf(ServiceException.class);
        verify(mapper, never()).updateBizRoleAssignment(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void frozenSemesterRejectsSourceDeletion(int status)
    {
        when(mapper.selectBizRoleAssignmentById(7L)).thenReturn(assignment());
        freeze(status);
        assertThatThrownBy(() -> service.deleteBizRoleAssignmentByIds(new Long[] {7L}))
                .isInstanceOf(ServiceException.class);
        verify(mapper, never()).deleteBizRoleAssignmentByIds(any());
    }

    @Test
    void sourceUpdateCannotMoveToAnotherTeacherSemester()
    {
        when(mapper.selectBizRoleAssignmentById(7L)).thenReturn(assignment());
        BizRoleAssignment change = assignment();
        change.setUserId(99L);
        change.setSemester("2026-2027-1");

        service.updateBizRoleAssignment(change);

        assertThat(change.getUserId()).isEqualTo(1L);
        assertThat(change.getSemester()).isEqualTo("2025-2026-1");
        var order = inOrder(summaryMapper, mapper);
        order.verify(summaryMapper).selectByUserSemesterForUpdate(1L, "2025-2026-1");
        order.verify(mapper).updateBizRoleAssignment(change);
    }

    private void freeze(int status)
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setStatus(status);
        when(summaryMapper.selectByUserSemesterForUpdate(1L, "2025-2026-1")).thenReturn(summary);
    }

    private BizRoleAssignment assignment()
    {
        BizRoleAssignment assignment = new BizRoleAssignment();
        assignment.setId(7L);
        assignment.setUserId(1L);
        assignment.setSemester("2025-2026-1");
        assignment.setAllowanceRate(new BigDecimal("90"));
        return assignment;
    }
}
