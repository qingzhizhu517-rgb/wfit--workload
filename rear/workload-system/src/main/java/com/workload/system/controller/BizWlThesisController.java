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
import com.workload.system.domain.BizWlThesis;
import com.workload.system.service.IBizWlThesisService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * G5毕业论文明细Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/wlThesis")
public class BizWlThesisController extends BaseController
{
    @Autowired
    private IBizWlThesisService bizWlThesisService;

    /**
     * 查询G5毕业论文明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlThesis:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWlThesis bizWlThesis)
    {
        startPage();
        List<BizWlThesis> list = bizWlThesisService.selectBizWlThesisList(bizWlThesis);
        return getDataTable(list);
    }

    /**
     * 导出G5毕业论文明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:wlThesis:export')")
    @Log(title = "G5毕业论文明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWlThesis bizWlThesis)
    {
        List<BizWlThesis> list = bizWlThesisService.selectBizWlThesisList(bizWlThesis);
        ExcelUtil<BizWlThesis> util = new ExcelUtil<BizWlThesis>(BizWlThesis.class);
        util.exportExcel(response, list, "G5毕业论文明细数据");
    }

    /**
     * 获取G5毕业论文明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:wlThesis:query')")
    @GetMapping(value = "/{itemId}")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId)
    {
        return success(bizWlThesisService.selectBizWlThesisByItemId(itemId));
    }

    /**
     * 新增G5毕业论文明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlThesis:add')")
    @Log(title = "G5毕业论文明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWlThesis bizWlThesis)
    {
        return toAjax(bizWlThesisService.insertBizWlThesis(bizWlThesis));
    }

    /**
     * 修改G5毕业论文明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlThesis:edit')")
    @Log(title = "G5毕业论文明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWlThesis bizWlThesis)
    {
        return toAjax(bizWlThesisService.updateBizWlThesis(bizWlThesis));
    }

    /**
     * 删除G5毕业论文明细
     */
    @PreAuthorize("@ss.hasPermi('system:wlThesis:remove')")
    @Log(title = "G5毕业论文明细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{itemIds}")
    public AjaxResult remove(@PathVariable Long[] itemIds)
    {
        return toAjax(bizWlThesisService.deleteBizWlThesisByItemIds(itemIds));
    }
}
