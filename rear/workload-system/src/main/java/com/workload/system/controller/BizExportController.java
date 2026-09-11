package com.workload.system.controller;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.excel.EasyExcel;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.entity.SysUser;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DataScopeUtil;
import com.workload.common.utils.StringUtils;
import com.workload.system.domain.dto.Attachment1RowDTO;
import com.workload.system.domain.dto.Attachment1TeacherDTO;
import com.workload.system.domain.dto.PaySummaryExportDTO;
import com.workload.system.domain.dto.PersonalWorkloadDetailExportDTO;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.mapper.BizExportMapper;
import com.workload.system.service.ISysUserService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 工作量报表导出 Controller
 *
 * @author wflg
 */
@RestController
@RequestMapping("/system/export")
public class BizExportController extends BaseController
{
    @Autowired
    private BizExportMapper exportMapper;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private RuleParamService ruleParamService;

    /**
     * 导出个人工作量明细表（附件1格式）
     * <p>
     * 每行除核算结果外还带出各系数原值与「系数说明」（第几次开课、哪个班、是否封顶），
     * 用于事后核对「这条为什么只算 0.8」，不必再回查 biz_wl_* 明细表。
     */
    @PreAuthorize("@ss.hasPermi('system:export:personal')")
    @GetMapping("/personalWorkload")
    public void exportPersonalWorkload(
            @RequestParam("userId") Long userId,
            @RequestParam("semester") String semester,
            HttpServletResponse response) throws Exception
    {
        if (userId == null)
        {
            throw new ServiceException("userId 不能为空");
        }
        if (StringUtils.isEmpty(semester))
        {
            throw new ServiceException("semester 不能为空");
        }
        // 教师角色强制导出本人数据，忽略传入的他人 userId（修复越权导出 P2-01）
        userId = DataScopeUtil.resolveUserId(userId);

        List<PersonalWorkloadDetailExportDTO> exportData = exportMapper.selectPersonalWorkloadExport(userId, semester);
        if (exportData.isEmpty())
        {
            // 没有明细即没有可导出的表体；不写空 Excel，避免用户拿到空文件误以为「本学期没工作量」
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("未找到该教师该学期的工作量明细");
            return;
        }
        for (PersonalWorkloadDetailExportDTO dto : exportData)
        {
            dto.setCoefRemark(buildCoefRemark(dto));
        }

        writeExcel(response, "工作量明细_" + userLabel(userId) + "_" + semester + ".xlsx",
                "工作量明细", PersonalWorkloadDetailExportDTO.class, exportData);
    }

    /** 模板常量（0-based）：数据区从第 5 行起，模板自带 3 条样式行，固定块（总计/签字/备注）从第 8 行起 */
    private static final int ATT1_DATA_START = 4;
    private static final int ATT1_TEMPLATE_DATA_ROWS = 3;
    private static final int ATT1_FIXED_START = 7;
    private static final int ATT1_COLUMNS = 39;

    /**
     * 导出附件1（标准表一）：一行一开课任务，39 列 A~AM。
     * <p>
     * 与 {@code /personalWorkload}（追溯明细）并存：本端点对齐教务处「-新」模板——
     * POI 打开 classpath 模板逐格填值，保住三层表头、X/Y 文理分列、总计/签字/备注固定行
     * 与全部合并单元格；数据行数随任务数伸缩（样式克隆自模板首条数据行）。
     * 教师级列（学院/姓名/G7~AM）只填首行，与教务处参考件填法一致。
     */
    @PreAuthorize("@ss.hasPermi('system:export:personal')")
    @GetMapping("/attachment1")
    public void exportAttachment1(
            @RequestParam("userId") Long userId,
            @RequestParam("semester") String semester,
            HttpServletResponse response) throws Exception
    {
        if (userId == null)
        {
            throw new ServiceException("userId 不能为空");
        }
        if (StringUtils.isEmpty(semester))
        {
            throw new ServiceException("semester 不能为空");
        }
        userId = DataScopeUtil.resolveUserId(userId);

        List<Attachment1RowDTO> rows = exportMapper.selectAttachment1Rows(userId, semester);
        if (rows.isEmpty())
        {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("未找到该教师该学期的工作量明细");
            return;
        }
        Attachment1TeacherDTO teacher = exportMapper.selectAttachment1Teacher(userId, semester);
        if (teacher == null)
        {
            throw new ServiceException("该教师该学期尚未核算汇总，请先核算汇总后再导出表一");
        }

        try (InputStream is = new ClassPathResource("templates/attachment1.xlsx").getInputStream();
             XSSFWorkbook wb = new XSSFWorkbook(is))
        {
            XSSFSheet sheet = wb.getSheetAt(0);
            fillAttachment1(sheet, teacher, rows, semester);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(
                            "表一_" + userLabel(userId) + "_" + semester + ".xlsx", StandardCharsets.UTF_8));
            wb.write(response.getOutputStream());
        }
    }

    /**
     * 模板填值主流程：标题 → 行数伸缩（移动固定块） → 数据行（样式就位后写值） → 教师级列 → 总计行。
     */
    private void fillAttachment1(XSSFSheet sheet, Attachment1TeacherDTO teacher,
                                 List<Attachment1RowDTO> rows, String semester)
    {
        int n = rows.size();

        // 1. 标题：模板占位「潍坊理工学院***学年第*学期****学院教师教育教学工作量统计表（表一）」
        String academicYear = teacher != null && StringUtils.isNotEmpty(teacher.getAcademicYear())
                ? teacher.getAcademicYear() : semester.replaceAll("-\\d+$", "");
        String semNo = semester.substring(semester.lastIndexOf('-') + 1);
        String college = teacher != null && StringUtils.isNotEmpty(teacher.getCollegeName())
                ? teacher.getCollegeName() : "";
        sheet.getRow(0).getCell(0).setCellValue("潍坊理工学院" + academicYear + "学年第" + semNo
                + "学期" + college + "教师教育教学工作量统计表（表一）");

        // 2. 行数伸缩：固定块（总计/签字/备注，模板行 8~12 → 0-based 7~11）整体平移
        if (n != ATT1_TEMPLATE_DATA_ROWS)
        {
            sheet.shiftRows(ATT1_FIXED_START, sheet.getLastRowNum(), n - ATT1_TEMPLATE_DATA_ROWS, true, false);
        }

        // 3. 数据行：先保证样式（克隆模板首条数据行），再写值
        XSSFRow styleDonor = sheet.getRow(ATT1_DATA_START);
        for (int i = 0; i < n; i++)
        {
            XSSFRow row = sheet.getRow(ATT1_DATA_START + i);
            if (row == null)
            {
                row = sheet.createRow(ATT1_DATA_START + i);
            }
            row.setHeight(styleDonor.getHeight());
            for (int c = 0; c < ATT1_COLUMNS; c++)
            {
                XSSFCell cell = row.getCell(c);
                if (cell == null)
                {
                    cell = row.createCell(c);
                }
                XSSFCell donor = styleDonor.getCell(c);
                if (donor != null)
                {
                    cell.setCellStyle(donor.getCellStyle());
                }
            }
            fillAttachment1Row(row, rows.get(i));
        }

        // 4. 教师级列只填首行（A/B 与 AC~AM）
        if (teacher != null)
        {
            XSSFRow first = sheet.getRow(ATT1_DATA_START);
            setText(first, 0, teacher.getCollegeName());
            setText(first, 1, teacher.getTeacherName());
            setNum(first, 28, teacher.getG7());
            setNum(first, 29, teacher.getG8());
            setNum(first, 30, teacher.getG9());
            setText(first, 31, teacher.getG8Remark());
            setNum(first, 32, teacher.getG10());
            setNum(first, 33, teacher.getG11());
            setText(first, 34, teacher.getG11Remark());
            setNum(first, 35, teacher.getTotalWorkload());
            setNum(first, 36, teacher.getRatedWorkload());
            setNum(first, 37, teacher.getExcessWorkload());
            setText(first, 38, teacher.getTeacherSign());
        }

        // 5. 总计行 AJ 列：教师级 AJ 只在首行出现，合计即 summary 总量（对齐参考件 SUM 填法）
        XSSFRow totalRow = sheet.getRow(ATT1_DATA_START + n);
        if (totalRow != null && teacher != null)
        {
            XSSFCell aj = totalRow.getCell(35);
            if (aj == null)
            {
                aj = totalRow.createCell(35);
            }
            if (teacher.getTotalWorkload() != null)
            {
                aj.setCellValue(teacher.getTotalWorkload().doubleValue());
            }
        }

        // 6. 清理 shiftRows 平移残留的尾部空行：模板固定块整体平移后，
        //    POI 不保证清尾，末端可能留下无值空行（拖出一条空打印页），逐行判断后移除
        int expectedLast = ATT1_DATA_START + n + 4; // 0-based：固定块 5 行（总计/签字×2/备注×2）的最后一行
        for (int r = sheet.getLastRowNum(); r > expectedLast; r--)
        {
            XSSFRow stale = sheet.getRow(r);
            if (stale == null || !isRowBlank(stale))
            {
                break;
            }
            sheet.removeRow(stale);
        }
    }

    /** 全部单元格无值才算空行：有格无值的样式行才可安全 removeRow */
    private boolean isRowBlank(XSSFRow row)
    {
        for (short c = 0; c < ATT1_COLUMNS; c++)
        {
            XSSFCell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != org.apache.poi.ss.usermodel.CellType.BLANK)
            {
                return false;
            }
        }
        return true;
    }

    /** 写一条任务行：C~AB 列（列索引 2~27），空值留空不写 0 */
    private void fillAttachment1Row(XSSFRow row, Attachment1RowDTO dto)
    {
        setText(row, 2, dto.getCourseName());
        setText(row, 3, dto.getEducationLevel());
        setNum(row, 4, dto.getJ1());
        setNum(row, 5, dto.getC1());
        setNum(row, 6, dto.getK1());
        setNum(row, 7, dto.getQ1());
        setNum(row, 8, dto.getQ2());
        setNum(row, 9, dto.getQ3());
        setNum(row, 10, dto.getN());
        setNum(row, 11, dto.getG1());
        setNum(row, 12, dto.getJ2());
        setNum(row, 13, dto.getPracticeK());
        setNum(row, 14, dto.getC2());
        setNum(row, 15, dto.getG2());
        setNum(row, 16, dto.getT());
        setNum(row, 17, dto.getD());
        setNum(row, 18, dto.getInternK());
        setNum(row, 19, dto.getG3());
        setNum(row, 20, dto.getJ4());
        setNum(row, 21, dto.getR4());
        setNum(row, 22, dto.getG4());
        setNum(row, 23, dto.getG5Liberal());
        setNum(row, 24, dto.getG5Scitech());
        setNum(row, 25, dto.getW());
        setNum(row, 26, dto.getR6());
        setNum(row, 27, dto.getG6());
    }

    /** 空串/空白串不覆盖模板空单元格，避免导出出现一列「」 */
    private void setText(XSSFRow row, int col, String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return;
        }
        XSSFCell cell = row.getCell(col);
        if (cell == null)
        {
            cell = row.createCell(col);
        }
        cell.setCellValue(value.trim());
    }

    /** null 不写：DB 无值的列在表上就是空格，写 0 会让人误以为学时确为 0 */
    private void setNum(XSSFRow row, int col, BigDecimal value)
    {
        if (value == null)
        {
            return;
        }
        XSSFCell cell = row.getCell(col);
        if (cell == null)
        {
            cell = row.createCell(col);
        }
        cell.setCellValue(value.doubleValue());
    }

    /**
     * 导出绩效酬金统计表（附件2格式）
     * <p>
     * 绩效/其他/总金额三列取 biz_pay_record 落库值，与「酬金记录」页同源；
     * 未核算酬金的教师照样出现，酬金列为空表示「待核算」。
     */
    @PreAuthorize("@ss.hasPermi('system:export:paySummary')")
    @GetMapping("/paySummary")
    public void exportPaySummary(
            @RequestParam("semester") String semester,
            HttpServletResponse response) throws Exception
    {
        if (StringUtils.isEmpty(semester))
        {
            throw new ServiceException("semester 不能为空");
        }
        // 教师角色强制只导出本人数据；管理角色下返回 null，即全学期
        List<PaySummaryExportDTO> exportData =
                exportMapper.selectPaySummaryExport(semester, DataScopeUtil.resolveUserId(null));
        if (exportData.isEmpty())
        {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("未找到该学期的汇总数据");
            return;
        }
        // 封顶线取规则表当前值，勿在 SQL 里写死 540：政策调整只改 biz_workload_rule
        BigDecimal cap200 = ruleParamService.get("CAP_200PCT", new BigDecimal("540"));
        for (PaySummaryExportDTO dto : exportData)
        {
            fillPayExplain(dto, cap200);
        }

        writeExcel(response, "绩效酬金统计_" + semester + ".xlsx",
                "绩效酬金统计", PaySummaryExportDTO.class, exportData);
    }

    /**
     * 补齐附件2 的三列解释信息：是否触顶、计酬超额工作量、备注。
     * <p>
     * 与 {@code SummaryCalcServiceImpl} 的绩效口径严格对齐：
     * 计酬基数 = min(总工作量, CAP_200PCT) − 额定，下限 0；仅「专任」（或无档案）计发。
     * 备注把「为什么是 0」写清楚，免得看表的人拿 超额×单位酬金 去对绩效酬金对不上。
     */
    private void fillPayExplain(PaySummaryExportDTO dto, BigDecimal cap200)
    {
        boolean capped = dto.getCapped() != null && dto.getCapped() == 1;
        dto.setCappedLabel(capped ? "是" : "否");

        BigDecimal total = dto.getTotalWorkload();
        BigDecimal rated = dto.getRatedWorkload();
        if (total != null && rated != null)
        {
            dto.setPayableExcess(total.min(cap200).subtract(rated).max(BigDecimal.ZERO));
        }

        List<String> notes = new ArrayList<>();
        String nature = dto.getTeacherNature();
        if (StringUtils.isNotEmpty(nature) && !"专任".equals(nature))
        {
            notes.add("人员性质「" + nature + "」不计发绩效酬金");
        }
        if (capped)
        {
            notes.add("总工作量已触 200% 上限 " + cap200.stripTrailingZeros().toPlainString() + "，绩效按封顶值计发");
        }
        if (dto.getTotalPay() == null)
        {
            notes.add("尚未核算酬金，请先执行一键核算");
        }
        dto.setRemark(notes.isEmpty() ? "-" : String.join("；", notes));
    }

    /**
     * 拼「系数说明」：把追溯一条明细所需的上下文压进一列。
     * <p>
     * 重复次序是重点——G1 的 C1 与 G3 的 K 都按第几次开课递减，
     * 只看系数值看不出是哪个班被判成第二次，故一并写出班级。
     */
    private String buildCoefRemark(PersonalWorkloadDetailExportDTO dto)
    {
        List<String> parts = new ArrayList<>();
        Long order = dto.getRepeatOrder();
        if (order != null)
        {
            // G3 按「轮」计（实习实训一轮一批学生），其余按「次」
            String unit = "G3".equals(dto.getItemType()) ? "轮" : "次";
            StringBuilder sb = new StringBuilder("第 ").append(order).append(" ").append(unit);
            if (StringUtils.isNotEmpty(dto.getClassName()))
            {
                sb.append("（").append(dto.getClassName()).append("）");
            }
            if (dto.getRepeatCoef() != null)
            {
                sb.append("，重复系数 ").append(dto.getRepeatCoef().stripTrailingZeros().toPlainString());
            }
            parts.add(sb.toString());
        }
        if (dto.getOverLimit() != null && dto.getOverLimit() == 1)
        {
            // is_over_limit 三类语义（2026-09-10 起 G4 也置标记）：
            // G4 超告警阈值但未封顶（第十四条4 无「超出不计」，按实际人数计）；
            // G5 超报批门槛但未封顶（第十四条5 按实际人数计）；
            // G6 真按 min(R6, CAP_R6_MAX) 封顶算（第十四条6注4）。
            // 一律写「已按封顶值核算」会让审核人误判 G4/G5 行的学时被砍过。
            if ("G4".equals(dto.getItemType()))
            {
                parts.add("人数超 60 告警阈值，学时按实际人数计（第十四条4 无「超出不计」）");
            }
            else if ("G5".equals(dto.getItemType()))
            {
                parts.add("人数超申报上限，须报院长批准、教务处备案（学时按实际人数计，未封顶）");
            }
            else
            {
                parts.add("人数超上限，已按封顶值核算");
            }
        }
        if (StringUtils.isNotEmpty(dto.getRoleType()))
        {
            String basis = StringUtils.isNotEmpty(dto.getProrationBasis())
                    ? "，折算依据 " + dto.getProrationBasis() : "";
            parts.add("岗位 " + dto.getRoleType() + basis);
        }
        return parts.isEmpty() ? "-" : String.join("；", parts);
    }

    /** 取「姓名(工号)」作文件名片段；查不到用户则回落 userId，不让导出因此失败 */
    private String userLabel(Long userId)
    {
        try
        {
            SysUser user = sysUserService.selectUserById(userId);
            if (user != null && StringUtils.isNotEmpty(user.getNickName()))
            {
                return user.getNickName() + "(" + user.getUserName() + ")";
            }
        }
        catch (Exception e)
        {
            logger.warn("导出取教师姓名失败，回落 userId: {}", userId, e);
        }
        return String.valueOf(userId);
    }

    /** 统一写出 xlsx：文件名 URL 编码，避免中文名在部分浏览器乱码 */
    private <T> void writeExcel(HttpServletResponse response, String fileName, String sheetName,
                                Class<T> clazz, List<T> data) throws Exception
    {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        EasyExcel.write(response.getOutputStream(), clazz).sheet(sheetName).doWrite(data);
    }
}
