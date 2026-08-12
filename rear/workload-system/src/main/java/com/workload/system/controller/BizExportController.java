package com.workload.system.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DataScopeUtil;
import com.workload.common.utils.SecurityUtils;
import com.workload.common.utils.StringUtils;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.dto.PersonalWorkloadExportDTO;
import com.workload.system.service.IBizWorkloadItemService;
import com.workload.system.service.IBizWorkloadSummaryService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 工作量报表导出 Controller
 *
 * @author wflg
 */
@RestController
@RequestMapping("/system/export")
public class BizExportController extends BaseController
{
    @Autowired
    private IBizWorkloadSummaryService summaryService;

    @Autowired
    private IBizWorkloadItemService workloadItemService;

    /**
     * 导出个人工作量明细表（附件1格式）
     */
    @PreAuthorize("@ss.hasPermi('system:export:personal')")
    @GetMapping("/personalWorkload")
    public void exportPersonalWorkload(
            @RequestParam("userId") Long userId,
            @RequestParam("semester") String semester,
            HttpServletResponse response) throws Exception
    {
        if (userId == null)
        {
            throw new ServiceException("userId 不能为空");
        }
        if (StringUtils.isEmpty(semester))
        {
            throw new ServiceException("semester 不能为空");
        }
        // 教师角色强制导出本人数据，忽略传入的他人 userId（修复越权导出 P2-01）
        userId = DataScopeUtil.resolveUserId(userId);

        // 查询汇总
        BizWorkloadSummary summary = summaryService.selectBizWorkloadSummaryByUserAndSemester(userId, semester);
        if (summary == null)
        {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("未找到该教师该学期的汇总数据");
            return;
        }

        // 查询明细
        BizWorkloadItem query = new BizWorkloadItem();
        query.setUserId(userId);
        query.setSemester(semester);
        List<BizWorkloadItem> items = workloadItemService.selectBizWorkloadItemList(query);

        // 构建导出数据
        List<PersonalWorkloadExportDTO> exportData = new ArrayList<>();
        for (BizWorkloadItem item : items)
        {
            PersonalWorkloadExportDTO dto = new PersonalWorkloadExportDTO();
            dto.setItemType(item.getItemType());
            dto.setCourseName(item.getCourseName() != null ? item.getCourseName() : "-");
            dto.setCalculatedWorkload(item.getCalculatedWorkload());
            dto.setSourceType(item.getSourceType());
            dto.setStatus(item.getStatus());
            exportData.add(dto);
        }

        // 写出 Excel
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = "工作量明细_" + userId + "_" + semester + ".xlsx";
        response.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));

        EasyExcel.write(response.getOutputStream(), PersonalWorkloadExportDTO.class)
                .sheet("工作量明细")
                .doWrite(exportData);
    }

    /**
     * 导出绩效酬金统计表（附件2格式）
     */
    @PreAuthorize("@ss.hasPermi('system:export:paySummary')")
    @GetMapping("/paySummary")
    public void exportPaySummary(
            @RequestParam("semester") String semester,
            HttpServletResponse response) throws Exception
    {
        if (StringUtils.isEmpty(semester))
        {
            throw new ServiceException("semester 不能为空");
        }
        // 查询该学期所有汇总；教师角色强制只导出本人数据
        BizWorkloadSummary query = new BizWorkloadSummary();
        query.setSemester(semester);
        query.setUserId(DataScopeUtil.resolveUserId(null));
        List<BizWorkloadSummary> summaries = summaryService.selectBizWorkloadSummaryList(query);

        if (summaries.isEmpty())
        {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("未找到该学期的汇总数据");
            return;
        }

        // 构建导出数据
        List<PersonalWorkloadExportDTO> exportData = new ArrayList<>();
        for (BizWorkloadSummary s : summaries)
        {
            PersonalWorkloadExportDTO dto = new PersonalWorkloadExportDTO();
            dto.setUserId(s.getUserId());
            dto.setSemester(s.getSemester());
            dto.setTitle(s.getTitle());
            dto.setTotalWorkload(s.getTotalWorkload());
            dto.setRatedWorkload(s.getRatedWorkload());
            dto.setExcessWorkload(s.getExcessWorkload());
            dto.setPayRate(s.getPayRate());
            dto.setPerformancePay(s.getPerformancePay());
            exportData.add(dto);
        }

        // 写出 Excel
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = "绩效酬金统计_" + semester + ".xlsx";
        response.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));

        EasyExcel.write(response.getOutputStream(), PersonalWorkloadExportDTO.class)
                .sheet("绩效酬金统计")
                .doWrite(exportData);
    }
}
