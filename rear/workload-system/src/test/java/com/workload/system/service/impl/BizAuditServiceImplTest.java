package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.mapper.BizAuditLogMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

@ExtendWith(MockitoExtension.class)
class BizAuditServiceImplTest
{
    @InjectMocks private BizAuditServiceImpl service;
    @Mock private BizWorkloadSummaryMapper summaryMapper;
    @Mock private BizAuditLogMapper auditLogMapper;
    @Mock private BizAuditLogWriter auditLogWriter;

    @Test
    void finishedSummaryCannotBeUnlocked()
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setId(7L);
        summary.setStatus(WorkloadSummaryStatus.FINISHED);
        when(summaryMapper.selectBizWorkloadSummaryById(7L)).thenReturn(summary);

        assertThatThrownBy(() -> service.unlock(7L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("永久锁定");
        verify(summaryMapper, never()).unlockById(7L, null);
    }
}
