package com.workload.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.workload.common.annotation.Log;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.enums.BusinessType;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.service.IBizTeachingTaskService;
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
    @Autowired
    private IBizTeachingTaskService bizTeachingTaskService;

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
