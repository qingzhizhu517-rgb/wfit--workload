package com.workload.system.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.workload.system.calc.WorkloadCalcService;
import com.workload.system.calc.WorkloadCalcServiceImpl;
import com.workload.system.service.impl.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** 实例化真实核算/解释/明细服务；外围存储与参数服务替身不替代被测依赖链。 */
class CalculationContextTest
{
    @Test
    void calculationServicesStartWithoutCircularReferenceCompatibilityFlag()
    {
        List<Class<?>> actual = List.of(WorkloadCalcServiceImpl.class,
                WorkloadFactorFormulaServiceImpl.class, BizWlTheoryServiceImpl.class,
                BizWlPracticeServiceImpl.class, BizWlInternshipTrainingServiceImpl.class,
                BizWlCourseDesignServiceImpl.class, BizWlThesisServiceImpl.class,
                BizWlConcentratedInternshipServiceImpl.class, BizWlManagementServiceImpl.class,
                BizTeachingTaskServiceImpl.class);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
        {
            context.getDefaultListableBeanFactory().setAllowCircularReferences(false);
            actual.forEach(context::register);
            Set<Class<?>> dependencies = new HashSet<>();
            for (Class<?> type : actual)
            {
                for (var field : type.getDeclaredFields())
                {
                    if (field.isAnnotationPresent(Autowired.class)
                            && actual.stream().noneMatch(field.getType()::isAssignableFrom))
                    {
                        dependencies.add(field.getType());
                    }
                }
            }
            dependencies.forEach(type -> registerMock(context, type));
            assertThatCode(context::refresh).doesNotThrowAnyException();
            assertThat(context.getBean(WorkloadCalcService.class)).isInstanceOf(WorkloadCalcServiceImpl.class);
            assertThat(context.getBean(IWorkloadFactorFormulaService.class)).isInstanceOf(WorkloadFactorFormulaServiceImpl.class);
        }
    }

    private <T> void registerMock(AnnotationConfigApplicationContext context, Class<T> type)
    {
        context.getBeanFactory().registerSingleton(type.getName(), mock(type));
    }
}
