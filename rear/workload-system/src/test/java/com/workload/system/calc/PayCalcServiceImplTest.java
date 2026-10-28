package com.workload.system.calc;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizAllowanceItem;
import com.workload.system.domain.BizPayRecord;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.mapper.BizAllowanceItemMapper;
import com.workload.system.mapper.BizPayRecordMapper;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

@ExtendWith(MockitoExtension.class)
class PayCalcServiceImplTest
{
    @org.mockito.Mock private com.workload.system.calc.WorkloadWriteGuard writeGuard;

    private static final Long USER = 1L;
    private static final String SEMESTER = "2025-2026-1";

    @InjectMocks private PayCalcServiceImpl service;
    @Mock private BizWorkloadSummaryMapper summaryMapper;
    @Mock private BizAllowanceItemMapper allowanceMapper;
    @Mock private BizPayRecordMapper payRecordMapper;
    @Mock private BizTeacherProfileMapper teacherProfileMapper;

    @BeforeEach
    void setUp()
    {
        lenient().when(teacherProfileMapper.selectBizTeacherProfileByUserId(USER))
                .thenReturn(new BizTeacherProfile());
    }

    // ==================== 汇总金额 / 停用过滤 / 舍入 ====================

    @Test
    void dirtySummaryCannotProducePayFromStaleAmounts()
    {
        BizWorkloadSummary dirty = summaryWithStatus(WorkloadSummaryStatus.DRAFT);
        dirty.setNeedsRecalculation(1);
        dirty.setPerformancePay(new BigDecimal("100.00"));
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER)).thenReturn(dirty);

        assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
                .isInstanceOf(ServiceException.class).hasMessageContaining("重新核算");
        org.mockito.Mockito.verifyNoInteractions(allowanceMapper, payRecordMapper);
    }

    @Test
    void sumsOnlyEnabledAllowancesAndRoundsTotalHalfUp()
    {
        BizWorkloadSummary summary = summaryWithStatus(WorkloadSummaryStatus.DRAFT);
        summary.setPerformancePay(new BigDecimal("7190.40"));
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER)).thenReturn(summary);
        // 仅 status=1 的其他酬金参与汇总：断言查询对象确实带 status=1
        when(allowanceMapper.selectBizAllowanceItemList(
                argThat(q -> q != null && Integer.valueOf(1).equals(q.getStatus()))))
                .thenReturn(List.of(amount("120.00"), amount("100.00"), amount("240.00"),
                        amount("80.00"), amount("90.00"), amount("450.00"), amount("120.49")));
        when(payRecordMapper.selectBizPayRecordList(any())).thenReturn(List.of());

        BizPayRecord result = service.recalcPay(USER, SEMESTER);

        assertThat(result.getCourseHourPay()).isEqualByComparingTo("7190.40");
        assertThat(result.getOtherPayTotal()).isEqualByComparingTo("1200.49");
        assertThat(result.getTotalPay()).isEqualTo(8391L); // 8390.89 HALF_UP -> 8391
    }

    @Test
    void totalPayRoundsDownBelowHalf()
    {
        BizWorkloadSummary summary = summaryWithStatus(WorkloadSummaryStatus.DRAFT);
        summary.setPerformancePay(new BigDecimal("100.00"));
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER)).thenReturn(summary);
        when(allowanceMapper.selectBizAllowanceItemList(any())).thenReturn(List.of(amount("0.49")));
        when(payRecordMapper.selectBizPayRecordList(any())).thenReturn(List.of());

        BizPayRecord result = service.recalcPay(USER, SEMESTER);

        assertThat(result.getTotalPay()).isEqualTo(100L); // 100.49 HALF_UP -> 100
    }

    @Test
    void totalPayRoundsUpAtHalf()
    {
        BizWorkloadSummary summary = summaryWithStatus(WorkloadSummaryStatus.DRAFT);
        summary.setPerformancePay(new BigDecimal("100.00"));
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER)).thenReturn(summary);
        when(allowanceMapper.selectBizAllowanceItemList(any())).thenReturn(List.of(amount("0.50")));
        when(payRecordMapper.selectBizPayRecordList(any())).thenReturn(List.of());

        BizPayRecord result = service.recalcPay(USER, SEMESTER);

        assertThat(result.getTotalPay()).isEqualTo(101L); // 100.50 HALF_UP -> 101
    }

    // ==================== 幂等 / 唯一键降级 ====================

    @Test
    void existingRecordIsUpdatedWithoutSecondInsert()
    {
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER))
                .thenReturn(summaryWithStatus(WorkloadSummaryStatus.DRAFT));
        when(allowanceMapper.selectBizAllowanceItemList(any())).thenReturn(Collections.emptyList());
        when(payRecordMapper.selectBizPayRecordList(any())).thenReturn(List.of(payRecord(99L, 0)));
        when(payRecordMapper.updateIfSummaryDraft(any())).thenReturn(1);

        service.recalcPay(USER, SEMESTER);
        service.recalcPay(USER, SEMESTER);

        verify(payRecordMapper, never()).insertBizPayRecord(any());
        verify(payRecordMapper, times(2))
                .updateIfSummaryDraft(argThat(r -> Long.valueOf(99L).equals(r.getId())));
    }

    @Test
    void duplicateInsertFallsBackToUpdateSameUniqueRecord()
    {
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER))
                .thenReturn(summaryWithStatus(WorkloadSummaryStatus.DRAFT));
        when(allowanceMapper.selectBizAllowanceItemList(any())).thenReturn(Collections.emptyList());
        when(payRecordMapper.selectBizPayRecordList(any()))
                .thenReturn(List.of(), List.of(payRecord(99L, 0)));
        doThrow(new DuplicateKeyException("uk_user_sem"))
                .when(payRecordMapper).insertBizPayRecord(any());
        when(payRecordMapper.updateIfSummaryDraft(any())).thenReturn(1);

        assertThat(service.recalcPay(USER, SEMESTER).getId()).isEqualTo(99L);
        verify(payRecordMapper).updateIfSummaryDraft(any());
    }

    @Test
    void duplicateInsertUpdatesAmountsAndPreservesRecordStatus()
    {
        BizWorkloadSummary summary = summaryWithStatus(WorkloadSummaryStatus.DRAFT);
        summary.setPerformancePay(new BigDecimal("100.50"));
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER)).thenReturn(summary);
        when(allowanceMapper.selectBizAllowanceItemList(any())).thenReturn(Collections.emptyList());
        when(payRecordMapper.selectBizPayRecordList(any()))
                .thenReturn(List.of(), List.of(payRecord(20L, 1)));
        doThrow(new DuplicateKeyException("duplicate"))
                .when(payRecordMapper).insertBizPayRecord(any());
        when(payRecordMapper.updateIfSummaryDraft(any())).thenReturn(1);

        BizPayRecord record = service.recalcPay(USER, SEMESTER);

        assertThat(record.getId()).isEqualTo(20L);
        assertThat(record.getStatus()).isEqualTo(1);
        assertThat(record.getCourseHourPay()).isEqualByComparingTo("100.50");
        assertThat(record.getTotalPay()).isEqualTo(101L);
        verify(payRecordMapper, never()).updateBizPayRecord(any());
    }

    @Test
    void concurrentFreezeRejectsExistingPayUpdate()
    {
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER))
                .thenReturn(summaryWithStatus(WorkloadSummaryStatus.DRAFT));
        when(allowanceMapper.selectBizAllowanceItemList(any())).thenReturn(Collections.emptyList());
        when(payRecordMapper.selectBizPayRecordList(any())).thenReturn(List.of(payRecord(99L, 0)));
        // updateIfSummaryDraft 未 stub -> 返回 0 -> 状态已变化，酬金未保存
        assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
                .isInstanceOf(ServiceException.class).hasMessageContaining("酬金未保存");
        verify(payRecordMapper, never()).updateBizPayRecord(any());
    }

    @Test
    void duplicateInsertCannotFallBackToUnconditionalUpdate()
    {
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER))
                .thenReturn(summaryWithStatus(WorkloadSummaryStatus.DRAFT));
        when(allowanceMapper.selectBizAllowanceItemList(any())).thenReturn(Collections.emptyList());
        when(payRecordMapper.selectBizPayRecordList(any()))
                .thenReturn(List.of(), List.of(payRecord(99L, 0)));
        doThrow(new DuplicateKeyException("duplicate"))
                .when(payRecordMapper).insertBizPayRecord(any());
        // 降级更新仍走带草稿前置条件的 updateIfSummaryDraft，返回 0 -> 拒绝，不得无条件 update
        assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
                .isInstanceOf(ServiceException.class).hasMessageContaining("酬金未保存");
        verify(payRecordMapper, never()).updateBizPayRecord(any());
    }

    // ==================== 写前状态竞态复核 ====================

    @Test
    void rechecksSummaryStatusUnderLockBeforeWritingPayRecord()
    {
        // 初查为草稿的场景由审批事务在写前置为待审：写前 selectForPayUpdate 行锁读到待审即拒绝
        BizWorkloadSummary pending = summaryWithStatus(WorkloadSummaryStatus.PENDING_AUDIT);
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER)).thenReturn(pending);

        assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
                .isInstanceOf(ServiceException.class).hasMessageContaining("酬金已冻结");

        verify(payRecordMapper, never()).insertBizPayRecord(any());
        verify(payRecordMapper, never()).updateIfSummaryDraft(any());
        verify(payRecordMapper, never()).updateBizPayRecord(any());
    }

    @Test
    void payLocksSummaryThenReadsAllowancesThenWritesRecord()
    {
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER))
                .thenReturn(summaryWithStatus(WorkloadSummaryStatus.DRAFT));
        when(allowanceMapper.selectBizAllowanceItemList(any())).thenReturn(Collections.emptyList());
        when(payRecordMapper.selectBizPayRecordList(any())).thenReturn(List.of(payRecord(99L, 0)));
        when(payRecordMapper.updateIfSummaryDraft(any())).thenReturn(1);

        service.recalcPay(USER, SEMESTER);

        // 锁定顺序 summary(行锁读) -> allowance -> pay_record 写
        InOrder order = inOrder(summaryMapper, allowanceMapper, payRecordMapper);
        order.verify(summaryMapper).selectForPayUpdate(USER, SEMESTER);
        order.verify(allowanceMapper).selectBizAllowanceItemList(any());
        order.verify(payRecordMapper).updateIfSummaryDraft(any());
    }

    @Test
    void draftSummaryAllowsPayRecalculation()
    {
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER))
                .thenReturn(summaryWithStatus(WorkloadSummaryStatus.DRAFT));
        when(allowanceMapper.selectBizAllowanceItemList(any())).thenReturn(Collections.emptyList());
        when(payRecordMapper.selectBizPayRecordList(any())).thenReturn(Collections.emptyList());

        assertThatCode(() -> service.recalcPay(USER, SEMESTER)).doesNotThrowAnyException();
    }

    @Test
    void pendingAuditSummaryCannotHavePayRecalculated()
    {
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER))
                .thenReturn(summaryWithStatus(WorkloadSummaryStatus.PENDING_AUDIT));

        assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("酬金已冻结");
    }

    @Test
    void finishedSummaryCannotHavePayRecalculated()
    {
        when(summaryMapper.selectForPayUpdate(USER, SEMESTER))
                .thenReturn(summaryWithStatus(WorkloadSummaryStatus.FINISHED));

        assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("酬金已冻结");
    }

    // ==================== 其他酬金编辑校验（只读普通查询，不持行锁） ====================

    @Test
    void draftSummaryAllowsAllowancesEdited()
    {
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(summaryWithStatus(WorkloadSummaryStatus.DRAFT)));

        assertThatCode(() -> service.assertAllowanceEditable(USER, SEMESTER)).doesNotThrowAnyException();
    }

    @Test
    void pendingAuditSummaryCannotHaveAllowancesEdited()
    {
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(summaryWithStatus(WorkloadSummaryStatus.PENDING_AUDIT)));

        assertThatThrownBy(() -> service.assertAllowanceEditable(USER, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("禁止修改");
    }

    @Test
    void finishedSummaryCannotHaveAllowancesEdited()
    {
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(summaryWithStatus(WorkloadSummaryStatus.FINISHED)));

        assertThatThrownBy(() -> service.assertAllowanceEditable(USER, SEMESTER))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("禁止修改");
    }

    // ==================== helpers ====================

    private BizWorkloadSummary summaryWithStatus(int status)
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setId(10L);
        summary.setUserId(USER);
        summary.setSemester(SEMESTER);
        summary.setStatus(status);
        return summary;
    }

    private BizAllowanceItem amount(String value)
    {
        BizAllowanceItem item = new BizAllowanceItem();
        item.setUserId(USER);
        item.setSemester(SEMESTER);
        item.setStatus(1);
        item.setAmount(new BigDecimal(value));
        return item;
    }

    private BizPayRecord payRecord(Long id, int status)
    {
        BizPayRecord record = new BizPayRecord();
        record.setId(id);
        record.setUserId(USER);
        record.setSemester(SEMESTER);
        record.setStatus(status);
        return record;
    }
}
