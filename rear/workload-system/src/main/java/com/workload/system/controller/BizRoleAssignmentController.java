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
import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.service.IBizRoleAssignmentService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.StringUtils;
import com.workload.common.core.page.TableDataInfo;

/**
 * 岗位任职Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/roleAssignment")
public class BizRoleAssignmentController extends BaseController
{
    @Autowired
    private IBizRoleAssignmentService bizRoleAssignmentService;

    /**
     * 查询岗位任职列表
     */
    @PreAuthorize("@ss.hasPermi('system:roleAssignment:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizRoleAssignment bizRoleAssignment)
    {
        startPage();
        List<BizRoleAssignment> list = bizRoleAssignmentService.selectBizRoleAssignmentList(bizRoleAssignment);
        return getDataTable(list);
    }

    /**
     * 导出岗位任职列表
     */
    @PreAuthorize("@ss.hasPermi('system:roleAssignment:export')")
    @Log(title = "岗位任职", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizRoleAssignment bizRoleAssignment)
    {
        List<BizRoleAssignment> list = bizRoleAssignmentService.selectBizRoleAssignmentList(bizRoleAssignment);
        ExcelUtil<BizRoleAssignment> util = new ExcelUtil<BizRoleAssignment>(BizRoleAssignment.class);
        util.exportExcel(response, list, "岗位任职数据");
    }

    /**
     * 获取岗位任职详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:roleAssignment:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizRoleAssignmentService.selectBizRoleAssignmentById(id));
    }

    /**
     * 新增岗位任职
     */
    @PreAuthorize("@ss.hasPermi('system:roleAssignment:add')")
    @Log(title = "岗位任职", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizRoleAssignment bizRoleAssignment)
    {
        validateBizRoleAssignment(bizRoleAssignment, true);
        return toAjax(bizRoleAssignmentService.insertBizRoleAssignment(bizRoleAssignment));
    }

    /**
     * 修改岗位任职
     */
    @PreAuthorize("@ss.hasPermi('system:roleAssignment:edit')")
    @Log(title = "岗位任职", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizRoleAssignment bizRoleAssignment)
    {
        if (bizRoleAssignment.getId() == null)
        {
            throw new ServiceException("缺少记录ID，无法修改");
        }
        validateBizRoleAssignment(bizRoleAssignment, false);
        return toAjax(bizRoleAssignmentService.updateBizRoleAssignment(bizRoleAssignment));
    }

    /**
     * 删除岗位任职
     */
    @PreAuthorize("@ss.hasPermi('system:roleAssignment:remove')")
    @Log(title = "岗位任职", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择要删除的记录");
        }
        return toAjax(bizRoleAssignmentService.deleteBizRoleAssignmentByIds(ids));
    }

    /**
     * 关键入参非空与合法性校验，防止 NPE 与脏写
     *
     * @param bizRoleAssignment 入参实体
     * @param isInsert 是否新增场景（新增时关键业务字段必填）
     */
    private void validateBizRoleAssignment(BizRoleAssignment bizRoleAssignment, boolean isInsert)
    {
        if (isInsert && bizRoleAssignment.getUserId() == null)
        {
            throw new ServiceException("userId 不能为空");
        }
        if (bizRoleAssignment.getUserId() != null && bizRoleAssignment.getUserId() <= 0)
        {
            throw new ServiceException("userId 非法");
        }
        if (isInsert && StringUtils.isEmpty(bizRoleAssignment.getRoleType()))
        {
            throw new ServiceException("岗位类型 roleType 不能为空");
        }
        if (isInsert && StringUtils.isEmpty(bizRoleAssignment.getSemester()))
        {
            throw new ServiceException("学期 semester 不能为空");
        }
        if (bizRoleAssignment.getAllowanceRate() != null && bizRoleAssignment.getAllowanceRate().signum() < 0)
        {
            throw new ServiceException("岗位标准学时 allowanceRate 不能为负数");
        }
        if (bizRoleAssignment.getStartDate() != null && bizRoleAssignment.getEndDate() != null
                && bizRoleAssignment.getEndDate().before(bizRoleAssignment.getStartDate()))
        {
            throw new ServiceException("任职止不能早于任职起");
        }
    }
}
