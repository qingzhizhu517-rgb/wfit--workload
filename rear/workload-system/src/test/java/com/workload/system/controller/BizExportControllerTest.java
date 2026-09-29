package com.workload.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
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
import com.workload.system.domain.dto.PaySummaryExportDTO;
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

    /**
     * 附件2 同源验收：Controller 只补「是否触顶 / 计酬超额 / 备注」三列，
     * 绩效/其他/总金额三列必须原样取 DTO（源自 biz_pay_record），绝不在 Controller 重算。
     * <p>
     * 五行覆盖：已核算专任、外聘、540 封顶、无费率、totalPay=null 未核算。
     * 备注文案与 {@code BizExportController.fillPayExplain} 现有实现严格对齐：
     * 外聘含「不计发」、封顶含「540」（取 CAP_200PCT 规则值）、未核算含「尚未核算」。
     * 金额透传用哨兵证明：专任正常行 totalPay 置 5（若 Controller 重算应为 8390），
     * 断言导出仍为 5，即证明未重算。
     */
    @Test
    void paySummaryExportRendersDtoAmountsAndExplainRemarks() throws Exception
    {
        List<PaySummaryExportDTO> rows = List.of(
                // 专任正常：绩效 7190.40 / 其他 1200.00；总金额置哨兵 5（重算应为 8390）证明原样透传
                payDto("PAY_T99001", "酬金专任教授", "专任", "282.72", "180", "102.72", 0, "70",
                        "7190.40", "1200.00", 5L),
                // 外聘：备注含「不计发」
                payDto("PAY_T99003", "酬金外聘讲师", "外聘", "982.33", "180", "802.33", 1, "50",
                        "0.00", "0.00", 0L),
                // 540 封顶：备注含「540」
                payDto("PAY_T99004", "酬金封顶教授", "专任", "600", "180", "420", 1, "70",
                        "25200.00", "0.00", 25200L),
                // 无费率职称：pay_rate NULL
                payDto("PAY_T99005", "酬金无费率师", "专任", "300", "180", "120", 0, null,
                        "0.00", "0.00", 0L),
                // 未核算：酬金三列均 null，备注含「尚未核算」
                payDto("PAY_T99002", "酬金未核算师", "专任", "200", "180", "20", 0, "50",
                        null, null, null));

        when(exportMapper.selectPaySummaryExport(eq("2099-2100-1"), isNull())).thenReturn(rows);
        when(ruleParamService.get(eq("CAP_200PCT"), any())).thenReturn(new BigDecimal("540"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MockedStatic<DataScopeUtil> scope = mockStatic(DataScopeUtil.class))
        {
            scope.when(() -> DataScopeUtil.resolveUserId(null)).thenReturn(null);
            controller.exportPaySummary("2099-2100-1", response);
        }

        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> col = headerIndex(sheet);
            Map<String, Row> byName = dataRowsByName(sheet, col.get("姓名"));

            // 金额列取 DTO（不在 Controller 重算）：哨兵总金额 5 原样透传
            Row regular = byName.get("酬金专任教授");
            assertThat(regular.getCell(col.get("绩效酬金(元)")).getNumericCellValue())
                    .isCloseTo(7190.40, within(0.001));
            assertThat(regular.getCell(col.get("其他酬金(元)")).getNumericCellValue())
                    .isCloseTo(1200.00, within(0.001));
            assertThat(regular.getCell(col.get("总金额(元)")).getNumericCellValue())
                    .isCloseTo(5.0, within(0.001));

            // 外聘：备注含「不计发」
            assertThat(byName.get("酬金外聘讲师").getCell(col.get("备注")).getStringCellValue())
                    .contains("不计发");
            // 540 封顶：备注含「540」
            assertThat(byName.get("酬金封顶教授").getCell(col.get("备注")).getStringCellValue())
                    .contains("540");
            // 未核算：备注含「尚未核算」
            assertThat(byName.get("酬金未核算师").getCell(col.get("备注")).getStringCellValue())
                    .contains("尚未核算");
        }
    }

    /** 造一行附件2 导出 DTO；金额/费率为 null 时不设值（模拟未核算或无费率） */
    private static PaySummaryExportDTO payDto(String code, String name, String nature,
            String total, String rated, String excess, Integer capped, String rate,
            String coursePay, String otherPay, Long totalPay)
    {
        PaySummaryExportDTO dto = new PaySummaryExportDTO();
        dto.setUserCode(code);
        dto.setUserName(name);
        dto.setSemester("2099-2100-1");
        dto.setTeacherNature(nature);
        dto.setTotalWorkload(total == null ? null : new BigDecimal(total));
        dto.setRatedWorkload(rated == null ? null : new BigDecimal(rated));
        dto.setExcessWorkload(excess == null ? null : new BigDecimal(excess));
        dto.setCapped(capped);
        dto.setPayRate(rate == null ? null : new BigDecimal(rate));
        dto.setCoursePay(coursePay == null ? null : new BigDecimal(coursePay));
        dto.setOtherPayTotal(otherPay == null ? null : new BigDecimal(otherPay));
        dto.setTotalPay(totalPay);
        return dto;
    }

    /** 读表头行（EasyExcel 写在第 0 行）为「列名 -> 列号」，按名定位避免依赖列序 */
    private static Map<String, Integer> headerIndex(Sheet sheet)
    {
        Map<String, Integer> index = new HashMap<>();
        Row header = sheet.getRow(0);
        for (int c = 0; c < header.getLastCellNum(); c++)
        {
            Cell cell = header.getCell(c);
            if (cell != null && cell.getCellType() == CellType.STRING)
            {
                index.put(cell.getStringCellValue(), c);
            }
        }
        return index;
    }

    /** 数据行（第 1 行起）按「姓名」列值索引，便于按教师定位断言 */
    private static Map<String, Row> dataRowsByName(Sheet sheet, int nameCol)
    {
        Map<String, Row> byName = new HashMap<>();
        for (int r = 1; r <= sheet.getLastRowNum(); r++)
        {
            Row row = sheet.getRow(r);
            if (row == null)
            {
                continue;
            }
            Cell cell = row.getCell(nameCol);
            if (cell != null && cell.getCellType() == CellType.STRING)
            {
                byName.put(cell.getStringCellValue(), row);
            }
        }
        return byName;
    }
}
