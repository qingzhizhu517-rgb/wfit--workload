package com.workload.system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workload.common.annotation.Log;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.enums.BusinessType;
import com.workload.system.service.BizAuditService;

/**
 * 工作量审批 Controller
 * <p>
 * 审批流程：0填报中 → 1教务助理待审 → 2院领导待签 → 3已完结。
 * 状态机逻辑全部下沉至 {@link BizAuditService}，本类只做参数绑定、权限校验与调用。
 *
 * @author wflg
 */
@RestController
@RequestMapping("/system/audit")
public class BizAuditController extends BaseController
{
    @Autowired
    private BizAuditService bizAuditService;

    /**
     * 提交审核（填报中 → 待审）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:submit')")
    @Log(title = "提交工作量审核", businessType = BusinessType.UPDATE)
    @PostMapping("/submit")
    public AjaxResult submit(@RequestParam("id") Long id)
    {
        bizAuditService.submit(id);
        return success();
    }

    /**
     * 教务助理审核通过（待审 → 待签）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:approve')")
    @Log(title = "教务助理审核通过", businessType = BusinessType.UPDATE)
    @PostMapping("/approve")
    public AjaxResult approve(@RequestParam("id") Long id)
    {
        bizAuditService.approve(id);
        return success();
    }

    /**
     * 教务助理驳回（待审 → 填报中）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:reject')")
    @Log(title = "驳回工作量审核", businessType = BusinessType.UPDATE)
    @PostMapping("/reject")
    public AjaxResult reject(@RequestParam("id") Long id, @RequestParam(value = "reason", required = false) String reason)
    {
        bizAuditService.reject(id, reason);
        return success();
    }

    /**
     * 院领导签字确认（待签 → 已完结）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:sign')")
    @Log(title = "院领导签字确认", businessType = BusinessType.UPDATE)
    @PostMapping("/sign")
    public AjaxResult sign(@RequestParam("id") Long id)
    {
        bizAuditService.sign(id);
        return success();
    }

    /**
     * 解锁（管理员：已完结 → 填报中）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:unlock')")
    @Log(title = "解锁工作量汇总", businessType = BusinessType.UPDATE)
    @PostMapping("/unlock")
    public AjaxResult unlock(@RequestParam("id") Long id)
    {
        bizAuditService.unlock(id);
        return success();
    }

    /**
     * 教师本人确认汇总（写 teacher_sign/teacher_sign_time）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:teacherConfirm')")
    @Log(title = "教师确认工作量汇总", businessType = BusinessType.UPDATE)
    @PostMapping("/teacherConfirm")
    public AjaxResult teacherConfirm(@RequestParam("id") Long id)
    {
        bizAuditService.teacherConfirm(id);
        return success();
    }

    /**
     * 批量提交审核（逐条独立事务，返回成功/失败明细回执）
     */
    @PreAuthorize("@ss.hasPermi('system:audit:submit')")
    @Log(title = "批量提交工作量审核", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSubmit")
    public AjaxResult batchSubmit(@RequestParam("ids") Long[] ids)
    {
        return success(bizAuditService.batchSubmit(ids));
    }
}
