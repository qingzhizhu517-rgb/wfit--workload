package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.mapper.BizTeachingTaskMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;

@ExtendWith(MockitoExtension.class)
class BizTeachingTaskServiceImplTest
{
    private static final Long ID = 9L;
    private static final Long USER = 8L;
    private static final String SEMESTER = "2025-2026-1";

    @InjectMocks private BizTeachingTaskServiceImpl service;
    @Mock private BizTeachingTaskMapper taskMapper;
    @Mock private BizWorkloadItemMapper itemMapper;
    @Mock private WorkloadWriteGuard writeGuard;

    @Test
    void insertChecksRequestOwnershipBeforeWrite()
    {
        BizTeachingTask task = task(ID, USER, SEMESTER);
        org.mockito.Mockito.doThrow(frozen()).when(writeGuard).lockDraftOrAbsent(USER, SEMESTER);

        assertThatThrownBy(() -> service.insertBizTeachingTask(task)).hasMessageContaining("冻结");
        verify(taskMapper, never()).insertBizTeachingTask(any());
    }

    @Test
    void updateChecksPersistedOwnershipBeforeWrite()
    {
        BizTeachingTask persisted = task(ID, USER, SEMESTER);
        when(taskMapper.selectBizTeachingTaskById(ID)).thenReturn(persisted);
        org.mockito.Mockito.doThrow(frozen()).when(writeGuard).lockDraftOrAbsent(USER, SEMESTER);
        BizTeachingTask request = task(ID, 999L, "2099-2100-1");

        assertThatThrownBy(() -> service.updateBizTeachingTask(request)).hasMessageContaining("冻结");
        verify(writeGuard).lockDraftOrAbsent(USER, SEMESTER);
        verify(taskMapper, never()).updateBizTeachingTask(any());
    }

    @Test
    void deleteChecksPersistedOwnershipBeforeReferenceLookup()
    {
        BizTeachingTask persisted = task(ID, USER, SEMESTER);
        when(taskMapper.selectBizTeachingTaskById(ID)).thenReturn(persisted);
        org.mockito.Mockito.doThrow(frozen()).when(writeGuard).lockDraftOrAbsent(USER, SEMESTER);

        assertThatThrownBy(() -> service.deleteBizTeachingTaskByIds(new Long[] {ID}))
                .hasMessageContaining("冻结");
        verify(itemMapper, never()).selectBizWorkloadItemList(any());
        verify(taskMapper, never()).deleteBizTeachingTaskByIds(any());
    }

    private BizTeachingTask task(Long id, Long userId, String semester)
    {
        BizTeachingTask task = new BizTeachingTask();
        task.setId(id);
        task.setUserId(userId);
        task.setSemester(semester);
        return task;
    }

    private ServiceException frozen()
    {
        return new ServiceException("学期汇总已进入审批流程，数据已冻结");
    }
}
