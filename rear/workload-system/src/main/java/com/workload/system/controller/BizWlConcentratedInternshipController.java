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
import com.workload.system.domain.BizWlConcentratedInternship;
import com.workload.system.service.IBizWlConcentratedInternshipService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * G6集中实习明细Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/wlConcentratedInternship")
public class BizWlConcentratedInternshipController extends BaseController
{
    @Autowired
    private IBizWlConcentratedInternshipService bizWlConcentratedInternshipService;

    /**
     * 查询G6集中实习明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlConcentratedInternship:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWlConcentratedInternship bizWlConcentratedInternship)
    {
        startPage();
        List<BizWlConcentratedInternship> list = bizWlConcentratedInternshipService.selectBizWlConcentratedInternshipList(bizWlConcentratedInternship);
        return getDataTable(list);
    }

    /**
     * 导出G6集中实习明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlConcentratedInternship:export')")
    @Log(title = "G6集中实习明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWlConcentratedInternship bizWlConcentratedInternship)
    {
        List<BizWlConcentratedInternship> list = bizWlConcentratedInternshipService.selectBizWlConcentratedInternshipList(bizWlConcentratedInternship);
        ExcelUtil<BizWlConcentratedInternship> util = new ExcelUtil<BizWlConcentratedInternship>(BizWlConcentratedInternship.class);
        util.exportExcel(response, list, "G6集中实习明细数据");
    }

    /**
     * 获取G6集中实习明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:wlConcentratedInternship:query')")
    @GetMapping(value = "/{itemId}")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId)
    {
        return success(bizWlConcentratedInternshipService.selectBizWlConcentratedInternshipByItemId(itemId));
    }

    /**
     * 新增G6集中实习明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlConcentratedInternship:add')")
    @Log(title = "G6集中实习明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWlConcentratedInternship bizWlConcentratedInternship)
    {
        return toAjax(bizWlConcentratedInternshipService.insertBizWlConcentratedInternship(bizWlConcentratedInternship));
    }

    /**
     * 修改G6集中实习明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlConcentratedInternship:edit')")
    @Log(title = "G6集中实习明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWlConcentratedInternship bizWlConcentratedInternship)
    {
        return toAjax(bizWlConcentratedInternshipService.updateBizWlConcentratedInternship(bizWlConcentratedInternship));
    }

    /**
     * 删除G6集中实习明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlConcentratedInternship:remove')")
    @Log(title = "G6集中实习明细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{itemIds}")
    public AjaxResult remove(@PathVariable Long[] itemIds)
    {
        return toAjax(bizWlConcentratedInternshipService.deleteBizWlConcentratedInternshipByItemIds(itemIds));
    }
}
