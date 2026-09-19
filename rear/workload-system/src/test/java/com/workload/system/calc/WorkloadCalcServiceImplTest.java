package com.workload.system.calc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.calc.strategy.WorkloadCalcStrategy;
import com.workload.system.domain.BizWorkloadCalcSnapshot;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import com.workload.system.service.ISysUserService;
import com.workload.system.service.IWorkloadFactorFormulaService;
import com.workload.system.service.WorkloadSnapshotService;

@ExtendWith(MockitoExtension.class)
class WorkloadCalcServiceImplTest
{
    private static final Long ITEM_ID = 11L;
    private static final Long USER = 1L;
    private static final String SEMESTER = "2025-2026-1";

    @InjectMocks private WorkloadCalcServiceImpl service;
    @Mock private BizWorkloadItemMapper itemMapper;
    @Mock private BizTeacherProfileMapper teacherProfileMapper;
    @Mock private BizWorkloadSummaryMapper summaryMapper;
    @Mock private CalcStrategyFactory calcStrategyFactory;
    @Mock private WorkloadCalcStrategy strategy;
    @Mock private SummaryCalcService summaryCalcService;
    @Mock private PayCalcService payCalcService;
    @Mock private ISysUserService sysUserService;
    @Mock private IWorkloadFactorFormulaService factorFormulaService;
    @Mock private WorkloadSnapshotService snapshotService;

    private FactorFormulaVo aFormula()
    {
        return new FactorFormulaVo("G1", "J1 × C1 × K1 × Q1 × Q2 × N",
                new BigDecimal("52.80"), "理论课", Collections.emptyList());
    }

    private BizWorkloadCalcSnapshot aSnapshot(long version)
    {
        BizWorkloadCalcSnapshot snapshot = new BizWorkloadCalcSnapshot();
        snapshot.setItemId(ITEM_ID);
        snapshot.setCalculationVersion(version);
        return snapshot;
    }

    @Test
    void draftSummaryAllowsItemModification()
    {
        givenItemAndSummary(WorkloadSummaryStatus.DRAFT);

        assertThatCode(() -> service.assertEditable(ITEM_ID)).doesNotThrowAnyException();
    }

    @Test
    void pendingAuditSummaryRejectsItemModification()
    {
        givenItemAndSummary(WorkloadSummaryStatus.PENDING_AUDIT);

        assertThatThrownBy(() -> service.assertEditable(ITEM_ID))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("禁止修改明细");
    }

    @Test
    void finishedSummaryStillRejectsItemModification()
    {
        givenItemAndSummary(WorkloadSummaryStatus.FINISHED);

        assertThatThrownBy(() -> service.assertEditable(ITEM_ID))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("禁止修改明细");
    }

    @Test
    void recalcItemRejectsConcurrentFreeze()
    {
        BizWorkloadItem item = givenDraftItemWithoutSummary();
        item.setItemType("G1");
        when(calcStrategyFactory.get("G1")).thenReturn(strategy);
        when(strategy.calculate(item)).thenReturn(new BigDecimal("52.80"));
        when(factorFormulaService.build(item)).thenReturn(aFormula());
        when(snapshotService.capture(eq(item), any(), anyString())).thenReturn(aSnapshot(1L));
        when(itemMapper.updateCalculationIfEditable(any(), eq(1), eq(0))).thenReturn(0);

        assertThatThrownBy(() -> service.recalcItem(ITEM_ID))
                .isInstanceOf(ServiceException.class)
                .hasMessage("明细或汇总状态已变化，请刷新后重试");
        verify(itemMapper, never()).updateBizWorkloadItem(any());
    }

    @Test
    void successfulRecalcUsesOnlyConditionalCalculationUpdate()
    {
        BizWorkloadItem item = givenDraftItemWithoutSummary();
        item.setItemType("G1");
        when(calcStrategyFactory.get("G1")).thenReturn(strategy);
        when(strategy.calculate(item)).thenReturn(new BigDecimal("52.80"));
        when(factorFormulaService.build(item)).thenReturn(aFormula());
        when(snapshotService.capture(eq(item), any(), anyString())).thenReturn(aSnapshot(1L));
        when(itemMapper.updateCalculationIfEditable(any(), eq(1), eq(0))).thenReturn(1);

        assertThatCode(() -> service.recalcItem(ITEM_ID)).doesNotThrowAnyException();
        verify(itemMapper).updateCalculationIfEditable(item, 1, 0);
        verify(itemMapper, never()).updateBizWorkloadItem(any());
    }

    @Test
    void successfulRecalcPersistsSnapshotBeforeConditionalUpdate()
    {
        BizWorkloadItem item = givenDraftItemWithoutSummary();
        item.setItemType("G1");
        when(calcStrategyFactory.get("G1")).thenReturn(strategy);
        when(strategy.calculate(item)).thenReturn(new BigDecimal("52.80"));
        when(factorFormulaService.build(item)).thenReturn(aFormula());
        when(snapshotService.capture(eq(item), any(), anyString())).thenReturn(aSnapshot(7L));
        when(itemMapper.updateCalculationIfEditable(any(), eq(1), eq(0))).thenReturn(1);

        service.recalcItem(ITEM_ID);

        // 先固化快照，再原子条件更新主表；主表版本与快照版本一致
        InOrder order = inOrder(snapshotService, itemMapper);
        order.verify(snapshotService).capture(eq(item), any(), anyString());
        order.verify(itemMapper).updateCalculationIfEditable(item, 1, 0);
        assertThat(item.getCalculationVersion()).isEqualTo(7L);
    }

    @Test
    void recalcItemFailsLoudWhenFormulaMissing()
    {
        BizWorkloadItem item = givenDraftItemWithoutSummary();
        item.setItemType("G1");
        when(calcStrategyFactory.get("G1")).thenReturn(strategy);
        when(strategy.calculate(item)).thenReturn(new BigDecimal("52.80"));
        when(factorFormulaService.build(item)).thenReturn(null);

        assertThatThrownBy(() -> service.recalcItem(ITEM_ID))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("无法构建计算公式");
        verify(snapshotService, never()).capture(any(), any(), anyString());
        verify(itemMapper, never()).updateCalculationIfEditable(any(), eq(1), eq(0));
    }

    private BizWorkloadItem givenDraftItemWithoutSummary()
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(ITEM_ID);
        item.setUserId(USER);
        item.setSemester(SEMESTER);
        item.setStatus(0);
        when(itemMapper.selectBizWorkloadItemById(ITEM_ID)).thenReturn(item);
        when(summaryMapper.selectBizWorkloadSummaryList(any())).thenReturn(Collections.emptyList());
        return item;
    }

    private void givenItemAndSummary(int summaryStatus)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(ITEM_ID);
        item.setUserId(USER);
        item.setSemester(SEMESTER);
        item.setStatus(0);
        when(itemMapper.selectBizWorkloadItemById(ITEM_ID)).thenReturn(item);

        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setUserId(USER);
        summary.setSemester(SEMESTER);
        summary.setStatus(summaryStatus);
        when(summaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(summary));
    }
}
