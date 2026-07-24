package com.workload.system.controller;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workload.common.annotation.Log;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.core.domain.entity.SysUser;
import com.workload.common.enums.BusinessType;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.SecurityUtils;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.service.IBizWorkloadSummaryService;

/**
 * 工作量审批 Controller
 * <p>
 * 审批流程：0填报中 → 1教务助理待审 → 2院领导待签 → 3已完结
 *
 * @author wflg
 */
@RestController
@RequestMapping("/system/audit")
public class BizAuditController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(BizAuditController.class);

    @Autowired
    private IBizWorkloadSummaryService summaryService;

    /**
     * 提交审核（教务助理：填报中 → 待审）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:submit')")
    @Log(title = "提交工作量审核", businessType = BusinessType.UPDATE)
    @PostMapping("/submit")
    public AjaxResult submit(@RequestParam("id") Long id)
    {
        BizWorkloadSummary summary = summaryService.selectBizWorkloadSummaryById(id);
        assertStatus(summary, 0, "只有草稿状态才能提交审核");

        summary.setStatus(1);
        summary.setUpdateBy(SecurityUtils.getUsername());
        summary.setUpdateTime(new Date());
        return toAjax(summaryService.updateBizWorkloadSummary(summary));
    }

    /**
     * 教务助理审核通过（待审 → 待签）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:approve')")
    @Log(title = "教务助理审核通过", businessType = BusinessType.UPDATE)
    @PostMapping("/approve")
    public AjaxResult approve(@RequestParam("id") Long id)
    {
        BizWorkloadSummary summary = summaryService.selectBizWorkloadSummaryById(id);
        assertStatus(summary, 1, "只有待审状态才能审核");

        String username = SecurityUtils.getUsername();
        summary.setStatus(2);
        summary.setAcademicAssistantSign(username);
        summary.setAcademicAssistantSignTime(new Date());
        summary.setUpdateBy(username);
        summary.setUpdateTime(new Date());
        return toAjax(summaryService.updateBizWorkloadSummary(summary));
    }

    /**
     * 教务助理驳回（待审 → 填报中）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:reject')")
    @Log(title = "驳回工作量审核", businessType = BusinessType.UPDATE)
    @PostMapping("/reject")
    public AjaxResult reject(@RequestParam("id") Long id, @RequestParam(value = "reason", required = false) String reason)
    {
        BizWorkloadSummary summary = summaryService.selectBizWorkloadSummaryById(id);
        assertStatus(summary, 1, "只有待审状态才能驳回");

        String username = SecurityUtils.getUsername();
        summary.setStatus(0);
        summary.setRemark(reason != null ? reason : summary.getRemark());
        summary.setUpdateBy(username);
        summary.setUpdateTime(new Date());
        return toAjax(summaryService.updateBizWorkloadSummary(summary));
    }

    /**
     * 院领导签字确认（待签 → 已完结）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:sign')")
    @Log(title = "院领导签字确认", businessType = BusinessType.UPDATE)
    @PostMapping("/sign")
    public AjaxResult sign(@RequestParam("id") Long id)
    {
        BizWorkloadSummary summary = summaryService.selectBizWorkloadSummaryById(id);
        assertStatus(summary, 2, "只有待签状态才能签字");

        String username = SecurityUtils.getUsername();
        summary.setStatus(3);
        summary.setDeptLeaderSign(username);
        summary.setDeptLeaderSignTime(new Date());
        summary.setLockTime(new Date());
        summary.setUpdateBy(username);
        summary.setUpdateTime(new Date());
        return toAjax(summaryService.updateBizWorkloadSummary(summary));
    }

    /**
     * 解锁（管理员：已完结 → 填报中）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:unlock')")
    @Log(title = "解锁工作量汇总", businessType = BusinessType.UPDATE)
    @PostMapping("/unlock")
    public AjaxResult unlock(@RequestParam("id") Long id)
    {
        BizWorkloadSummary summary = summaryService.selectBizWorkloadSummaryById(id);
        assertStatus(summary, 3, "只有已完结状态才能解锁");

        String username = SecurityUtils.getUsername();
        summary.setStatus(0);
        summary.setLockTime(null);
        summary.setUpdateBy(username);
        summary.setUpdateTime(new Date());
        log.info("管理员 {} 解锁了汇总 id={}", username, id);
        return toAjax(summaryService.updateBizWorkloadSummary(summary));
    }

    /**
     * 批量提交审核
     */
    @PreAuthorize("@ss.hasPermi('system:audit:submit')")
    @Log(title = "批量提交工作量审核", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSubmit")
    public AjaxResult batchSubmit(@RequestParam("ids") Long[] ids)
    {
        int success = 0;
        for (Long id : ids)
        {
            try
            {
                BizWorkloadSummary summary = summaryService.selectBizWorkloadSummaryById(id);
                if (summary != null && summary.getStatus() == 0)
                {
                    summary.setStatus(1);
                    summary.setUpdateBy(SecurityUtils.getUsername());
                    summary.setUpdateTime(new Date());
                    summaryService.updateBizWorkloadSummary(summary);
                    success++;
                }
            }
            catch (Exception e)
            {
                log.warn("批量提交失败 id={}: {}", id, e.getMessage());
            }
        }
        return success("成功提交 " + success + " 条");
    }

    /**
     * 状态校验
     */
    private void assertStatus(BizWorkloadSummary summary, int expectedStatus, String message)
    {
        if (summary == null)
        {
            throw new ServiceException("汇总记录不存在");
        }
        if (!Integer.valueOf(expectedStatus).equals(summary.getStatus()))
        {
            throw new ServiceException(message + "（当前状态: " + summary.getStatus() + "）");
        }
    }
}
