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
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.service.IBizWorkloadSummaryService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.utils.SecurityUtils;
import com.workload.common.core.page.TableDataInfo;

/**
 * 学期工作量汇总Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/workloadSummary")
public class BizWorkloadSummaryController extends BaseController
{
    @Autowired
    private IBizWorkloadSummaryService bizWorkloadSummaryService;

    /**
     * 查询学期工作量汇总列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWorkloadSummary bizWorkloadSummary)
    {
        // 教师角色只能查看自己的数据
        if (SecurityUtils.hasRole("teacher"))
        {
            bizWorkloadSummary.setUserId(SecurityUtils.getUserId());
        }
        startPage();
        List<BizWorkloadSummary> list = bizWorkloadSummaryService.selectBizWorkloadSummaryList(bizWorkloadSummary);
        return getDataTable(list);
    }

    /**
     * 导出学期工作量汇总列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:export')")
    @Log(title = "学期工作量汇总", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWorkloadSummary bizWorkloadSummary)
    {
        // 教师角色只能导出自己的数据
        if (SecurityUtils.hasRole("teacher"))
        {
            bizWorkloadSummary.setUserId(SecurityUtils.getUserId());
        }
        List<BizWorkloadSummary> list = bizWorkloadSummaryService.selectBizWorkloadSummaryList(bizWorkloadSummary);
        ExcelUtil<BizWorkloadSummary> util = new ExcelUtil<BizWorkloadSummary>(BizWorkloadSummary.class);
        util.exportExcel(response, list, "学期工作量汇总数据");
    }

    /**
     * 获取学期工作量汇总详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizWorkloadSummaryService.selectBizWorkloadSummaryById(id));
    }

    /**
     * 新增学期工作量汇总
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:add')")
    @Log(title = "学期工作量汇总", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWorkloadSummary bizWorkloadSummary)
    {
        return toAjax(bizWorkloadSummaryService.insertBizWorkloadSummary(bizWorkloadSummary));
    }

    /**
     * 修改学期工作量汇总
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:edit')")
    @Log(title = "学期工作量汇总", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWorkloadSummary bizWorkloadSummary)
    {
        return toAjax(bizWorkloadSummaryService.updateBizWorkloadSummary(bizWorkloadSummary));
    }

    /**
     * 删除学期工作量汇总
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:remove')")
    @Log(title = "学期工作量汇总", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizWorkloadSummaryService.deleteBizWorkloadSummaryByIds(ids));
    }
}
