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
import com.workload.system.domain.BizWorkloadCategoryDict;
import com.workload.system.service.IBizWorkloadCategoryDictService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * 工作量类别字典Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/workloadCategoryDict")
public class BizWorkloadCategoryDictController extends BaseController
{
    @Autowired
    private IBizWorkloadCategoryDictService bizWorkloadCategoryDictService;

    /**
     * 查询工作量类别字典列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadCategoryDict:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWorkloadCategoryDict bizWorkloadCategoryDict)
    {
        startPage();
        List<BizWorkloadCategoryDict> list = bizWorkloadCategoryDictService.selectBizWorkloadCategoryDictList(bizWorkloadCategoryDict);
        return getDataTable(list);
    }

    /**
     * 导出工作量类别字典列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadCategoryDict:export')")
    @Log(title = "工作量类别字典", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWorkloadCategoryDict bizWorkloadCategoryDict)
    {
        List<BizWorkloadCategoryDict> list = bizWorkloadCategoryDictService.selectBizWorkloadCategoryDictList(bizWorkloadCategoryDict);
        ExcelUtil<BizWorkloadCategoryDict> util = new ExcelUtil<BizWorkloadCategoryDict>(BizWorkloadCategoryDict.class);
        util.exportExcel(response, list, "工作量类别字典数据");
    }

    /**
     * 获取工作量类别字典详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:workloadCategoryDict:query')")
    @GetMapping(value = "/{typeCode}")
    public AjaxResult getInfo(@PathVariable("typeCode") String typeCode)
    {
        return success(bizWorkloadCategoryDictService.selectBizWorkloadCategoryDictByTypeCode(typeCode));
    }

    /**
     * 新增工作量类别字典
     */
    @PreAuthorize("@ss.hasPermi('system:workloadCategoryDict:add')")
    @Log(title = "工作量类别字典", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWorkloadCategoryDict bizWorkloadCategoryDict)
    {
        return toAjax(bizWorkloadCategoryDictService.insertBizWorkloadCategoryDict(bizWorkloadCategoryDict));
    }

    /**
     * 修改工作量类别字典
     */
    @PreAuthorize("@ss.hasPermi('system:workloadCategoryDict:edit')")
    @Log(title = "工作量类别字典", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWorkloadCategoryDict bizWorkloadCategoryDict)
    {
        return toAjax(bizWorkloadCategoryDictService.updateBizWorkloadCategoryDict(bizWorkloadCategoryDict));
    }

    /**
     * 删除工作量类别字典
     */
    @PreAuthorize("@ss.hasPermi('system:workloadCategoryDict:remove')")
    @Log(title = "工作量类别字典", businessType = BusinessType.DELETE)
	@DeleteMapping("/{typeCodes}")
    public AjaxResult remove(@PathVariable String[] typeCodes)
    {
        return toAjax(bizWorkloadCategoryDictService.deleteBizWorkloadCategoryDictByTypeCodes(typeCodes));
    }
}
