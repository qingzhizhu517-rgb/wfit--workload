package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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

    // --- v2 数量字段按类别校验（必填 + 互斥 + 精度/人数） ---

    /**
     * 数量字段互斥矩阵：每类只接受语义匹配的数量列，缺必填或填了不适用列均拒绝，
     * 错误信息含类别与字段名。
     */
    @ParameterizedTest
    @MethodSource("quantityCases")
    void validatesQuantityByCategory(TeachingTaskImportDTO row, String expectedMessage)
    {
        assertThatThrownBy(() -> invokeValidateRow(row))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(expectedMessage);
    }

    static Stream<Arguments> quantityCases()
    {
        return Stream.of(
                arguments(g1(null, null), "G1必须填写计划学时"),
                arguments(g3WithHours(), "G3只能填写天数"),
                arguments(g4(null, 15), "G4必须填写课程设计学分"),
                arguments(g5(null), "G5必须填写指导人数"),
                arguments(g6(new BigDecimal("4"), null), "G6必须填写指导人数"));
    }

    /** G1/G2/G4/G6 填了课程系数列即拒绝（仅 G2/G3/G5 允许）。 */
    @Test
    void g1RejectsCourseCoefficientColumn()
    {
        TeachingTaskImportDTO dto = g1(new BigDecimal("48"), 80);
        dto.setCourseCoefficient(new BigDecimal("1.2"));
        assertThatThrownBy(() -> invokeValidateRow(dto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("G1不支持课程系数列");
    }

    /** G3 允许课程系数列（个案覆盖指导系数 D）。 */
    @Test
    void g3AcceptsCourseCoefficientColumn()
    {
        TeachingTaskImportDTO dto = g3(new BigDecimal("15"));
        dto.setCourseCoefficient(new BigDecimal("2.0"));
        assertThatCode(() -> invokeValidateRow(dto)).doesNotThrowAnyException();
    }

    /** 合法 G1 行（仅计划学时）通过校验。 */
    @Test
    void validG1PassesValidation()
    {
        assertThatCode(() -> invokeValidateRow(g1(new BigDecimal("48"), 80)))
                .doesNotThrowAnyException();
    }

    /** 计划学时超过 2 位小数即拒绝。 */
    @Test
    void rejectsMoreThanTwoDecimalPlannedHours()
    {
        assertThatThrownBy(() -> invokeValidateRow(g1(new BigDecimal("48.123"), 80)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("计划学时");
    }

    /** 指导人数为 0/负数即拒绝（须正整数）。 */
    @Test
    void rejectsNonPositiveStudentCountForG5()
    {
        assertThatThrownBy(() -> invokeValidateRow(g5(0)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("指导人数");
    }

    // --- DTO 构造辅助（v2 数量字段） ---

    private static TeachingTaskImportDTO base(String type)
    {
        TeachingTaskImportDTO dto = new TeachingTaskImportDTO();
        dto.setSemester("2025-2026-1");
        dto.setUserCode("T999");
        dto.setCourseName("测试课程");
        dto.setWorkloadType(type);
        return dto;
    }

    private static TeachingTaskImportDTO g1(BigDecimal plannedHours, Integer studentCount)
    {
        TeachingTaskImportDTO dto = base("G1");
        dto.setPlannedHours(plannedHours);
        dto.setStudentCount(studentCount);
        return dto;
    }

    /** G3 只应填天数，这里错填了计划学时用于触发互斥校验。 */
    private static TeachingTaskImportDTO g3WithHours()
    {
        TeachingTaskImportDTO dto = base("G3");
        dto.setPlannedHours(new BigDecimal("48"));
        dto.setStudentCount(35);
        return dto;
    }

    private static TeachingTaskImportDTO g3(BigDecimal days)
    {
        TeachingTaskImportDTO dto = base("G3");
        dto.setDays(days);
        return dto;
    }

    private static TeachingTaskImportDTO g4(BigDecimal credits, Integer studentCount)
    {
        TeachingTaskImportDTO dto = base("G4");
        dto.setCredits(credits);
        dto.setStudentCount(studentCount);
        return dto;
    }

    private static TeachingTaskImportDTO g5(Integer studentCount)
    {
        TeachingTaskImportDTO dto = base("G5");
        dto.setStudentCount(studentCount);
        return dto;
    }

    private static TeachingTaskImportDTO g6(BigDecimal weeks, Integer studentCount)
    {
        TeachingTaskImportDTO dto = base("G6");
        dto.setWeeks(weeks);
        dto.setStudentCount(studentCount);
        return dto;
    }

    /** 构造一条通过 validateRow 校验的行（学期/工号/课程/类别/数量齐全，按类别填对应字段） */
    private TeachingTaskImportDTO validRow(String workloadType)
    {
        TeachingTaskImportDTO dto = base(workloadType);
        switch (workloadType)
        {
            case "G3":
                dto.setDays(BigDecimal.valueOf(5));
                break;
            case "G4":
                dto.setCredits(BigDecimal.valueOf(2));
                dto.setStudentCount(30);
                break;
            case "G5":
                dto.setStudentCount(8);
                break;
            case "G6":
                dto.setWeeks(BigDecimal.valueOf(4));
                dto.setStudentCount(25);
                break;
            default:
                dto.setPlannedHours(BigDecimal.valueOf(16));
        }
        return dto;
    }

    private void invokeValidateRow(TeachingTaskImportDTO dto) throws Throwable
    {
        Method method = TeachingTaskImportServiceImpl.class
                .getDeclaredMethod("validateRow", TeachingTaskImportDTO.class);
        method.setAccessible(true);
        try
        {
            method.invoke(new TeachingTaskImportServiceImpl(), dto);
        }
        catch (InvocationTargetException e)
        {
            throw e.getCause();
        }
    }

    private void setField(Object target, String name, Object value) throws Exception
    {
        java.lang.reflect.Field field = TeachingTaskImportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
