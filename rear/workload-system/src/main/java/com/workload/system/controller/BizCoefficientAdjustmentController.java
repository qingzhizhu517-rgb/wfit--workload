package com.workload.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.workload.common.annotation.Log;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.core.page.TableDataInfo;
import com.workload.common.enums.BusinessType;
import com.workload.common.utils.DataScopeUtil;
import com.workload.system.domain.BizCoefficientAdjustment;
import com.workload.system.mapper.BizCoefficientAdjustmentMapper;
import com.workload.system.service.ICoefficientAdjustmentService;
import com.workload.system.service.ICoefficientAdjustmentService.CoefficientAdjustmentRequest;

/**
 * G1/G2 系数调整申请Controller（教师提交 / 教务审批）
 *
 * <p>教师提交的申请所属教师由服务端从明细带出，old_value 也由服务端读取，均不信任客户端；
 * 审核人从 SecurityContext 取。查询走 {@link DataScopeUtil#resolveUserId} 收口，
 * 教师入口（/myList）强制只看本人，教务入口（/list）可全量。</p>
 *
 * <p>注：权限串 {@code system:coefficientAdjustment:*} 的 sys_menu 登记属 Task 10，本处仅声明注解。</p>
 *
 * @author wflg
 * @date 2026-09-12
 */
@RestController
@RequestMapping("/system/coefficientAdjustment")
public class BizCoefficientAdjustmentController extends BaseController
{
    @Autowired
    private ICoefficientAdjustmentService coefficientAdjustmentService;

    @Autowired
    private BizCoefficientAdjustmentMapper coefficientAdjustmentMapper;

    /**
     * 教师提交系数调整申请。
     * <p>所属教师由服务端从 itemId 对应明细带出；old_value 由服务端读取，客户端传值被忽略。</p>
     */
    @PreAuthorize("@ss.hasPermi('system:coefficientAdjustment:add')")
    @Log(title = "系数调整申请", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CoefficientAdjustmentRequest request)
    {
        Long id = coefficientAdjustmentService.submit(request);
        return AjaxResult.success(id);
    }

    /**
     * 我的申请列表（教师视角）：强制只看本人，忽略入参中的他人 userId。
     */
    @PreAuthorize("@ss.hasPermi('system:coefficientAdjustment:list')")
    @GetMapping("/myList")
    public TableDataInfo myList(BizCoefficientAdjustment query)
    {
        startPage();
        query.setUserId(DataScopeUtil.resolveUserId(query.getUserId()));
        List<BizCoefficientAdjustment> list = coefficientAdjustmentMapper.selectCoefficientAdjustmentList(query);
        return getDataTable(list);
    }

    /**
     * 全部申请列表（教务审批视角）：管理角色全量；教师角色仍被收口为本人。
     */
    @PreAuthorize("@ss.hasPermi('system:coefficientAdjustment:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizCoefficientAdjustment query)
    {
        startPage();
        // 教师角色即便命中 list 权限，也只能看到本人；管理角色 resolveUserId 原样返回入参（含 null 全量）
        query.setUserId(DataScopeUtil.resolveUserId(query.getUserId()));
        List<BizCoefficientAdjustment> list = coefficientAdjustmentMapper.selectCoefficientAdjustmentList(query);
        return getDataTable(list);
    }

    /**
     * 通过申请：原子应用系数并重算（审核人取自当前登录用户）。
     */
    @PreAuthorize("@ss.hasPermi('system:coefficientAdjustment:approve')")
    @Log(title = "系数调整申请", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable("id") Long id, @RequestBody(required = false) ReviewBody body)
    {
        coefficientAdjustmentService.approve(id, body == null ? null : body.getReviewReason());
        return AjaxResult.success();
    }

    /**
     * 驳回申请（reviewReason 必填，由服务层校验）。
     */
    @PreAuthorize("@ss.hasPermi('system:coefficientAdjustment:reject')")
    @Log(title = "系数调整申请", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable("id") Long id, @RequestBody(required = false) ReviewBody body)
    {
        coefficientAdjustmentService.reject(id, body == null ? null : body.getReviewReason());
        return AjaxResult.success();
    }

    /** 审批入参：仅承载审核意见。 */
    public static class ReviewBody
    {
        private String reviewReason;

        public String getReviewReason()
        {
            return reviewReason;
        }

        public void setReviewReason(String reviewReason)
        {
            this.reviewReason = reviewReason;
        }
    }
}
