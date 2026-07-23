package com.workload.system.controller;

import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.alibaba.excel.EasyExcel;
import com.workload.common.annotation.Log;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.enums.BusinessType;
import com.workload.common.utils.excel.ExcelReadUtil;
import com.workload.common.utils.excel.ImportResult;
import com.workload.system.domain.BizTeachingTask;
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
    public AjaxResult importExcel(@RequestParam("file") MultipartFile file)
    {
        try
        {
            // 使用 ExcelImportListener 读取（支持逐行错误捕获，不因单行格式错误中断整个导入）
            List<TeachingTaskImportDTO> rows = new ArrayList<>();
            ImportResult readResult = ExcelReadUtil.read(
                    file.getInputStream(),
                    TeachingTaskImportDTO.class,
                    batch -> rows.addAll(batch)
            );

            // 如果 Excel 解析阶段就有错误（如单元格格式错误），直接返回
            if (readResult.hasErrors())
            {
                return error("Excel 解析失败，共 " + readResult.getFailCount() + " 处错误")
                        .put("data", readResult);
            }

            if (rows.isEmpty())
            {
                return error("Excel 文件为空或无有效数据行");
            }

            ImportResult result = teachingTaskImportService.importTeachingTasks(rows, file.getOriginalFilename());
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
    public void importTemplate(HttpServletResponse response) throws Exception
    {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=teachingTaskTemplate.xlsx");
        EasyExcel.write(response.getOutputStream(), TeachingTaskImportDTO.class)
                .sheet("教学任务导入模板")
                .doWrite(new ArrayList<>());
    }

    /**
     * 导出导入教学任务列表
     */
    @PreAuthorize("@ss.hasPermi('system:teachingTask:export')")
    @Log(title = "导入教学任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizTeachingTask bizTeachingTask)
    {
        List<BizTeachingTask> list = bizTeachingTaskService.selectBizTeachingTaskList(bizTeachingTask);
        ExcelUtil<BizTeachingTask> util = new ExcelUtil<BizTeachingTask>(BizTeachingTask.class);
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
