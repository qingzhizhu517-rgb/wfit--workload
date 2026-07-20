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
import com.workload.system.domain.BizAllowanceItem;
import com.workload.system.service.IBizAllowanceItemService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * 其他酬金明细Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/allowanceItem")
public class BizAllowanceItemController extends BaseController
{
    @Autowired
    private IBizAllowanceItemService bizAllowanceItemService;

    /**
     * 查询其他酬金明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:allowanceItem:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizAllowanceItem bizAllowanceItem)
    {
        startPage();
        List<BizAllowanceItem> list = bizAllowanceItemService.selectBizAllowanceItemList(bizAllowanceItem);
        return getDataTable(list);
    }

    /**
     * 导出其他酬金明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:allowanceItem:export')")
    @Log(title = "其他酬金明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizAllowanceItem bizAllowanceItem)
    {
        List<BizAllowanceItem> list = bizAllowanceItemService.selectBizAllowanceItemList(bizAllowanceItem);
        ExcelUtil<BizAllowanceItem> util = new ExcelUtil<BizAllowanceItem>(BizAllowanceItem.class);
        util.exportExcel(response, list, "其他酬金明细数据");
    }

    /**
     * 获取其他酬金明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:allowanceItem:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizAllowanceItemService.selectBizAllowanceItemById(id));
    }

    /**
     * 新增其他酬金明细
     */
    @PreAuthorize("@ss.hasPermi('system:allowanceItem:add')")
    @Log(title = "其他酬金明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizAllowanceItem bizAllowanceItem)
    {
        return toAjax(bizAllowanceItemService.insertBizAllowanceItem(bizAllowanceItem));
    }

    /**
     * 修改其他酬金明细
     */
    @PreAuthorize("@ss.hasPermi('system:allowanceItem:edit')")
    @Log(title = "其他酬金明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizAllowanceItem bizAllowanceItem)
    {
        return toAjax(bizAllowanceItemService.updateBizAllowanceItem(bizAllowanceItem));
    }

    /**
     * 删除其他酬金明细
     */
    @PreAuthorize("@ss.hasPermi('system:allowanceItem:remove')")
    @Log(title = "其他酬金明细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizAllowanceItemService.deleteBizAllowanceItemByIds(ids));
    }
}
