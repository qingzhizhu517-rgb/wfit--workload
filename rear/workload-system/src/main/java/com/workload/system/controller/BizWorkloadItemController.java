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
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.service.IBizWorkloadItemService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.utils.SecurityUtils;
import com.workload.common.core.page.TableDataInfo;

/**
 * 工作量明细主表Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/workloadItem")
public class BizWorkloadItemController extends BaseController
{
    @Autowired
    private IBizWorkloadItemService bizWorkloadItemService;

    /**
     * 查询工作量明细主表列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWorkloadItem bizWorkloadItem)
    {
        // 教师角色只能查看自己的数据（不用 hasRole，避免 admin 绕过）
        if (!SecurityUtils.isAdmin() && SecurityUtils.getLoginUser().getUser().getRoles().stream()
                .anyMatch(r -> "teacher".equals(r.getRoleKey())))
        {
            bizWorkloadItem.setUserId(SecurityUtils.getUserId());
        }
        startPage();
        List<BizWorkloadItem> list = bizWorkloadItemService.selectBizWorkloadItemList(bizWorkloadItem);
        return getDataTable(list);
    }

    /**
     * 导出工作量明细主表列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:export')")
    @Log(title = "工作量明细主表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWorkloadItem bizWorkloadItem)
    {
        // 教师角色只能导出自己的数据
        if (!SecurityUtils.isAdmin() && SecurityUtils.getLoginUser().getUser().getRoles().stream()
                .anyMatch(r -> "teacher".equals(r.getRoleKey())))
        {
            bizWorkloadItem.setUserId(SecurityUtils.getUserId());
        }
        List<BizWorkloadItem> list = bizWorkloadItemService.selectBizWorkloadItemList(bizWorkloadItem);
        ExcelUtil<BizWorkloadItem> util = new ExcelUtil<BizWorkloadItem>(BizWorkloadItem.class);
        util.exportExcel(response, list, "工作量明细主表数据");
    }

    /**
     * 获取工作量明细主表详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizWorkloadItemService.selectBizWorkloadItemById(id));
    }

    /**
     * 新增工作量明细主表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:add')")
    @Log(title = "工作量明细主表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWorkloadItem bizWorkloadItem)
    {
        return toAjax(bizWorkloadItemService.insertBizWorkloadItem(bizWorkloadItem));
    }

    /**
     * 修改工作量明细主表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:edit')")
    @Log(title = "工作量明细主表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWorkloadItem bizWorkloadItem)
    {
        return toAjax(bizWorkloadItemService.updateBizWorkloadItem(bizWorkloadItem));
    }

    /**
     * 删除工作量明细主表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:remove')")
    @Log(title = "工作量明细主表", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizWorkloadItemService.deleteBizWorkloadItemByIds(ids));
    }
}
