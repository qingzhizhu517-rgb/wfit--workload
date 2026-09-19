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
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.workload.common.annotation.Log;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.enums.BusinessType;
import com.workload.common.utils.DataScopeUtil;
import com.workload.common.utils.excel.ImportResult;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.domain.dto.TeachingTaskExportDTO;
import com.workload.system.domain.dto.TeachingTaskImportDTO;
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
     * Excel 导入教学任务
     * <p>
     * 上传 Excel 文件，自动解析并创建工作量明细
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:import')")
    @Log(title = "导入教学任务Excel", businessType = BusinessType.IMPORT)
    @PostMapping("/importExcel")
    public AjaxResult importExcel(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "templateType", required = false) String templateType)
    {
        // 上传安全校验：空文件/扩展名白名单/文件大小上限
        String invalidMsg = ImportFileValidator.validateExcelFile(file, importMaxSizeMb);
        if (invalidMsg != null)
        {
            return error(invalidMsg);
        }
        // templateType 省略即通用（ALL），向后兼容既有不带该参的调用
        String template = StringUtils.hasText(templateType) ? templateType : "ALL";
        try
        {
            // 流式导入：EasyExcel 分批回调，边读边逐行入库（每行独立事务），
            // 不再把整份文件累积进内存，规避大文件 OOM 与超长事务。
            // 分类模板（G1/G2/G3）在写库前拒绝非同类行；ALL 放行 G1~G6。
            ImportResult result = teachingTaskImportService.importTeachingTasksStreaming(
                    file.getInputStream(), file.getOriginalFilename(), template);

            if (result.getTotalCount() == 0)
            {
                return error("Excel 文件为空或无有效数据行");
            }
            if (result.getSuccessCount() == 0 && result.hasErrors())
            {
                return error("导入失败，共 " + result.getFailCount() + " 行错误").put("data", result);
            }
            return success("导入完成").put("data", result);
        }
        catch (Exception e)
        {
            log.error("导入教学任务异常", e);
            return error("导入失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /**
     * 下载导入模板
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:import')")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response,
            @RequestParam(value = "templateType", required = false) String templateType) throws Exception
    {
        String template = StringUtils.hasText(templateType) ? templateType.trim().toUpperCase() : "ALL";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment;filename=teachingTaskTemplate" + ("ALL".equals(template) ? "" : "_" + template) + ".xlsx");

        ExcelWriterBuilder builder = EasyExcel.write(response.getOutputStream(), TeachingTaskImportDTO.class);
        // 分类模板（G1/G2/G3）用 includeColumnFieldNames 精简列，保留教师/学期/课程/层次/专业/
        // 性质/级别/角色/评价/人数/班级/重复次序及数量/系数列；ALL 模板输出全部列，行为不变。
        if (!"ALL".equals(template))
        {
            builder.includeColumnFieldNames(typedTemplateFields());
        }
        String sheetName = "ALL".equals(template) ? "教学任务导入模板" : (template + " 分类导入模板");
        builder.sheet(sheetName).doWrite(new ArrayList<>());
    }

    /**
     * G1/G2/G3 分类模板保留的列（DTO 字段名）：教师工号/姓名、学期、课程名/代码、
     * 工作量类别、层次、专业、性质、级别、角色、评价、人数、数量(计划学时/天数/周数)、
     * 系数、班级、重复次序。三类模板列集一致，差异体现在文件名与 sheet 名，导入时按类别锁定。
     */
    private static List<String> typedTemplateFields()
    {
        return Arrays.asList(
                "semester", "userCode", "userName", "courseName", "courseCode", "workloadType",
                "educationLevel", "majorCategory", "courseNature", "courseLevel", "courseRole",
                "teachingEval", "studentCount", "baseValue", "courseCoefficient", "className", "repeatOrder");
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
