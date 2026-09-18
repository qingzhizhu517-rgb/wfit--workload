package com.workload.system.calc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.domain.BizWlManagement;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizRoleAssignmentMapper;
import com.workload.system.mapper.BizWlManagementMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;

@ExtendWith(MockitoExtension.class)
class ManagementItemGeneratorImplTest
{
    private static final Long USER = 1L;
    private static final Long ASSIGNMENT = 100L;
    private static final Long ITEM = 200L;
    private static final String SEMESTER = "2025-2026-1";

    @InjectMocks private ManagementItemGeneratorImpl generator;
    @Mock private BizRoleAssignmentMapper assignmentMapper;
    @Mock private BizWorkloadItemMapper itemMapper;
    @Mock private BizWlManagementMapper managementMapper;
    @Mock private WorkloadCalcService workloadCalcService;
    @Mock private WorkloadWriteGuard writeGuard;

    @Test
    void semesterBatchReadsCurrentSourceAfterAcquiringTeacherLock()
    {
        when(assignmentMapper.selectBizRoleAssignmentList(any())).thenReturn(List.of(assignment("90", "OLD")));
        when(assignmentMapper.selectActiveByUserSemesterForUpdate(USER, SEMESTER))
                .thenReturn(List.of(assignment("120", "NEW")));
        when(itemMapper.insertBizWorkloadItem(any())).thenAnswer(invocation -> {
            ((BizWorkloadItem) invocation.getArgument(0)).setId(ITEM);
            return 1;
        });

        generator.generateForSemester(SEMESTER);

        assertThat(captureInsertedDetail().getProratedAmount()).isEqualByComparingTo("120");
        assertThat(captureInsertedDetail().getSourceBatchId()).isEqualTo("NEW");
        InOrder order = inOrder(writeGuard, assignmentMapper);
        order.verify(writeGuard).lockDraftOrAbsent(USER, SEMESTER);
        order.verify(assignmentMapper).selectActiveByUserSemesterForUpdate(USER, SEMESTER);
    }

    @Test
    void missingPositionWorkloadCannotSilentlyBecomeZero()
    {
        BizRoleAssignment source = assignment("90", null);
        source.setAllowanceRate(null);
        stubAssignments(source);

        assertThatThrownBy(() -> generator.generate(USER, SEMESTER))
                .isInstanceOf(ServiceException.class).hasMessageContaining("岗位减免");
        verifyNoInteractions(itemMapper, managementMapper, workloadCalcService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void semesterBatchRequiresExplicitSemester(String semester)
    {
        assertThatThrownBy(() -> generator.generateForSemester(semester))
                .isInstanceOf(ServiceException.class);
        verifyNoInteractions(assignmentMapper, itemMapper, managementMapper, workloadCalcService);
    }

    @Test
    void positionWorkloadIsCopiedDirectlyWithoutDateProration()
    {
        BizRoleAssignment assignment = assignment("90", "BATCH-7");
        assignment.setStartDate(java.sql.Date.valueOf(LocalDate.of(2025, 12, 1)));
        stubAssignments(assignment);
        when(itemMapper.selectBizWorkloadItemList(any())).thenReturn(List.of());
        when(itemMapper.insertBizWorkloadItem(any())).thenAnswer(invocation -> {
            ((BizWorkloadItem) invocation.getArgument(0)).setId(ITEM);
            return 1;
        });

        generator.generate(USER, SEMESTER);

        BizWlManagement detail = captureInsertedDetail();
        assertThat(detail.getProratedAmount()).isEqualByComparingTo("90");
        assertThat(detail.getSourceBatchId()).isEqualTo("BATCH-7");
        assertThat(detail.getProrationBasis()).contains("岗位减免工作量（本学期）", "计入 G11")
                .doesNotContain("折算", "天");
        InOrder order = inOrder(writeGuard, assignmentMapper, itemMapper);
        order.verify(writeGuard).lockDraftOrAbsent(USER, SEMESTER);
        order.verify(assignmentMapper).selectActiveByUserSemesterForUpdate(USER, SEMESTER);
        order.verify(itemMapper).insertBizWorkloadItem(any());
    }

    @Test
    void frozenSummaryCausesZeroWrites()
    {
        doThrow(new ServiceException("数据已冻结")).when(writeGuard).lockDraftOrAbsent(USER, SEMESTER);

        assertThatThrownBy(() -> generator.generate(USER, SEMESTER)).hasMessageContaining("冻结");

        verifyNoInteractions(assignmentMapper, itemMapper, managementMapper, workloadCalcService);
    }

    @Test
    void confirmedItemIsNotModified()
    {
        stubExistingItem(1);

        generator.generate(USER, SEMESTER);

        verify(managementMapper, never()).updateProrationIfEditable(any(), any(), any(), any(), any());
        verify(itemMapper, never()).insertBizWorkloadItem(any());
        verify(workloadCalcService, never()).recalcItem(any());
    }

    @Test
    void disputedItemKeepsIdentityAndIsResynchronized()
    {
        stubExistingItem(2);
        when(managementMapper.selectBizWlManagementByItemId(ITEM)).thenReturn(new BizWlManagement());
        when(managementMapper.updateProrationIfEditable(eq(ITEM), any(), any(), any(), any())).thenReturn(1);

        generator.generate(USER, SEMESTER);

        verify(itemMapper, never()).insertBizWorkloadItem(any());
        verify(managementMapper).updateProrationIfEditable(eq(ITEM), eq("班主任"),
                eq(new BigDecimal("90")), any(), eq("BATCH-7"));
        verify(workloadCalcService).recalcItem(ITEM);
    }

    @Test
    void concurrentFreezeDuringDetailUpdateRollsBack()
    {
        stubExistingItem(0);
        when(managementMapper.selectBizWlManagementByItemId(ITEM)).thenReturn(new BizWlManagement());
        when(managementMapper.updateProrationIfEditable(eq(ITEM), any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> generator.generate(USER, SEMESTER))
                .isInstanceOf(ServiceException.class).hasMessageContaining("状态已变化");
        verify(workloadCalcService, never()).recalcItem(any());
    }

    private void stubExistingItem(int status)
    {
        stubAssignments(assignment("90", "BATCH-7"));
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(ITEM);
        item.setUserId(USER);
        item.setSemester(SEMESTER);
        item.setAssignmentId(ASSIGNMENT);
        item.setRoleType("班主任");
        item.setStatus(status);
        when(itemMapper.selectBizWorkloadItemList(any())).thenReturn(List.of(item));
    }

    private void stubAssignments(BizRoleAssignment assignment)
    {
        when(assignmentMapper.selectActiveByUserSemesterForUpdate(USER, SEMESTER)).thenReturn(List.of(assignment));
    }

    private BizRoleAssignment assignment(String workload, String sourceBatchId)
    {
        BizRoleAssignment assignment = new BizRoleAssignment();
        assignment.setId(ASSIGNMENT);
        assignment.setUserId(USER);
        assignment.setRoleType("班主任");
        assignment.setAllowanceRate(new BigDecimal(workload));
        assignment.setSourceBatchId(sourceBatchId);
        assignment.setSemester(SEMESTER);
        assignment.setStatus(1);
        return assignment;
    }

    private BizWlManagement captureInsertedDetail()
    {
        ArgumentCaptor<BizWlManagement> captor = ArgumentCaptor.forClass(BizWlManagement.class);
        verify(managementMapper).insertBizWlManagement(captor.capture());
        return captor.getValue();
    }
}
