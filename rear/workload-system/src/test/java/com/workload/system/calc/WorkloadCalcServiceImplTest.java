package com.workload.system.calc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.AopContext;

import com.workload.common.exception.ServiceException;
import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.calc.strategy.WorkloadCalcStrategy;
import com.workload.system.domain.BizPayRecord;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.BizWorkloadCalcSnapshot;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.domain.dto.CalculationRunRequest;
import com.workload.system.domain.vo.CalculationRunResult;
import com.workload.system.domain.vo.CalculationRunResult.StageResult;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.mapper.BizCoefficientAdjustmentMapper;
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

    @Spy @InjectMocks private WorkloadCalcServiceImpl service;
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
    @Mock private ManagementItemGenerator managementItemGenerator;
    @Mock private WorkloadWriteGuard writeGuard;
    @Mock private BizCoefficientAdjustmentMapper adjustmentMapper;

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

    @Test
    void fullRunReportsEveryStage()
    {
        when(teacherProfileMapper.selectBizTeacherProfileByUserId(USER)).thenReturn(new BizTeacherProfile());
        when(adjustmentMapper.countPendingByUserSemester(USER, SEMESTER)).thenReturn(0);
        when(itemMapper.selectBizWorkloadItemList(any())).thenReturn(Collections.emptyList());
        when(managementItemGenerator.generate(USER, SEMESTER)).thenReturn(2);
        doReturn(7).when(service).recalcItems(USER, SEMESTER);
        when(summaryCalcService.recalcSummary(USER, SEMESTER, true)).thenReturn(new BizWorkloadSummary());
        when(payCalcService.recalcPay(USER, SEMESTER)).thenReturn(new BizPayRecord());
        when(summaryCalcService.countUnconfirmed(USER, SEMESTER)).thenReturn(0);

        CalculationRunResult result = service.run(new CalculationRunRequest(USER, SEMESTER, true));

        assertThat(result.getStages()).extracting(StageResult::getCode)
                .containsExactly("VALIDATE", "GENERATE_G11", "RECALC_ITEMS", "RECALC_SUMMARY", "RECALC_PAY");
        assertThat(result.getStages()).allMatch(StageResult::isOk);
        assertThat(result.getGeneratedG11Count()).isEqualTo(2);
        assertThat(result.getRecalcItemCount()).isEqualTo(7);
        verify(managementItemGenerator).generate(USER, SEMESTER);
    }

    @Test
    void runWithoutG11DoesNotInvokeGenerator()
    {
        when(teacherProfileMapper.selectBizTeacherProfileByUserId(USER)).thenReturn(new BizTeacherProfile());
        when(adjustmentMapper.countPendingByUserSemester(USER, SEMESTER)).thenReturn(0);
        when(itemMapper.selectBizWorkloadItemList(any())).thenReturn(Collections.emptyList());
        doReturn(3).when(service).recalcItems(USER, SEMESTER);
        when(summaryCalcService.recalcSummary(USER, SEMESTER, true)).thenReturn(new BizWorkloadSummary());
        when(payCalcService.recalcPay(USER, SEMESTER)).thenReturn(new BizPayRecord());
        when(summaryCalcService.countUnconfirmed(USER, SEMESTER)).thenReturn(0);

        CalculationRunResult result = service.run(new CalculationRunRequest(USER, SEMESTER, false));

        // GENERATE_G11 阶段仍在列表中占位，但生成器绝不被调用、计数为 0
        assertThat(result.getStages()).extracting(StageResult::getCode)
                .containsExactly("VALIDATE", "GENERATE_G11", "RECALC_ITEMS", "RECALC_SUMMARY", "RECALC_PAY");
        assertThat(result.getGeneratedG11Count()).isEqualTo(0);
        verify(managementItemGenerator, never()).generate(any(), anyString());
    }

    @Test
    void runFailsValidateAndWritesNothingWhenSubTableMissing()
    {
        when(teacherProfileMapper.selectBizTeacherProfileByUserId(USER)).thenReturn(new BizTeacherProfile());
        when(adjustmentMapper.countPendingByUserSemester(USER, SEMESTER)).thenReturn(0);
        BizWorkloadItem g1 = new BizWorkloadItem();
        g1.setId(ITEM_ID);
        g1.setItemType("G1");
        g1.setStatus(0);
        when(itemMapper.selectBizWorkloadItemList(any())).thenReturn(Collections.singletonList(g1));
        when(calcStrategyFactory.get("G1")).thenReturn(strategy);
        when(factorFormulaService.build(g1)).thenReturn(null);

        assertThatThrownBy(() -> service.run(new CalculationRunRequest(USER, SEMESTER, true)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("子表");
        // 校验失败即零写入：既不生成 G11、也不重算明细/汇总/酬金
        verify(managementItemGenerator, never()).generate(any(), anyString());
        verify(service, never()).recalcItems(any(), anyString());
        verify(summaryCalcService, never()).recalcSummary(any(), anyString(), eq(true));
    }

    @Test
    void runFailsValidateWhenPendingAdjustmentExists()
    {
        when(teacherProfileMapper.selectBizTeacherProfileByUserId(USER)).thenReturn(new BizTeacherProfile());
        when(adjustmentMapper.countPendingByUserSemester(USER, SEMESTER)).thenReturn(1);

        assertThatThrownBy(() -> service.run(new CalculationRunRequest(USER, SEMESTER, true)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("系数调整申请");
        verify(managementItemGenerator, never()).generate(any(), anyString());
        verify(service, never()).recalcItems(any(), anyString());
    }

    @Test
    void batchAccumulatesOnlySuccessfulTeacherCounts()
    {
        BizWorkloadItem summaryItem = new BizWorkloadItem();
        CalculationRunResult ok = new CalculationRunResult();
        ok.setRecalcItemCount(5);
        ok.setGeneratedG11Count(2);

        try (MockedStatic<AopContext> aop = mockStatic(AopContext.class))
        {
            aop.when(AopContext::currentProxy).thenReturn(service);
            doReturn(ok).when(service).run(argThat(r -> r != null && USER.equals(r.getUserId())));
            doThrow(new ServiceException("教师档案不存在，无法重算"))
                    .when(service).run(argThat(r -> r != null && Long.valueOf(2L).equals(r.getUserId())));

            Map<String, Object> data = service.runBatch(Arrays.asList(USER, 2L), SEMESTER, true);

            assertThat(data.get("successCount")).isEqualTo(1);
            assertThat(data.get("failCount")).isEqualTo(1);
            assertThat(data.get("recalcItemCount")).isEqualTo(5);
            assertThat(data.get("generatedG11Count")).isEqualTo(2);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> failures = (List<Map<String, Object>>) data.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).isEqualTo("教师档案不存在，无法重算");
        }
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
