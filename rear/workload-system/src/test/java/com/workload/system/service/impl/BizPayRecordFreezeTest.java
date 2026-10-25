package com.workload.system.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.domain.BizPayRecord;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.mapper.BizPayRecordMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BizPayRecordFreezeTest
{
    private final BizPayRecordMapper records = mock(BizPayRecordMapper.class);
    private final BizWorkloadSummaryMapper summaries = mock(BizWorkloadSummaryMapper.class);
    private final BizPayRecordServiceImpl service = new BizPayRecordServiceImpl();

    private void setup(int status)
    {
        ReflectionTestUtils.setField(service, "bizPayRecordMapper", records);
        ReflectionTestUtils.setField(service, "writeGuard", new WorkloadWriteGuard(summaries));
        BizPayRecord record = new BizPayRecord();
        record.setId(8L); record.setUserId(20L); record.setSemester("2025-2026-1"); record.setSummaryId(6L);
        when(records.selectBizPayRecordById(8L)).thenReturn(record);
        BizWorkloadSummary summary = new BizWorkloadSummary(); summary.setStatus(status);
        when(summaries.selectByUserSemesterForUpdate(20L, "2025-2026-1")).thenReturn(summary);
    }

    @ParameterizedTest @ValueSource(ints = {1, 2})
    void frozenSummaryRejectsEditEvenWhenRequestOmitsParent(int status)
    {
        setup(status);
        BizPayRecord change = new BizPayRecord(); change.setId(8L); change.setTotalPay(99999L);
        assertThatThrownBy(() -> service.updateBizPayRecord(change)).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        verify(records, never()).updateBizPayRecord(any());
    }

    @ParameterizedTest @ValueSource(ints = {1, 2})
    void frozenSummaryRejectsSingleAndBatchDelete(int status)
    {
        setup(status);
        assertThatThrownBy(() -> service.deleteBizPayRecordById(8L)).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        assertThatThrownBy(() -> service.deleteBizPayRecordByIds(new Long[]{8L})).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        verify(records, never()).deleteBizPayRecordById(anyLong());
        verify(records, never()).deleteBizPayRecordByIds(any());
    }

    @Test
    void draftRecordCannotBeMovedToAnotherTeacherOrSemester()
    {
        setup(0);
        BizPayRecord change = new BizPayRecord(); change.setId(8L); change.setUserId(21L);
        assertThatThrownBy(() -> service.updateBizPayRecord(change)).isInstanceOf(ServiceException.class).hasMessageContaining("归属");
        change.setUserId(null); change.setSemester("2026-2027-1");
        assertThatThrownBy(() -> service.updateBizPayRecord(change)).isInstanceOf(ServiceException.class).hasMessageContaining("归属");
        change.setSemester(null); change.setSummaryId(99L);
        assertThatThrownBy(() -> service.updateBizPayRecord(change)).isInstanceOf(ServiceException.class).hasMessageContaining("归属");
        verify(records, never()).updateBizPayRecord(any());
    }

    @Test
    void draftEditLocksDatabaseParentBeforeWriting()
    {
        setup(0);
        BizPayRecord change = new BizPayRecord(); change.setId(8L); change.setRemark("复核备注");
        service.updateBizPayRecord(change);
        var order = inOrder(records, summaries);
        order.verify(records).selectBizPayRecordById(8L);
        order.verify(summaries).selectByUserSemesterForUpdate(20L, "2025-2026-1");
        order.verify(records).updateBizPayRecord(argThat(saved -> saved.getId().equals(8L)
                && "复核备注".equals(saved.getRemark())));
    }

    @ParameterizedTest @ValueSource(ints = {1, 2})
    void cannotInsertPayIntoFrozenSemester(int status)
    {
        setup(status);
        BizPayRecord record = new BizPayRecord();
        record.setUserId(20L); record.setSemester("2025-2026-1");
        assertThatThrownBy(() -> service.insertBizPayRecord(record))
                .isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        verify(records, never()).insertBizPayRecord(any());
    }

    @Test
    void batchChecksEveryParentBeforeDeletingAnything()
    {
        setup(2);
        BizPayRecord draft = new BizPayRecord();
        draft.setId(9L); draft.setUserId(21L); draft.setSemester("2025-2026-1");
        when(records.selectBizPayRecordById(9L)).thenReturn(draft);
        assertThatThrownBy(() -> service.deleteBizPayRecordByIds(new Long[]{9L, 8L}))
                .isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        verify(records, never()).deleteBizPayRecordByIds(any());
        verify(records, never()).deleteBizPayRecordById(anyLong());
    }

    @Test
    void draftEditCannotOverwriteCalculatedPayOrPaymentStatus()
    {
        setup(0);
        BizPayRecord change = new BizPayRecord(); change.setId(8L); change.setRemark("说明");
        change.setTotalPay(99999L); change.setCourseHourPay(java.math.BigDecimal.TEN);
        change.setOtherPayTotal(java.math.BigDecimal.TEN); change.setStatus(1);
        service.updateBizPayRecord(change);
        var saved = org.mockito.ArgumentCaptor.forClass(BizPayRecord.class);
        verify(records).updateBizPayRecord(saved.capture());
        assertThat(saved.getValue().getRemark()).isEqualTo("说明");
        assertThat(saved.getValue().getTotalPay()).isNull();
        assertThat(saved.getValue().getCourseHourPay()).isNull();
        assertThat(saved.getValue().getOtherPayTotal()).isNull();
        assertThat(saved.getValue().getStatus()).isNull();
    }

    @Test
    void genericInsertCannotManufacturePayRecord()
    {
        setup(0);
        BizPayRecord record = new BizPayRecord(); record.setUserId(20L); record.setSemester("2025-2026-1");
        assertThatThrownBy(() -> service.insertBizPayRecord(record))
                .isInstanceOf(ServiceException.class).hasMessageContaining("核算");
        verify(records, never()).insertBizPayRecord(any());
    }
}
