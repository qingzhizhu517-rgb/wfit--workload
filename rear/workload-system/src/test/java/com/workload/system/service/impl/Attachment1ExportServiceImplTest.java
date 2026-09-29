package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.workload.common.exception.ServiceException;
import com.workload.system.domain.dto.Attachment1BatchRequest.Format;
import com.workload.system.domain.dto.Attachment1RowDTO;
import com.workload.system.domain.dto.Attachment1TeacherDTO;
import com.workload.system.mapper.BizExportMapper;
import com.workload.system.service.ISysUserService;

/**
 * 附件1（表一）导出服务单元测试。
 * <p>
 * 覆盖三层：
 * <ol>
 *   <li>批量准备（Task 2）：一次批量行/教师查询 + 按 userId 分组，绝不回落单人查询。</li>
 *   <li>单人渲染（Task 3）：writeSingle 与原 GET 行为一致——无明细文本、无汇总抛异常、否则 39 列 xlsx。</li>
 *   <li>批量流式（Task 3）：XLSX 多 Sheet（含同名消歧 + 失败清单）、ZIP 逐 entry、阈值与文件名。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class Attachment1ExportServiceImplTest
{
    private static final String SEMESTER = "2026-2027-1";

    @Mock
    private BizExportMapper mapper;

    @Mock
    private ISysUserService sysUserService;

    @InjectMocks
    private Attachment1ExportServiceImpl service;

    @BeforeEach
    void setThresholds()
    {
        ReflectionTestUtils.setField(service, "workbookMaxTeachers", 20);
        ReflectionTestUtils.setField(service, "zipMaxTeachers", 100);
        ReflectionTestUtils.setField(service, "maxRowsPerTeacher", 5000);
    }

    // ---------------------------------------------------------------- Task 2：批量准备

    @Test
    void batchLoadsRowsAndTeachersInTwoQueries() throws Exception
    {
        when(mapper.selectAttachment1RowsBatch(List.of(2L, 1L), SEMESTER))
                .thenReturn(List.of(row(1L, "课程A"), row(2L, "课程B")));
        when(mapper.selectAttachment1TeachersBatch(List.of(2L, 1L), SEMESTER))
                .thenReturn(List.of(teacher(1L, "甲"), teacher(2L, "乙")));

        LinkedHashMap<Long, Attachment1ExportServiceImpl.TeacherExportData> prepared =
                service.prepareBatch(SEMESTER, List.of(2L, 1L));

        verify(mapper, times(1)).selectAttachment1RowsBatch(anyList(), org.mockito.ArgumentMatchers.eq(SEMESTER));
        verify(mapper, times(1)).selectAttachment1TeachersBatch(anyList(), org.mockito.ArgumentMatchers.eq(SEMESTER));
        verify(mapper, never()).selectAttachment1Rows(anyLong(), anyString());

        assertThat(prepared.keySet()).containsExactly(2L, 1L);
        assertThat(prepared.get(1L).teacher().getTeacherName()).isEqualTo("甲");
        assertThat(prepared.get(1L).rows()).extracting(Attachment1RowDTO::getCourseName).containsExactly("课程A");
        assertThat(prepared.get(2L).teacher().getTeacherName()).isEqualTo("乙");
        assertThat(prepared.get(2L).rows()).extracting(Attachment1RowDTO::getCourseName).containsExactly("课程B");
    }

    // ---------------------------------------------------------------- Task 3：单人渲染

    @Test
    void writeSingleWritesPlainTextWhenNoDetail() throws Exception
    {
        when(mapper.selectAttachment1Rows(2003L, SEMESTER)).thenReturn(List.of());
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.writeSingle(2003L, SEMESTER, response);

        assertThat(response.getContentType()).contains("text/plain");
        assertThat(response.getContentAsString()).contains("未找到该教师该学期的工作量明细");
    }

    @Test
    void writeSingleRequiresCalculatedSummary()
    {
        when(mapper.selectAttachment1Rows(2003L, SEMESTER)).thenReturn(List.of(row(2003L, "尚未汇总的课程")));
        when(mapper.selectAttachment1Teacher(2003L, SEMESTER)).thenReturn(null);

        assertThatThrownBy(() -> service.writeSingle(2003L, SEMESTER, new MockHttpServletResponse()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("先核算汇总");
    }

    @Test
    void writeSingleRendersThirtyNineColumnsWithoutRecalculatingFactors() throws Exception
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

        when(mapper.selectAttachment1Rows(2003L, SEMESTER)).thenReturn(List.of(row));
        when(mapper.selectAttachment1Teacher(2003L, SEMESTER)).thenReturn(teacher);

        MockHttpServletResponse response = new MockHttpServletResponse();
        service.writeSingle(2003L, SEMESTER, response);

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
            assertThat(sheet.getLastRowNum()).isEqualTo(9);
            assertThat(sheet.getRow(5).getCell(1).getStringCellValue()).contains("总计");
        }
    }

    // ---------------------------------------------------------------- Task 3：批量 XLSX

    @Test
    void xlsxContainsOneSheetPerSuccessAndErrorSheet() throws Exception
    {
        List<Long> ids = List.of(1L, 2L);
        stubBatch(ids, List.of(row(1L, "课程A")), List.of(teacher(1L, "同名教师"), teacher(2L, "同名教师")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.writeBatch(SEMESTER, ids, Format.XLSX, response);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            // teacher 1 有明细 -> 成功 Sheet；teacher 2 无明细 -> 失败清单
            assertThat(wb.getSheet("同名教师")).isNotNull();
            assertThat(wb.getSheet("_失败清单")).isNotNull();
            assertThat(wb.getSheet("_失败清单").getRow(1).getCell(2).getStringCellValue())
                    .contains("无工作量明细");
            assertThat(wb.getSheet("_失败清单").getRow(1).getCell(0).getNumericCellValue()).isEqualTo(2.0);
        }
    }

    @Test
    void xlsxDisambiguatesSameNameSuccessSheets() throws Exception
    {
        List<Long> ids = List.of(1L, 2L);
        stubBatch(ids, List.of(row(1L, "课A"), row(2L, "课B")),
                List.of(teacher(1L, "同名教师"), teacher(2L, "同名教师")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.writeBatch(SEMESTER, ids, Format.XLSX, response);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            assertThat(wb.getSheet("同名教师")).isNotNull();
            assertThat(wb.getSheet("同名教师(2)")).isNotNull();
            // 两位成功教师 Sheet + 一张空的失败清单
            assertThat(wb.getNumberOfSheets()).isEqualTo(3);
        }
    }

    @Test
    void xlsxRejectsMoreTeachersThanWorkbookMaxBeforeWriting() throws Exception
    {
        ReflectionTestUtils.setField(service, "workbookMaxTeachers", 1);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> service.writeBatch(SEMESTER, List.of(1L, 2L), Format.XLSX, response))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("xlsx 最多 1 位教师");

        // 阈值在查询/写字节之前生效
        verify(mapper, never()).selectAttachment1RowsBatch(anyList(), anyString());
        assertThat(response.getContentAsByteArray()).isEmpty();
    }

    @Test
    void xlsxFilenameCarriesSemesterAndTeacherCount() throws Exception
    {
        List<Long> ids = List.of(1L, 2L);
        stubBatch(ids, List.of(row(1L, "课A"), row(2L, "课B")), List.of(teacher(1L, "甲"), teacher(2L, "乙")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.writeBatch(SEMESTER, ids, Format.XLSX, response);

        String expected = URLEncoder.encode("表一批量_2026-2027-1_2人.xlsx", StandardCharsets.UTF_8);
        assertThat(response.getHeader("Content-Disposition")).contains(expected);
    }

    // ---------------------------------------------------------------- Task 3：批量 ZIP

    @Test
    void zipStreamsEachWorkbookAndErrorsEntry() throws Exception
    {
        List<Long> ids = List.of(1L, 2L);
        stubBatch(ids, List.of(row(1L, "课程A")), List.of(teacher(1L, "同名教师"), teacher(2L, "同名教师")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.writeBatch(SEMESTER, ids, Format.ZIP, response);

        assertThat(response.getContentType()).contains("application/zip");
        assertThat(zipEntryNames(response.getContentAsByteArray()))
                .contains("表一_同名教师_2026-2027-1.xlsx", "失败清单.xlsx");
    }

    @Test
    void zipRejectsMoreTeachersThanZipMaxBeforeWriting() throws Exception
    {
        ReflectionTestUtils.setField(service, "zipMaxTeachers", 1);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> service.writeBatch(SEMESTER, List.of(1L, 2L), Format.ZIP, response))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("单次最多 1 位教师");

        verify(mapper, never()).selectAttachment1RowsBatch(anyList(), anyString());
        assertThat(response.getContentAsByteArray()).isEmpty();
    }

    @Test
    void zipSanitizesEntryNamesPreventingPathTraversal() throws Exception
    {
        List<Long> ids = List.of(1L);
        stubBatch(ids, List.of(row(1L, "课")), List.of(teacher(1L, "../../evil/name:x*?")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.writeBatch(SEMESTER, ids, Format.ZIP, response);

        List<String> names = zipEntryNames(response.getContentAsByteArray());
        String teacherEntry = names.stream().filter(n -> n.startsWith("表一_")).findFirst().orElseThrow();
        assertThat(teacherEntry).doesNotContain("..").doesNotContain("/").doesNotContain("\\");
    }

    // ---------------------------------------------------------------- Task 3：单教师超行隔离

    @Test
    void oversizeTeacherBecomesFailureWithoutDroppingOtherTeachers() throws Exception
    {
        ReflectionTestUtils.setField(service, "maxRowsPerTeacher", 2);
        List<Long> ids = List.of(1L, 2L);
        List<Attachment1RowDTO> rows = List.of(row(1L, "a"), row(1L, "b"), row(1L, "c"), row(2L, "d"));
        stubBatch(ids, rows, List.of(teacher(1L, "甲"), teacher(2L, "乙")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.writeBatch(SEMESTER, ids, Format.XLSX, response);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            assertThat(wb.getSheet("乙")).isNotNull();      // user 2 (1 行) 仍成功
            assertThat(wb.getSheet("甲")).isNull();         // user 1 (3 行) 超限 -> 无 Sheet
            assertThat(wb.getSheet("_失败清单").getRow(1).getCell(0).getNumericCellValue()).isEqualTo(1.0);
            assertThat(wb.getSheet("_失败清单").getRow(1).getCell(2).getStringCellValue()).contains("超过");
        }
    }

    // ---------------------------------------------------------------- Task 5：安全与失败矩阵

    /**
     * 文件名 / Sheet 名净化回归：教师姓名含 {@code / : * ? [ ] \} 时——
     * ZIP entry 既无路径穿越（{@code .. / \}）也无 Windows 非法文件名字符残留；
     * XLSX 每个 Sheet 名合法（≤31 且不含 Excel 禁用字符 {@code [ ] : * ? / \}）。
     * 断言 Task 3 已加的净化确实生效，防回归。
     */
    @Test
    void sanitizesZipEntryAndSheetNames() throws Exception
    {
        String nasty = "甲/乙:丙*丁?戊[己]庚\\辛";
        List<Long> ids = List.of(1L);
        stubBatch(ids, List.of(row(1L, "课")), List.of(teacher(1L, nasty)));

        // ZIP：entry 名无路径穿越、无 Windows 非法字符
        MockHttpServletResponse zipResp = new MockHttpServletResponse();
        service.writeBatch(SEMESTER, ids, Format.ZIP, zipResp);
        String entry = zipEntryNames(zipResp.getContentAsByteArray()).stream()
                .filter(n -> n.startsWith("表一_")).findFirst().orElseThrow();
        assertThat(entry).doesNotContain("..").doesNotContain("/").doesNotContain("\\")
                .doesNotContainPattern("[:*?<>|\"\\[\\]]");

        // XLSX：每个 Sheet 名合法
        MockHttpServletResponse xlsxResp = new MockHttpServletResponse();
        service.writeBatch(SEMESTER, ids, Format.XLSX, xlsxResp);
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xlsxResp.getContentAsByteArray())))
        {
            for (int i = 0; i < wb.getNumberOfSheets(); i++)
            {
                String sheetName = wb.getSheetName(i);
                assertThat(sheetName.length()).isLessThanOrEqualTo(31);
                assertThat(sheetName).doesNotContainPattern("[\\[\\]:*?/\\\\]");
            }
        }
    }

    /**
     * 失败原因可区分：rows 空 → 「无工作量明细」；有明细但无汇总 → 「尚未核算汇总」。
     * 两类教师同批导出，失败清单里原因不能混同，否则对账时无法判断该补明细还是补核算。
     */
    @Test
    void noSummaryAndNoRowsProduceDifferentReasons() throws Exception
    {
        List<Long> ids = List.of(1L, 2L);
        // user 1：无任何明细；user 2：有明细但汇总缺失（teacher 为空）
        stubBatch(ids, List.of(row(2L, "课")), List.of());
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.writeBatch(SEMESTER, ids, Format.XLSX, response);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            Sheet fail = wb.getSheet("_失败清单");
            Map<Double, String> reasonByUser = new HashMap<>();
            for (int r = 1; r <= 2; r++)
            {
                reasonByUser.put(fail.getRow(r).getCell(0).getNumericCellValue(),
                        fail.getRow(r).getCell(2).getStringCellValue());
            }
            assertThat(reasonByUser.get(1.0)).contains("无工作量明细");
            assertThat(reasonByUser.get(2.0)).contains("尚未核算汇总");
            assertThat(reasonByUser.get(1.0)).isNotEqualTo(reasonByUser.get(2.0));
        }
    }

    // ---------------------------------------------------------------- 辅助

    private void stubBatch(List<Long> ids, List<Attachment1RowDTO> rows, List<Attachment1TeacherDTO> teachers)
    {
        when(mapper.selectAttachment1RowsBatch(ids, SEMESTER)).thenReturn(rows);
        when(mapper.selectAttachment1TeachersBatch(ids, SEMESTER)).thenReturn(teachers);
    }

    private static List<String> zipEntryNames(byte[] bytes) throws Exception
    {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes)))
        {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null)
            {
                names.add(entry.getName());
                zip.closeEntry();
            }
        }
        return names;
    }

    private static Attachment1RowDTO row(Long userId, String courseName)
    {
        Attachment1RowDTO row = new Attachment1RowDTO();
        row.setUserId(userId);
        row.setCourseName(courseName);
        return row;
    }

    private static Attachment1TeacherDTO teacher(Long userId, String name)
    {
        Attachment1TeacherDTO teacher = new Attachment1TeacherDTO();
        teacher.setUserId(userId);
        teacher.setTeacherName(name);
        teacher.setAcademicYear("2026-2027");
        teacher.setTotalWorkload(new BigDecimal("56.00"));
        return teacher;
    }
}
