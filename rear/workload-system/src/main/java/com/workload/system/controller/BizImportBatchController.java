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
import com.workload.system.domain.BizImportBatch;
import com.workload.system.service.IBizImportBatchService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * 导入批次记录Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/importBatch")
public class BizImportBatchController extends BaseController
{
    @Autowired
    private IBizImportBatchService bizImportBatchService;

    /**
     * 查询导入批次记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizImportBatch bizImportBatch)
    {
        startPage();
        List<BizImportBatch> list = bizImportBatchService.selectBizImportBatchList(bizImportBatch);
        return getDataTable(list);
    }

    /**
     * 导出导入批次记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:export')")
    @Log(title = "导入批次记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizImportBatch bizImportBatch)
    {
        List<BizImportBatch> list = bizImportBatchService.selectBizImportBatchList(bizImportBatch);
        ExcelUtil<BizImportBatch> util = new ExcelUtil<BizImportBatch>(BizImportBatch.class);
        util.exportExcel(response, list, "导入批次记录数据");
    }

    /**
     * 获取导入批次记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizImportBatchService.selectBizImportBatchById(id));
    }

    /**
     * 新增导入批次记录
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:add')")
    @Log(title = "导入批次记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizImportBatch bizImportBatch)
    {
        return toAjax(bizImportBatchService.insertBizImportBatch(bizImportBatch));
    }

    /**
     * 修改导入批次记录
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:edit')")
    @Log(title = "导入批次记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizImportBatch bizImportBatch)
    {
        return toAjax(bizImportBatchService.updateBizImportBatch(bizImportBatch));
    }

    /**
     * 删除导入批次记录
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:remove')")
    @Log(title = "导入批次记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizImportBatchService.deleteBizImportBatchByIds(ids));
    }
}
