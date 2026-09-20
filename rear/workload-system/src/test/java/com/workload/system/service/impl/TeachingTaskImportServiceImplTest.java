package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    // --- 两阶段导入：预检只读「导入数据」Sheet 且零业务写入 ---

    /**
     * 预检只解析「导入数据」Sheet（说明页 5 行示例绝不入库），逐行落 biz_import_row，
     * 但绝不写教学任务/工作量主表。readyCount 只统计数据页 2 行。
     */
    @Test
    void previewPersistsOnlyDataSheetRowsAndDoesNotCreateBusinessData() throws Exception
    {
        TeachingTaskImportServiceImpl service = new TeachingTaskImportServiceImpl();
        com.workload.system.mapper.BizImportBatchMapper batchMapper =
                mock(com.workload.system.mapper.BizImportBatchMapper.class);
        com.workload.system.mapper.BizImportRowMapper rowMapper =
                mock(com.workload.system.mapper.BizImportRowMapper.class);
        com.workload.system.mapper.BizTeachingTaskMapper taskMapper =
                mock(com.workload.system.mapper.BizTeachingTaskMapper.class);
        BizWorkloadItemMapper itemMapper = mock(BizWorkloadItemMapper.class);
        com.workload.system.service.ImportPreviewStore previewStore =
                mock(com.workload.system.service.ImportPreviewStore.class);
        com.workload.system.service.ImportTokenHasher tokenHasher =
                new com.workload.system.service.ImportTokenHasher();

        setField(service, "importBatchMapper", batchMapper);
        setField(service, "importRowMapper", rowMapper);
        setField(service, "teachingTaskMapper", taskMapper);
        setField(service, "workloadItemMapper", itemMapper);
        setField(service, "importPreviewStore", previewStore);
        setField(service, "importTokenHasher", tokenHasher);

        byte[] workbook = twoDataRowsFiveInstructionRows();
        com.workload.system.domain.dto.importing.ImportPreviewResponse result =
                service.preview(new java.io.ByteArrayInputStream(workbook), "smoke.xlsx", "V2");

        org.assertj.core.api.Assertions.assertThat(result.readyCount()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(result.blockedCount()).isZero();
        verify(taskMapper, never()).insertBizTeachingTask(any());
        verify(itemMapper, never()).insertBizWorkloadItem(any());
        verify(rowMapper, org.mockito.Mockito.times(2))
                .insertBizImportRow(any(com.workload.system.domain.BizImportRow.class));
    }

    /**
     * V1 旧文件（单一「计划学时/天数/周数」基数列）预检不再全行被拒：
     * baseValue 按类别转成 v2 语义字段（G1→计划学时），2 行 G1 均为 READY。
     */
    @Test
    void previewConvertsV1BaseValueToV2Quantity() throws Exception
    {
        TeachingTaskImportServiceImpl service = new TeachingTaskImportServiceImpl();
        com.workload.system.mapper.BizImportBatchMapper batchMapper =
                mock(com.workload.system.mapper.BizImportBatchMapper.class);
        com.workload.system.mapper.BizImportRowMapper rowMapper =
                mock(com.workload.system.mapper.BizImportRowMapper.class);
        com.workload.system.service.ImportPreviewStore previewStore =
                mock(com.workload.system.service.ImportPreviewStore.class);
        com.workload.system.service.ImportTokenHasher tokenHasher =
                new com.workload.system.service.ImportTokenHasher();

        setField(service, "importBatchMapper", batchMapper);
        setField(service, "importRowMapper", rowMapper);
        setField(service, "importPreviewStore", previewStore);
        setField(service, "importTokenHasher", tokenHasher);

        byte[] workbook = v1LegacyTwoG1Rows();
        com.workload.system.domain.dto.importing.ImportPreviewResponse result =
                service.preview(new java.io.ByteArrayInputStream(workbook), "legacy.xlsx", "V1");

        org.assertj.core.api.Assertions.assertThat(result.readyCount()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(result.blockedCount()).isZero();
        // 规范化行应含 plannedHours（由 baseValue 转来），不再因缺数量列被阻断
        org.mockito.ArgumentCaptor<com.workload.system.domain.BizImportRow> captor =
                org.mockito.ArgumentCaptor.forClass(com.workload.system.domain.BizImportRow.class);
        verify(rowMapper, org.mockito.Mockito.times(2)).insertBizImportRow(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getAllValues().get(0).getNormalizedJson())
                .contains("plannedHours");
    }

    /**
     * 确认按 row_no 顺序回放 processSingleRow：重复行（幂等键命中）默认 SKIPPED 不重写，
     * 新行走 processSingleRow 并标记 IMPORTED，最后 markConfirmed。
     */
    @Test
    void confirmReplaysRowsIdempotentlySkippingDuplicates() throws Exception
    {
        TeachingTaskImportServiceImpl real = new TeachingTaskImportServiceImpl();
        TeachingTaskImportServiceImpl service = org.mockito.Mockito.spy(real);

        com.workload.system.mapper.BizImportBatchMapper batchMapper =
                mock(com.workload.system.mapper.BizImportBatchMapper.class);
        com.workload.system.mapper.BizImportRowMapper rowMapper =
                mock(com.workload.system.mapper.BizImportRowMapper.class);
        com.workload.system.mapper.BizTeachingTaskMapper taskMapper =
                mock(com.workload.system.mapper.BizTeachingTaskMapper.class);
        com.workload.system.service.ImportPreviewStore previewStore =
                mock(com.workload.system.service.ImportPreviewStore.class);
        ISysUserService sysUserService = mock(ISysUserService.class);

        setField(service, "importBatchMapper", batchMapper);
        setField(service, "importRowMapper", rowMapper);
        setField(service, "teachingTaskMapper", taskMapper);
        setField(service, "importPreviewStore", previewStore);
        setField(service, "sysUserService", sysUserService);

        com.workload.system.domain.BizImportBatch batch = new com.workload.system.domain.BizImportBatch();
        batch.setId(50L);
        batch.setBatchNo("IMP-CONFIRM");
        when(batchMapper.selectBizImportBatchById(50L)).thenReturn(batch);

        // 两行 READY：第 1 行为重复（幂等键命中），第 2 行为新行
        com.workload.system.domain.BizImportRow dup = importRow(1, normalizedJson("G1", "T001", "CS101", "计算机2401"));
        com.workload.system.domain.BizImportRow fresh = importRow(2, normalizedJson("G1", "T002", "CS102", "计算机2402"));
        when(rowMapper.selectByBatchIdAndStatus(50L, "READY")).thenReturn(java.util.List.of(dup, fresh));

        com.workload.common.core.domain.entity.SysUser u1 = new com.workload.common.core.domain.entity.SysUser();
        u1.setUserId(1001L);
        com.workload.common.core.domain.entity.SysUser u2 = new com.workload.common.core.domain.entity.SysUser();
        u2.setUserId(1002L);
        when(sysUserService.selectUserByUserName("T001")).thenReturn(u1);
        when(sysUserService.selectUserByUserName("T002")).thenReturn(u2);
        // 幂等键计数：第 1 行已存在(1) → 重复；第 2 行不存在(0) → 新行
        when(taskMapper.countImportedTask(eq(1001L), any(), eq("CS101"), eq("计算机2401"), eq("G1"))).thenReturn(1);
        when(taskMapper.countImportedTask(eq(1002L), any(), eq("CS102"), eq("计算机2402"), eq("G1"))).thenReturn(0);

        // self() 返回 spy 自身，绕过 AOP 代理；processSingleRow 桩掉不跑真实写库
        org.mockito.Mockito.doReturn(service).when(service).self();
        org.mockito.Mockito.doReturn(java.math.BigDecimal.TEN)
                .when(service).processSingleRow(any(TeachingTaskImportDTO.class), org.mockito.ArgumentMatchers.eq("IMP-CONFIRM"), org.mockito.ArgumentMatchers.eq("ALL"));

        com.workload.common.utils.excel.ImportResult result =
                service.confirm(new com.workload.system.domain.dto.importing.ImportConfirmRequest(50L, "tok", false));

        // 新行走 processSingleRow 一次，重复行不调用
        verify(service, org.mockito.Mockito.times(1))
                .processSingleRow(any(TeachingTaskImportDTO.class), org.mockito.ArgumentMatchers.eq("IMP-CONFIRM"), org.mockito.ArgumentMatchers.eq("ALL"));
        verify(previewStore).assertConfirmable(any());
        verify(previewStore).markConfirmed(50L);
        // 重复行 SKIPPED、新行 IMPORTED
        org.assertj.core.api.Assertions.assertThat(result.getSkipCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(result.getSuccessCount()).isEqualTo(1);
    }

    /** updateSupport=true 未实现更新语义，confirm 入口即拒绝，绝不回放行、绝不改批次终态。 */
    @Test
    void confirmRejectsUpdateSupportToAvoidDuplicateInsert()
    {
        TeachingTaskImportServiceImpl service = org.mockito.Mockito.spy(new TeachingTaskImportServiceImpl());
        com.workload.system.mapper.BizImportRowMapper rowMapper =
                mock(com.workload.system.mapper.BizImportRowMapper.class);
        com.workload.system.service.ImportPreviewStore previewStore =
                mock(com.workload.system.service.ImportPreviewStore.class);
        try
        {
            setField(service, "importRowMapper", rowMapper);
            setField(service, "importPreviewStore", previewStore);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        assertThatThrownBy(() -> service.confirm(
                new com.workload.system.domain.dto.importing.ImportConfirmRequest(70L, "tok", true)))
                .isInstanceOf(com.workload.common.exception.ServiceException.class)
                .hasMessageContaining("updateSupport");
        // 拒绝发生在回放前：不查 READY 行、不置任何终态
        verify(rowMapper, never()).selectByBatchIdAndStatus(any(), any());
        verify(previewStore, never()).markConfirmed(any());
        verify(previewStore, never()).markFailed(any());
    }

    /** 任一行写库失败 → 批次置失败(4)而非已导入(2)；启用状态机的 1→4 转移。 */
    @Test
    void confirmMarksBatchFailedWhenAnyRowFails() throws Exception
    {
        TeachingTaskImportServiceImpl service = org.mockito.Mockito.spy(new TeachingTaskImportServiceImpl());
        com.workload.system.mapper.BizImportBatchMapper batchMapper =
                mock(com.workload.system.mapper.BizImportBatchMapper.class);
        com.workload.system.mapper.BizImportRowMapper rowMapper =
                mock(com.workload.system.mapper.BizImportRowMapper.class);
        com.workload.system.mapper.BizTeachingTaskMapper taskMapper =
                mock(com.workload.system.mapper.BizTeachingTaskMapper.class);
        com.workload.system.service.ImportPreviewStore previewStore =
                mock(com.workload.system.service.ImportPreviewStore.class);
        ISysUserService sysUserService = mock(ISysUserService.class);
        setField(service, "importBatchMapper", batchMapper);
        setField(service, "importRowMapper", rowMapper);
        setField(service, "teachingTaskMapper", taskMapper);
        setField(service, "importPreviewStore", previewStore);
        setField(service, "sysUserService", sysUserService);

        com.workload.system.domain.BizImportBatch batch = new com.workload.system.domain.BizImportBatch();
        batch.setId(60L);
        batch.setBatchNo("IMP-FAIL");
        when(batchMapper.selectBizImportBatchById(60L)).thenReturn(batch);

        com.workload.system.domain.BizImportRow bad = importRow(1, normalizedJson("G1", "T001", "CS101", "计算机2401"));
        when(rowMapper.selectByBatchIdAndStatus(60L, "READY")).thenReturn(java.util.List.of(bad));
        com.workload.common.core.domain.entity.SysUser u1 = new com.workload.common.core.domain.entity.SysUser();
        u1.setUserId(1001L);
        when(sysUserService.selectUserByUserName("T001")).thenReturn(u1);
        when(taskMapper.countImportedTask(eq(1001L), any(), eq("CS101"), eq("计算机2401"), eq("G1"))).thenReturn(0);

        org.mockito.Mockito.doReturn(service).when(service).self();
        org.mockito.Mockito.doThrow(new com.workload.common.exception.ServiceException("写库失败"))
                .when(service).processSingleRow(any(TeachingTaskImportDTO.class),
                        org.mockito.ArgumentMatchers.eq("IMP-FAIL"), org.mockito.ArgumentMatchers.eq("ALL"));

        com.workload.common.utils.excel.ImportResult result =
                service.confirm(new com.workload.system.domain.dto.importing.ImportConfirmRequest(60L, "tok", false));

        org.assertj.core.api.Assertions.assertThat(result.getFailCount()).isEqualTo(1);
        // 有失败 → markFailed，不 markConfirmed
        verify(previewStore).markFailed(60L);
        verify(previewStore, never()).markConfirmed(60L);
    }

    private static com.workload.system.domain.BizImportRow importRow(int rowNo, String normalizedJson)
    {
        com.workload.system.domain.BizImportRow row = new com.workload.system.domain.BizImportRow();
        row.setRowNo(rowNo);
        row.setStatus("READY");
        row.setNormalizedJson(normalizedJson);
        return row;
    }

    private static String normalizedJson(String type, String userCode, String courseCode, String className)
    {
        return "{\"semester\":\"2025-2026-1\",\"userCode\":\"" + userCode
                + "\",\"courseName\":\"测试课\",\"courseCode\":\"" + courseCode
                + "\",\"workloadType\":\"" + type + "\",\"className\":\"" + className
                + "\",\"plannedHours\":16,\"studentCount\":40}";
    }

    /** 构造 5 行说明 + 2 行 G1 数据的 v2 双 Sheet 工作簿。 */
    private static byte[] twoDataRowsFiveInstructionRows() throws Exception
    {
        String[] headers = v2Headers();
        try (org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream())
        {
            org.apache.poi.ss.usermodel.Sheet instruction = wb.createSheet("示例与说明");
            writeHeader(instruction, headers);
            for (int i = 1; i <= 5; i++)
            {
                org.apache.poi.ss.usermodel.Row r = instruction.createRow(i);
                r.createCell(0).setCellValue("2025-2026-2");
                r.createCell(2).setCellValue("示例教师");
            }
            org.apache.poi.ss.usermodel.Sheet data = wb.createSheet("导入数据");
            writeHeader(data, headers);
            writeG1DataRow(data, 1, "T001", "计算机2401");
            writeG1DataRow(data, 2, "T002", "计算机2402");
            wb.write(out);
            return out.toByteArray();
        }
    }

    /** 构造 2 行 G1 的 V1 旧模板（单一「计划学时/天数/周数」基数列），单 Sheet。 */
    private static byte[] v1LegacyTwoG1Rows() throws Exception
    {
        String[] headers = {"学年学期", "教师工号", "教师姓名", "课程名称", "课程代码", "工作量类别",
                "授课层次", "专业大类", "课程性质", "课程级别", "课程角色", "教学评价", "选课人数",
                "计划学时/天数/周数", "课程系数", "班级", "重复次序"};
        try (org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream())
        {
            org.apache.poi.ss.usermodel.Sheet data = wb.createSheet("教学任务导入模板");
            writeHeader(data, headers);
            for (int i = 1; i <= 2; i++)
            {
                org.apache.poi.ss.usermodel.Row r = data.createRow(i);
                r.createCell(0).setCellValue("2025-2026-1");
                r.createCell(1).setCellValue("T00" + i);
                r.createCell(2).setCellValue("老师" + i);
                r.createCell(3).setCellValue("高等数学");
                r.createCell(4).setCellValue("MATH10" + i);
                r.createCell(5).setCellValue("G1");
                r.createCell(6).setCellValue("本科");
                r.createCell(7).setCellValue("理工类");
                r.createCell(8).setCellValue("必修");
                r.createCell(12).setCellValue(50);
                r.createCell(13).setCellValue(48); // baseValue → 计划学时
                r.createCell(15).setCellValue("班级" + i);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static String[] v2Headers()
    {
        return new String[]{"学年学期", "教师工号", "教师姓名", "课程名称", "课程代码", "工作量类别",
                "授课层次", "专业大类", "课程性质", "课程级别", "课程角色", "教学评价", "选课/指导人数",
                "计划学时", "天数", "周数", "课程设计学分", "课程系数", "班级", "重复次序"};
    }

    private static void writeHeader(org.apache.poi.ss.usermodel.Sheet sheet, String[] headers)
    {
        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++)
        {
            header.createCell(i).setCellValue(headers[i]);
        }
    }

    private static void writeG1DataRow(org.apache.poi.ss.usermodel.Sheet sheet, int rowIdx,
                                       String userCode, String className)
    {
        org.apache.poi.ss.usermodel.Row r = sheet.createRow(rowIdx);
        r.createCell(0).setCellValue("2025-2026-1");
        r.createCell(1).setCellValue(userCode);
        r.createCell(2).setCellValue("教师" + rowIdx);
        r.createCell(3).setCellValue("高等数学");
        r.createCell(4).setCellValue("MATH101");
        r.createCell(5).setCellValue("G1");
        r.createCell(6).setCellValue("本科");
        r.createCell(7).setCellValue("理工类");
        r.createCell(8).setCellValue("必修");
        r.createCell(12).setCellValue(50);      // 选课/指导人数
        r.createCell(13).setCellValue(48);      // 计划学时
        r.createCell(18).setCellValue(className); // 班级
    }
}
