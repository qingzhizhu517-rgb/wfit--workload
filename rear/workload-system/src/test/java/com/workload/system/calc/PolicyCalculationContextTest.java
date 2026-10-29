package com.workload.system.calc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import com.workload.system.calc.rule.*;
import com.workload.system.calc.strategy.*;
import com.workload.system.domain.*;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.mapper.*;
import com.workload.system.service.*;
import com.workload.system.service.impl.WorkloadFactorFormulaServiceImpl;

class PolicyCalculationContextTest {
    private BizWorkloadItem item(String type) { BizWorkloadItem i=new BizWorkloadItem();i.setId(7L);i.setItemType(type);i.setSemester("2025-2026-1"); return i; }
    private CalculationPolicyContext context(Map<String,Integer> calls) {
        return new CalculationPolicyContext(LocalDate.of(2026,1,31),(code,date,f,p) -> {
            int count=calls.merge(code,1,Integer::sum);
            return RuleResolution.fallback(code,date,count==1 ? f : new BigDecimal("999"),p,"NO_EFFECTIVE_RULE");
        });
    }
    @Test void g4SharesActualConstantAndWarningAcrossStrategyCallbackAndFormula() {
        BizWlCourseDesignMapper mapper=mock(BizWlCourseDesignMapper.class);
        BizWlCourseDesign d=new BizWlCourseDesign();d.setJ4(new BigDecimal("2"));d.setR4(70L);
        when(mapper.selectBizWlCourseDesignByItemId(7L)).thenReturn(d);
        CourseDesignCalcStrategy strategy=new CourseDesignCalcStrategy(); ReflectionTestUtils.setField(strategy,"bizWlCourseDesignMapper",mapper);
        WorkloadFactorFormulaServiceImpl builder=new WorkloadFactorFormulaServiceImpl();ReflectionTestUtils.setField(builder,"courseDesignMapper",mapper);
        Map<String,Integer> calls=new HashMap<>();CalculationPolicyContext c=context(calls);BizWorkloadItem i=item("G4");
        BigDecimal value=strategy.calculate(i,c);i.setCalculatedWorkload(value);strategy.afterCalculated(i,value,c);
        FactorFormulaVo f=builder.buildFresh(i,c);
        assertThat(value).isEqualByComparingTo("56");assertThat(i.getIsOverLimit()).isEqualTo(1);
        assertThat(calls).containsOnly(entry("CONST_COURSE_DESIGN",1),entry("CAP_R4_MAX",1));
        assertThat(f.getPolicySources()).hasSize(2);assertThat(f.getDecisionEvidence()).hasSize(1);
        assertThat(f.getEvidenceStatus()).isEqualTo("COMPLETE");assertThat(f.isReproducible()).isTrue();
        new WorkloadSnapshotService(mock(BizWorkloadCalcSnapshotMapper.class)).capture(i,f,c.policySetHash());
    }
    @Test void g6CapturesCapConstantAndDerivedBindingWithoutSecondResolution() {
        BizWlConcentratedInternshipMapper mapper=mock(BizWlConcentratedInternshipMapper.class);
        BizWlConcentratedInternship d=new BizWlConcentratedInternship();d.setW(new BigDecimal("2"));d.setR6(30L);
        when(mapper.selectBizWlConcentratedInternshipByItemId(7L)).thenReturn(d);
        ConcentratedInternshipCalcStrategy strategy=new ConcentratedInternshipCalcStrategy();ReflectionTestUtils.setField(strategy,"bizWlConcentratedInternshipMapper",mapper);
        WorkloadFactorFormulaServiceImpl builder=new WorkloadFactorFormulaServiceImpl();ReflectionTestUtils.setField(builder,"concentratedInternshipMapper",mapper);
        Map<String,Integer> calls=new HashMap<>();CalculationPolicyContext c=context(calls);BizWorkloadItem i=item("G6");
        BigDecimal value=strategy.calculate(i,c);i.setCalculatedWorkload(value);strategy.afterCalculated(i,value,c);
        FactorFormulaVo f=builder.buildFresh(i,c);assertThat(value).isEqualByComparingTo("16");
        assertThat(calls.values()).containsOnly(1);assertThat(f.getEvidenceStatus()).isEqualTo("COMPLETE");
        new WorkloadSnapshotService(mock(BizWorkloadCalcSnapshotMapper.class)).capture(i,f,c.policySetHash());
    }
    @Test void g5RecordsSelectedEducationThresholdButDoesNotInferHistoricalK5Source() {
        for (String level:List.of("本科","专科")) {
            BizWlThesisMapper mapper=mock(BizWlThesisMapper.class);BizWlThesis d=new BizWlThesis();d.setR5(20L);d.setK5(new BigDecimal("9"));d.setEducationLevel(level);
            when(mapper.selectBizWlThesisByItemId(7L)).thenReturn(d);
            ThesisCalcStrategy strategy=new ThesisCalcStrategy();ReflectionTestUtils.setField(strategy,"bizWlThesisMapper",mapper);
            WorkloadFactorFormulaServiceImpl builder=new WorkloadFactorFormulaServiceImpl();ReflectionTestUtils.setField(builder,"thesisMapper",mapper);
            CalculationPolicyContext c=context(new HashMap<>());BizWorkloadItem i=item("G5");BigDecimal v=strategy.calculate(i,c);i.setCalculatedWorkload(v);strategy.afterCalculated(i,v,c);
            FactorFormulaVo f=builder.buildFresh(i,c);
            assertThat(c.sources()).extracting(RuleResolution::requestedCode).containsExactly(level.equals("本科")?"APPROVAL_R5_BACHELOR":"APPROVAL_R5_JUNIOR");
            assertThat(f.getEvidenceStatus()).isEqualTo("PARTIAL");
            assertThat(f.getFactorEvidence()).filteredOn(e -> e.key().equals("K5")).allMatch(e -> e.kind().equals("MISSING_HISTORICAL_SOURCE"));
            new WorkloadSnapshotService(mock(BizWorkloadCalcSnapshotMapper.class)).capture(i,f,c.policySetHash());
        }
    }
    @Test void equalMaterializedG1CoefficientNeverBorrowsCurrentRuleOrApproval() {
        BizWlTheoryMapper mapper=mock(BizWlTheoryMapper.class);BizWlTheory d=new BizWlTheory();
        d.setJ1(new BigDecimal("32"));d.setC1(BigDecimal.ONE);d.setK1(BigDecimal.ONE);d.setQ1(BigDecimal.ONE);d.setQ2(BigDecimal.ONE);d.setN(BigDecimal.ONE);
        when(mapper.selectBizWlTheoryByItemId(7L)).thenReturn(d);
        WorkloadFactorFormulaServiceImpl builder=new WorkloadFactorFormulaServiceImpl();ReflectionTestUtils.setField(builder,"theoryMapper",mapper);
        RuleParamService rules=mock(RuleParamService.class);BizCoefficientAdjustmentMapper approvals=mock(BizCoefficientAdjustmentMapper.class);
        ReflectionTestUtils.setField(builder,"ruleParamService",rules);
        CalculationPolicyContext c=new CalculationPolicyContext(LocalDate.of(2026,1,31),rules);BizWorkloadItem i=item("G1");i.setCalculatedWorkload(new BigDecimal("32"));
        FactorFormulaVo f=builder.buildFresh(i,c);
        assertThat(f.isReproducible()).isTrue();assertThat(f.getEvidenceStatus()).isEqualTo("PARTIAL");
        assertThat(f.getPolicySources()).isEmpty();assertThat(f.getAsOfDate()).isEqualTo("2026-01-31");assertThat(f.getFactorEvidence()).filteredOn(e -> e.key().equals("Q2"))
                .allMatch(e -> e.kind().equals("MISSING_HISTORICAL_SOURCE") && e.ruleRef()==null);
        verifyNoInteractions(rules,approvals);
        var s=new WorkloadSnapshotService(mock(BizWorkloadCalcSnapshotMapper.class)).capture(i,f,c.policySetHash());
        SnapshotIntegrity.verify(s);
    }

    @Test void missingRecordedCountsKeepOriginalNullAsZeroCalculationAndPartialEvidence() {
        for (String type:List.of("G4","G5","G6")) {
            BizWorkloadItem i=item(type);WorkloadCalcStrategy strategy;WorkloadFactorFormulaServiceImpl builder=new WorkloadFactorFormulaServiceImpl();
            if (type.equals("G4")) {
                var mapper=mock(BizWlCourseDesignMapper.class);var d=new BizWlCourseDesign();d.setJ4(BigDecimal.ONE);when(mapper.selectBizWlCourseDesignByItemId(7L)).thenReturn(d);
                var st=new CourseDesignCalcStrategy();ReflectionTestUtils.setField(st,"bizWlCourseDesignMapper",mapper);strategy=st;ReflectionTestUtils.setField(builder,"courseDesignMapper",mapper);
            } else if (type.equals("G5")) {
                var mapper=mock(BizWlThesisMapper.class);var d=new BizWlThesis();d.setK5(BigDecimal.ONE);when(mapper.selectBizWlThesisByItemId(7L)).thenReturn(d);
                var st=new ThesisCalcStrategy();ReflectionTestUtils.setField(st,"bizWlThesisMapper",mapper);strategy=st;ReflectionTestUtils.setField(builder,"thesisMapper",mapper);
            } else {
                var mapper=mock(BizWlConcentratedInternshipMapper.class);var d=new BizWlConcentratedInternship();d.setW(BigDecimal.ONE);when(mapper.selectBizWlConcentratedInternshipByItemId(7L)).thenReturn(d);
                var st=new ConcentratedInternshipCalcStrategy();ReflectionTestUtils.setField(st,"bizWlConcentratedInternshipMapper",mapper);strategy=st;ReflectionTestUtils.setField(builder,"concentratedInternshipMapper",mapper);
            }
            CalculationPolicyContext c=context(new HashMap<>());BigDecimal v=strategy.calculate(i,c);i.setCalculatedWorkload(v);strategy.afterCalculated(i,v,c);
            assertThat(v).isZero();FactorFormulaVo f=builder.buildFresh(i,c);assertThat(f.getEvidenceStatus()).isEqualTo("PARTIAL");assertThat(f.isReproducible()).isFalse();
            assertThat(f.getMissingEvidenceReasons()).anyMatch(reason -> reason.startsWith("MISSING_RECORDED_INPUT"));
            new WorkloadSnapshotService(mock(BizWorkloadCalcSnapshotMapper.class)).capture(i,f,c.policySetHash());
        }
    }

}
