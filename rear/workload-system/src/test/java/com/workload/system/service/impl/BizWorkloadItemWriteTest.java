package com.workload.system.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.workload.common.core.domain.entity.*;
import com.workload.common.core.domain.model.LoginUser;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.domain.*;
import com.workload.system.mapper.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BizWorkloadItemWriteTest
{
    @Mock BizWorkloadItemMapper bizWorkloadItemMapper;
    @Mock BizWlTheoryMapper theory;
    @Mock BizCoefficientAdjustmentMapper adjustmentMapper;
    @Mock BizWlPracticeMapper practice;
    @Mock BizWlInternshipTrainingMapper intern;
    @Mock BizWlCourseDesignMapper design;
    @Mock BizWlThesisMapper thesis;
    @Mock BizWlConcentratedInternshipMapper concentrated;
    @Mock BizWlManagementMapper management;
    final BizWorkloadSummaryMapper summaries = mock(BizWorkloadSummaryMapper.class);
    @Spy WorkloadWriteGuard writeGuard = new WorkloadWriteGuard(summaries);
    @InjectMocks BizWorkloadItemServiceImpl service;

    @BeforeEach void login()
    {
        SysUser user = new SysUser(20L); SysRole role = new SysRole(); role.setRoleKey("teacher");
        user.setRoles(List.of(role));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser(20L, 1L, user, Set.of()), null, List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private BizWorkloadItem item()
    {
        BizWorkloadItem item = new BizWorkloadItem(); item.setId(8L); item.setUserId(20L);
        item.setSemester("2025-2026-1"); item.setItemType("G8"); item.setStatus(0);
        item.setCalculatedWorkload(new BigDecimal("10")); return item;
    }
    @Test void teacherCannotApproveOwnManualHours()
    {
        assertThatThrownBy(() -> service.confirm(8L, 0L, new BigDecimal("10")))
                .isInstanceOf(ServiceException.class).hasMessageContaining("核对权限");
        verifyNoInteractions(bizWorkloadItemMapper);
    }

    @Test void reviewerMustSeeTheSameCalculationVersion()
    {
        ((LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .setPermissions(Set.of("system:workloadItem:review"));
        BizWorkloadItem existing = item(); existing.setCalculationVersion(3L);
        when(bizWorkloadItemMapper.selectBizWorkloadItemById(8L)).thenReturn(existing);
        assertThatThrownBy(() -> service.confirm(8L, 2L, new BigDecimal("10")))
                .isInstanceOf(ServiceException.class).hasMessageContaining("版本");
    }

    @Test void reviewerCanConfirmUnchangedManualItem()
    {
        ((LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .setPermissions(Set.of("system:workloadItem:review"));
        BizWorkloadItem existing = item();
        when(bizWorkloadItemMapper.selectBizWorkloadItemById(8L)).thenReturn(existing);
        when(bizWorkloadItemMapper.confirmIfUnchanged(eq(8L), eq(0L), eq(new BigDecimal("10")), any())).thenReturn(1);
        assertThat(service.confirm(8L, 0L, new BigDecimal("10"))).isEqualTo(1);
    }

    @Test void pendingApplicationBlocksItemConfirmation()
    {
        ((LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .setPermissions(Set.of("system:workloadItem:review"));
        when(bizWorkloadItemMapper.selectBizWorkloadItemById(8L)).thenReturn(item());
        when(adjustmentMapper.selectPendingIdsForUpdate(20L, "2025-2026-1")).thenReturn(List.of(81L));
        assertThatThrownBy(() -> service.confirm(8L, 0L, new BigDecimal("10")))
                .isInstanceOf(ServiceException.class).hasMessageContaining("待审");
        verify(bizWorkloadItemMapper, never()).confirmIfUnchanged(any(), any(), any(), any());
    }

    @ParameterizedTest @CsvSource({"1,teacher", "2,teacher", "1,biz_admin", "2,biz_admin"})
    void frozenParentRejectsUpdateAndDeletes(int status, String role)
    {
        ((LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUser().getRoles().get(0).setRoleKey(role);
        BizWorkloadItem existing = item();
        when(bizWorkloadItemMapper.selectBizWorkloadItemById(8L)).thenReturn(existing);
        BizWorkloadSummary summary = new BizWorkloadSummary(); summary.setStatus(status);
        when(summaries.selectByUserSemesterForUpdate(20L, existing.getSemester())).thenReturn(summary);
        BizWorkloadItem update = new BizWorkloadItem(); update.setId(8L); update.setRemark("修改");
        assertThatThrownBy(() -> service.updateBizWorkloadItem(update)).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        assertThatThrownBy(() -> service.deleteBizWorkloadItemById(8L)).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        assertThatThrownBy(() -> service.deleteBizWorkloadItemByIds(new Long[]{8L})).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        verify(bizWorkloadItemMapper, never()).updateBizWorkloadItem(any());
        verify(bizWorkloadItemMapper, never()).deleteBizWorkloadItemById(anyLong());
        verify(bizWorkloadItemMapper, never()).deleteBizWorkloadItemByIds(any());
    }
    @Test void confirmedItemCannotBeEditedInDraftSemester()
    {
        BizWorkloadItem existing = item(); existing.setStatus(1);
        when(bizWorkloadItemMapper.selectBizWorkloadItemById(8L)).thenReturn(existing);
        assertThatThrownBy(() -> service.updateBizWorkloadItem(existing)).isInstanceOf(ServiceException.class).hasMessageContaining("已核对");
        verify(bizWorkloadItemMapper, never()).updateBizWorkloadItem(any());
    }
    @ParameterizedTest @ValueSource(strings = {"G1", "G2", "G3", "G4", "G5", "G6", "G11"})
    void teacherCannotManufactureAutomaticCalculationItems(String type)
    {
        BizWorkloadItem input = item(); input.setItemType(type);
        assertThatThrownBy(() -> service.insertBizWorkloadItem(input)).isInstanceOf(ServiceException.class).hasMessageContaining("G8/G9");
        verify(bizWorkloadItemMapper, never()).insertBizWorkloadItem(any());
    }
    @Test void teacherSubmissionCannotSetConfirmationOrApprovalMetadata()
    {
        BizWorkloadItem input = item(); input.setStatus(1); input.setSourceType("AUTO");
        input.setUserId(999L); input.setCalculationVersion(99L); input.setDeanApprovalStatus(1);
        input.setTaskId(55L); input.setAssignmentId(66L);
        service.insertBizWorkloadItem(input);
        var saved = ArgumentCaptor.forClass(BizWorkloadItem.class);
        verify(bizWorkloadItemMapper).insertBizWorkloadItem(saved.capture());
        assertThat(saved.getValue().getStatus()).isZero();
        assertThat(saved.getValue().getUserId()).isEqualTo(20L);
        assertThat(saved.getValue().getSourceType()).isEqualTo("SELF");
        assertThat(saved.getValue().getCalculationVersion()).isNull();
        assertThat(saved.getValue().getDeanApprovalStatus()).isNull();
        assertThat(saved.getValue().getTaskId()).isNull();
        assertThat(saved.getValue().getAssignmentId()).isNull();
        assertThat(saved.getValue().getCalculatedWorkload()).isEqualByComparingTo("10");
    }
    @Test void draftItemCannotBeMovedToAnotherSemester()
    {
        when(bizWorkloadItemMapper.selectBizWorkloadItemById(8L)).thenReturn(item());
        BizWorkloadItem update = new BizWorkloadItem(); update.setId(8L); update.setSemester("2026-2027-1");
        assertThatThrownBy(() -> service.updateBizWorkloadItem(update)).isInstanceOf(ServiceException.class).hasMessageContaining("归属");
        verify(bizWorkloadItemMapper, never()).updateBizWorkloadItem(any());
    }
    @Test void draftRemarkUpdateRemainsAvailable()
    {
        when(bizWorkloadItemMapper.selectBizWorkloadItemById(8L)).thenReturn(item());
        BizWorkloadItem update = new BizWorkloadItem(); update.setId(8L); update.setRemark("补充说明");
        service.updateBizWorkloadItem(update);
        var order = inOrder(bizWorkloadItemMapper, summaries);
        order.verify(bizWorkloadItemMapper).selectBizWorkloadItemById(8L);
        order.verify(summaries).selectByUserSemesterForUpdate(20L, "2025-2026-1");
        order.verify(bizWorkloadItemMapper).updateBizWorkloadItem(update);
    }
}
