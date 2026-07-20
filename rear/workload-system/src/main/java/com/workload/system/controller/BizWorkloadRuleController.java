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
import com.workload.system.domain.BizWorkloadRule;
import com.workload.system.service.IBizWorkloadRuleService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * 全局核算规则参数Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/workloadRule")
public class BizWorkloadRuleController extends BaseController
{
    @Autowired
    private IBizWorkloadRuleService bizWorkloadRuleService;

    /**
     * 查询全局核算规则参数列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadRule:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWorkloadRule bizWorkloadRule)
    {
        startPage();
        List<BizWorkloadRule> list = bizWorkloadRuleService.selectBizWorkloadRuleList(bizWorkloadRule);
        return getDataTable(list);
    }

    /**
     * 导出全局核算规则参数列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadRule:export')")
    @Log(title = "全局核算规则参数", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWorkloadRule bizWorkloadRule)
    {
        List<BizWorkloadRule> list = bizWorkloadRuleService.selectBizWorkloadRuleList(bizWorkloadRule);
        ExcelUtil<BizWorkloadRule> util = new ExcelUtil<BizWorkloadRule>(BizWorkloadRule.class);
        util.exportExcel(response, list, "全局核算规则参数数据");
    }

    /**
     * 获取全局核算规则参数详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:workloadRule:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizWorkloadRuleService.selectBizWorkloadRuleById(id));
    }

    /**
     * 新增全局核算规则参数
     */
    @PreAuthorize("@ss.hasPermi('system:workloadRule:add')")
    @Log(title = "全局核算规则参数", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWorkloadRule bizWorkloadRule)
    {
        return toAjax(bizWorkloadRuleService.insertBizWorkloadRule(bizWorkloadRule));
    }

    /**
     * 修改全局核算规则参数
     */
    @PreAuthorize("@ss.hasPermi('system:workloadRule:edit')")
    @Log(title = "全局核算规则参数", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWorkloadRule bizWorkloadRule)
    {
        return toAjax(bizWorkloadRuleService.updateBizWorkloadRule(bizWorkloadRule));
    }

    /**
     * 删除全局核算规则参数
     */
    @PreAuthorize("@ss.hasPermi('system:workloadRule:remove')")
    @Log(title = "全局核算规则参数", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizWorkloadRuleService.deleteBizWorkloadRuleByIds(ids));
    }
}
