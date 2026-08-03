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
import com.workload.system.domain.BizPayRecord;
import com.workload.system.service.IBizPayRecordService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.utils.SecurityUtils;
import com.workload.common.core.page.TableDataInfo;

/**
 * 酬金汇总Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/payRecord")
public class BizPayRecordController extends BaseController
{
    @Autowired
    private IBizPayRecordService bizPayRecordService;

    /**
     * 查询酬金汇总列表
     */
    @PreAuthorize("@ss.hasPermi('system:payRecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizPayRecord bizPayRecord)
    {
        // 教师角色只能查看自己的数据（不用 hasRole，避免 admin 绕过）
        if (!SecurityUtils.isAdmin() && SecurityUtils.getLoginUser().getUser().getRoles().stream()
                .anyMatch(r -> "teacher".equals(r.getRoleKey())))
        {
            bizPayRecord.setUserId(SecurityUtils.getUserId());
        }
        startPage();
        List<BizPayRecord> list = bizPayRecordService.selectBizPayRecordList(bizPayRecord);
        return getDataTable(list);
    }

    /**
     * 导出酬金汇总列表
     */
    @PreAuthorize("@ss.hasPermi('system:payRecord:export')")
    @Log(title = "酬金汇总", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizPayRecord bizPayRecord)
    {
        // 教师角色只能导出自己的数据
        if (!SecurityUtils.isAdmin() && SecurityUtils.getLoginUser().getUser().getRoles().stream()
                .anyMatch(r -> "teacher".equals(r.getRoleKey())))
        {
            bizPayRecord.setUserId(SecurityUtils.getUserId());
        }
        List<BizPayRecord> list = bizPayRecordService.selectBizPayRecordList(bizPayRecord);
        ExcelUtil<BizPayRecord> util = new ExcelUtil<BizPayRecord>(BizPayRecord.class);
        util.exportExcel(response, list, "酬金汇总数据");
    }

    /**
     * 获取酬金汇总详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:payRecord:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizPayRecordService.selectBizPayRecordById(id));
    }

    /**
     * 新增酬金汇总
     */
    @PreAuthorize("@ss.hasPermi('system:payRecord:add')")
    @Log(title = "酬金汇总", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizPayRecord bizPayRecord)
    {
        return toAjax(bizPayRecordService.insertBizPayRecord(bizPayRecord));
    }

    /**
     * 修改酬金汇总
     */
    @PreAuthorize("@ss.hasPermi('system:payRecord:edit')")
    @Log(title = "酬金汇总", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizPayRecord bizPayRecord)
    {
        return toAjax(bizPayRecordService.updateBizPayRecord(bizPayRecord));
    }

    /**
     * 删除酬金汇总
     */
    @PreAuthorize("@ss.hasPermi('system:payRecord:remove')")
    @Log(title = "酬金汇总", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizPayRecordService.deleteBizPayRecordByIds(ids));
    }
}
