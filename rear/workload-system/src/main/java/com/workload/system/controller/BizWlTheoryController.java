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
import com.workload.system.domain.BizWlTheory;
import com.workload.system.service.IBizWlTheoryService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * G1理论课明细Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/wlTheory")
public class BizWlTheoryController extends BaseController
{
    @Autowired
    private IBizWlTheoryService bizWlTheoryService;

    /**
     * 查询G1理论课明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlTheory:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWlTheory bizWlTheory)
    {
        startPage();
        List<BizWlTheory> list = bizWlTheoryService.selectBizWlTheoryList(bizWlTheory);
        return getDataTable(list);
    }

    /**
     * 导出G1理论课明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlTheory:export')")
    @Log(title = "G1理论课明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWlTheory bizWlTheory)
    {
        List<BizWlTheory> list = bizWlTheoryService.selectBizWlTheoryList(bizWlTheory);
        ExcelUtil<BizWlTheory> util = new ExcelUtil<BizWlTheory>(BizWlTheory.class);
        util.exportExcel(response, list, "G1理论课明细数据");
    }

    /**
     * 获取G1理论课明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:wlTheory:query')")
    @GetMapping(value = "/{itemId}")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId)
    {
        return success(bizWlTheoryService.selectBizWlTheoryByItemId(itemId));
    }

    /**
     * 新增G1理论课明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlTheory:add')")
    @Log(title = "G1理论课明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWlTheory bizWlTheory)
    {
        return toAjax(bizWlTheoryService.insertBizWlTheory(bizWlTheory));
    }

    /**
     * 修改G1理论课明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlTheory:edit')")
    @Log(title = "G1理论课明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWlTheory bizWlTheory)
    {
        return toAjax(bizWlTheoryService.updateBizWlTheory(bizWlTheory));
    }

    /**
     * 删除G1理论课明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlTheory:remove')")
    @Log(title = "G1理论课明细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{itemIds}")
    public AjaxResult remove(@PathVariable Long[] itemIds)
    {
        return toAjax(bizWlTheoryService.deleteBizWlTheoryByItemIds(itemIds));
    }
}
