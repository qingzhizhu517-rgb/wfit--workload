package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import com.workload.common.exception.ServiceException;
import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.calc.strategy.WorkloadCalcStrategy;
import com.workload.system.domain.BizWlThesis;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.dto.TeachingTaskImportDTO;
import com.workload.system.mapper.BizTeachingTaskMapper;
import com.workload.system.mapper.BizWlThesisMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.service.ISysUserService;

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

        setField(service, "wlThesisMapper", thesisMapper);
        setField(service, "calcStrategyFactory", strategyFactory);

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

    /**
     * 分类模板放行同类行：G1 模板导入 G1 行、G2 模板导入 G2 行、G3 模板导入 G3 行时，
     * 模板类别校验不拦截，导入流程继续推进（此处后续在工号查找处失败，证明已越过类别闸门，
     * 且报错不含「模板类别」文案）。
     */
    @ParameterizedTest
    @CsvSource({"G1,G1", "G2,G2", "G3,G3"})
    void typedImportAcceptsMatchingCategory(String templateType, String rowType) throws Exception
    {
        TeachingTaskImportServiceImpl service = new TeachingTaskImportServiceImpl();
        // 工号查找返回 null（默认 mock 行为），使匹配类别的行在越过类别闸门后于查找处失败
        setField(service, "sysUserService", mock(ISysUserService.class));

        assertThatThrownBy(() -> service.processSingleRow(validRow(rowType), "IMP", templateType))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不存在")
                .hasMessageNotContaining("模板类别");
    }

    /**
     * G1 模板遇到 G2 行：写库前即拒绝，报错含「模板类别 G1」，且不触库
     * （既不 insert 教学任务，也不 insert 工作量明细）。
     */
    @Test
    void g1TemplateRejectsG2RowBeforeInsert() throws Exception
    {
        TeachingTaskImportServiceImpl service = new TeachingTaskImportServiceImpl();
        BizTeachingTaskMapper taskMapper = mock(BizTeachingTaskMapper.class);
        BizWorkloadItemMapper itemMapper = mock(BizWorkloadItemMapper.class);
        setField(service, "teachingTaskMapper", taskMapper);
        setField(service, "workloadItemMapper", itemMapper);

        assertThatThrownBy(() -> service.processSingleRow(validRow("G2"), "IMP", "G1"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("模板类别 G1");

        verify(taskMapper, never()).insertBizTeachingTask(any());
        verifyNoInteractions(itemMapper);
    }

    /** 构造一条通过 validateRow 校验的行（学期/工号/课程/类别/学时齐全） */
    private TeachingTaskImportDTO validRow(String workloadType)
    {
        TeachingTaskImportDTO dto = new TeachingTaskImportDTO();
        dto.setSemester("2025-2026-1");
        dto.setUserCode("T999");
        dto.setCourseName("测试课程");
        dto.setWorkloadType(workloadType);
        dto.setBaseValue(java.math.BigDecimal.valueOf(16));
        return dto;
    }

    private void setField(Object target, String name, Object value) throws Exception
    {
        java.lang.reflect.Field field = TeachingTaskImportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
