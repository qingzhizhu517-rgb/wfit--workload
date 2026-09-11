package com.workload.system.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DataScopeUtil;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.service.IBizWorkloadSummaryService;

@ExtendWith(MockitoExtension.class)
class BizWorkloadSummaryControllerTest
{
    @InjectMocks private BizWorkloadSummaryController controller;
    @Mock private IBizWorkloadSummaryService summaryService;

    @Test
    void finishedSummaryCannotBeDeleted()
    {
        BizWorkloadSummary finished = new BizWorkloadSummary();
        finished.setId(7L);
        finished.setStatus(2);
        when(summaryService.selectBizWorkloadSummaryById(7L)).thenReturn(finished);

        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            dataScope.when(DataScopeUtil::isTeacherOnly).thenReturn(false);
            assertThatThrownBy(() -> controller.remove(new Long[] {7L}))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("先解锁");
        }
    }
}
