package com.workload.system.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import com.workload.system.domain.dto.CalculationRunRequest;

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

    /**
     * 批量一键核算：逐教师执行「明细 -> 汇总 -> 酬金」，每人独立事务。
     * <p>
     * userIds 传空（或整个 body 省略）表示该学期全部有明细的教师；传列表则只算勾选的这几位。
     * 与 {@code /recalcAll} 的差别在事务粒度：本端点单个教师失败只记入 failures，其余照算。
     * <p>
     * 教师角色被 {@link DataScopeUtil#resolveUserId} 强制收敛为「只能算自己」，
     * 不允许借批量入口绕开数据范围拿到他人数据。
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:edit')")
    @Log(title = "计算引擎", businessType = BusinessType.UPDATE)
    @PostMapping("/recalcAllBatch")
    public AjaxResult recalcAllBatch(@RequestParam String semester,
                                     @RequestBody(required = false) List<Long> userIds)
    {
        // 教师角色：无论传了谁，一律收敛成本人；管理角色下 resolveUserId(null) 返回 null，保持全量语义
        Long scoped = DataScopeUtil.resolveUserId(null);
        List<Long> targets = scoped != null ? Collections.singletonList(scoped) : userIds;
        return success(workloadCalcService.recalcAllBatch(targets, semester));
    }

    /**
     * 阶段化一键核算（单教师）：校验 → 同步G11(可选) → 明细 → 汇总 → 酬金，逐阶段返回。
     * <p>
     * 教师角色被 {@link DataScopeUtil#resolveUserId} 强制收敛为「只能算自己」。
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:edit')")
    @Log(title = "计算引擎", businessType = BusinessType.UPDATE)
    @PostMapping("/run")
    public AjaxResult run(@RequestBody CalculationRunRequest request)
    {
        if (request == null)
        {
            request = new CalculationRunRequest();
        }
        request.setUserId(DataScopeUtil.resolveUserId(request.getUserId()));
        return success(workloadCalcService.run(request));
    }

    /**
     * 批量阶段化一键核算：逐教师独立事务执行 {@link #run}，单人失败只记入 failures。
     * <p>
     * userIds 传空（或整个 body 省略）表示该学期全部有明细的教师；教师角色一律收敛为本人。
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:edit')")
    @Log(title = "计算引擎", businessType = BusinessType.UPDATE)
    @PostMapping("/runBatch")
    public AjaxResult runBatch(@RequestParam String semester,
                               @RequestBody(required = false) RunBatchBody body)
    {
        boolean includeG11 = body != null && body.isIncludeG11();
        List<Long> requested = body != null ? body.getUserIds() : null;
        // 教师角色：无论传了谁，一律收敛成本人；管理角色下 resolveUserId(null) 返回 null，保持全量语义
        Long scoped = DataScopeUtil.resolveUserId(null);
        List<Long> targets = scoped != null ? Collections.singletonList(scoped) : requested;
        return success(workloadCalcService.runBatch(targets, semester, includeG11));
    }

    /** 批量核算请求体：勾选的教师列表 + 是否同步 G11。 */
    public static class RunBatchBody
    {
        private List<Long> userIds;
        private boolean includeG11;

        public List<Long> getUserIds()
        {
            return userIds;
        }

        public void setUserIds(List<Long> userIds)
        {
            this.userIds = userIds;
        }

        public boolean isIncludeG11()
        {
            return includeG11;
        }

        public void setIncludeG11(boolean includeG11)
        {
            this.includeG11 = includeG11;
        }
    }
}
