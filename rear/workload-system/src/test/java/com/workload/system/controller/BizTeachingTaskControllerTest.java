package com.workload.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.workload.common.utils.DataScopeUtil;
import com.workload.common.utils.excel.ImportResult;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.domain.dto.TeachingTaskExportDTO;
import com.workload.system.domain.dto.importing.ImportConfirmRequest;
import com.workload.system.domain.dto.importing.ImportPreviewResponse;
import com.workload.system.service.IBizTeachingTaskService;
import com.workload.system.service.ITeachingTaskImportService;

@ExtendWith(MockitoExtension.class)
class BizTeachingTaskControllerTest
{
    private static final List<String> EXPECTED_HEADERS = Arrays.asList(
            "教师工号", "教师姓名", "学年学期", "学年", "课程名称", "课程代码", "授课层次", "专业大类",
            "课程性质", "课程级别", "课程角色", "班级", "合堂人数", "理论学时J1", "实践学时J2",
            "同名课第几次(1/2/3+ -> C1 1.0/0.9/0.8)", "导入来源", "导入批次", "导入时间", "状态");

    @Mock
    private IBizTeachingTaskService bizTeachingTaskService;

    @Mock
    private ITeachingTaskImportService teachingTaskImportService;

    @InjectMocks
    private BizTeachingTaskController controller;

    @Test
    void exportUsesDedicatedDtoWithoutInternalUserIdOrGeneratedHeaders() throws Exception
    {
        BizTeachingTask query = new BizTeachingTask();
        query.setUserId(42L);
        Date importedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2026-09-11 08:30:45");
        TeachingTaskExportDTO dto = exportRow(importedAt);
        when(bizTeachingTaskService.selectBizTeachingTaskExportList(query)).thenReturn(List.of(dto));

        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            dataScope.when(() -> DataScopeUtil.resolveUserId(42L)).thenReturn(42L);
            controller.export(response, query);
            dataScope.verify(() -> DataScopeUtil.resolveUserId(42L));
        }

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("导入教学任务数据");
            Row header = sheet.getRow(0);
            assertThat(header.getLastCellNum()).isEqualTo((short) 20);
            assertThat(cellValues(header)).containsExactlyElementsOf(EXPECTED_HEADERS);
            assertThat(cellValues(header)).noneMatch(value -> value.contains("教师ID")
                    || value.contains("${comment}") || value.contains("&gt;"));

            List<String> data = cellValues(sheet.getRow(1));
            assertThat(data).hasSize(20);
            assertThat(data.get(0)).isEqualTo("T0001");
            assertThat(data.get(1)).isEqualTo("张三");
            assertThat(data.get(16)).isEqualTo("Excel导入");
            assertThat(data.get(18)).isEqualTo("2026-09-11 08:30:45");
            assertThat(data.get(19)).isEqualTo("正常");
            assertThat(data).doesNotContain("42");
        }

        verify(bizTeachingTaskService).selectBizTeachingTaskExportList(query);
        verify(bizTeachingTaskService, never()).selectBizTeachingTaskList(query);
    }

    @Test
    void exportAppliesResolvedUserScopeBeforeQuerying()
    {
        BizTeachingTask query = new BizTeachingTask();
        query.setUserId(999L);

        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            dataScope.when(() -> DataScopeUtil.resolveUserId(999L)).thenReturn(1003L);
            when(bizTeachingTaskService.selectBizTeachingTaskExportList(query)).thenReturn(List.of());

            controller.export(new MockHttpServletResponse(), query);

            assertThat(query.getUserId()).isEqualTo(1003L);
            verify(bizTeachingTaskService).selectBizTeachingTaskExportList(query);
            dataScope.verify(() -> DataScopeUtil.resolveUserId(999L));
        }
    }

    @Test
    void exportPreservesSeedSourceLabel() throws Exception
    {
        BizTeachingTask query = new BizTeachingTask();
        TeachingTaskExportDTO dto = exportRow(new Date());
        dto.setImportSource("SEED");
        when(bizTeachingTaskService.selectBizTeachingTaskExportList(query)).thenReturn(List.of(dto));

        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            dataScope.when(() -> DataScopeUtil.resolveUserId(null)).thenReturn(null);
            controller.export(response, query);
        }

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            assertThat(cellValues(workbook.getSheetAt(0).getRow(1)).get(16)).isEqualTo("种子数据");
        }
    }

    /**
     * V2 模板必须分离「示例与说明」和「导入数据」两个 Sheet：数据页仅有表头零数据行，
     * 说明页含 G1/G3/G4/G5/G6 五类示例（虚构「示例教师」标签），数据页表头含拆分后的
     * 计划学时/天数/周数/课程设计学分四个数量列。
     */
    @Test
    void v2TemplateSeparatesInstructionsFromImportRows() throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.importTemplate("V2", null, response);
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            Sheet dataSheet = wb.getSheet("导入数据");
            Sheet instructionSheet = wb.getSheet("示例与说明");
            assertThat(dataSheet).isNotNull();
            assertThat(instructionSheet).isNotNull();
            // 数据页只有表头（第 0 行），无数据行
            assertThat(dataSheet.getLastRowNum()).isZero();
            // 数据页表头含 v2 拆分数量列
            assertThat(cellValues(dataSheet.getRow(0)))
                    .contains("计划学时", "天数", "周数", "课程设计学分");
            // 说明页含 5 类示例行（G1/G3/G4/G5/G6）
            assertThat(instructionSheet.getLastRowNum()).isEqualTo(5);
            // 第一条示例：学期 2025-2026-2、虚构教师标签
            assertThat(instructionSheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("2025-2026-2");
            assertThat(cellValues(instructionSheet.getRow(1))).contains("示例教师");
        }
    }

    /**
     * /importExcel 内部改走两阶段 preview：0 阻断 + legacyAutoConfirm=true 才自动确认，
     * 返回确认结果；否则只返回预检结果并带弃用提示，不再静默单阶段写库。
     */
    @Test
    void importExcelLegacyAutoConfirmsWhenNoBlockedRows() throws Exception
    {
        ReflectionTestUtils.setField(controller, "importMaxSizeMb", 10L);
        MockMultipartFile file = new MockMultipartFile("file", "tasks.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
        ImportPreviewResponse preview = new ImportPreviewResponse(9L, "TEACHING_TASK", "V1",
                2, 2, 0, java.util.List.of(), java.util.List.of(), "tok");
        when(teachingTaskImportService.preview(any(), eq("tasks.xlsx"), eq("V1"))).thenReturn(preview);
        ImportResult result = new ImportResult();
        result.addSuccess();
        when(teachingTaskImportService.confirm(any())).thenReturn(result);

        controller.importExcel(file, "V1", true);

        verify(teachingTaskImportService).preview(any(), eq("tasks.xlsx"), eq("V1"));
        verify(teachingTaskImportService).confirm(any(ImportConfirmRequest.class));
    }

    @Test
    void importExcelWithoutAutoConfirmReturnsPreviewOnly() throws Exception
    {
        ReflectionTestUtils.setField(controller, "importMaxSizeMb", 10L);
        MockMultipartFile file = new MockMultipartFile("file", "tasks.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
        ImportPreviewResponse preview = new ImportPreviewResponse(9L, "TEACHING_TASK", "V2",
                2, 1, 1, java.util.List.of(), java.util.List.of("有 1 行被阻断"), "tok");
        when(teachingTaskImportService.preview(any(), eq("tasks.xlsx"), eq("V2"))).thenReturn(preview);

        controller.importExcel(file, null, false);

        verify(teachingTaskImportService).preview(any(), eq("tasks.xlsx"), eq("V2"));
        verify(teachingTaskImportService, never()).confirm(any());
    }

    /** /importPreview 端点把上传文件、模板版本与模板类别透传给服务预检。 */
    @Test
    void importPreviewDelegatesToService() throws Exception
    {
        ReflectionTestUtils.setField(controller, "importMaxSizeMb", 10L);
        MockMultipartFile file = new MockMultipartFile("file", "tasks.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
        ImportPreviewResponse preview = new ImportPreviewResponse(9L, "TEACHING_TASK", "V2",
                2, 2, 0, java.util.List.of(), java.util.List.of(), "tok");
        when(teachingTaskImportService.preview(any(), eq("tasks.xlsx"), eq("V2"), eq("ALL"))).thenReturn(preview);

        controller.importPreview(file, "V2", "ALL");

        verify(teachingTaskImportService).preview(any(), eq("tasks.xlsx"), eq("V2"), eq("ALL"));
    }

    /** /importPreview 端点把分类模板类别（G1）透传给服务预检，用于分类导入锁定类别。 */
    @Test
    void importPreviewPassesTemplateTypeForTypedImport() throws Exception
    {
        ReflectionTestUtils.setField(controller, "importMaxSizeMb", 10L);
        MockMultipartFile file = new MockMultipartFile("file", "tasks.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
        ImportPreviewResponse preview = new ImportPreviewResponse(9L, "TEACHING_TASK", "V2",
                2, 2, 0, java.util.List.of(), java.util.List.of(), "tok");
        when(teachingTaskImportService.preview(any(), eq("tasks.xlsx"), eq("V2"), eq("G1"))).thenReturn(preview);

        controller.importPreview(file, "V2", "G1");

        verify(teachingTaskImportService).preview(any(), eq("tasks.xlsx"), eq("V2"), eq("G1"));
    }

    /** /importConfirm 端点把确认请求透传给服务确认。 */
    @Test
    void importConfirmDelegatesToService()
    {
        ImportConfirmRequest request = new ImportConfirmRequest(9L, "tok", false);
        ImportResult result = new ImportResult();
        result.addSuccess();
        when(teachingTaskImportService.confirm(request)).thenReturn(result);

        controller.importConfirm(request);

        verify(teachingTaskImportService).confirm(request);
    }

    private TeachingTaskExportDTO exportRow(Date importedAt)
    {
        TeachingTaskExportDTO dto = new TeachingTaskExportDTO();
        dto.setUserCode("T0001");
        dto.setUserName("张三");
        dto.setSemester("2025-2026-1");
        dto.setAcademicYear("2025-2026");
        dto.setCourseName("软件工程");
        dto.setCourseCode("SE001");
        dto.setEducationLevel("本科");
        dto.setMajorCategory("理工");
        dto.setCourseNature("必修");
        dto.setCourseLevel("普通课程");
        dto.setCourseRole("主讲");
        dto.setClassName("软件工程2401班");
        dto.setStudentCount(35L);
        dto.setTheoryHours(new java.math.BigDecimal("32"));
        dto.setPracticeHours(new java.math.BigDecimal("16"));
        dto.setRepeatOrder(1L);
        dto.setImportSource("EXCEL");
        dto.setImportBatch("BATCH-001");
        dto.setImportTime(importedAt);
        dto.setStatus(1);
        return dto;
    }

    private List<String> cellValues(Row row)
    {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        for (int index = 0; index < row.getLastCellNum(); index++)
        {
            values.add(row.getCell(index).toString());
        }
        return values;
    }
}
