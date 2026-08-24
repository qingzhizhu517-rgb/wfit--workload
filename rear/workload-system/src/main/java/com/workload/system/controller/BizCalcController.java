package com.workload.system.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.workload.common.annotation.Log;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.enums.BusinessType;
import com.workload.common.utils.DataScopeUtil;
import com.workload.system.calc.ManagementItemGenerator;
import com.workload.system.calc.PayCalcService;
import com.workload.system.calc.SummaryCalcService;
import com.workload.system.calc.WorkloadCalcService;
import com.workload.system.domain.BizWorkloadSummary;

/**
 * 工作量计算引擎Controller（单条重算/学期汇总/预览）
 *
 * @author wflg
 * @date 2026-07-21
 */
@RestController
@RequestMapping("/system/calc")
public class BizCalcController extends BaseController
{
    @Autowired
    private WorkloadCalcService workloadCalcService;

    @Autowired
    private SummaryCalcService summaryCalcService;

    @Autowired
    private PayCalcService payCalcService;

    @Autowired
    private ManagementItemGenerator managementItemGenerator;

    /**
     * 重算单条明细工作量
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:edit')")
    @Log(title = "计算引擎", businessType = BusinessType.UPDATE)
    @PostMapping("/recalcItem/{itemId}")
    public AjaxResult recalcItem(@PathVariable("itemId") Long itemId)
    {
        return success(workloadCalcService.recalcItem(itemId));
    }

    /**
     * 重算某教师某学期全部未冻结明细
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:edit')")
    @Log(title = "计算引擎", businessType = BusinessType.UPDATE)
    @PostMapping("/recalcItems")
    public AjaxResult recalcItems(@RequestParam Long userId, @RequestParam String semester)
    {
        return success(workloadCalcService.recalcItems(userId, semester));
    }

    /**
     * 重算学期汇总（落库）
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:edit')")
    @Log(title = "计算引擎", businessType = BusinessType.UPDATE)
    @PostMapping("/recalcSummary")
    public AjaxResult recalcSummary(@RequestParam Long userId, @RequestParam String semester)
    {
        BizWorkloadSummary summary = summaryCalcService.recalcSummary(userId, semester, true);
        Map<String, Object> data = new HashMap<>();
        data.put("summary", summary);
        data.put("unconfirmedCount", summaryCalcService.countUnconfirmed(userId, semester));
        return success(data);
    }

    /**
     * 汇总预览（不落库，用于导出前仿真预览）
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:query')")
    @GetMapping("/preview")
    public AjaxResult preview(@RequestParam Long userId, @RequestParam String semester)
    {
        // 教师角色只能预览本人汇总（统一收口至 DataScopeUtil），防越权查看他人数据
        userId = DataScopeUtil.resolveUserId(userId);
        BizWorkloadSummary summary = summaryCalcService.recalcSummary(userId, semester, false);
        Map<String, Object> data = new HashMap<>();
        data.put("summary", summary);
        data.put("unconfirmedCount", summaryCalcService.countUnconfirmed(userId, semester));
        return success(data);
    }

    /**
     * 重算酬金（需先重算汇总）
     */
    @PreAuthorize("@ss.hasPermi('system:payRecord:edit')")
    @Log(title = "计算引擎", businessType = BusinessType.UPDATE)
    @PostMapping("/recalcPay")
    public AjaxResult recalcPay(@RequestParam Long userId, @RequestParam String semester)
    {
        return success(payCalcService.recalcPay(userId, semester));
    }

    /**
     * 由岗位任职生成/更新 G11 管理服务明细
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:add')")
    @Log(title = "计算引擎", businessType = BusinessType.INSERT)
    @PostMapping("/genG11")
    public AjaxResult genG11(@RequestParam String semester, @RequestParam(required = false) Long userId)
    {
        // 教师角色只能生成本人 G11 明细（统一收口至 DataScopeUtil）；管理角色保留入参，null 表示全量
        Long targetUserId = DataScopeUtil.resolveUserId(userId);
        int count = targetUserId == null ? managementItemGenerator.generateForSemester(semester)
                : managementItemGenerator.generate(targetUserId, semester);
        return success(count);
    }

    /**
     * 一把梭：重算全部未冻结明细 -> 汇总 -> 酬金（Service 层单事务编排，失败整体回滚）
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:edit')")
    @Log(title = "计算引擎", businessType = BusinessType.UPDATE)
    @PostMapping("/recalcAll")
    public AjaxResult recalcAll(@RequestParam Long userId, @RequestParam String semester)
    {
        return success(workloadCalcService.recalcAll(userId, semester));
    }
}
