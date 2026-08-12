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
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.service.IBizWorkloadItemService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.utils.DataScopeUtil;
import com.workload.common.exception.ServiceException;
import com.workload.common.core.page.TableDataInfo;

/**
 * 工作量明细主表Controller
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
    public AjaxResult add(@RequestBody BizWorkloadItem bizWorkloadItem)
    {
        // 教师只能为本人新增明细
        bizWorkloadItem.setUserId(DataScopeUtil.resolveUserId(bizWorkloadItem.getUserId()));
        return toAjax(bizWorkloadItemService.insertBizWorkloadItem(bizWorkloadItem));
    }

    /**
     * 修改工作量明细主表
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
        if (DataScopeUtil.isTeacherOnly())
        {
            // 教师仅可修改说明/申诉理由/备注等非敏感字段；
            // 敏感字段置空，利用 Mapper 动态 SQL 跳过 null 的特性不被更新（防 mass-assignment）
            bizWorkloadItem.setUserId(null);
            bizWorkloadItem.setSemester(null);
            bizWorkloadItem.setAcademicYear(null);
            bizWorkloadItem.setItemType(null);
            bizWorkloadItem.setSourceType(null);
            bizWorkloadItem.setTaskId(null);
            bizWorkloadItem.setAssignmentId(null);
            bizWorkloadItem.setCourseName(null);
            bizWorkloadItem.setEducationLevel(null);
            bizWorkloadItem.setMajorCategory(null);
            bizWorkloadItem.setCalculatedWorkload(null);
            bizWorkloadItem.setIsOverLimit(null);
            bizWorkloadItem.setDeanApprovalStatus(null);
            bizWorkloadItem.setDeanApprovalBy(null);
            bizWorkloadItem.setDeanApprovalTime(null);
            bizWorkloadItem.setAppealStatus(null);
            bizWorkloadItem.setAppealReply(null);
            bizWorkloadItem.setStatus(null);
            bizWorkloadItem.setCreateBy(null);
            bizWorkloadItem.setCreateTime(null);
        }
        return toAjax(bizWorkloadItemService.updateBizWorkloadItem(bizWorkloadItem));
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
