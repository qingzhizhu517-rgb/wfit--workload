package com.workload.system.service.impl;

import java.math.BigDecimal;
import com.workload.system.calc.rule.CalculationPolicyContext;
import com.workload.system.calc.rule.PolicyHash;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import com.workload.common.core.domain.entity.SysUser;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.calc.strategy.WorkloadCalcStrategy;
import com.workload.system.domain.*;
import com.workload.system.domain.dto.TeachingTaskImportDTO;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.mapper.*;
import com.workload.system.service.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class TeachingTaskInputContractTest
{
    private TeachingTaskImportDTO row(String type)
    {
        TeachingTaskImportDTO row = new TeachingTaskImportDTO();
        row.setSemester("2025-2026-1");
        row.setUserCode("T008");
        row.setCourseName("测试课程");
        row.setWorkloadType(type);
        switch (type)
        {
            case "G1", "G2" -> row.setPlannedHours(new BigDecimal("48"));
            case "G3" -> row.setDays(new BigDecimal("5"));
            case "G4" -> row.setCredits(new BigDecimal("2"));
            case "G6" -> row.setWeeks(new BigDecimal("4"));
            default -> { }
        }
        return row;
    }

    @ParameterizedTest
    @CsvSource({"G1,-1", "G2,0", "G3,-2", "G4,0", "G5,-2", "G6,0"})
    void rejectsNonPositiveStudentCountBeforeDatabaseAccess(String type, int count)
    {
        TeachingTaskImportServiceImpl service = new TeachingTaskImportServiceImpl();
        ISysUserService users = mock(ISysUserService.class);
        ReflectionTestUtils.setField(service, "sysUserService", users);
        TeachingTaskImportDTO input = row(type);
        input.setStudentCount(count);
        assertThatThrownBy(() -> service.processSingleRow(input, "IMP"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("人数");
        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5"})
    void rejectsNonPositiveExplicitCoefficientBeforeDatabaseAccess(String coefficient)
    {
        TeachingTaskImportServiceImpl service = new TeachingTaskImportServiceImpl();
        ISysUserService users = mock(ISysUserService.class);
        ReflectionTestUtils.setField(service, "sysUserService", users);
        TeachingTaskImportDTO input = row("G2");
        input.setCourseCoefficient(new BigDecimal(coefficient));
        assertThatThrownBy(() -> service.processSingleRow(input, "IMP"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("系数");
        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"G4", "G5", "G6"})
    void guidanceRequiresStudentCount(String type)
    {
        TeachingTaskImportServiceImpl service = new TeachingTaskImportServiceImpl();
        ISysUserService users = mock(ISysUserService.class);
        ReflectionTestUtils.setField(service, "sysUserService", users);
        assertThatThrownBy(() -> service.processSingleRow(row(type), "IMP"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("人数");
        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void persistedDefaultsAndFactorsAgree(String blank)
    {
        TeachingTaskImportServiceImpl service = new TeachingTaskImportServiceImpl();
        ISysUserService users = mock(ISysUserService.class);
        when(users.selectUserByUserName("T008")).thenReturn(new SysUser(8L));
        BizTeachingTaskMapper tasks = mock(BizTeachingTaskMapper.class);
        BizWorkloadItemMapper items = mock(BizWorkloadItemMapper.class);
        BizWlTheoryMapper theory = mock(BizWlTheoryMapper.class);
        BizWlPracticeMapper practice = mock(BizWlPracticeMapper.class);
        RuleParamService rules = mock(RuleParamService.class);
        when(rules.resolve(anyString(), org.mockito.ArgumentMatchers.eq(java.time.LocalDate.of(2026, 1, 31)), any(BigDecimal.class), anyString())).thenAnswer(i -> com.workload.system.calc.rule.RuleResolution.fallback(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3), "NO_EFFECTIVE_RULE"));
        CalcStrategyFactory factory = mock(CalcStrategyFactory.class);
        WorkloadCalcStrategy strategy = mock(WorkloadCalcStrategy.class);
        when(factory.get(anyString())).thenReturn(strategy);
        when(strategy.calculate(any(), any(CalculationPolicyContext.class))).thenReturn(BigDecimal.ONE);
        IWorkloadFactorFormulaService formulas = mock(IWorkloadFactorFormulaService.class);
        when(formulas.buildFresh(any(), any(CalculationPolicyContext.class))).thenReturn(mock(FactorFormulaVo.class));
        WorkloadSnapshotService snapshots = mock(WorkloadSnapshotService.class);
        BizWorkloadCalcSnapshot snapshot = new BizWorkloadCalcSnapshot();
        snapshot.setCalculationVersion(1L);
        when(snapshots.capture(any(), any(), anyString())).thenReturn(snapshot);
        ReflectionTestUtils.setField(service, "sysUserService", users);
        ReflectionTestUtils.setField(service, "teachingTaskMapper", tasks);
        ReflectionTestUtils.setField(service, "workloadItemMapper", items);
        ReflectionTestUtils.setField(service, "wlTheoryMapper", theory);
        ReflectionTestUtils.setField(service, "wlPracticeMapper", practice);
        ReflectionTestUtils.setField(service, "ruleParamService", rules);
        ReflectionTestUtils.setField(service, "calcStrategyFactory", factory);
        ReflectionTestUtils.setField(service, "workloadWriteGuard", mock(WorkloadWriteGuard.class));
        ReflectionTestUtils.setField(service, "factorFormulaService", formulas);
        ReflectionTestUtils.setField(service, "snapshotService", snapshots);
        for (String type : new String[]{"G1", "G2"})
        {
            TeachingTaskImportDTO input = row(type);
            input.setCourseNature(blank);
            input.setMajorCategory(blank);
            service.processSingleRow(input, "IMP");
        }
        ArgumentCaptor<BizTeachingTask> task = ArgumentCaptor.forClass(BizTeachingTask.class);
        verify(tasks, times(2)).insertBizTeachingTask(task.capture());
        assertThat(task.getAllValues()).allSatisfy(t -> {
            assertThat(t.getCourseNature()).isEqualTo("必修");
            assertThat(t.getMajorCategory()).isEqualTo("理工类");
        });
        ArgumentCaptor<BizWlTheory> g1 = ArgumentCaptor.forClass(BizWlTheory.class);
        verify(theory).insertBizWlTheory(g1.capture());
        assertThat(g1.getValue().getK1()).isEqualByComparingTo("1.1");
        ArgumentCaptor<BizWlPractice> g2 = ArgumentCaptor.forClass(BizWlPractice.class);
        verify(practice).insertBizWlPractice(g2.capture());
        assertThat(g2.getValue().getK()).isEqualByComparingTo("1.0");
    }
}
