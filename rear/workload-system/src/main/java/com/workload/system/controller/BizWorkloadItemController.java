package com.workload.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.service.IBizWorkloadItemService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.utils.DataScopeUtil;
import com.workload.common.exception.ServiceException;
import com.workload.common.core.page.TableDataInfo;

/**
 * 工作量明细主表Controller
 * <p>
 * TODO 异议（objection）/申诉（appeal）/院部审批（dean_approval）接口属下一迭代；
 * 当前 deanApproval 类、appealStatus/appealReply 等字段不提供写入口，edit 白名单也不允许直改。
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/workloadItem")
public class BizWorkloadItemController extends BaseController
{
    @Autowired
    private IBizWorkloadItemService bizWorkloadItemService;

    /**
     * 查询工作量明细主表列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizWorkloadItem bizWorkloadItem)
    {
        // 教师角色只能查看自己的数据（统一收口至 DataScopeUtil）
        bizWorkloadItem.setUserId(DataScopeUtil.resolveUserId(bizWorkloadItem.getUserId()));
        startPage();
        List<BizWorkloadItem> list = bizWorkloadItemService.selectBizWorkloadItemList(bizWorkloadItem);
        return getDataTable(list);
    }

    /**
     * 导出工作量明细主表列表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:export')")
    @Log(title = "工作量明细主表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizWorkloadItem bizWorkloadItem)
    {
        // 教师角色只能导出自己的数据（统一收口至 DataScopeUtil）
        bizWorkloadItem.setUserId(DataScopeUtil.resolveUserId(bizWorkloadItem.getUserId()));
        List<BizWorkloadItem> list = bizWorkloadItemService.selectBizWorkloadItemList(bizWorkloadItem);
        ExcelUtil<BizWorkloadItem> util = new ExcelUtil<BizWorkloadItem>(BizWorkloadItem.class);
        util.exportExcel(response, list, "工作量明细主表数据");
    }

    /**
     * 获取工作量明细主表详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        BizWorkloadItem bizWorkloadItem = bizWorkloadItemService.selectBizWorkloadItemById(id);
        if (bizWorkloadItem != null)
        {
            // 教师只能查看本人记录，防 IDOR 遍历
            DataScopeUtil.assertOwnOrAdmin(bizWorkloadItem.getUserId());
        }
        return success(bizWorkloadItem);
    }

    /**
     * 新增工作量明细主表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:add')")
    @Log(title = "工作量明细主表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated(BizWorkloadItem.Add.class) @RequestBody BizWorkloadItem bizWorkloadItem)
    {
        // 教师只能为本人新增明细
        bizWorkloadItem.setUserId(DataScopeUtil.resolveUserId(bizWorkloadItem.getUserId()));
        return toAjax(bizWorkloadItemService.insertBizWorkloadItem(bizWorkloadItem));
    }

    /**
     * 修改工作量明细主表
     * <p>
     * 白名单更新（P1-13）：无论教师还是管理端，仅允许回写业务白名单字段，
     * 禁止直改 status、calculatedWorkload、审批/签署类（deanApproval*）及申诉处理类字段。
     * <p>
     * 不加 @Validated：本端点为部分字段更新，必填校验（Add 分组）仅适用于新增；
     * 白名单本身已约束可写字段。
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:edit')")
    @Log(title = "工作量明细主表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizWorkloadItem bizWorkloadItem)
    {
        if (bizWorkloadItem.getId() == null)
        {
            throw new ServiceException("缺少记录ID，无法修改");
        }
        BizWorkloadItem existing = bizWorkloadItemService.selectBizWorkloadItemById(bizWorkloadItem.getId());
        if (existing == null)
        {
            throw new ServiceException("待修改的记录不存在");
        }
        // 按数据库记录的归属人校验，防止伪造 userId 绕过
        DataScopeUtil.assertOwnOrAdmin(existing.getUserId());
        // 构造仅含白名单字段的更新对象（防 mass-assignment）：
        // 未列入白名单的字段保持 null，Mapper 动态 SQL 会跳过 null 字段不更新
        BizWorkloadItem update = new BizWorkloadItem();
        update.setId(bizWorkloadItem.getId());
        if (DataScopeUtil.isTeacherOnly())
        {
            // 教师仅可修改说明/申诉理由/备注等非敏感字段；
            // roleType（岗位类型归属）不在教师白名单：教师申报时通过 add 写入，
            // 后续归属变更需由管理端核准，防止教师自行篡改岗位类型
            update.setDescription(bizWorkloadItem.getDescription());
            // TODO 申诉（appeal）链路属下一迭代，当前仅允许填写申诉理由字段
            update.setAppealReason(bizWorkloadItem.getAppealReason());
            update.setRemark(bizWorkloadItem.getRemark());
        }
        else
        {
            // 管理端白名单：展示与申报类业务字段
            update.setSemester(bizWorkloadItem.getSemester());
            update.setAcademicYear(bizWorkloadItem.getAcademicYear());
            update.setItemType(bizWorkloadItem.getItemType());
            update.setSourceType(bizWorkloadItem.getSourceType());
            update.setTaskId(bizWorkloadItem.getTaskId());
            update.setAssignmentId(bizWorkloadItem.getAssignmentId());
            update.setRoleType(bizWorkloadItem.getRoleType());
            update.setCourseName(bizWorkloadItem.getCourseName());
            update.setEducationLevel(bizWorkloadItem.getEducationLevel());
            update.setMajorCategory(bizWorkloadItem.getMajorCategory());
            update.setDescription(bizWorkloadItem.getDescription());
            update.setIsOverLimit(bizWorkloadItem.getIsOverLimit());
            update.setRemark(bizWorkloadItem.getRemark());
        }
        return toAjax(bizWorkloadItemService.updateBizWorkloadItem(update));
    }

    /**
     * 删除工作量明细主表
     */
    @PreAuthorize("@ss.hasPermi('system:workloadItem:remove')")
    @Log(title = "工作量明细主表", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        if (DataScopeUtil.isTeacherOnly())
        {
            // 教师只能删除本人记录
            for (Long id : ids)
            {
                BizWorkloadItem item = bizWorkloadItemService.selectBizWorkloadItemById(id);
                if (item != null)
                {
                    DataScopeUtil.assertOwnOrAdmin(item.getUserId());
                }
            }
        }
        return toAjax(bizWorkloadItemService.deleteBizWorkloadItemByIds(ids));
    }
}
