package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.domain.BizWlConcentratedInternship;
import com.workload.system.domain.BizWlCourseDesign;
import com.workload.system.domain.BizWlInternshipTraining;
import com.workload.system.domain.BizWlManagement;
import com.workload.system.domain.BizWlPractice;
import com.workload.system.domain.BizWlTheory;
import com.workload.system.domain.BizWlThesis;
import com.workload.system.domain.BizWorkloadCalcSnapshot;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.mapper.BizWorkloadCalcSnapshotMapper;
import com.workload.system.mapper.BizTeachingTaskMapper;
import com.workload.system.mapper.BizWlConcentratedInternshipMapper;
import com.workload.system.mapper.BizWlCourseDesignMapper;
import com.workload.system.mapper.BizWlInternshipTrainingMapper;
import com.workload.system.mapper.BizWlManagementMapper;
import com.workload.system.mapper.BizWlPracticeMapper;
import com.workload.system.mapper.BizWlTheoryMapper;
import com.workload.system.mapper.BizWlThesisMapper;

@ExtendWith(MockitoExtension.class)
class WorkloadFactorFormulaBuilderTest
{
    @InjectMocks private WorkloadFactorFormulaServiceImpl builder;
    @Mock private BizWlTheoryMapper theoryMapper;
    @Mock private BizWlPracticeMapper practiceMapper;
    @Mock private BizWlInternshipTrainingMapper internshipTrainingMapper;
    @Mock private BizWlCourseDesignMapper courseDesignMapper;
    @Mock private BizWlThesisMapper thesisMapper;
    @Mock private BizWlConcentratedInternshipMapper concentratedInternshipMapper;
    @Mock private BizWlManagementMapper managementMapper;
    @Mock private RuleParamService ruleParamService;
    @Mock private BizWorkloadCalcSnapshotMapper snapshotMapper;
    @Mock private BizTeachingTaskMapper teachingTaskMapper;
    @Mock private com.workload.system.mapper.BizCoefficientAdjustmentMapper adjustmentMapper;

    @Test
    void invalidatedVersionDoesNotReuseTheLastHistoricalSnapshot()
    {
        BizWorkloadItem item = item("G1", "IMPORT", "0");
        item.setCalculationVersion(4L);
        when(snapshotMapper.selectLatestByItemId(7L)).thenReturn(snapshotWithQ2("1.5", "APPROVED_OVERRIDE"));
        BizWlTheory current = new BizWlTheory(); current.setQ2(n("1.2"));
        when(theoryMapper.selectBizWlTheoryByItemId(7L)).thenReturn(current);

        FactorFormulaVo result = builder.build(item);

        assertThat(result.isLegacy()).isTrue();
        assertThat(result.isReproducible()).isFalse();
        assertThat(result.getSnapshotVersion()).isNull();
        assertThat(result.getFactors()).filteredOn(f -> "Q2".equals(f.getKey()))
                .extracting(FactorFormulaVo.FactorVo::getValue).containsExactly(n("1.2"));
    }

    @Test
    void freshFormulaUnderLockReadsCurrentDetailInsteadOfSnapshotRead()
    {
        BizWorkloadItem item = item("G1", "IMPORT", "48");
        BizWlTheory stale = new BizWlTheory(); stale.setQ2(n("1.0"));
        BizWlTheory current = new BizWlTheory(); current.setQ2(n("1.5"));
        when(theoryMapper.selectBizWlTheoryByItemId(7L)).thenReturn(stale);
        when(theoryMapper.selectByItemIdForUpdate(7L)).thenReturn(current);
        FactorFormulaVo before = builder.buildFresh(item);
        item.getParams().put("currentRead", true);
        FactorFormulaVo after = builder.buildFresh(item);
        assertThat(before.getFactors()).filteredOn(f -> "Q2".equals(f.getKey()))
                .extracting(FactorFormulaVo.FactorVo::getValue).containsExactly(n("1.0"));
        assertThat(after.getFactors()).filteredOn(f -> "Q2".equals(f.getKey()))
                .extracting(FactorFormulaVo.FactorVo::getValue).containsExactly(n("1.5"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"result", "formula", "source", "context"})
    void tamperedCapturedSnapshotMustFailBeforeReturningHistoricalFormula(String field)
    {
        BizWorkloadItem item = item("G1", "IMPORT", "40");
        FactorFormulaVo formula = new FactorFormulaVo("G1", "Q2", n("1.5"), "evidence",
                java.util.List.of(new FactorFormulaVo.FactorVo("Q2", n("1.5"), "IMPORT_VALUE", "quality", "NORMAL")));
        BizWorkloadCalcSnapshot snapshot = new com.workload.system.service.WorkloadSnapshotService(snapshotMapper).capture(item, formula, "v1");
        item.setCalculationVersion(snapshot.getCalculationVersion());
        switch (field) {
            case "result" -> snapshot.setResult(n("2"));
            case "formula" -> snapshot.setFormulaExpression("Q2 × 2");
            case "source" -> snapshot.setSourceJson("{\"Q2\":\"FORGED\"}");
            case "context" -> snapshot.setContextJson(snapshot.getContextJson().replace("evidence", "forged"));
        }
        when(snapshotMapper.selectLatestByItemId(7L)).thenReturn(snapshot);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> builder.build(item))
                .isInstanceOf(com.workload.common.exception.ServiceException.class).hasMessageContaining("完整性");
        verifyNoInteractions(ruleParamService, theoryMapper, teachingTaskMapper);
    }

    @Test
    void snapshotVersionAheadOfItemCannotBeDisguisedAsLegacyCurrentData()
    {
        BizWorkloadItem item = item("G1", "IMPORT", "40"); item.setCalculationVersion(3L);
        BizWorkloadCalcSnapshot snapshot = snapshotWithQ2("1", "RULE_DEFAULT"); snapshot.setCalculationVersion(4L);
        when(snapshotMapper.selectLatestByItemId(7L)).thenReturn(snapshot);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> builder.build(item))
                .isInstanceOf(com.workload.common.exception.ServiceException.class).hasMessageContaining("版本");
        verifyNoInteractions(ruleParamService, theoryMapper, teachingTaskMapper);
    }

    @Test
    void detailUsesPersistedSnapshotSources()
    {
        BizWorkloadItem item = item("G1", "IMPORT", "57.60");
        when(snapshotMapper.selectLatestByItemId(7L)).thenReturn(snapshotWithQ2("1.50", "APPROVED_OVERRIDE"));

        FactorFormulaVo vo = builder.build(item);

        assertThat(vo).isNotNull();
        assertThat(vo.isLegacy()).isFalse();
        assertThat(vo.getSnapshotVersion()).isEqualTo(3L);
        assertThat(vo.getSnapshotHash()).isEqualTo(snapshotWithQ2("1.50", "APPROVED_OVERRIDE").getSnapshotHash());
        assertThat(vo.getCalculatedAt()).isNotNull();
        assertThat(vo.getFactors()).filteredOn(f -> "Q2".equals(f.getKey()))
                .extracting(FactorFormulaVo.FactorVo::getSource)
                .containsExactly("APPROVED_OVERRIDE");
        // 取值也来自快照，不用现场规则重算
        assertThat(vo.getFactors()).filteredOn(f -> "Q2".equals(f.getKey()))
                .extracting(f -> ((BigDecimal) f.getValue()))
                .first().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.BIG_DECIMAL)
                .isEqualByComparingTo("1.50");
    }

    @Test
    void historicalFormulaDoesNotDependOnCurrentRulesOrDeletedSourceRows()
    {
        BizWorkloadItem item = item("G4", "IMPORT", "999");
        item.setTaskId(555L);
        BizWorkloadCalcSnapshot snapshot = snapshotWithQ2("1", "RULE_DEFAULT");
        snapshot.setFormulaExpression("J4 × R4 × 0.4");
        snapshot.setResult(n("40"));
        snapshot.setRuleVersion("historical-v1");
        snapshot.setFactorJson("{\"J4\":\"2\",\"R4\":\"50\",\"CONST\":\"0.4\"}");
        snapshot.setSourceJson("{\"J4\":\"IMPORT_VALUE\",\"R4\":\"IMPORT_VALUE\",\"CONST\":\"FORMULA_CONSTANT\"}");
        signLegacy(snapshot);
        when(snapshotMapper.selectLatestByItemId(7L)).thenReturn(snapshot);

        FactorFormulaVo result = builder.build(item);

        assertThat(result).isNotNull();
        assertThat(result.getExpression()).isEqualTo("J4 × R4 × 0.4");
        assertThat(result.getResult()).isEqualByComparingTo("40");
        assertThat(result.getFactors()).extracting(FactorFormulaVo.FactorVo::getKey)
                .containsExactly("J4", "R4", "CONST");
        assertThat(result.getFactors().get(2).getValue()).isEqualTo(n("0.4"));
        assertThat(result.getFactors().get(2).getRuleVersion()).isEqualTo("historical-v1");
        assertThat(result.getEvidenceStatus()).isEqualTo("PARTIAL");
        assertThat(result.getMissingEvidenceReasons()).contains("MISSING_HISTORICAL_SOURCE");
        assertThat(result.getSourceTask()).isNull();
        assertThat(result.getDescription()).contains("未保存", "源数据");
        verifyNoInteractions(ruleParamService, courseDesignMapper, teachingTaskMapper);
    }

    @Test
    void incompleteSnapshotCannotPretendToBeReproducible()
    {
        BizWorkloadCalcSnapshot snapshot = snapshotWithQ2("1", "RULE_DEFAULT");
        snapshot.setFormulaExpression(null);
        when(snapshotMapper.selectLatestByItemId(7L)).thenReturn(snapshot);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> builder.build(item("G4", "IMPORT", "40")))
                .isInstanceOf(com.workload.common.exception.ServiceException.class)
                .hasMessageContaining("快照");
    }

    @Test
    void missingHistoricalFactorValueIsNotReproducible()
    {
        BizWorkloadCalcSnapshot snapshot = snapshotWithQ2("1", "RULE_DEFAULT");
        snapshot.setFactorJson(snapshot.getFactorJson().replace("\"J1\":\"32\"", "\"J1\":null"));
        signLegacy(snapshot);
        when(snapshotMapper.selectLatestByItemId(7L)).thenReturn(snapshot);

        assertThat(builder.build(item("G1", "IMPORT", "40")).isReproducible()).isFalse();
    }

    @Test
    void capturedContextRestoresCourseAndFactorEvidenceWithoutLiveQueries()
    {
        FactorFormulaVo.FactorVo factor = new FactorFormulaVo.FactorVo("Q2", n("1.5"),
                "APPROVED_OVERRIDE", "课程质量系数", "APPROVED");
        factor.setSourceRef("coefficient-adjustment:81");
        factor.setRuleCode("COEF_Q2");
        factor.setRuleVersion("rule-v1");
        FactorFormulaVo formula = new FactorFormulaVo("G1", "Q2", n("1.5"),
                "历史审批依据", java.util.List.of(factor));
        FactorFormulaVo.SourceTaskVo task = new FactorFormulaVo.SourceTaskVo();
        task.setCourseNature("必修");
        task.setClassName("历史班级");
        task.setRepeatOrder(2L);
        formula.setSourceTask(task);
        BizWorkloadItem item = item("G1", "IMPORT", "999");
        BizWorkloadCalcSnapshot snapshot = new com.workload.system.service.WorkloadSnapshotService(snapshotMapper)
                .capture(item, formula, "rule-v1");
        item.setCalculationVersion(snapshot.getCalculationVersion());
        when(snapshotMapper.selectLatestByItemId(7L)).thenReturn(snapshot);
        task.setClassName("后来修改的班级");
        factor.setSourceRef("later-change");

        FactorFormulaVo restored = builder.build(item);

        assertThat(restored.getSourceTask()).isNotNull();
        assertThat(restored.getSourceTask().getClassName()).isEqualTo("历史班级");
        assertThat(restored.getSourceTask().getCourseNature()).isEqualTo("必修");
        assertThat(restored.getDescription()).isEqualTo("历史审批依据");
        assertThat(restored.getFactors().get(0).getSourceRef()).isEqualTo("coefficient-adjustment:81");
        assertThat(restored.getFactors().get(0).getRuleCode()).isEqualTo("COEF_Q2");
        assertThat(restored.getFactors().get(0).getStatus()).isEqualTo("APPROVED");
        verifyNoInteractions(ruleParamService, teachingTaskMapper, theoryMapper);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "{", "{\"formatVersion\":99}",
            "{\"formatVersion\":1,\"formulaType\":\"G1\",\"factors\":[]}"
    })
    void malformedOrMismatchedContextMustNotFallBackToCurrentData(String context)
    {
        BizWorkloadCalcSnapshot snapshot = snapshotWithQ2("1", "RULE_DEFAULT");
        snapshot.setContextJson(context);
        when(snapshotMapper.selectLatestByItemId(7L)).thenReturn(snapshot);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> builder.build(item("G1", "IMPORT", "40")))
                .isInstanceOf(com.workload.common.exception.ServiceException.class)
                .hasMessageContaining("快照上下文");
        verifyNoInteractions(ruleParamService, theoryMapper, teachingTaskMapper);
    }

    @Test
    void equalFreshFactorCannotClaimUnrecordedApprovalReference()
    {
        BizWlTheory detail = new BizWlTheory();
        detail.setQ2(n("1.5"));
        when(theoryMapper.selectBizWlTheoryByItemId(7L)).thenReturn(detail);
        com.workload.system.domain.BizCoefficientAdjustment approved = new com.workload.system.domain.BizCoefficientAdjustment();
        approved.setId(81L); approved.setFactorCode("Q2"); approved.setRequestedValue(n("1.50"));
        org.mockito.Mockito.lenient().when(adjustmentMapper.selectCoefficientAdjustmentList(any())).thenReturn(java.util.List.of(approved));
        FactorFormulaVo result = builder.buildFresh(item("G1", "IMPORT", "48"));
        FactorFormulaVo.FactorVo q2 = result.getFactors().stream().filter(f -> "Q2".equals(f.getKey())).findFirst().orElseThrow();
        assertThat(q2.getSource()).isEqualTo("MISSING_HISTORICAL_SOURCE");
        assertThat(q2.getSourceRef()).isNull();
        assertThat(result.getEvidenceStatus()).isEqualTo("PARTIAL");
        verifyNoInteractions(adjustmentMapper);
    }

    @Test
    void changedValueMustNotReuseAnOlderMatchingApproval()
    {
        BizWlTheory detail = new BizWlTheory(); detail.setQ2(n("1.5"));
        when(theoryMapper.selectBizWlTheoryByItemId(7L)).thenReturn(detail);
        com.workload.system.domain.BizCoefficientAdjustment latest = new com.workload.system.domain.BizCoefficientAdjustment();
        latest.setId(82L); latest.setFactorCode("Q2"); latest.setRequestedValue(n("1.8"));
        com.workload.system.domain.BizCoefficientAdjustment older = new com.workload.system.domain.BizCoefficientAdjustment();
        older.setId(81L); older.setFactorCode("Q2"); older.setRequestedValue(n("1.5"));
        org.mockito.Mockito.lenient().when(adjustmentMapper.selectCoefficientAdjustmentList(any())).thenReturn(java.util.List.of(latest, older));

        FactorFormulaVo result = builder.buildFresh(item("G1", "IMPORT", "48"));
        FactorFormulaVo.FactorVo q2 = result.getFactors().stream().filter(f -> "Q2".equals(f.getKey())).findFirst().orElseThrow();
        assertThat(q2.getSource()).isNotEqualTo("APPROVED_OVERRIDE");
        assertThat(q2.getSourceRef()).isNull();
    }

    @Test
    void detailExposesSourceTaskFields()
    {
        BizWorkloadItem item = item("G1", "IMPORT", "42.24");
        item.setTaskId(555L);
        BizWlTheory d = new BizWlTheory();
        d.setJ1(n("32")); d.setC1(n("1")); d.setK1(n("1.1"));
        d.setQ1(n("1")); d.setQ2(n("1")); d.setQ3(n("1.5")); d.setN(n("1.2"));
        when(theoryMapper.selectBizWlTheoryByItemId(7L)).thenReturn(d);
        BizTeachingTask task = new BizTeachingTask();
        task.setCourseLevel("省级一流");
        task.setCourseRole("主持人");
        task.setCourseNature("必修");
        task.setClassName("计算机2401");
        task.setRepeatOrder(2L);
        when(teachingTaskMapper.selectBizTeachingTaskById(555L)).thenReturn(task);

        FactorFormulaVo vo = builder.build(item);

        assertThat(vo.getSourceTask()).isNotNull();
        assertThat(vo.getSourceTask().getCourseLevel()).isEqualTo("省级一流");
        assertThat(vo.getSourceTask().getCourseRole()).isEqualTo("主持人");
        assertThat(vo.getSourceTask().getCourseNature()).isEqualTo("必修");
        assertThat(vo.getSourceTask().getClassName()).isEqualTo("计算机2401");
        assertThat(vo.getSourceTask().getRepeatOrder()).isEqualTo(2L);
    }

    @Test
    void legacyWhenNoSnapshotMarksNotReproducible()
    {
        BizWorkloadItem item = item("G1", "IMPORT", "42.24");
        BizWlTheory d = new BizWlTheory();
        d.setJ1(n("32")); d.setC1(n("1")); d.setK1(n("1.1"));
        d.setQ1(n("1")); d.setQ2(n("1")); d.setQ3(n("1.5")); d.setN(n("1.2"));
        when(theoryMapper.selectBizWlTheoryByItemId(7L)).thenReturn(d);

        FactorFormulaVo vo = builder.build(item);

        assertThat(vo.isLegacy()).isTrue();
        assertThat(vo.isReproducible()).isFalse();
    }

    /**
     * capture 路径回归：buildFresh 只按当前子表构造，绝不 overlay 旧快照。
     * 即便存在含 Q2=1.50/APPROVED_OVERRIDE 的旧快照，buildFresh 仍返回子表原值
     * （Q2=1、source=MISSING_HISTORICAL_SOURCE），且不查询快照——否则新快照会冻进旧因子。
     */
    @Test
    void buildFreshIgnoresPriorSnapshotForCapture()
    {
        BizWorkloadItem item = item("G1", "IMPORT", "48.00");
        BizWlTheory d = new BizWlTheory();
        d.setJ1(n("32")); d.setC1(n("1")); d.setK1(n("1.1"));
        d.setQ1(n("1")); d.setQ2(n("1")); d.setQ3(n("1.5")); d.setN(n("1.2"));
        when(theoryMapper.selectBizWlTheoryByItemId(7L)).thenReturn(d);

        FactorFormulaVo vo = builder.buildFresh(item);

        assertThat(vo).isNotNull();
        // Q2 取子表原值 1，原始政策来源缺失，不被旧快照的 1.50/APPROVED_OVERRIDE 覆盖
        assertThat(vo.getFactors()).filteredOn(f -> "Q2".equals(f.getKey()))
                .extracting(f -> ((BigDecimal) f.getValue()))
                .first().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.BIG_DECIMAL)
                .isEqualByComparingTo("1");
        assertThat(vo.getFactors()).filteredOn(f -> "Q2".equals(f.getKey()))
                .extracting(FactorFormulaVo.FactorVo::getSource)
                .containsExactly("MISSING_HISTORICAL_SOURCE");
        // buildFresh 绝不查询快照
        verify(snapshotMapper, never()).selectLatestByItemId(any());
    }

    private BizWorkloadCalcSnapshot snapshotWithQ2(String q2Value, String q2Source)
    {
        BizWorkloadCalcSnapshot s = new BizWorkloadCalcSnapshot();
        s.setItemId(7L);
        s.setCalculationVersion(3L);
        s.setCreatedAt(new Date());
        s.setFormulaExpression("J1 × C1 × K1 × Q1 × Q2 × N");
        s.setResult(n("57.60"));
        s.setFactorJson("{\"J1\":\"32\",\"C1\":\"1\",\"K1\":\"1.1\",\"Q1\":\"1\",\"Q2\":\""
                + q2Value + "\",\"N\":\"1.2\",\"Q3\":\"1.5\"}");
        s.setSourceJson("{\"J1\":\"IMPORT_VALUE\",\"C1\":\"RULE_DEFAULT\",\"K1\":\"RULE_DEFAULT\","
                + "\"Q1\":\"RULE_DEFAULT\",\"Q2\":\"" + q2Source
                + "\",\"N\":\"RULE_DEFAULT\",\"Q3\":\"DISPLAY_ONLY\"}");
        signLegacy(s);
        return s;
    }

    private void signLegacy(BizWorkloadCalcSnapshot s)
    {
        s.setSnapshotHash(com.workload.system.service.SnapshotHashTestFixture.legacyHash(s));
    }

    @Test
    void shouldExplainDirectSemesterG11WithItsSourceBatch()
    {
        BizWlManagement detail = new BizWlManagement();
        detail.setProratedAmount(n("90"));
        detail.setSourceBatchId("POSITION-2025-1");
        when(managementMapper.selectBizWlManagementByItemId(7L)).thenReturn(detail);

        FactorFormulaVo result = builder.build(item("G11", "IMPORT", "90"));

        assertThat(result).isNotNull();
        assertThat(result.getExpression()).isEqualTo("岗位减免工作量（本学期）");
        assertThat(result.getResult()).isEqualByComparingTo("90");
        assertThat(result.getDescription()).contains("来源批次 POSITION-2025-1", "计入 G11", "180");
        assertThat(result.getFactors()).hasSize(1);
        assertThat(result.getFactors().get(0).getValue()).isEqualTo(n("90"));
        verifyNoInteractions(ruleParamService);
    }

    @Test
    void shouldBuildG1AndKeepQ3DisplayOnly()
    {
        BizWorkloadItem item = item("G1", "IMPORT", "42.24");
        BizWlTheory d = new BizWlTheory();
        d.setJ1(n("32")); d.setC1(n("1")); d.setK1(n("1.1"));
        d.setQ1(n("1")); d.setQ2(n("1")); d.setQ3(n("1.5")); d.setN(n("1.2"));
        when(theoryMapper.selectBizWlTheoryByItemId(7L)).thenReturn(d);

        FactorFormulaVo result = builder.build(item);

        assertThat(result.getExpression()).isEqualTo("J1 × C1 × K1 × Q1 × Q2 × N");
        assertThat(result.getResult()).isEqualByComparingTo("42.24");
        assertThat(result.getFactors()).extracting(FactorFormulaVo.FactorVo::getKey)
                .containsExactly("J1", "C1", "K1", "Q1", "Q2", "N", "Q3");
        assertThat(result.getFactors().get(6).getStatus()).isEqualTo("DISPLAY_ONLY");
    }

    @Test
    void shouldDescribeG4WithoutCappingActualHeadcount()
    {
        BizWorkloadItem item = item("G4", "MANUAL", "28.00");
        item.setIsOverLimit(1);
        BizWlCourseDesign d = new BizWlCourseDesign();
        d.setJ4(n("1")); d.setR4(70L);
        when(courseDesignMapper.selectBizWlCourseDesignByItemId(7L)).thenReturn(d);
        when(ruleParamService.get("CONST_COURSE_DESIGN", n("0.4"), java.time.LocalDate.of(2026, 1, 31))).thenReturn(n("0.5"));

        FactorFormulaVo result = builder.build(item);

        assertThat(result.getExpression()).isEqualTo("J4 × R4 × 0.5");
        assertThat(result.getDescription()).contains("实际人数", "不截断", "仅告警");
        assertThat((BigDecimal) result.getFactors().get(1).getValue()).isEqualByComparingTo("70");
        assertThat(result.getFactors().get(1).getStatus()).isEqualTo("WARNING");
        assertThat(result.getFactors().get(2).getSource()).isEqualTo("FORMULA_CONSTANT");
        assertThat(result.getFactors().get(2).getValue()).isEqualTo(n("0.5"));
        assertThat(result.isReproducible()).isFalse();
    }

    @Test
    void shouldDescribeG5ApprovalWithoutCapping()
    {
        BizWorkloadItem item = item("G5", "IMPORT", "99.00");
        item.setIsOverLimit(1);
        BizWlThesis d = new BizWlThesis();
        d.setR5(11L); d.setK5(n("9"));
        when(thesisMapper.selectBizWlThesisByItemId(7L)).thenReturn(d);

        FactorFormulaVo result = builder.build(item);

        assertThat(result.getExpression()).isEqualTo("R5 × K5");
        assertThat(result.getDescription()).contains("实际人数", "未封顶", "报批");
        assertThat(result.getFactors().get(0).getStatus()).isEqualTo("APPROVAL_REQUIRED");
    }

    @Test
    void shouldDescribeG6CappingAtTwenty()
    {
        BizWorkloadItem item = item("G6", "IMPORT", "32.00");
        BizWlConcentratedInternship d = new BizWlConcentratedInternship();
        d.setW(n("4")); d.setR6(25L);
        when(concentratedInternshipMapper.selectBizWlConcentratedInternshipByItemId(7L)).thenReturn(d);
        when(ruleParamService.get("CAP_R6_MAX", n("20"), java.time.LocalDate.of(2026, 1, 31))).thenReturn(n("24"));
        when(ruleParamService.get("CONST_COURSE_DESIGN", n("0.4"), java.time.LocalDate.of(2026, 1, 31))).thenReturn(n("0.5"));

        FactorFormulaVo result = builder.build(item);

        assertThat(result.getExpression()).isEqualTo("W × min(R6, 24) × 0.5");
        assertThat(result.isReproducible()).isFalse();
        assertThat(result.getFactors()).extracting(FactorFormulaVo.FactorVo::getKey)
                .containsExactly("W", "R6", "R6_EFFECTIVE", "CONST");
        assertThat(result.getFactors().get(1).getStatus()).isEqualTo("CAPPED");
        assertThat(result.getFactors().get(1).getValue()).isEqualTo(n("25"));
        assertThat((BigDecimal) result.getFactors().get(2).getValue()).isEqualByComparingTo("24");
        assertThat(result.getFactors().get(2).getSource()).isEqualTo("DERIVED");
        assertThat(result.getFactors().get(2).getDescription()).contains("min(R6, 24)");
        assertThat(result.getFactors().get(3).getValue()).isEqualTo(n("0.5"));
    }

    @Test
    void shouldReturnNullForAggregateTypeWithoutQueryingDetailTables()
    {
        assertThat(builder.build(item("G8", "MANUAL", "10"))).isNull();
        verifyNoInteractions(theoryMapper, practiceMapper, internshipTrainingMapper,
                courseDesignMapper, thesisMapper, concentratedInternshipMapper);
    }

    @Test
    void shouldUseCurrentExpressionsForG2AndG3()
    {
        BizWorkloadItem g2 = item("G2", "IMPORT", "12.96");
        BizWlPractice p = new BizWlPractice();
        p.setJ2(n("16")); p.setK(n("0.9")); p.setC2(n("0.9"));
        p.setQ1(n("1")); p.setQ2(n("1")); p.setQ3(n("1.5"));
        when(practiceMapper.selectBizWlPracticeByItemId(7L)).thenReturn(p);
        assertThat(builder.build(g2).getExpression()).isEqualTo("J2 × K × C2 × Q1 × Q2");

        BizWorkloadItem g3 = item("G3", "IMPORT", "10");
        BizWlInternshipTraining t = new BizWlInternshipTraining();
        t.setT(n("5")); t.setD(n("2")); t.setK(n("1"));
        t.setQ1(n("1")); t.setQ2(n("1")); t.setQ3(n("1.5"));
        when(internshipTrainingMapper.selectBizWlInternshipTrainingByItemId(7L)).thenReturn(t);
        assertThat(builder.build(g3).getExpression()).isEqualTo("T × D × K × Q1 × Q2");
    }

    @Test
    void shouldMarkNullableCoefficientsAsNotReproducible()
    {
        BizWorkloadItem item = item("G1", "IMPORT", "32.00");
        BizWlTheory detail = new BizWlTheory();
        detail.setJ1(n("32"));
        when(theoryMapper.selectBizWlTheoryByItemId(7L)).thenReturn(detail);

        FactorFormulaVo result = builder.build(item);

        assertThat(result.getFactors()).filteredOn(factor -> !"J1".equals(factor.getKey())
                && !"Q3".equals(factor.getKey()))
                .extracting(FactorFormulaVo.FactorVo::getValue)
                .containsOnlyNulls();
        assertThat(result.isReproducible()).isFalse();
        assertThat(result.getDescription()).contains("缺失因子");
    }

    private BizWorkloadItem item(String type, String source, String workload)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setSemester("2025-2026-1");
        item.setId(7L);
        item.setItemType(type);
        item.setSourceType(source);
        item.setCalculationVersion(3L);
        item.setCalculatedWorkload(n(workload));
        return item;
    }

    private BigDecimal n(String value)
    {
        return new BigDecimal(value);
    }
}
