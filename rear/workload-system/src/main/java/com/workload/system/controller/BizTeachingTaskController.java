package com.workload.system.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.workload.common.annotation.Log;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.enums.BusinessType;
import com.workload.common.utils.DataScopeUtil;
import com.workload.common.utils.excel.ImportResult;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.domain.dto.TeachingTaskExportDTO;
import com.workload.system.domain.dto.TeachingTaskImportDTO;
import com.workload.system.domain.dto.TeachingTaskImportV1DTO;
import com.workload.system.domain.dto.importing.ImportConfirmRequest;
import com.workload.system.domain.dto.importing.ImportPreviewResponse;
import com.workload.system.service.IBizTeachingTaskService;
import com.workload.system.service.ITeachingTaskImportService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * 导入教学任务Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/teachingTask")
public class BizTeachingTaskController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(BizTeachingTaskController.class);

    /** Excel 导入文件大小上限（MB） */
    @Value("${wfit.import.max-size:10}")
    private long importMaxSizeMb;

    @Autowired
    private IBizTeachingTaskService bizTeachingTaskService;

    @Autowired
    private ITeachingTaskImportService teachingTaskImportService;

    /**
     * 查询导入教学任务列表
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizTeachingTask bizTeachingTask)
    {
        startPage();
        List<BizTeachingTask> list = bizTeachingTaskService.selectBizTeachingTaskList(bizTeachingTask);
        return getDataTable(list);
    }

    /**
     * Excel 导入教学任务（兼容入口，保留一个发布周期）。
     * <p>
     * 内部改走两阶段 {@code preview}：仅当 0 阻断且显式 {@code legacyAutoConfirm=true} 时自动确认，
     * 否则只返回预检结果 + 弃用提示，不再静默单阶段写库。{@code templateVersion} 省略即 V2；
     * 传 V1 时用旧模板 DTO 把基数列转 v2 语义字段，避免旧文件全行被拒。
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:import')")
    @Log(title = "导入教学任务Excel", businessType = BusinessType.IMPORT)
    @PostMapping("/importExcel")
    public AjaxResult importExcel(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "templateVersion", required = false) String templateVersion,
            @RequestParam(value = "legacyAutoConfirm", required = false, defaultValue = "false") boolean legacyAutoConfirm)
    {
        // 上传安全校验：空文件/扩展名白名单/文件大小上限
        String invalidMsg = ImportFileValidator.validateExcelFile(file, importMaxSizeMb);
        if (invalidMsg != null)
        {
            return error(invalidMsg);
        }
        String version = StringUtils.hasText(templateVersion) ? templateVersion : "V2";
        try
        {
            ImportPreviewResponse preview = teachingTaskImportService.preview(
                    file.getInputStream(), file.getOriginalFilename(), version);

            if (preview.totalCount() == 0)
            {
                return error("Excel 文件为空或无有效数据行").put("data", preview);
            }
            // 0 阻断 + 显式自动确认：走确认写库（兼容旧「上传即导入」的调用）
            if (legacyAutoConfirm && preview.blockedCount() == 0)
            {
                ImportResult result = teachingTaskImportService.confirm(
                        new ImportConfirmRequest(preview.batchId(), preview.confirmToken(), false));
                return success("导入完成（/importExcel 已弃用，请改用两阶段 importPreview/importConfirm）")
                        .put("data", result).put("preview", preview);
            }
            // 否则只返回预检结果，交由前端确认；不再静默写库
            return success("预检完成，请确认后导入（/importExcel 已弃用，请改用 importPreview/importConfirm）")
                    .put("data", preview);
        }
        catch (Exception e)
        {
            log.error("导入教学任务异常", e);
            return error("导入失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /**
     * 两阶段导入 · 预检：上传文件只解析「导入数据」页，逐行落 biz_import_row 暂存，
     * 绝不写业务表；返回可导入/被阻断行数与一次性确认令牌。
     * <p>
     * {@code templateType}（ALL/G1/G2/G3）用于分类导入锁定类别：非 ALL 时数据页中类别不符的行
     * 判 BLOCKED，并落批次 template_category 供确认阶段二次闸门。省略即 ALL（通用模板）。
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:import')")
    @Log(title = "教学任务导入预检", businessType = BusinessType.IMPORT)
    @PostMapping("/importPreview")
    public AjaxResult importPreview(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "templateVersion", required = false, defaultValue = "V2") String templateVersion,
            @RequestParam(value = "templateType", required = false, defaultValue = "ALL") String templateType)
    {
        String invalidMsg = ImportFileValidator.validateExcelFile(file, importMaxSizeMb);
        if (invalidMsg != null)
        {
            return error(invalidMsg);
        }
        try
        {
            ImportPreviewResponse preview = teachingTaskImportService.preview(
                    file.getInputStream(), file.getOriginalFilename(), templateVersion, templateType);
            return success("预检完成").put("data", preview);
        }
        catch (Exception e)
        {
            log.error("教学任务导入预检异常", e);
            return error("预检失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /**
     * 两阶段导入 · 确认：校验令牌与 0 阻断后，按 row_no 顺序回放写库，幂等键命中默认跳过。
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:import')")
    @Log(title = "教学任务导入确认", businessType = BusinessType.IMPORT)
    @PostMapping("/importConfirm")
    public AjaxResult importConfirm(@RequestBody ImportConfirmRequest request)
    {
        ImportResult result = teachingTaskImportService.confirm(request);
        return success("导入完成").put("data", result);
    }

    /**
     * 下载导入模板。
     * <p>
     * {@code version}（V1/V2）决定版式：V2（默认）返回双 Sheet 工作簿（「示例与说明」+「导入数据」），
     * V1 返回旧单 Sheet 兼容模板（17 列，含单一基数列）。
     * {@code templateType}（ALL/G1/G2/G3）仅用于文件名/说明标注——模板列集不变，分类锁定靠导入时
     * 预检/确认闸门校验，而非按类别裁列。两参均可省略，向后兼容。
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:import')")
    @PostMapping("/importTemplate")
    public void importTemplate(@RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "templateType", required = false) String templateType,
            HttpServletResponse response) throws Exception
    {
        String v = StringUtils.hasText(version) ? version.trim().toUpperCase() : "V2";
        // 类别仅用于文件名标注，不裁列；非法/空归 ALL（不加类别后缀）
        String category = normalizeTemplateTypeLabel(templateType);
        String suffix = "ALL".equals(category) ? "" : "_" + category;
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        if ("V1".equals(v))
        {
            response.setHeader("Content-Disposition",
                    "attachment;filename=teachingTaskTemplate_V1" + suffix + ".xlsx");
            EasyExcel.write(response.getOutputStream(), TeachingTaskImportV1DTO.class)
                    .sheet("教学任务导入模板").doWrite(new ArrayList<>());
            return;
        }

        response.setHeader("Content-Disposition", "attachment;filename=teachingTaskTemplate" + suffix + ".xlsx");
        // 双 Sheet：说明页（含五类示例）+ 数据页（仅表头）。用同一 ExcelWriter 写两个 Sheet。
        try (com.alibaba.excel.ExcelWriter writer = EasyExcel.write(response.getOutputStream(),
                TeachingTaskImportDTO.class).build())
        {
            WriteSheet instruction = EasyExcel.writerSheet(0, "示例与说明").build();
            writer.write(instructionExamples(), instruction);
            WriteSheet data = EasyExcel.writerSheet(1, "导入数据").build();
            writer.write(new ArrayList<>(), data);
        }
    }

    /** 模板类别标签归一化（仅供文件名标注）：null/空/非法 → ALL；其余转大写限 G1/G2/G3。 */
    private static String normalizeTemplateTypeLabel(String templateType)
    {
        if (!StringUtils.hasText(templateType))
        {
            return "ALL";
        }
        String t = templateType.trim().toUpperCase();
        return ("G1".equals(t) || "G2".equals(t) || "G3".equals(t)) ? t : "ALL";
    }

    /**
     * 说明页五类示例（G1/G3/G4/G5/G6），学期统一 2025-2026-2，教师用虚构「示例教师」标签。
     * 数量列按类别语义填写：G1 计划学时、G3 天数、G4 学分+人数、G5 人数、G6 周数+人数。
     */
    private static List<TeachingTaskImportDTO> instructionExamples()
    {
        List<TeachingTaskImportDTO> examples = new ArrayList<>();
        examples.add(example("G1", "高等数学", ex -> {
            ex.setPlannedHours(new java.math.BigDecimal("48"));
            ex.setStudentCount(80);
        }));
        examples.add(example("G3", "金工实习", ex -> {
            ex.setDays(new java.math.BigDecimal("15"));
            ex.setStudentCount(35);
        }));
        examples.add(example("G4", "课程设计", ex -> {
            ex.setCredits(new java.math.BigDecimal("2"));
            ex.setStudentCount(60);
        }));
        examples.add(example("G5", "毕业论文", ex -> ex.setStudentCount(8)));
        examples.add(example("G6", "集中实习", ex -> {
            ex.setWeeks(new java.math.BigDecimal("4"));
            ex.setStudentCount(25);
        }));
        return examples;
    }

    private static TeachingTaskImportDTO example(String type, String courseName,
            java.util.function.Consumer<TeachingTaskImportDTO> quantity)
    {
        TeachingTaskImportDTO dto = new TeachingTaskImportDTO();
        dto.setSemester("2025-2026-2");
        dto.setUserCode("SHILI001");
        dto.setUserName("示例教师");
        dto.setCourseName(courseName);
        dto.setCourseCode("EXAMPLE-" + type);
        dto.setWorkloadType(type);
        dto.setEducationLevel("本科");
        dto.setMajorCategory("理工类");
        dto.setCourseNature("必修");
        dto.setClassName("示例班级");
        quantity.accept(dto);
        return dto;
    }

    /**
     * 导出导入教学任务列表
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:export')")
    @Log(title = "导入教学任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizTeachingTask bizTeachingTask)
    {
        bizTeachingTask.setUserId(DataScopeUtil.resolveUserId(bizTeachingTask.getUserId()));
        List<TeachingTaskExportDTO> list = bizTeachingTaskService.selectBizTeachingTaskExportList(bizTeachingTask);
        ExcelUtil<TeachingTaskExportDTO> util = new ExcelUtil<TeachingTaskExportDTO>(TeachingTaskExportDTO.class);
        util.exportExcel(response, list, "导入教学任务数据");
    }

    /**
     * 获取导入教学任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizTeachingTaskService.selectBizTeachingTaskById(id));
    }

    /**
     * 新增导入教学任务
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:add')")
    @Log(title = "导入教学任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizTeachingTask bizTeachingTask)
    {
        return toAjax(bizTeachingTaskService.insertBizTeachingTask(bizTeachingTask));
    }

    /**
     * 修改导入教学任务
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:edit')")
    @Log(title = "导入教学任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizTeachingTask bizTeachingTask)
    {
        return toAjax(bizTeachingTaskService.updateBizTeachingTask(bizTeachingTask));
    }

    /**
     * 删除导入教学任务
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:remove')")
    @Log(title = "导入教学任务", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizTeachingTaskService.deleteBizTeachingTaskByIds(ids));
    }
}
