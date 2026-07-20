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
import com.workload.system.domain.BizPayRate;
import com.workload.system.service.IBizPayRateService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * 职称单位酬金费率Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/payRate")
public class BizPayRateController extends BaseController
{
    @Autowired
    private IBizPayRateService bizPayRateService;

    /**
     * 查询职称单位酬金费率列表
     */
    @PreAuthorize("@ss.hasPermi('system:payRate:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizPayRate bizPayRate)
    {
        startPage();
        List<BizPayRate> list = bizPayRateService.selectBizPayRateList(bizPayRate);
        return getDataTable(list);
    }

    /**
     * 导出职称单位酬金费率列表
     */
    @PreAuthorize("@ss.hasPermi('system:payRate:export')")
    @Log(title = "职称单位酬金费率", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizPayRate bizPayRate)
    {
        List<BizPayRate> list = bizPayRateService.selectBizPayRateList(bizPayRate);
        ExcelUtil<BizPayRate> util = new ExcelUtil<BizPayRate>(BizPayRate.class);
        util.exportExcel(response, list, "职称单位酬金费率数据");
    }

    /**
     * 获取职称单位酬金费率详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:payRate:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizPayRateService.selectBizPayRateById(id));
    }

    /**
     * 新增职称单位酬金费率
     */
    @PreAuthorize("@ss.hasPermi('system:payRate:add')")
    @Log(title = "职称单位酬金费率", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizPayRate bizPayRate)
    {
        return toAjax(bizPayRateService.insertBizPayRate(bizPayRate));
    }

    /**
     * 修改职称单位酬金费率
     */
    @PreAuthorize("@ss.hasPermi('system:payRate:edit')")
    @Log(title = "职称单位酬金费率", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizPayRate bizPayRate)
    {
        return toAjax(bizPayRateService.updateBizPayRate(bizPayRate));
    }

    /**
     * 删除职称单位酬金费率
     */
    @PreAuthorize("@ss.hasPermi('system:payRate:remove')")
    @Log(title = "职称单位酬金费率", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizPayRateService.deleteBizPayRateByIds(ids));
    }
}
