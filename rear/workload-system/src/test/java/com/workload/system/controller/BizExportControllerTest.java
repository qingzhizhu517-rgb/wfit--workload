package com.workload.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DataScopeUtil;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.dto.Attachment1BatchRequest;
import com.workload.system.domain.dto.Attachment1RowDTO;
import com.workload.system.domain.dto.Attachment1TeacherDTO;
import com.workload.system.mapper.BizExportMapper;
import com.workload.system.service.impl.Attachment1ExportServiceImpl;
import com.workload.system.service.ISysUserService;

/**
 * BizExportController 单元测试。
 * <p>
 * 单人 GET 渲染契约（39 列 / G4/G6 / 固定块）是回归底线：这些用例通过一个
 * <b>真实但被 spy 包裹</b>的 {@link Attachment1ExportServiceImpl} 保持原样通过——
 * Task 3 把渲染下沉到服务后，GET 只委托，故断言必须仍打到真实渲染路径而非 Mock。
 * 批量端点仍以 {@code verify(spy)} 校验委托语义（真实方法一并执行，无副作用影响断言）。
 */
@ExtendWith(MockitoExtension.class)
class BizExportControllerTest
{
    @Mock
    private BizExportMapper exportMapper;

    @Mock
    private ISysUserService sysUserService;

    @Mock
    private RuleParamService ruleParamService;

    private Attachment1ExportServiceImpl attachment1ExportService;

    private BizExportController controller;

    @BeforeEach
    void wire()
    {
        // 真实服务（spy）：GET 渲染走真实 writeSingle，批量端点仍可 verify 委托
        attachment1ExportService = spy(new Attachment1ExportServiceImpl(exportMapper, sysUserService));
        ReflectionTestUtils.setField(attachment1ExportService, "workbookMaxTeachers", 20);
        ReflectionTestUtils.setField(attachment1ExportService, "zipMaxTeachers", 100);
        ReflectionTestUtils.setField(attachment1ExportService, "maxRowsPerTeacher", 5000);

        controller = new BizExportController();
        ReflectionTestUtils.setField(controller, "exportMapper", exportMapper);
        ReflectionTestUtils.setField(controller, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(controller, "ruleParamService", ruleParamService);
        ReflectionTestUtils.setField(controller, "attachment1ExportService", attachment1ExportService);
    }

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

    @Test
    void batchAttachment1RejectsTeacherRole()
    {
        Attachment1BatchRequest request = new Attachment1BatchRequest();
        request.setSemester("2026-2027-1");
        request.setUserIds(List.of(2001L, 2002L));
        request.setFormat("xlsx");
        try (MockedStatic<DataScopeUtil> scope = mockStatic(DataScopeUtil.class))
        {
            scope.when(DataScopeUtil::isTeacherOnly).thenReturn(true);
            assertThatThrownBy(() -> controller.exportAttachment1Batch(request,
                    new MockHttpServletResponse()))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("教师角色不允许批量导出");
        }
    }

    @Test
    void batchAttachment1DelegatesDistinctUsers() throws Exception
    {
        Attachment1BatchRequest request = new Attachment1BatchRequest();
        request.setSemester("2026-2027-1");
        request.setUserIds(List.of(2002L, 2001L, 2002L));
        request.setFormat("xlsx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MockedStatic<DataScopeUtil> scope = mockStatic(DataScopeUtil.class))
        {
            scope.when(DataScopeUtil::isTeacherOnly).thenReturn(false);
            controller.exportAttachment1Batch(request, response);
        }
        verify(attachment1ExportService).writeBatch(eq("2026-2027-1"),
                eq(List.of(2002L, 2001L)), eq(Attachment1BatchRequest.Format.XLSX), any());
    }

    @Test
    void batchAttachment1RejectsEmptySemester()
    {
        Attachment1BatchRequest request = new Attachment1BatchRequest();
        request.setSemester("   ");
        request.setUserIds(List.of(2001L));
        request.setFormat("xlsx");
        try (MockedStatic<DataScopeUtil> scope = mockStatic(DataScopeUtil.class))
        {
            scope.when(DataScopeUtil::isTeacherOnly).thenReturn(false);
            assertThatThrownBy(() -> controller.exportAttachment1Batch(request,
                    new MockHttpServletResponse()))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("semester 不能为空");
        }
    }

    @Test
    void batchAttachment1RejectsEmptyUserIds()
    {
        Attachment1BatchRequest request = new Attachment1BatchRequest();
        request.setSemester("2026-2027-1");
        request.setUserIds(java.util.Arrays.asList((Long) null, null));
        request.setFormat("xlsx");
        try (MockedStatic<DataScopeUtil> scope = mockStatic(DataScopeUtil.class))
        {
            scope.when(DataScopeUtil::isTeacherOnly).thenReturn(false);
            assertThatThrownBy(() -> controller.exportAttachment1Batch(request,
                    new MockHttpServletResponse()))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("至少选择 1 位教师");
        }
    }

    /**
     * 格式校验收口：空串 / 未支持格式 / 夹带路径穿越的格式串都必须抛业务异常，
     * 绝不静默回落成 XLSX，否则前端可借 format 探测/绕过。规范化逻辑落在
     * {@link Attachment1BatchRequest#normalizedFormat()}，直接对契约断言最精确。
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "pdf", "../../zip"})
    void rejectsUnsupportedFormat(String format)
    {
        Attachment1BatchRequest request = new Attachment1BatchRequest();
        request.setSemester("2026-2027-1");
        request.setUserIds(List.of(2001L));
        request.setFormat(format);
        assertThatThrownBy(request::normalizedFormat)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("format 仅支持");
    }

    /**
     * 单人 GET 越权收口回归：教师传他人 userId（999），经 {@code DataScopeUtil.resolveUserId}
     * 强制收敛为本人（1003）后才委托服务，导出的必然是本人数据而非被探测的他人。
     */
    @Test
    void singleGetStillScopesTeacherToSelf() throws Exception
    {
        when(exportMapper.selectAttachment1Rows(1003L, "2026-2027-1")).thenReturn(List.of());
        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MockedStatic<DataScopeUtil> scope = mockStatic(DataScopeUtil.class))
        {
            scope.when(() -> DataScopeUtil.resolveUserId(999L)).thenReturn(1003L);
            controller.exportAttachment1(999L, "2026-2027-1", response);
        }
        verify(attachment1ExportService).writeSingle(eq(1003L), eq("2026-2027-1"), any());
    }
}
