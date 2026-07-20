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
import com.workload.system.domain.BizWlManagement;
import com.workload.system.service.IBizWlManagementService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * G11管理服务明细Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/wlManagement")
public class BizWlManagementController extends BaseController
{
    @Autowired
    private IBizWlManagementService bizWlManagementService;

    /**
     * 查询G11管理服务明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlManagement:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWlManagement bizWlManagement)
    {
        startPage();
        List<BizWlManagement> list = bizWlManagementService.selectBizWlManagementList(bizWlManagement);
        return getDataTable(list);
    }

    /**
     * 导出G11管理服务明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlManagement:export')")
    @Log(title = "G11管理服务明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWlManagement bizWlManagement)
    {
        List<BizWlManagement> list = bizWlManagementService.selectBizWlManagementList(bizWlManagement);
        ExcelUtil<BizWlManagement> util = new ExcelUtil<BizWlManagement>(BizWlManagement.class);
        util.exportExcel(response, list, "G11管理服务明细数据");
    }

    /**
     * 获取G11管理服务明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:wlManagement:query')")
    @GetMapping(value = "/{itemId}")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId)
    {
        return success(bizWlManagementService.selectBizWlManagementByItemId(itemId));
    }

    /**
     * 新增G11管理服务明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlManagement:add')")
    @Log(title = "G11管理服务明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWlManagement bizWlManagement)
    {
        return toAjax(bizWlManagementService.insertBizWlManagement(bizWlManagement));
    }

    /**
     * 修改G11管理服务明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlManagement:edit')")
    @Log(title = "G11管理服务明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWlManagement bizWlManagement)
    {
        return toAjax(bizWlManagementService.updateBizWlManagement(bizWlManagement));
    }

    /**
     * 删除G11管理服务明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlManagement:remove')")
    @Log(title = "G11管理服务明细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{itemIds}")
    public AjaxResult remove(@PathVariable Long[] itemIds)
    {
        return toAjax(bizWlManagementService.deleteBizWlManagementByItemIds(itemIds));
    }
}
