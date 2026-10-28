package com.workload.system.calc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.workload.system.calc.PayAcceptanceFixture.SummaryCase;
import com.workload.system.calc.rule.ComplianceChecker;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizPayRate;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.mapper.BizPayRateMapper;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

/**
 * 汇总服务制度性校验集成单元测试（第六条三门理论课视同达标 + remark 告警）。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("汇总服务·三门理论课与告警集成（办法第六条）")
class SummaryCalcServiceImplTest
{
    private static final Long USER = 1L;

    private static final String SEMESTER = "2025-2026-1";

    @InjectMocks
    private SummaryCalcServiceImpl service;

    @Mock
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    @Mock
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    @Mock
    private BizTeacherProfileMapper bizTeacherProfileMapper;

    @Mock
    private BizPayRateMapper bizPayRateMapper;

    @Mock
    private RuleParamService ruleParamService;

    @Mock
    private ComplianceChecker complianceChecker;

    @Mock private WorkloadWriteGuard writeGuard;
    @Mock private PayCalcService payCalcService;
    @Mock private com.workload.system.mapper.BizTeachingTaskMapper teachingTaskMapper;
    @Mock private com.workload.system.mapper.BizRoleAssignmentMapper roleAssignmentMapper;

    @Test
    void activeTeachingTaskWithoutItemCannotClearDirtySummary()
    {
        BizWorkloadSummary draft = summaryWithStatus(0); draft.setNeedsRecalculation(1);
        when(bizWorkloadSummaryMapper.selectByUserSemesterForUpdate(USER, SEMESTER)).thenReturn(draft);
        var task = new com.workload.system.domain.BizTeachingTask(); task.setId(91L); task.setStatus(1);
        when(teachingTaskMapper.selectBizTeachingTaskList(any())).thenReturn(java.util.List.of(task));

        assertThatThrownBy(() -> service.recalcSummary(USER, SEMESTER, true))
                .hasMessageContaining("尚未生成工作量明细的教学任务");
        assertThat(draft.getNeedsRecalculation()).isEqualTo(1);
        verify(teachingTaskMapper).selectBizTeachingTaskList(org.mockito.ArgumentMatchers.argThat(q ->
                USER.equals(q.getUserId()) && SEMESTER.equals(q.getSemester())
                        && Integer.valueOf(1).equals(q.getStatus())
                        && Boolean.TRUE.equals(q.getParams().get("currentRead"))));
        verify(bizWorkloadSummaryMapper, never()).updateCalculatedFieldsIfStatus(any(), any());
        verify(bizWorkloadSummaryMapper, never()).insertBizWorkloadSummary(any());
        org.mockito.Mockito.verifyNoInteractions(payCalcService);
    }

    @Test
    void activeRoleWithoutItemCannotClearDirtySummary()
    {
        BizWorkloadSummary draft = summaryWithStatus(0); draft.setNeedsRecalculation(1);
        when(bizWorkloadSummaryMapper.selectByUserSemesterForUpdate(USER, SEMESTER)).thenReturn(draft);
        var role = new com.workload.system.domain.BizRoleAssignment(); role.setId(92L); role.setStatus(1);
        when(roleAssignmentMapper.selectBizRoleAssignmentList(any())).thenReturn(java.util.List.of(role));

        assertThatThrownBy(() -> service.recalcSummary(USER, SEMESTER, true))
                .hasMessageContaining("尚未同步的有效岗位记录");
        assertThat(draft.getNeedsRecalculation()).isEqualTo(1);
        verify(roleAssignmentMapper).selectBizRoleAssignmentList(org.mockito.ArgumentMatchers.argThat(q ->
                USER.equals(q.getUserId()) && SEMESTER.equals(q.getSemester())
                        && Integer.valueOf(1).equals(q.getStatus())
                        && Boolean.TRUE.equals(q.getParams().get("currentRead"))));
        verify(bizWorkloadSummaryMapper, never()).updateCalculatedFieldsIfStatus(any(), any());
        verify(bizWorkloadSummaryMapper, never()).insertBizWorkloadSummary(any());
        org.mockito.Mockito.verifyNoInteractions(payCalcService);
    }

    @Test
    void rejectedLinkedItemsCountAsProcessedSourcesButContributeNoWorkload()
    {
        var task = new com.workload.system.domain.BizTeachingTask(); task.setId(91L); task.setStatus(1);
        var role = new com.workload.system.domain.BizRoleAssignment(); role.setId(92L); role.setStatus(1);
        when(teachingTaskMapper.selectBizTeachingTaskList(any())).thenReturn(java.util.List.of(task));
        when(roleAssignmentMapper.selectBizRoleAssignmentList(any())).thenReturn(java.util.List.of(role));
        BizWorkloadItem taskItem = item("G1", "40"); taskItem.setTaskId(91L); taskItem.setStatus(3);
        BizWorkloadItem roleItem = item("G11", "90"); roleItem.setAssignmentId(92L); roleItem.setStatus(3);
        when(bizWorkloadItemMapper.selectBizWorkloadItemList(any())).thenReturn(java.util.List.of(taskItem, roleItem));
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));

        BizWorkloadSummary result = service.recalcSummary(USER, SEMESTER, true);

        assertThat(result.getTotalWorkload()).isEqualByComparingTo("0");
        assertThat(result.getNeedsRecalculation()).isZero();
        verify(bizWorkloadSummaryMapper).insertBizWorkloadSummary(result);
        verify(payCalcService).recalcPay(USER, SEMESTER);
    }

    @Test
    void persistedSummarySynchronizesPayAfterSavingFreshAmounts()
    {
        BizWorkloadSummary draft = summaryWithStatus(0);
        draft.setNeedsRecalculation(1);
        when(bizWorkloadSummaryMapper.selectByUserSemesterForUpdate(USER, SEMESTER)).thenReturn(draft);
        when(bizWorkloadSummaryMapper.updateCalculatedFieldsIfStatus(any(), eq(0))).thenReturn(1);
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));

        service.recalcSummary(USER, SEMESTER, true);

        var order = org.mockito.Mockito.inOrder(bizWorkloadSummaryMapper, payCalcService);
        order.verify(bizWorkloadSummaryMapper).updateCalculatedFieldsIfStatus(
                org.mockito.ArgumentMatchers.argThat(saved -> Integer.valueOf(0).equals(saved.getNeedsRecalculation())
                        && saved.getTotalWorkload().compareTo(new BigDecimal("30")) == 0), eq(0));
        order.verify(payCalcService).recalcPay(USER, SEMESTER);
    }

    @Test
    void payFailurePropagatesSoSummaryTransactionCannotCommitAlone()
    {
        BizWorkloadSummary draft = summaryWithStatus(0);
        when(bizWorkloadSummaryMapper.selectByUserSemesterForUpdate(USER, SEMESTER)).thenReturn(draft);
        when(bizWorkloadSummaryMapper.updateCalculatedFieldsIfStatus(any(), eq(0))).thenReturn(1);
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        when(payCalcService.recalcPay(USER, SEMESTER))
                .thenThrow(new com.workload.common.exception.ServiceException("酬金同步失败"));

        assertThatThrownBy(() -> service.recalcSummary(USER, SEMESTER, true))
                .hasMessage("酬金同步失败");
    }

    @Test
    void persistedSummaryAggregatesCurrentReadsAndClearsDirtyFlag()
    {
        BizWorkloadSummary draft = summaryWithStatus(0);
        draft.setNeedsRecalculation(1);
        when(bizWorkloadSummaryMapper.selectByUserSemesterForUpdate(USER, SEMESTER)).thenReturn(draft);
        when(bizWorkloadSummaryMapper.updateCalculatedFieldsIfStatus(any(), eq(0))).thenReturn(1);
        when(bizWorkloadItemMapper.selectBizWorkloadItemList(any())).thenAnswer(inv -> {
            BizWorkloadItem query = inv.getArgument(0);
            return Collections.singletonList(item("G8", Boolean.TRUE.equals(query.getParams().get("currentRead")) ? "90" : "10"));
        });
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));

        BizWorkloadSummary result = service.recalcSummary(USER, SEMESTER, true);

        assertThat(result.getTotalWorkload()).isEqualByComparingTo("90");
        assertThat(result.getNeedsRecalculation()).isZero();
        var order = org.mockito.Mockito.inOrder(writeGuard, bizWorkloadSummaryMapper, bizWorkloadItemMapper);
        order.verify(writeGuard).lockDraftOrAbsent(USER, SEMESTER);
        order.verify(bizWorkloadSummaryMapper).selectByUserSemesterForUpdate(USER, SEMESTER);
        order.verify(bizWorkloadItemMapper).selectBizWorkloadItemList(any());
        order.verify(bizWorkloadSummaryMapper).updateCalculatedFieldsIfStatus(draft, 0);
    }

    @Test
    void concurrentFreezeRejectsConditionalUpdateMiss()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        when(bizWorkloadSummaryMapper.selectByUserSemesterForUpdate(USER, SEMESTER))
                .thenReturn(summaryWithStatus(0));

        assertThatThrownBy(() -> service.recalcSummary(USER, SEMESTER, true))
                .hasMessageContaining("状态已变化");
        verify(bizWorkloadSummaryMapper, never()).updateBizWorkloadSummary(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void duplicateInsertUsesConditionalUpdateForDraftSummary(int affectedRows)
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        BizWorkloadSummary existing = summaryWithStatus(0);
        existing.setId(42L);
        when(bizWorkloadSummaryMapper.selectByUserSemesterForUpdate(USER, SEMESTER))
                .thenReturn(null, existing);
        when(bizWorkloadSummaryMapper.insertBizWorkloadSummary(any()))
                .thenThrow(new DuplicateKeyException("concurrent summary"));
        when(bizWorkloadSummaryMapper.updateCalculatedFieldsIfStatus(any(), eq(0))).thenReturn(affectedRows);

        if (affectedRows == 0)
        {
            assertThatThrownBy(() -> service.recalcSummary(USER, SEMESTER, true)).hasMessageContaining("状态已变化");
        }
        else
        {
            BizWorkloadSummary saved = service.recalcSummary(USER, SEMESTER, true);
            assertThat(saved.getId()).isEqualTo(42L);
            assertThat(saved.getTotalWorkload()).isEqualByComparingTo("30");
        }
        verify(bizWorkloadSummaryMapper, never()).updateBizWorkloadSummary(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void duplicateInsertCannotOverwriteFrozenSummary(int status)
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        when(bizWorkloadSummaryMapper.selectByUserSemesterForUpdate(USER, SEMESTER))
                .thenReturn(null, summaryWithStatus(status));
        when(bizWorkloadSummaryMapper.insertBizWorkloadSummary(any()))
                .thenThrow(new DuplicateKeyException("concurrent summary"));

        assertThatThrownBy(() -> service.recalcSummary(USER, SEMESTER, true))
                .hasMessageContaining("状态已变化");
        verify(bizWorkloadSummaryMapper, never()).updateBizWorkloadSummary(any());
    }

    @BeforeEach
    void setUp()
    {
        // 规则参数一律回落默认值；职称快照为空 → 绩效 0、达标线 BASIC_TEACH_ASSIST=192/2=96
        lenient().when(ruleParamService.get(anyString(), any(BigDecimal.class)))
                .thenAnswer(inv -> inv.getArgument(1));
        lenient().when(bizTeacherProfileMapper.selectBizTeacherProfileByUserId(USER))
                .thenReturn(new BizTeacherProfile());
        lenient().when(bizPayRateMapper.selectBizPayRateList(any())).thenReturn(Collections.emptyList());
        lenient().when(bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.emptyList());
        lenient().when(bizWorkloadItemMapper.selectBizWorkloadItemList(any()))
                .thenReturn(Arrays.asList(
                        item("G1", "10.00"), item("G1", "10.00"), item("G1", "10.00")));
    }

    @Test
    @DisplayName("三门理论课 → basicTeachingMet=1（G10=30 不足 96 也视同达标，第六条）")
    void threeTheoryCoursesDeemMet()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(true));

        BizWorkloadSummary summary = service.recalcSummary(USER, SEMESTER, false);

        assertThat(summary.getBasicTeachingMet()).isEqualTo(1);
        // 视同达标不改标准线本身，仍按职称标准展示
        assertThat(summary.getBasicTeachingStandard()).isEqualByComparingTo("96.00");
    }

    @Test
    @DisplayName("不足三门理论课且 G10=30 < 96 → basicTeachingMet=0（此前会误判欠额的项）")
    void notMetWithoutThreeTheory()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));

        BizWorkloadSummary summary = service.recalcSummary(USER, SEMESTER, false);

        assertThat(summary.getBasicTeachingMet()).isEqualTo(0);
    }

    @Test
    @DisplayName("制度性告警写入 remark；无告警时 remark 置空（保持与当前数据一致）")
    void warningsWrittenToRemark()
    {
        ComplianceChecker.Result warned = result(false);
        warned.getWarnings().add("承担课程数 4 门，超第六条 3 门上限（原则上）");
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(warned);

        BizWorkloadSummary summary = service.recalcSummary(USER, SEMESTER, false);

        assertThat(summary.getRemark()).isEqualTo("承担课程数 4 门，超第六条 3 门上限（原则上）");

        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        assertThat(service.recalcSummary(USER, SEMESTER, false).getRemark()).isEmpty();
    }

    @Test
    @DisplayName("G11 累计 200 超 180/学期 → 封顶为 180 且 remark 含封顶告警（第十六条注）")
    void g11CapWarningWritten()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        when(bizWorkloadItemMapper.selectBizWorkloadItemList(any())).thenReturn(Collections.singletonList(
                item("G11", "200.00")));

        BizWorkloadSummary summary = service.recalcSummary(USER, SEMESTER, false);

        assertThat(summary.getG11()).isEqualByComparingTo("180.00");
        assertThat(summary.getRemark()).contains("G11 管理服务累计 200").contains("180/学期封顶");
    }

    @Test
    @DisplayName("总工作量 580 超 CAP_200PCT=540 → isCapped=1 且 remark 含绩效封顶告警（第二十条）")
    void cap200WarningWritten()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        when(bizWorkloadItemMapper.selectBizWorkloadItemList(any())).thenReturn(Collections.singletonList(
                item("G1", "580.00")));

        BizWorkloadSummary summary = service.recalcSummary(USER, SEMESTER, false);

        assertThat(summary.getIsCapped()).isEqualTo(1);
        assertThat(summary.getRemark()).contains("CAP_200PCT=540").contains("绩效酬金已按封顶值核算");
    }

    @Test
    @DisplayName("草稿状态 0 允许持久化重算")
    void draftSummaryCanBePersisted()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        BizWorkloadSummary draft = summaryWithStatus(WorkloadSummaryStatus.DRAFT);
        when(bizWorkloadSummaryMapper.selectByUserSemesterForUpdate(USER, SEMESTER))
                .thenReturn(draft);
        when(bizWorkloadSummaryMapper.updateCalculatedFieldsIfStatus(any(), eq(0))).thenReturn(1);

        service.recalcSummary(USER, SEMESTER, true);

        verify(bizWorkloadSummaryMapper).updateCalculatedFieldsIfStatus(draft, 0);
        verify(bizWorkloadSummaryMapper, never()).updateBizWorkloadSummary(any());
    }

    @Test
    @DisplayName("待审状态 1 禁止持久化重算")
    void pendingAuditSummaryCannotBePersisted()
    {
        BizWorkloadSummary pending = summaryWithStatus(WorkloadSummaryStatus.PENDING_AUDIT);
        when(bizWorkloadSummaryMapper.selectByUserSemesterForUpdate(USER, SEMESTER))
                .thenReturn(pending);

        assertThatThrownBy(() -> service.recalcSummary(USER, SEMESTER, true))
                .isInstanceOf(com.workload.common.exception.ServiceException.class)
                .hasMessageContaining("禁止重算");
    }

    @Test
    @DisplayName("草稿、待审和已完结状态均允许预览，且绝不落库")
    void allSummaryStatusesCanBePreviewedWithoutPersistence()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        BizWorkloadSummary draft = summaryWithStatus(WorkloadSummaryStatus.DRAFT);
        BizWorkloadSummary pending = summaryWithStatus(WorkloadSummaryStatus.PENDING_AUDIT);
        BizWorkloadSummary finished = summaryWithStatus(WorkloadSummaryStatus.FINISHED);
        when(bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.singletonList(draft), Collections.singletonList(pending),
                        Collections.singletonList(finished));

        service.recalcSummary(USER, SEMESTER, false);
        service.recalcSummary(USER, SEMESTER, false);
        service.recalcSummary(USER, SEMESTER, false);

        verify(bizWorkloadSummaryMapper, never()).insertBizWorkloadSummary(any());
        verify(bizWorkloadSummaryMapper, never()).updateBizWorkloadSummary(any());
        verify(bizWorkloadSummaryMapper, never()).updateCalculatedFieldsIfStatus(any(), any());
        verify(writeGuard, never()).lockDraftOrAbsent(any(), any());
        org.mockito.Mockito.verifyNoInteractions(payCalcService);
    }

    @Test
    @DisplayName("已完结状态 2 禁止持久化重算，防止签字后的汇总被覆盖")
    void finishedSummaryCannotBeRecalculated()
    {
        BizWorkloadSummary finished = summaryWithStatus(WorkloadSummaryStatus.FINISHED);
        when(bizWorkloadSummaryMapper.selectByUserSemesterForUpdate(USER, SEMESTER))
                .thenReturn(finished);

        assertThatThrownBy(() -> service.recalcSummary(USER, SEMESTER, true))
                .isInstanceOf(com.workload.common.exception.ServiceException.class)
                .hasMessageContaining("禁止重算");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.workload.system.calc.PayAcceptanceFixture#performanceCases")
    @DisplayName("绩效边界固化：专任/低于额定/外聘/540封顶/无费率（第二十条）")
    void performancePayBoundariesMatchFixture(SummaryCase c)
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        stubPerformanceCase(c);

        BizWorkloadSummary summary = service.recalcSummary(USER, SEMESTER, false);

        assertThat(summary.getTotalWorkload()).isEqualByComparingTo(c.total());
        assertThat(summary.getPerformancePay()).isEqualByComparingTo(c.expectedPay());
        assertThat(summary.getIsCapped()).isEqualTo(c.capped());
        if (c.rate() == null)
        {
            // 无费率口径（待办#9 当前行为）：不报错、payRate=null、绩效 0.00
            assertThat(summary.getPayRate()).isNull();
        }
        else
        {
            assertThat(summary.getPayRate()).isEqualByComparingTo(c.rate());
        }
        if (c.capped() == 1)
        {
            assertThat(summary.getRemark()).contains("绩效酬金已按封顶值核算");
        }
        else
        {
            assertThat(summary.getRemark()).doesNotContain("绩效酬金已按封顶值核算");
        }
    }

    @Test
    @DisplayName("540 边界确认 > 语义：540 不触顶、540.01 触顶（isCapped 用严格大于）")
    void cap200BoundaryUsesStrictGreaterThan()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));

        stubPerformanceCase(new SummaryCase("恰好540", "专任", "教授", "540", "70", "25200.00", 0));
        BizWorkloadSummary exactly = service.recalcSummary(USER, SEMESTER, false);
        assertThat(exactly.getIsCapped()).isEqualTo(0);
        assertThat(exactly.getPerformancePay()).isEqualByComparingTo("25200.00");

        stubPerformanceCase(new SummaryCase("540.01", "专任", "教授", "540.01", "70", "25200.00", 1));
        BizWorkloadSummary over = service.recalcSummary(USER, SEMESTER, false);
        assertThat(over.getIsCapped()).isEqualTo(1);
        assertThat(over.getPerformancePay()).isEqualByComparingTo("25200.00");
    }

    /**
     * 让明细总和等于 case.total（单条 G1 明细），档案返回 nature/title，
     * 费率 Mapper 按 rate 返回当前有效费率或空列表（无费率场景）。
     */
    private void stubPerformanceCase(SummaryCase c)
    {
        when(bizWorkloadItemMapper.selectBizWorkloadItemList(any()))
                .thenReturn(Collections.singletonList(item("G1", c.total())));
        BizTeacherProfile profile = new BizTeacherProfile();
        profile.setTeacherNature(c.nature());
        profile.setTitle(c.title());
        when(bizTeacherProfileMapper.selectBizTeacherProfileByUserId(USER)).thenReturn(profile);
        if (c.rate() == null)
        {
            when(bizPayRateMapper.selectBizPayRateList(any())).thenReturn(Collections.emptyList());
        }
        else
        {
            when(bizPayRateMapper.selectBizPayRateList(any()))
                    .thenReturn(Collections.singletonList(payRate(c.title(), c.rate())));
        }
    }

    private BizPayRate payRate(String title, String rate)
    {
        BizPayRate payRate = new BizPayRate();
        payRate.setTitle(title);
        payRate.setRate(new BigDecimal(rate));
        payRate.setStatus(1);
        return payRate;
    }

    private BizWorkloadSummary summaryWithStatus(int status)
    {
        BizWorkloadSummary summary = new BizWorkloadSummary();
        summary.setUserId(USER);
        summary.setSemester(SEMESTER);
        summary.setStatus(status);
        return summary;
    }

    private ComplianceChecker.Result result(boolean threeTheory)
    {
        ComplianceChecker.Result result = new ComplianceChecker.Result();
        result.setThreeTheoryCourses(threeTheory);
        return result;
    }

    private BizWorkloadItem item(String type, String workload)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setUserId(USER);
        item.setSemester(SEMESTER);
        item.setItemType(type);
        item.setCalculatedWorkload(new BigDecimal(workload));
        item.setStatus(0);
        return item;
    }
}
