package com.workload.system.calc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import com.workload.system.calc.allowance.*;
import com.workload.system.calc.rule.*;
import com.workload.system.calc.strategy.*;
import com.workload.system.domain.*;
import com.workload.system.domain.dto.TeachingTaskImportDTO;
import com.workload.system.mapper.*;
import com.workload.system.service.impl.TeachingTaskImportServiceImpl;

class SemesterDatePropagationTest {
    private static final String SEM="2025-2026-2";
    private static final LocalDate END=LocalDate.of(2026,6,30);
    private SemesterCalendar calendar() {var c=new SemesterCalendar();c.setSpringEnd("06-30");return c;}
    private RuleParamService rules() {
        var r=mock(RuleParamService.class);
        when(r.get(anyString(),any(BigDecimal.class),eq(END))).thenAnswer(i -> i.getArgument(1));return r;
    }
    @ParameterizedTest @ValueSource(strings={"A","B","C","D","E","F","G"})
    void everyAllowanceUsesItsOwnConfiguredSemesterEnd(String type) {
        AllowanceCalcStrategy strategy=switch(type) {
            case "A" -> new AllowanceAStrategy();case "B" -> new AllowanceBStrategy();case "C" -> new AllowanceCStrategy();
            case "D" -> new AllowanceDStrategy();case "E" -> new AllowanceEStrategy();case "F" -> new AllowanceFStrategy();default -> new AllowanceGStrategy();
        };
        var r=rules(); ReflectionTestUtils.setField(strategy,"ruleParamService",r);ReflectionTestUtils.setField(strategy,"semesterCalendar",calendar());
        var item=new BizAllowanceItem();item.setSemester(SEM);item.setFeeSubtype("自学");item.setStudentCount(5L);
        item.setDays(BigDecimal.ONE);item.setDurationHours(BigDecimal.ONE);item.setWorkloadUnits(BigDecimal.ONE);item.setClassCount(1L);
        assertThat(strategy.calculate(item)).isNotNull();
        verify(r,atLeastOnce()).get(anyString(),any(BigDecimal.class),eq(END));
        verify(r,never()).get(anyString(),any(BigDecimal.class));
    }
    @Test void g4CalculationAndThresholdUseSameHistoricalDate() {
        var strategy=new CourseDesignCalcStrategy();var r=rules();var m=mock(BizWlCourseDesignMapper.class);
        ReflectionTestUtils.setField(strategy,"ruleParamService",r);ReflectionTestUtils.setField(strategy,"semesterCalendar",calendar());ReflectionTestUtils.setField(strategy,"bizWlCourseDesignMapper",m);
        var d=new BizWlCourseDesign();d.setJ4(BigDecimal.ONE);d.setR4(61L);when(m.selectBizWlCourseDesignByItemId(1L)).thenReturn(d);
        var item=item();assertThat(strategy.calculate(item)).isEqualByComparingTo("24.4");strategy.afterCalculated(item,item.getCalculatedWorkload());
        verify(r).get("CONST_COURSE_DESIGN",new BigDecimal("0.4"),END);verify(r).get("CAP_R4_MAX",new BigDecimal("60"),END);
        assertThat(item.getIsOverLimit()).isEqualTo(1);
    }
    @Test void g6CalculationAndThresholdUseSameHistoricalDate() {
        var strategy=new ConcentratedInternshipCalcStrategy();var r=rules();var m=mock(BizWlConcentratedInternshipMapper.class);
        ReflectionTestUtils.setField(strategy,"ruleParamService",r);ReflectionTestUtils.setField(strategy,"semesterCalendar",calendar());ReflectionTestUtils.setField(strategy,"bizWlConcentratedInternshipMapper",m);
        var d=new BizWlConcentratedInternship();d.setW(BigDecimal.ONE);d.setR6(25L);when(m.selectBizWlConcentratedInternshipByItemId(1L)).thenReturn(d);
        var item=item();assertThat(strategy.calculate(item)).isEqualByComparingTo("8");strategy.afterCalculated(item,BigDecimal.ZERO);
        verify(r,times(2)).get("CAP_R6_MAX",new BigDecimal("20"),END);assertThat(item.getIsOverLimit()).isEqualTo(1);
    }
    @ParameterizedTest @ValueSource(strings={"本科","专科"})
    void g5ThresholdUsesSemesterDate(String level) {
        var strategy=new ThesisCalcStrategy();var r=rules();var m=mock(BizWlThesisMapper.class);
        ReflectionTestUtils.setField(strategy,"ruleParamService",r);ReflectionTestUtils.setField(strategy,"semesterCalendar",calendar());ReflectionTestUtils.setField(strategy,"bizWlThesisMapper",m);
        var d=new BizWlThesis();d.setR5(16L);d.setEducationLevel(level);when(m.selectBizWlThesisByItemId(1L)).thenReturn(d);
        var item=item();strategy.afterCalculated(item,BigDecimal.ZERO);
        verify(r).get("专科".equals(level)?"APPROVAL_R5_JUNIOR":"APPROVAL_R5_BACHELOR",new BigDecimal("专科".equals(level)?"15":"8"),END);
        assertThat(item.getIsOverLimit()).isEqualTo(1);
    }
    @Test void importDerivedRulesAllUseDtoSemesterEnd() {
        var service=new TeachingTaskImportServiceImpl();var r=rules();ReflectionTestUtils.setField(service,"ruleParamService",r);ReflectionTestUtils.setField(service,"semesterCalendar",calendar());
        var dto=new TeachingTaskImportDTO();dto.setSemester(SEM);dto.setStudentCount(151);dto.setEducationLevel("本科");dto.setMajorCategory("理工");
        ReflectionTestUtils.invokeMethod(service,"calcC1",1L,dto);ReflectionTestUtils.invokeMethod(service,"calcC1",2L,dto);ReflectionTestUtils.invokeMethod(service,"calcC1",3L,dto);
        ReflectionTestUtils.invokeMethod(service,"calcG3RepeatK",2L,dto);ReflectionTestUtils.invokeMethod(service,"calcG2K",dto);ReflectionTestUtils.invokeMethod(service,"calcG3D",dto);
        ReflectionTestUtils.invokeMethod(service,"calcG5K5",dto);ReflectionTestUtils.invokeMethod(service,"calcN",dto);
        verify(r,times(8)).get(anyString(),any(BigDecimal.class),eq(END));verify(r,never()).get(anyString(),any(BigDecimal.class));
    }
    private BizWorkloadItem item() {var i=new BizWorkloadItem();i.setId(1L);i.setSemester(SEM);return i;}
}
