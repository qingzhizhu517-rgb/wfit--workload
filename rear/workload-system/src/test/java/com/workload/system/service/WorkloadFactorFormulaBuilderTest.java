package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizWlConcentratedInternship;
import com.workload.system.domain.BizWlCourseDesign;
import com.workload.system.domain.BizWlInternshipTraining;
import com.workload.system.domain.BizWlManagement;
import com.workload.system.domain.BizWlPractice;
import com.workload.system.domain.BizWlTheory;
import com.workload.system.domain.BizWlThesis;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.service.IBizWlConcentratedInternshipService;
import com.workload.system.service.IBizWlCourseDesignService;
import com.workload.system.service.IBizWlInternshipTrainingService;
import com.workload.system.service.IBizWlManagementService;
import com.workload.system.service.IBizWlPracticeService;
import com.workload.system.service.IBizWlTheoryService;
import com.workload.system.service.IBizWlThesisService;

@ExtendWith(MockitoExtension.class)
class WorkloadFactorFormulaBuilderTest
{
    @InjectMocks private WorkloadFactorFormulaServiceImpl builder;
    @Mock private IBizWlTheoryService theoryService;
    @Mock private IBizWlPracticeService practiceService;
    @Mock private IBizWlInternshipTrainingService internshipTrainingService;
    @Mock private IBizWlCourseDesignService courseDesignService;
    @Mock private IBizWlThesisService thesisService;
    @Mock private IBizWlConcentratedInternshipService concentratedInternshipService;
    @Mock private IBizWlManagementService managementService;
    @Mock private RuleParamService ruleParamService;

    @Test
    void shouldExplainDirectSemesterG11WithItsSourceBatch()
    {
        BizWlManagement detail = new BizWlManagement();
        detail.setProratedAmount(n("90"));
        detail.setSourceBatchId("POSITION-2025-1");
        when(managementService.selectBizWlManagementByItemId(7L)).thenReturn(detail);

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
        when(theoryService.selectBizWlTheoryByItemId(7L)).thenReturn(d);

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
        when(courseDesignService.selectBizWlCourseDesignByItemId(7L)).thenReturn(d);
        when(ruleParamService.get("CONST_COURSE_DESIGN", n("0.4"))).thenReturn(n("0.5"));

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
        when(thesisService.selectBizWlThesisByItemId(7L)).thenReturn(d);

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
        when(concentratedInternshipService.selectBizWlConcentratedInternshipByItemId(7L)).thenReturn(d);
        when(ruleParamService.get("CAP_R6_MAX", n("20"))).thenReturn(n("24"));
        when(ruleParamService.get("CONST_COURSE_DESIGN", n("0.4"))).thenReturn(n("0.5"));

        FactorFormulaVo result = builder.build(item);

        assertThat(result.getExpression()).isEqualTo("W × min(R6, 24) × 0.5");
        assertThat(result.isReproducible()).isFalse();
        assertThat(result.getFactors()).extracting(FactorFormulaVo.FactorVo::getKey)
                .containsExactly("W", "R6", "R6_EFFECTIVE", "CONST");
        assertThat(result.getFactors().get(1).getStatus()).isEqualTo("CAPPED");
        assertThat(result.getFactors().get(1).getValue()).isEqualTo(n("25"));
        assertThat((BigDecimal) result.getFactors().get(2).getValue()).isEqualByComparingTo("24");
        assertThat(result.getFactors().get(2).getSource()).isEqualTo("CURRENT_RULE_DERIVED");
        assertThat(result.getFactors().get(2).getDescription()).contains("min(R6, 24)");
        assertThat(result.getFactors().get(3).getValue()).isEqualTo(n("0.5"));
    }

    @Test
    void shouldReturnNullForAggregateTypeWithoutQueryingDetailTables()
    {
        assertThat(builder.build(item("G8", "MANUAL", "10"))).isNull();
        verifyNoInteractions(theoryService, practiceService, internshipTrainingService,
                courseDesignService, thesisService, concentratedInternshipService);
    }

    @Test
    void shouldUseCurrentExpressionsForG2AndG3()
    {
        BizWorkloadItem g2 = item("G2", "IMPORT", "12.96");
        BizWlPractice p = new BizWlPractice();
        p.setJ2(n("16")); p.setK(n("0.9")); p.setC2(n("0.9"));
        p.setQ1(n("1")); p.setQ2(n("1")); p.setQ3(n("1.5"));
        when(practiceService.selectBizWlPracticeByItemId(7L)).thenReturn(p);
        assertThat(builder.build(g2).getExpression()).isEqualTo("J2 × K × C2 × Q1 × Q2");

        BizWorkloadItem g3 = item("G3", "IMPORT", "10");
        BizWlInternshipTraining t = new BizWlInternshipTraining();
        t.setT(n("5")); t.setD(n("2")); t.setK(n("1"));
        t.setQ1(n("1")); t.setQ2(n("1")); t.setQ3(n("1.5"));
        when(internshipTrainingService.selectBizWlInternshipTrainingByItemId(7L)).thenReturn(t);
        assertThat(builder.build(g3).getExpression()).isEqualTo("T × D × K × Q1 × Q2");
    }

    @Test
    void shouldMarkNullableCoefficientsAsNotReproducible()
    {
        BizWorkloadItem item = item("G1", "IMPORT", "32.00");
        BizWlTheory detail = new BizWlTheory();
        detail.setJ1(n("32"));
        when(theoryService.selectBizWlTheoryByItemId(7L)).thenReturn(detail);

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
        item.setId(7L);
        item.setItemType(type);
        item.setSourceType(source);
        item.setCalculatedWorkload(n(workload));
        return item;
    }

    private BigDecimal n(String value)
    {
        return new BigDecimal(value);
    }
}
