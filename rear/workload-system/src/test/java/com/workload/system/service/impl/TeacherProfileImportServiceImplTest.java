package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workload.system.domain.dto.importing.ImportRowView;
import com.workload.system.domain.dto.importing.TeacherRosterPreviewResponse;

/**
 * 教师花名册字段白名单预检测试（Task 6，preview-only，绝不导入真实人员）。
 *
 * <p>覆盖：重复工号阻断（WFIT1882 两行）、职称「无/讲师待遇」阻断、
 * 人员性质映射（教师→专任、银铃教师→银龄、教师兼行政/行政→待确认）、
 * 敏感列表头被识别但敏感值绝不进入响应、合法行 READY、统计计数正确。</p>
 */
class TeacherProfileImportServiceImplTest
{
    /** 真实花名册的表头顺序（含混入的敏感列），验证白名单按列名匹配、敏感列被忽略。 */
    private static final String[] ROSTER_HEADERS = {
            "工号", "员工姓名", "系", "教研室", "职务", "职务匹配工作量",
            "岗位", "职称", "联系方式", "身份证号", "家庭住址"};

    // ==================== 重复工号 ====================

    @Test
    void duplicateEmployeeCodeBlocksBothRows() throws Exception
    {
        TeacherProfileImportServiceImpl service = new TeacherProfileImportServiceImpl();

        byte[] wb = workbook(
                row("WFIT1882", "甲", "人工智能系", "AI教研室", "", "0", "教师", "讲师", "13800000001", "", ""),
                row("WFIT1882", "乙", "机械工程系", "机械教研室", "", "0", "教师", "副教授", "13800000002", "", ""),
                row("WFIT2001", "丙", "大数据系", "数据教研室", "", "0", "教师", "教授", "13800000003", "", ""));

        TeacherRosterPreviewResponse result = service.previewRoster(new ByteArrayInputStream(wb), "roster.xlsx");

        assertThat(result.blockedCount()).isEqualTo(2);
        assertThat(result.readyCount()).isEqualTo(1);
        assertThat(result.duplicateCodes()).contains("WFIT1882");
        assertThat(result.warnings()).anyMatch(w -> w.contains("WFIT1882"));
        // 两条重复行的状态均为 BLOCKED
        assertThat(result.rows()).filteredOn(r -> "WFIT1882".equals(r.userCode()))
                .allMatch(r -> "BLOCKED".equals(r.status()));
    }

    // ==================== 职称阻断 ====================

    @Test
    void unknownTitleBlocksRowAndListsUnresolvedTitles() throws Exception
    {
        TeacherProfileImportServiceImpl service = new TeacherProfileImportServiceImpl();

        byte[] wb = workbook(
                row("T101", "甲", "人工智能系", "AI教研室", "", "0", "教师", "无", "13800000001", "", ""),
                row("T102", "乙", "人工智能系", "AI教研室", "", "0", "教师", "讲师待遇", "13800000002", "", ""),
                row("T103", "丙", "人工智能系", "AI教研室", "", "0", "教师", "讲师", "13800000003", "", ""));

        TeacherRosterPreviewResponse result = service.previewRoster(new ByteArrayInputStream(wb), "roster.xlsx");

        assertThat(result.blockedCount()).isEqualTo(2);
        assertThat(result.readyCount()).isEqualTo(1);
        assertThat(result.unresolvedTitles()).contains("无", "讲师待遇");
    }

    // ==================== 人员性质映射 ====================

    @Test
    void natureMappingResolvesTeacherAndYinlingButQueuesAdministrative() throws Exception
    {
        TeacherProfileImportServiceImpl service = new TeacherProfileImportServiceImpl();

        byte[] wb = workbook(
                row("T201", "甲", "人工智能系", "AI教研室", "", "0", "教师", "讲师", "13800000001", "", ""),
                row("T202", "乙", "人工智能系", "AI教研室", "", "0", "银铃教师", "教授", "13800000002", "", ""),
                row("T203", "丙", "人工智能系", "AI教研室", "科长", "0", "教师兼行政", "讲师", "13800000003", "", ""),
                row("T204", "丁", "人工智能系", "AI教研室", "处长", "0", "行政", "讲师", "13800000004", "", ""));

        TeacherRosterPreviewResponse result = service.previewRoster(new ByteArrayInputStream(wb), "roster.xlsx");

        // 教师、银铃教师可映射 → READY；教师兼行政、行政待确认 → BLOCKED
        assertThat(result.readyCount()).isEqualTo(2);
        assertThat(result.blockedCount()).isEqualTo(2);
        assertThat(result.unresolvedNatures()).contains("教师兼行政", "行政");
    }

    // ==================== 敏感列脱敏 ====================

    @Test
    void sensitiveHeadersDetectedButValuesNeverLeakIntoResponse() throws Exception
    {
        TeacherProfileImportServiceImpl service = new TeacherProfileImportServiceImpl();

        String idNumber = "370700199001011234";
        String homeAddress = "潍坊市奎文区示例路1号";
        byte[] wb = workbook(
                row("T301", "甲", "人工智能系", "AI教研室", "", "0", "教师", "讲师", "13800000001", idNumber, homeAddress));

        TeacherRosterPreviewResponse result = service.previewRoster(new ByteArrayInputStream(wb), "roster.xlsx");

        // 敏感列表头被识别
        assertThat(result.sensitiveHeaders()).contains("身份证号", "家庭住址");
        // 白名单字段仍按列名正确解析（证明系/岗位等被读到）
        assertThat(result.readyCount()).isEqualTo(1);
        assertThat(result.deptCounts()).containsEntry("人工智能系", 1);
        assertThat(result.employmentTypeCounts()).containsEntry("教师", 1);
        // 敏感值绝不出现在整个响应（含逐行视图、统计、警告）
        String serialized = new ObjectMapper().writeValueAsString(result);
        assertThat(serialized).doesNotContain(idNumber);
        assertThat(serialized).doesNotContain(homeAddress);
    }

    // ==================== 合法行与统计 ====================

    @Test
    void validRowsAreReadyAndStatisticsCounted() throws Exception
    {
        TeacherProfileImportServiceImpl service = new TeacherProfileImportServiceImpl();

        byte[] wb = workbook(
                row("T401", "甲", "人工智能系", "AI教研室", "", "0", "教师", "讲师", "13800000001", "", ""),
                row("T402", "乙", "人工智能系", "AI教研室", "", "0", "教师", "教授", "13800000002", "", ""),
                row("T403", "丙", "大数据系", "数据教研室", "", "0", "银铃教师", "副教授", "13800000003", "", ""));

        TeacherRosterPreviewResponse result = service.previewRoster(new ByteArrayInputStream(wb), "roster.xlsx");

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.readyCount()).isEqualTo(3);
        assertThat(result.blockedCount()).isZero();
        assertThat(result.uniqueUserCodes()).isEqualTo(3);
        assertThat(result.duplicateCodes()).isEmpty();
        assertThat(result.deptCounts()).containsEntry("人工智能系", 2).containsEntry("大数据系", 1);
        // preview-only：始终提示不会导入真实教师
        assertThat(result.warnings()).anyMatch(w -> w.contains("不会导入") || w.contains("不导入") || w.contains("字段参考"));
    }

    // ==================== 辅助 ====================

    /** 一行：工号/姓名/系/教研室/职务/职务匹配工作量/岗位/职称/联系方式/身份证号/家庭住址。 */
    private static String[] row(String userCode, String nickName, String dept, String office,
            String position, String workload, String employmentType, String title,
            String phone, String idNumber, String address)
    {
        return new String[]{userCode, nickName, dept, office, position, workload,
                employmentType, title, phone, idNumber, address};
    }

    /** 构造单 Sheet 花名册工作簿（表头含混入的敏感列 + 若干数据行）。 */
    private static byte[] workbook(String[]... rows) throws Exception
    {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            Sheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            for (int i = 0; i < ROSTER_HEADERS.length; i++)
            {
                header.createCell(i).setCellValue(ROSTER_HEADERS[i]);
            }
            for (int i = 0; i < rows.length; i++)
            {
                Row r = sheet.createRow(i + 1);
                for (int c = 0; c < rows[i].length; c++)
                {
                    r.createCell(c).setCellValue(rows[i][c]);
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }
}
