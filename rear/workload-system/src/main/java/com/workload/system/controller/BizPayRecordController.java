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
import com.workload.common.utils.DataScopeUtil;
import com.workload.common.exception.ServiceException;
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
        // 教师角色只能查看自己的数据（统一收口至 DataScopeUtil）
        bizPayRecord.setUserId(DataScopeUtil.resolveUserId(bizPayRecord.getUserId()));
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
        // 教师角色只能导出自己的数据（统一收口至 DataScopeUtil）
        bizPayRecord.setUserId(DataScopeUtil.resolveUserId(bizPayRecord.getUserId()));
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
        BizPayRecord bizPayRecord = bizPayRecordService.selectBizPayRecordById(id);
        if (bizPayRecord != null)
        {
            // 教师只能查看本人记录，防 IDOR 遍历
            DataScopeUtil.assertOwnOrAdmin(bizPayRecord.getUserId());
        }
        return success(bizPayRecord);
    }

    /**
     * 新增酬金汇总
     */
    @PreAuthorize("@ss.hasPermi('system:payRecord:add')")
    @Log(title = "酬金汇总", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizPayRecord bizPayRecord)
    {
        // 教师只能为本人新增酬金记录
        bizPayRecord.setUserId(DataScopeUtil.resolveUserId(bizPayRecord.getUserId()));
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
        if (bizPayRecord.getId() == null)
        {
            throw new ServiceException("缺少记录ID，无法修改");
        }
        BizPayRecord existing = bizPayRecordService.selectBizPayRecordById(bizPayRecord.getId());
        if (existing == null)
        {
            throw new ServiceException("待修改的记录不存在");
        }
        // 按数据库记录的归属人校验，防止伪造 userId 绕过
        DataScopeUtil.assertOwnOrAdmin(existing.getUserId());
        if (DataScopeUtil.isTeacherOnly())
        {
            // 酬金属敏感数据，教师仅可修改备注；敏感字段置空，
            // 利用 Mapper 动态 SQL 跳过 null 的特性不被更新（防 mass-assignment）
            bizPayRecord.setUserId(null);
            bizPayRecord.setSemester(null);
            bizPayRecord.setSummaryId(null);
            bizPayRecord.setCourseHourPay(null);
            bizPayRecord.setOtherPayTotal(null);
            bizPayRecord.setTotalPay(null);
            bizPayRecord.setStatus(null);
            bizPayRecord.setCreateBy(null);
            bizPayRecord.setCreateTime(null);
        }
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
        if (DataScopeUtil.isTeacherOnly())
        {
            // 教师只能删除本人记录
            for (Long id : ids)
            {
                BizPayRecord record = bizPayRecordService.selectBizPayRecordById(id);
                if (record != null)
                {
                    DataScopeUtil.assertOwnOrAdmin(record.getUserId());
                }
            }
        }
        return toAjax(bizPayRecordService.deleteBizPayRecordByIds(ids));
    }
}
