package com.workload.system.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.calc.strategy.WorkloadCalcStrategy;
import com.workload.system.domain.BizWlThesis;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.dto.TeachingTaskImportDTO;
import com.workload.system.mapper.BizWlThesisMapper;

class TeachingTaskImportServiceImplTest
{
    @Test
    void scienceAliasUsesScienceExportColumn() throws Exception
    {
        TeachingTaskImportServiceImpl service = new TeachingTaskImportServiceImpl();
        BizWlThesisMapper thesisMapper = mock(BizWlThesisMapper.class);
        CalcStrategyFactory strategyFactory = mock(CalcStrategyFactory.class);
        WorkloadCalcStrategy strategy = mock(WorkloadCalcStrategy.class);
        when(strategyFactory.get("G5")).thenReturn(strategy);

        java.lang.reflect.Field mapperField = TeachingTaskImportServiceImpl.class.getDeclaredField("wlThesisMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, thesisMapper);
        java.lang.reflect.Field factoryField = TeachingTaskImportServiceImpl.class.getDeclaredField("calcStrategyFactory");
        factoryField.setAccessible(true);
        factoryField.set(service, strategyFactory);

        TeachingTaskImportDTO dto = new TeachingTaskImportDTO();
        dto.setMajorCategory("理工科");
        dto.setEducationLevel("本科");
        dto.setCourseCoefficient(java.math.BigDecimal.ONE);

        Method method = TeachingTaskImportServiceImpl.class
                .getDeclaredMethod("createG5Detail", TeachingTaskImportDTO.class, BizWorkloadItem.class);
        method.setAccessible(true);
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(7L);
        method.invoke(service, dto, item);

        ArgumentCaptor<BizWlThesis> captor = ArgumentCaptor.forClass(BizWlThesis.class);
        verify(thesisMapper).insertBizWlThesis(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getDisciplineCategory())
                .isEqualTo("SCITECH");
    }
}
