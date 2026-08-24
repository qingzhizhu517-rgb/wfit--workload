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
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.service.IBizWorkloadSummaryService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.utils.DataScopeUtil;
import com.workload.common.exception.ServiceException;
import com.workload.common.core.page.TableDataInfo;

/**
 * 学期工作量汇总Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/workloadSummary")
public class BizWorkloadSummaryController extends BaseController
{
    /** 汇总状态：已完结（锁定） */
    private static final int STATUS_FINISHED = 3;

    @Autowired
    private IBizWorkloadSummaryService bizWorkloadSummaryService;

    /**
     * 查询学期工作量汇总列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWorkloadSummary bizWorkloadSummary)
    {
        // 教师角色只能查看自己的数据（统一收口至 DataScopeUtil）
        bizWorkloadSummary.setUserId(DataScopeUtil.resolveUserId(bizWorkloadSummary.getUserId()));
        startPage();
        List<BizWorkloadSummary> list = bizWorkloadSummaryService.selectBizWorkloadSummaryList(bizWorkloadSummary);
        return getDataTable(list);
    }

    /**
     * 导出学期工作量汇总列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:export')")
    @Log(title = "学期工作量汇总", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWorkloadSummary bizWorkloadSummary)
    {
        // 教师角色只能导出自己的数据（统一收口至 DataScopeUtil）
        bizWorkloadSummary.setUserId(DataScopeUtil.resolveUserId(bizWorkloadSummary.getUserId()));
        List<BizWorkloadSummary> list = bizWorkloadSummaryService.selectBizWorkloadSummaryList(bizWorkloadSummary);
        ExcelUtil<BizWorkloadSummary> util = new ExcelUtil<BizWorkloadSummary>(BizWorkloadSummary.class);
        util.exportExcel(response, list, "学期工作量汇总数据");
    }

    /**
     * 获取学期工作量汇总详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        BizWorkloadSummary bizWorkloadSummary = bizWorkloadSummaryService.selectBizWorkloadSummaryById(id);
        if (bizWorkloadSummary != null)
        {
            // 教师只能查看本人记录，防 IDOR 遍历
            DataScopeUtil.assertOwnOrAdmin(bizWorkloadSummary.getUserId());
        }
        return success(bizWorkloadSummary);
    }

    /**
     * 新增学期工作量汇总
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:add')")
    @Log(title = "学期工作量汇总", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizWorkloadSummary bizWorkloadSummary)
    {
        // 教师只能为本人新增汇总
        bizWorkloadSummary.setUserId(DataScopeUtil.resolveUserId(bizWorkloadSummary.getUserId()));
        return toAjax(bizWorkloadSummaryService.insertBizWorkloadSummary(bizWorkloadSummary));
    }

    /**
     * 修改学期工作量汇总
     *
     * 说明：审批状态迁移与签字一律走 /system/audit/* 审批链（带原子条件更新 + 审计日志），
     * 本通用 edit 端点对所有角色都禁止改动 status/各签字字段/lock_time（防状态机后门），
     * 且已完结(3)记录整体锁定不可改。教师额外仅可改备注。
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:edit')")
    @Log(title = "学期工作量汇总", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWorkloadSummary bizWorkloadSummary)
    {
        if (bizWorkloadSummary.getId() == null)
        {
            throw new ServiceException("缺少记录ID，无法修改");
        }
        BizWorkloadSummary existing = bizWorkloadSummaryService.selectBizWorkloadSummaryById(bizWorkloadSummary.getId());
        if (existing == null)
        {
            throw new ServiceException("待修改的记录不存在");
        }
        // 按数据库记录的归属人校验，防止伪造 userId 绕过
        DataScopeUtil.assertOwnOrAdmin(existing.getUserId());

        // 已完结(3)记录锁定，任何角色不可经通用 edit 修改；如需变更请先走审批解锁
        if (existing.getStatus() != null && existing.getStatus() == STATUS_FINISHED)
        {
            throw new ServiceException("该汇总已完结锁定，禁止修改；如需变更请先解锁");
        }

        // 审批流字段一律不允许经此端点写入（对所有角色生效），状态迁移只能走审批链。
        // 置 null 后 Mapper 动态 SQL 会跳过更新（防 mass-assignment / 状态机后门）。
        bizWorkloadSummary.setStatus(null);
        bizWorkloadSummary.setTeacherSign(null);
        bizWorkloadSummary.setTeacherSignTime(null);
        bizWorkloadSummary.setDeptLeaderSign(null);
        bizWorkloadSummary.setDeptLeaderSignTime(null);
        bizWorkloadSummary.setAcademicAssistantSign(null);
        bizWorkloadSummary.setAcademicAssistantSignTime(null);
        bizWorkloadSummary.setLockTime(null);

        if (DataScopeUtil.isTeacherOnly())
        {
            // 工作量/酬金等属敏感数据，教师仅可修改备注；敏感字段置空跳过更新
            bizWorkloadSummary.setUserId(null);
            bizWorkloadSummary.setSemester(null);
            bizWorkloadSummary.setAcademicYear(null);
            bizWorkloadSummary.setG7(null);
            bizWorkloadSummary.setG8(null);
            bizWorkloadSummary.setG9(null);
            bizWorkloadSummary.setG10(null);
            bizWorkloadSummary.setG11(null);
            bizWorkloadSummary.setTotalWorkload(null);
            bizWorkloadSummary.setRatedWorkload(null);
            bizWorkloadSummary.setExcessWorkload(null);
            bizWorkloadSummary.setTitle(null);
            bizWorkloadSummary.setPayRate(null);
            bizWorkloadSummary.setPerformancePay(null);
            bizWorkloadSummary.setIsCapped(null);
            bizWorkloadSummary.setBasicTeachingStandard(null);
            bizWorkloadSummary.setBasicTeachingMet(null);
            bizWorkloadSummary.setCreateBy(null);
            bizWorkloadSummary.setCreateTime(null);
        }
        return toAjax(bizWorkloadSummaryService.updateBizWorkloadSummary(bizWorkloadSummary));
    }

    /**
     * 删除学期工作量汇总
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:remove')")
    @Log(title = "学期工作量汇总", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        if (DataScopeUtil.isTeacherOnly())
        {
            // 教师只能删除本人记录
            for (Long id : ids)
            {
                BizWorkloadSummary summary = bizWorkloadSummaryService.selectBizWorkloadSummaryById(id);
                if (summary != null)
                {
                    DataScopeUtil.assertOwnOrAdmin(summary.getUserId());
                }
            }
        }
        return toAjax(bizWorkloadSummaryService.deleteBizWorkloadSummaryByIds(ids));
    }
}
