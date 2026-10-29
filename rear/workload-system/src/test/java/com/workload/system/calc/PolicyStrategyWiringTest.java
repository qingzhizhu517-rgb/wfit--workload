package com.workload.system.calc;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.calc.strategy.*;
import com.workload.system.mapper.*;
import com.workload.system.domain.*;
import java.math.BigDecimal;
import java.util.List;

class PolicyStrategyWiringTest {
    @Test void springCreatesAndInvokesAllThreeStrategiesWithTheirMapperDependencies() {
        try (var context=new AnnotationConfigApplicationContext()) {
            var g4=mock(BizWlCourseDesignMapper.class);var g5=mock(BizWlThesisMapper.class);var g6=mock(BizWlConcentratedInternshipMapper.class);
            BizWlCourseDesign d4=new BizWlCourseDesign();d4.setJ4(BigDecimal.ONE);d4.setR4(10L);
            BizWlThesis d5=new BizWlThesis();d5.setR5(2L);d5.setK5(BigDecimal.ONE);
            BizWlConcentratedInternship d6=new BizWlConcentratedInternship();d6.setW(BigDecimal.ONE);d6.setR6(10L);
            when(g4.selectBizWlCourseDesignByItemId(1L)).thenReturn(d4);
            when(g5.selectBizWlThesisByItemId(1L)).thenReturn(d5);
            when(g6.selectBizWlConcentratedInternshipByItemId(1L)).thenReturn(d6);
            var rules=mock(RuleParamService.class);when(rules.get(anyString(),any(BigDecimal.class),any(java.time.LocalDate.class))).thenAnswer(i -> i.getArgument(1));
            context.registerBean(BizWlCourseDesignMapper.class,()->g4);context.registerBean(BizWlThesisMapper.class,()->g5);context.registerBean(BizWlConcentratedInternshipMapper.class,()->g6);
            context.registerBean(RuleParamService.class,()->rules);context.registerBean(SemesterCalendar.class,SemesterCalendar::new);
            context.register(CourseDesignCalcStrategy.class,ThesisCalcStrategy.class,ConcentratedInternshipCalcStrategy.class);
            context.refresh();
            BizWorkloadItem item=new BizWorkloadItem();item.setId(1L);item.setSemester("2025-2026-1");
            for (Class<? extends WorkloadCalcStrategy> type:List.of(CourseDesignCalcStrategy.class,ThesisCalcStrategy.class,ConcentratedInternshipCalcStrategy.class))
                assertThat(context.getBean(type).calculate(item)).isPositive();
        }
    }
}
