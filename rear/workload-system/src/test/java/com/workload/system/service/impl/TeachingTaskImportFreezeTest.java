package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.core.domain.entity.SysUser;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.domain.dto.TeachingTaskImportDTO;
import com.workload.system.mapper.BizTeachingTaskMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.service.ISysUserService;

@ExtendWith(MockitoExtension.class)
class TeachingTaskImportFreezeTest
{
    private static final Long USER = 8L;
    private static final String SEMESTER = "2025-2026-1";

    @InjectMocks private TeachingTaskImportServiceImpl service;
    @Mock private ISysUserService sysUserService;
    @Mock private WorkloadWriteGuard writeGuard;
    @Mock private BizTeachingTaskMapper teachingTaskMapper;
    @Mock private BizWorkloadItemMapper workloadItemMapper;

    @Test
    void importRowChecksFreezeBeforeRepeatCountAndInsert()
    {
        SysUser user = new SysUser();
        user.setUserId(USER);
        when(sysUserService.selectUserByUserName("T008")).thenReturn(user);
        org.mockito.Mockito.doThrow(new ServiceException("学期汇总已进入审批流程，数据已冻结"))
                .when(writeGuard).lockDraftOrAbsent(USER, SEMESTER);

        assertThatThrownBy(() -> service.processSingleRow(validG1(), "IMP-1"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("冻结");

        InOrder order = inOrder(sysUserService, writeGuard, teachingTaskMapper);
        order.verify(sysUserService).selectUserByUserName("T008");
        order.verify(writeGuard).lockDraftOrAbsent(USER, SEMESTER);
        verify(teachingTaskMapper, never()).countSameCourseTask(any(), any(), any(), any());
        verify(teachingTaskMapper, never()).insertBizTeachingTask(any());
        verifyNoInteractions(workloadItemMapper);
    }

    private TeachingTaskImportDTO validG1()
    {
        TeachingTaskImportDTO dto = new TeachingTaskImportDTO();
        dto.setSemester(SEMESTER);
        dto.setUserCode("T008");
        dto.setCourseName("高等数学");
        dto.setWorkloadType("G1");
        dto.setBaseValue(java.math.BigDecimal.valueOf(32));
        return dto;
    }
}
