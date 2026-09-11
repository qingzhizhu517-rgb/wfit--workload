package com.workload.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
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
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.dto.Attachment1RowDTO;
import com.workload.system.domain.dto.Attachment1TeacherDTO;
import com.workload.system.mapper.BizExportMapper;
import com.workload.system.service.ISysUserService;

@ExtendWith(MockitoExtension.class)
class BizExportControllerTest
{
    @Mock
    private BizExportMapper exportMapper;

    @Mock
    private ISysUserService sysUserService;

    @Mock
    private RuleParamService ruleParamService;

    @InjectMocks
    private BizExportController controller;

    @Test
    void attachment1WritesCalculatedG4AndG6WithoutRecalculatingDisplayFactors() throws Exception
    {
        Attachment1RowDTO row = new Attachment1RowDTO();
        row.setCourseName("综合实践");
        row.setEducationLevel("本科");
        row.setJ4(new BigDecimal("2"));
        row.setR4(new BigDecimal("70"));
        row.setG4(new BigDecimal("56.00"));
        row.setW(new BigDecimal("4"));
        row.setR6(new BigDecimal("25"));
        row.setG6(new BigDecimal("32.00"));

        Attachment1TeacherDTO teacher = new Attachment1TeacherDTO();
        teacher.setCollegeName("信息工程学院");
        teacher.setTeacherName("钱伟");
        teacher.setAcademicYear("2026-2027");
        teacher.setG7(new BigDecimal("56.00"));
        teacher.setTotalWorkload(new BigDecimal("56.00"));
        teacher.setRatedWorkload(new BigDecimal("180.00"));
        teacher.setExcessWorkload(BigDecimal.ZERO);

        when(exportMapper.selectAttachment1Rows(2003L, "2026-2027-1")).thenReturn(List.of(row));
        when(exportMapper.selectAttachment1Teacher(2003L, "2026-2027-1")).thenReturn(teacher);

        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            dataScope.when(() -> DataScopeUtil.resolveUserId(2003L)).thenReturn(2003L);
            controller.exportAttachment1(2003L, "2026-2027-1", response);
        }

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(4).getLastCellNum()).isEqualTo((short) 39);
            assertThat(sheet.getRow(4).getCell(20).getNumericCellValue()).isEqualTo(2.0);
            assertThat(sheet.getRow(4).getCell(21).getNumericCellValue()).isEqualTo(70.0);
            assertThat(sheet.getRow(4).getCell(22).getNumericCellValue()).isEqualTo(56.0);
            assertThat(sheet.getRow(4).getCell(25).getNumericCellValue()).isEqualTo(4.0);
            assertThat(sheet.getRow(4).getCell(26).getNumericCellValue()).isEqualTo(25.0);
            assertThat(sheet.getRow(4).getCell(27).getNumericCellValue()).isEqualTo(32.0);
            assertThat(sheet.getRow(5).getCell(22).getCellType()).isEqualTo(CellType.BLANK);
            assertThat(sheet.getLastRowNum()).isEqualTo(9);
            assertThat(sheet.getRow(5).getCell(1).getStringCellValue()).contains("总计");
        }
    }

    @Test
    void attachment1RequiresCalculatedSummary()
    {
        Attachment1RowDTO row = new Attachment1RowDTO();
        row.setCourseName("尚未汇总的课程");
        when(exportMapper.selectAttachment1Rows(2003L, "2026-2027-1")).thenReturn(List.of(row));
        when(exportMapper.selectAttachment1Teacher(2003L, "2026-2027-1")).thenReturn(null);

        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            dataScope.when(() -> DataScopeUtil.resolveUserId(2003L)).thenReturn(2003L);
            assertThatThrownBy(() -> controller.exportAttachment1(
                    2003L, "2026-2027-1", new MockHttpServletResponse()))
                    .isInstanceOf(com.workload.common.exception.ServiceException.class)
                    .hasMessageContaining("先核算汇总");
        }
    }
}
