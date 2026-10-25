package com.workload.system.service.impl;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BizWorkloadSummaryWriteTest
{
    final BizWorkloadSummaryMapper mapper = mock(BizWorkloadSummaryMapper.class);
    @Spy WorkloadWriteGuard writeGuard = new WorkloadWriteGuard(mapper);
    @InjectMocks BizWorkloadSummaryServiceImpl service;

    // 同一个mapper同时供服务与真实冻结守卫使用。
    @org.junit.jupiter.api.BeforeEach void wire()
    {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "bizWorkloadSummaryMapper", mapper);
    }
    private void existing(int status)
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setId(8L); summary.setUserId(20L); summary.setSemester("2025-2026-1"); summary.setStatus(status);
        when(mapper.selectBizWorkloadSummaryById(8L)).thenReturn(summary);
        when(mapper.selectByUserSemesterForUpdate(20L, "2025-2026-1")).thenReturn(summary);
    }
    @ParameterizedTest @ValueSource(ints = {1, 2})
    void frozenSummaryRejectsGenericEditAndDelete(int status)
    {
        existing(status);
        BizWorkloadSummary change = new BizWorkloadSummary(); change.setId(8L); change.setPerformancePay(BigDecimal.TEN);
        assertThatThrownBy(() -> service.updateBizWorkloadSummary(change)).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        assertThatThrownBy(() -> service.deleteBizWorkloadSummaryById(8L)).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        assertThatThrownBy(() -> service.deleteBizWorkloadSummaryByIds(new Long[]{8L})).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        verify(mapper, never()).updateBizWorkloadSummary(any());
        verify(mapper, never()).deleteBizWorkloadSummaryById(anyLong());
        verify(mapper, never()).deleteBizWorkloadSummaryByIds(any());
    }
    @Test void genericEditOnlyWritesRemarkNotComputedOrApprovalFields()
    {
        existing(0);
        BizWorkloadSummary change = new BizWorkloadSummary(); change.setId(8L); change.setRemark("说明");
        change.setG7(BigDecimal.TEN); change.setPerformancePay(BigDecimal.TEN); change.setStatus(2); change.setTeacherSign("伪造");
        service.updateBizWorkloadSummary(change);
        var saved = ArgumentCaptor.forClass(BizWorkloadSummary.class);
        verify(mapper).updateBizWorkloadSummary(saved.capture());
        assertThat(saved.getValue().getRemark()).isEqualTo("说明");
        assertThat(saved.getValue().getG7()).isNull();
        assertThat(saved.getValue().getPerformancePay()).isNull();
        assertThat(saved.getValue().getStatus()).isNull();
        assertThat(saved.getValue().getTeacherSign()).isNull();
    }
    @Test void genericInsertCannotManufactureCalculatedSummary()
    {
        assertThatThrownBy(() -> service.insertBizWorkloadSummary(new BizWorkloadSummary()))
                .isInstanceOf(ServiceException.class).hasMessageContaining("核算");
        verifyNoInteractions(mapper);
    }
    @Test void cannotMoveSummaryToAnotherSemester()
    {
        existing(0);
        BizWorkloadSummary change = new BizWorkloadSummary(); change.setId(8L); change.setSemester("2026-2027-1");
        assertThatThrownBy(() -> service.updateBizWorkloadSummary(change)).isInstanceOf(ServiceException.class).hasMessageContaining("归属");
        verify(mapper, never()).updateBizWorkloadSummary(any());
    }
}
