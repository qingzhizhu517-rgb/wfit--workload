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

import com.workload.common.utils.DataScopeUtil;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.domain.dto.TeachingTaskExportDTO;
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
