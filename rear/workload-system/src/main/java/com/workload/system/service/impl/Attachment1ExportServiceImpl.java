package com.workload.system.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.workload.common.core.domain.entity.SysUser;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.StringUtils;
import com.workload.system.domain.dto.Attachment1BatchErrorDTO;
import com.workload.system.domain.dto.Attachment1BatchRequest;
import com.workload.system.domain.dto.Attachment1RowDTO;
import com.workload.system.domain.dto.Attachment1TeacherDTO;
import com.workload.system.mapper.BizExportMapper;
import com.workload.system.service.IAttachment1ExportService;
import com.workload.system.service.ISysUserService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 附件1（表一）导出服务实现。
 * <p>
 * 承接原 {@code BizExportController} 的模板渲染（{@code ATT1_*} 常量、{@link #fillAttachment1}
 * 及一族 {@code set*}/{@link #isRowBlank} 助手），对外提供两种输出：
 * <ul>
 *   <li>{@link #writeSingle}：单教师，行为与原 GET 完全一致——无明细回 UTF-8 文本、
 *       有明细无汇总抛「先核算汇总」、否则写 39 列 xlsx，文件名 {@code 表一_<姓名(工号)>_<学期>.xlsx}。</li>
 *   <li>{@link #writeBatch}：多教师，{@code XLSX} 在单工作簿里逐教师 {@code cloneSheet} 出一页
 *       （原生复制行高/列宽/样式/合并单元格/打印设置，保真度最高），{@code ZIP} 逐 entry 流式写出，
 *       两者都携带结构化失败清单。</li>
 * </ul>
 * 阈值（每种容器最多几位教师、单教师最多几行）由 {@code application.yml} 注入，
 * 在写任何响应头/字节之前裁决；失败原因分级：{@link ServiceException} 消息透出，
 * 其它异常一律遮蔽为「生成失败，请联系管理员」，完整堆栈只进日志。
 *
 * @author wflg
 */
@Service
public class Attachment1ExportServiceImpl implements IAttachment1ExportService
{
    private static final Logger log = LoggerFactory.getLogger(Attachment1ExportServiceImpl.class);

    /** 模板常量（0-based）：数据区从第 5 行起，模板自带 3 条样式行，固定块（总计/签字/备注）从第 8 行起 */
    private static final int ATT1_DATA_START = 4;
    private static final int ATT1_TEMPLATE_DATA_ROWS = 3;
    private static final int ATT1_FIXED_START = 7;
    private static final int ATT1_COLUMNS = 39;

    /** 失败清单 Sheet 名（保留名，避免同名教师抢占） */
    private static final String ERROR_SHEET_NAME = "_失败清单";
    /** Excel Sheet 名长度上限 */
    private static final int MAX_SHEET_NAME_LEN = 31;

    private final BizExportMapper exportMapper;

    private final ISysUserService sysUserService;

    /** xlsx 多 Sheet 单工作簿最多容纳的教师数，超出应改用 zip */
    @Value("${wfit.export.attachment1.workbook-max-teachers:20}")
    private int workbookMaxTeachers;

    /** zip 逐教师打包单次最多容纳的教师数 */
    @Value("${wfit.export.attachment1.zip-max-teachers:100}")
    private int zipMaxTeachers;

    /** 单教师任务行上限，超出只把该教师写入失败清单，不影响其余教师 */
    @Value("${wfit.export.attachment1.max-rows-per-teacher:5000}")
    private int maxRowsPerTeacher;

    public Attachment1ExportServiceImpl(BizExportMapper exportMapper, ISysUserService sysUserService)
    {
        this.exportMapper = exportMapper;
        this.sysUserService = sysUserService;
    }

    /**
     * 批量准备：一次批量查行 + 一次批量查教师，按 userId 分组并以请求 ID 顺序归位。
     * <p>
     * 分组不依赖 SQL 排序，而是按传入 {@code userIds} 顺序构造 {@link LinkedHashMap}，
     * 保证输出容器内教师次序与请求一致；无任务行的教师得到空列表，无汇总的教师
     * {@link TeacherExportData#teacher()} 为 {@code null}（是否可导出的判定见 {@link #assertRenderable}）。
     *
     * @param semester 学年学期
     * @param userIds  去重、保序后的教师用户 ID 列表
     * @return 按请求 ID 顺序的教师导出数据组
     */
    public LinkedHashMap<Long, TeacherExportData> prepareBatch(String semester, List<Long> userIds)
    {
        List<Attachment1RowDTO> allRows = exportMapper.selectAttachment1RowsBatch(userIds, semester);
        List<Attachment1TeacherDTO> allTeachers = exportMapper.selectAttachment1TeachersBatch(userIds, semester);

        Map<Long, List<Attachment1RowDTO>> rowsByUser = allRows.stream()
                .collect(Collectors.groupingBy(Attachment1RowDTO::getUserId));
        Map<Long, Attachment1TeacherDTO> teacherByUser = allTeachers.stream()
                .collect(Collectors.toMap(Attachment1TeacherDTO::getUserId, t -> t, (a, b) -> a));

        LinkedHashMap<Long, TeacherExportData> prepared = new LinkedHashMap<>();
        for (Long userId : userIds)
        {
            prepared.put(userId, new TeacherExportData(userId,
                    teacherByUser.get(userId),
                    rowsByUser.getOrDefault(userId, List.of())));
        }
        return prepared;
    }

    @Override
    public void writeSingle(Long userId, String semester, HttpServletResponse response) throws Exception
    {
        List<Attachment1RowDTO> rows = exportMapper.selectAttachment1Rows(userId, semester);
        if (rows.isEmpty())
        {
            // 没有明细即没有可导出的表体；不写空 Excel，避免用户拿到空文件误以为「本学期没工作量」
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("未找到该教师该学期的工作量明细");
            return;
        }
        Attachment1TeacherDTO teacher = exportMapper.selectAttachment1Teacher(userId, semester);
        if (teacher == null)
        {
            throw new ServiceException("该教师该学期尚未核算汇总，请先核算汇总后再导出表一");
        }

        try (InputStream is = template().getInputStream();
             XSSFWorkbook wb = new XSSFWorkbook(is))
        {
            fillAttachment1(wb.getSheetAt(0), teacher, rows, semester);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(
                            "表一_" + userLabel(userId) + "_" + semester + ".xlsx", StandardCharsets.UTF_8));
            wb.write(response.getOutputStream());
        }
    }

    @Override
    public void writeBatch(String semester, List<Long> userIds,
                           Attachment1BatchRequest.Format format,
                           HttpServletResponse response) throws Exception
    {
        // 阈值裁决必须在写任何响应头/字节之前：超限直接拒绝，绝不产出半截文件
        int count = userIds.size();
        if (format == Attachment1BatchRequest.Format.XLSX && count > workbookMaxTeachers)
        {
            throw new ServiceException("xlsx 最多 " + workbookMaxTeachers + " 位教师，请改用 zip");
        }
        if (format == Attachment1BatchRequest.Format.ZIP && count > zipMaxTeachers)
        {
            throw new ServiceException("单次最多 " + zipMaxTeachers + " 位教师");
        }

        LinkedHashMap<Long, TeacherExportData> prepared = prepareBatch(semester, userIds);
        if (format == Attachment1BatchRequest.Format.ZIP)
        {
            writeZip(semester, count, prepared, response);
        }
        else
        {
            writeXlsx(semester, count, prepared, response);
        }
    }

    /**
     * XLSX 多 Sheet：把模板载入输出工作簿，逐位成功教师 {@code cloneSheet(0)} 出一页再填值
     * （原生复制样式/合并/列宽/打印设置，保真度最高），末尾追加失败清单并移除模板原页。
     * <p>
     * 单工作簿在内存聚合是 xlsx 多 Sheet 的固有代价；真正需要控内存的大批量走 {@link #writeZip}。
     */
    private void writeXlsx(String semester, int count,
                           LinkedHashMap<Long, TeacherExportData> prepared,
                           HttpServletResponse response) throws Exception
    {
        try (InputStream is = template().getInputStream();
             XSSFWorkbook out = new XSSFWorkbook(is))
        {
            Set<String> usedNames = new HashSet<>();
            usedNames.add(ERROR_SHEET_NAME);
            usedNames.add(out.getSheetName(0)); // 模板原页名占位，防同名教师撞车
            List<Attachment1BatchErrorDTO> errors = new ArrayList<>();

            for (TeacherExportData data : prepared.values())
            {
                try
                {
                    assertRenderable(data);
                    XSSFSheet clone = out.cloneSheet(0);
                    try
                    {
                        // cloneSheet 不复制打印设置（POI 限制，会打 WARN），手动从模板页补齐，保住版式保真度
                        copyPrintSetup(out.getSheetAt(0), clone);
                        fillAttachment1(clone, data.teacher(), data.rows(), semester);
                        out.setSheetName(out.getSheetIndex(clone), uniqueSheetName(data, usedNames));
                    }
                    catch (Exception fillEx)
                    {
                        // 填值中途失败：移除已创建但半填充的克隆页，否则产物会残留一个 POI 自动命名
                        // 的垃圾 Sheet 且该教师又出现在失败清单，形成自相矛盾的工作簿
                        out.removeSheetAt(out.getSheetIndex(clone));
                        throw fillEx;
                    }
                }
                catch (ServiceException se)
                {
                    errors.add(buildError(data, se.getMessage()));
                }
                catch (Exception ex)
                {
                    log.error("批量导出表一渲染失败(xlsx) userId={}", data.userId(), ex);
                    errors.add(buildError(data, "生成失败，请联系管理员"));
                }
            }
            appendErrorSheet(out, errors);
            out.removeSheetAt(0); // 移除模板原页（成功页均为其克隆，失败清单已在末尾）

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename="
                    + URLEncoder.encode("表一批量_" + semester + "_" + count + "人.xlsx", StandardCharsets.UTF_8));
            out.write(response.getOutputStream());
        }
    }

    /**
     * ZIP 逐 entry：每位成功教师单独渲染成工作簿→字节→写入 entry 后立即关闭，
     * 内存边界锁定为「一位教师的工作簿」，绝不聚合整批 ZIP；失败教师汇入根目录失败清单.xlsx。
     */
    private void writeZip(String semester, int count,
                          LinkedHashMap<Long, TeacherExportData> prepared,
                          HttpServletResponse response) throws Exception
    {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment;filename="
                + URLEncoder.encode("表一批量_" + semester + "_" + count + "人.zip", StandardCharsets.UTF_8));

        Set<String> usedNames = new HashSet<>();
        List<Attachment1BatchErrorDTO> errors = new ArrayList<>();
        try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8))
        {
            for (TeacherExportData data : prepared.values())
            {
                try
                {
                    assertRenderable(data);
                    try (XSSFWorkbook wb = renderOne(data, semester);
                         ByteArrayOutputStream one = new ByteArrayOutputStream(256 * 1024))
                    {
                        wb.write(one);
                        zip.putNextEntry(new ZipEntry(uniqueFileName(data, semester, usedNames)));
                        one.writeTo(zip);
                        zip.closeEntry();
                    }
                }
                catch (ServiceException se)
                {
                    errors.add(buildError(data, se.getMessage()));
                }
                catch (Exception ex)
                {
                    log.error("批量导出表一渲染失败(zip) userId={}", data.userId(), ex);
                    errors.add(buildError(data, "生成失败，请联系管理员"));
                }
            }
            if (!errors.isEmpty())
            {
                writeErrorsEntry(zip, errors);
            }
            zip.finish();
        }
    }

    /** 单教师渲染成独立工作簿（供 ZIP 逐 entry 写出，调用方负责关闭） */
    private XSSFWorkbook renderOne(TeacherExportData data, String semester) throws Exception
    {
        try (InputStream is = template().getInputStream())
        {
            XSSFWorkbook wb = new XSSFWorkbook(is);
            try
            {
                fillAttachment1(wb.getSheetAt(0), data.teacher(), data.rows(), semester);
                return wb;
            }
            catch (Exception fillEx)
            {
                // 填值失败时 wb 不会被返回、调用方 try-with-resources 也拿不到它，
                // 必须在此关闭，否则 OPCPackage/临时文件句柄泄漏（批量多教师失败会累积）
                wb.close();
                throw fillEx;
            }
        }
    }

    /**
     * 可导出性校验：无明细 / 未汇总 / 超单教师行上限分别抛不同 {@link ServiceException}，
     * 使失败清单原因可区分。三类原因均为业务消息，可安全透出给用户。
     */
    private void assertRenderable(TeacherExportData data)
    {
        if (data.rows().isEmpty())
        {
            throw new ServiceException("无工作量明细");
        }
        if (data.teacher() == null)
        {
            throw new ServiceException("尚未核算汇总");
        }
        if (data.rows().size() > maxRowsPerTeacher)
        {
            throw new ServiceException("任务行数 " + data.rows().size() + " 超过单教师上限 " + maxRowsPerTeacher);
        }
    }

    /**
     * 生成合法且唯一的 Sheet 名：替换 {@code []:*?/\}、截断到 31 字符，重名时追加 {@code (userId)}。
     * 教师姓名为空时回落 {@code 教师<userId>}。
     */
    private String uniqueSheetName(TeacherExportData data, Set<String> used)
    {
        String base = safeSheetBase(teacherName(data), data.userId());
        String name = base;
        if (used.contains(name))
        {
            String suffix = "(" + data.userId() + ")";
            String trimmed = base.length() > MAX_SHEET_NAME_LEN - suffix.length()
                    ? base.substring(0, MAX_SHEET_NAME_LEN - suffix.length()) : base;
            name = trimmed + suffix;
            int k = 1;
            while (used.contains(name)) // 病态截断撞名兜底
            {
                String tag = "(" + data.userId() + "_" + (k++) + ")";
                String t2 = base.length() > MAX_SHEET_NAME_LEN - tag.length()
                        ? base.substring(0, MAX_SHEET_NAME_LEN - tag.length()) : base;
                name = t2 + tag;
            }
        }
        used.add(name);
        return name;
    }

    /** Sheet 名净化：非法字符替换为空格，截断 31，空白回落 教师<userId> */
    private String safeSheetBase(String name, Long userId)
    {
        String base = StringUtils.isEmpty(name) ? ("教师" + userId) : name.trim();
        base = base.replaceAll("[\\[\\]:*?/\\\\]", " ").trim();
        if (base.length() > MAX_SHEET_NAME_LEN)
        {
            base = base.substring(0, MAX_SHEET_NAME_LEN);
        }
        return StringUtils.isEmpty(base) ? ("教师" + userId) : base;
    }

    /**
     * 生成唯一 ZIP entry 文件名 {@code 表一_<姓名>_<学期>.xlsx}：清洗路径分隔符、控制字符与
     * {@code ..} 穿越，重名时在扩展名前插入 {@code (userId)}。
     */
    private String uniqueFileName(TeacherExportData data, String semester, Set<String> used)
    {
        String clean = cleanFileNamePart(teacherName(data), data.userId());
        String candidate = "表一_" + clean + "_" + semester + ".xlsx";
        if (used.contains(candidate))
        {
            candidate = "表一_" + clean + "(" + data.userId() + ")_" + semester + ".xlsx";
            int k = 1;
            while (used.contains(candidate))
            {
                candidate = "表一_" + clean + "(" + data.userId() + "_" + (k++) + ")_" + semester + ".xlsx";
            }
        }
        used.add(candidate);
        return candidate;
    }

    /** 文件名净化：移除路径分隔符/控制字符、中和 {@code ..}，并替换 Windows 非法文件名字符
     *  {@code :*?<>|"}，与 {@link #safeSheetBase} 口径一致，避免解压端因非法名丢文件 */
    private String cleanFileNamePart(String name, Long userId)
    {
        String s = StringUtils.isEmpty(name) ? ("教师" + userId) : name.trim();
        s = s.replaceAll("[\\\\/\\r\\n\\t\\x00-\\x1f]", "_"); // 路径分隔符 + 控制字符
        s = s.replaceAll("[:*?<>|\"\\[\\]]", "_"); // Windows 非法文件名字符（与 Sheet 名净化对齐）
        s = s.replace("..", "_"); // 中和目录穿越
        s = s.replaceAll("^\\.+", "_"); // 前导点
        return StringUtils.isEmpty(s.trim()) ? ("教师" + userId) : s.trim();
    }

    private String teacherName(TeacherExportData data)
    {
        return data.teacher() != null ? data.teacher().getTeacherName() : null;
    }

    /** 失败清单行标签：批量不逐人查 sys_user（避免 N 次查询），用汇总里的姓名 + userId */
    private Attachment1BatchErrorDTO buildError(TeacherExportData data, String reason)
    {
        String name = teacherName(data);
        String label = StringUtils.isNotEmpty(name) ? name.trim() + "(" + data.userId() + ")"
                : "教师(" + data.userId() + ")";
        return new Attachment1BatchErrorDTO(data.userId(), label, reason);
    }

    /** 在工作簿末尾追加失败清单 Sheet（表头 + 每行 userId/教师/原因），无失败也保留表头页 */
    private void appendErrorSheet(XSSFWorkbook workbook, List<Attachment1BatchErrorDTO> errors)
    {
        XSSFSheet sheet = workbook.createSheet(ERROR_SHEET_NAME);
        XSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue("教师ID");
        header.createCell(1).setCellValue("教师");
        header.createCell(2).setCellValue("失败原因");
        int r = 1;
        for (Attachment1BatchErrorDTO error : errors)
        {
            XSSFRow row = sheet.createRow(r++);
            if (error.getUserId() != null)
            {
                row.createCell(0).setCellValue(error.getUserId());
            }
            row.createCell(1).setCellValue(error.getUserLabel());
            row.createCell(2).setCellValue(error.getReason());
        }
    }

    /** ZIP 根目录失败清单.xlsx：独立小工作簿承载全部失败行 */
    private void writeErrorsEntry(ZipOutputStream zip, List<Attachment1BatchErrorDTO> errors) throws Exception
    {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream())
        {
            appendErrorSheet(wb, errors);
            wb.write(bos);
            zip.putNextEntry(new ZipEntry("失败清单.xlsx"));
            bos.writeTo(zip);
            zip.closeEntry();
        }
    }

    private ClassPathResource template()
    {
        return new ClassPathResource("templates/attachment1.xlsx");
    }

    /**
     * 补齐 {@code cloneSheet} 遗漏的打印设置（纸张方向/尺寸/缩放/适应页 + 居中），
     * 让批量 xlsx 每页与单人导出打印版式一致。
     */
    private void copyPrintSetup(XSSFSheet src, XSSFSheet dst)
    {
        dst.setAutobreaks(src.getAutobreaks());
        dst.setFitToPage(src.getFitToPage());
        dst.setHorizontallyCenter(src.getHorizontallyCenter());
        dst.setVerticallyCenter(src.getVerticallyCenter());
        PrintSetup s = src.getPrintSetup();
        PrintSetup d = dst.getPrintSetup();
        d.setLandscape(s.getLandscape());
        d.setPaperSize(s.getPaperSize());
        d.setScale(s.getScale());
        d.setFitWidth(s.getFitWidth());
        d.setFitHeight(s.getFitHeight());
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
            if (cell != null && cell.getCellType() != CellType.BLANK)
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
            log.warn("导出取教师姓名失败，回落 userId: {}", userId, e);
        }
        return String.valueOf(userId);
    }

    /**
     * 单教师批量导出数据组：定位字段 userId + 教师级信息 + 该教师任务行。
     *
     * @param userId  教师用户 ID
     * @param teacher 教师级信息（无汇总时为 null）
     * @param rows    该教师任务行（无任务时为空列表）
     */
    public record TeacherExportData(Long userId, Attachment1TeacherDTO teacher, List<Attachment1RowDTO> rows)
    {
    }
}
