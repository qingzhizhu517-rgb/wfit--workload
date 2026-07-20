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
import com.workload.system.domain.BizWlPractice;
import com.workload.system.service.IBizWlPracticeService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * G2课内实践明细Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/wlPractice")
public class BizWlPracticeController extends BaseController
{
    @Autowired
    private IBizWlPracticeService bizWlPracticeService;

    /**
     * 查询G2课内实践明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlPractice:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWlPractice bizWlPractice)
    {
        startPage();
        List<BizWlPractice> list = bizWlPracticeService.selectBizWlPracticeList(bizWlPractice);
        return getDataTable(list);
    }

    /**
     * 导出G2课内实践明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlPractice:export')")
    @Log(title = "G2课内实践明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWlPractice bizWlPractice)
    {
        List<BizWlPractice> list = bizWlPracticeService.selectBizWlPracticeList(bizWlPractice);
        ExcelUtil<BizWlPractice> util = new ExcelUtil<BizWlPractice>(BizWlPractice.class);
        util.exportExcel(response, list, "G2课内实践明细数据");
    }

    /**
     * 获取G2课内实践明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:wlPractice:query')")
    @GetMapping(value = "/{itemId}")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId)
    {
        return success(bizWlPracticeService.selectBizWlPracticeByItemId(itemId));
    }

    /**
     * 新增G2课内实践明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlPractice:add')")
    @Log(title = "G2课内实践明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWlPractice bizWlPractice)
    {
        return toAjax(bizWlPracticeService.insertBizWlPractice(bizWlPractice));
    }

    /**
     * 修改G2课内实践明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlPractice:edit')")
    @Log(title = "G2课内实践明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWlPractice bizWlPractice)
    {
        return toAjax(bizWlPracticeService.updateBizWlPractice(bizWlPractice));
    }

    /**
     * 删除G2课内实践明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlPractice:remove')")
    @Log(title = "G2课内实践明细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{itemIds}")
    public AjaxResult remove(@PathVariable Long[] itemIds)
    {
        return toAjax(bizWlPracticeService.deleteBizWlPracticeByItemIds(itemIds));
    }
}
