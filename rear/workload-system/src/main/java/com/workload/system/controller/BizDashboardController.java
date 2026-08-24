package com.workload.system.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.core.domain.entity.SysDept;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.service.IBizTeacherProfileService;
import com.workload.system.service.IBizTeachingTaskService;
import com.workload.system.service.IBizWorkloadItemService;
import com.workload.system.service.IBizWorkloadSummaryService;
import com.workload.system.service.ISysDeptService;

/**
 * 仪表盘统计 Controller
 *
 * @author wflg
 * @date 2026-07-22
 */
@RestController
@RequestMapping("/system/dashboard")
public class BizDashboardController extends BaseController
{
    @Autowired
    private IBizTeachingTaskService bizTeachingTaskService;

    @Autowired
    private IBizWorkloadItemService bizWorkloadItemService;

    @Autowired
    private IBizWorkloadSummaryService bizWorkloadSummaryService;

    @Autowired
    private IBizTeacherProfileService bizTeacherProfileService;

    @Autowired
    private ISysDeptService sysDeptService;

    /**
     * 管理员仪表盘统计
     */
    @PreAuthorize("@ss.hasPermi('system:dashboard:adminStats')")
    @GetMapping("/adminStats")
    public AjaxResult adminStats(@RequestParam(required = false) String semester)
    {
        Map<String, Object> stats = new HashMap<>();
        // 本学期教学任务数
        BizTeachingTask taskQuery = new BizTeachingTask();
        if (semester != null && !semester.isEmpty())
        {
            taskQuery.setSemester(semester);
        }
        List<BizTeachingTask> tasks = bizTeachingTaskService.selectBizTeachingTaskList(taskQuery);
        stats.put("taskCount", tasks.size());

        // 本学期已核算工作量明细数
        BizWorkloadItem itemQuery = new BizWorkloadItem();
        if (semester != null && !semester.isEmpty())
        {
            itemQuery.setSemester(semester);
        }
        List<BizWorkloadItem> items = bizWorkloadItemService.selectBizWorkloadItemList(itemQuery);
        stats.put("itemCount", items.size());

        // 参与核算教师数（去重 userId）
        long teacherCount = items.stream().map(BizWorkloadItem::getUserId).distinct().count();
        stats.put("teacherCount", teacherCount);

        // 已核算总工作量
        BigDecimal totalWorkload = items.stream()
                .map(i -> i.getCalculatedWorkload() == null ? BigDecimal.ZERO : i.getCalculatedWorkload())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalWorkload", totalWorkload);

        // 待处理异议数（appeal_status=1）
        long appealCount = items.stream().filter(i -> i.getAppealStatus() != null && i.getAppealStatus() == 1).count();
        stats.put("appealCount", appealCount);

        // 学期汇总数
        BizWorkloadSummary summaryQuery = new BizWorkloadSummary();
        if (semester != null && !semester.isEmpty())
        {
            summaryQuery.setSemester(semester);
        }
        List<BizWorkloadSummary> summaries = bizWorkloadSummaryService.selectBizWorkloadSummaryList(summaryQuery);
        stats.put("summaryCount", summaries.size());

        // 超工作量总额
        BigDecimal totalExcess = summaries.stream()
                .map(s -> s.getExcessWorkload() == null ? BigDecimal.ZERO : s.getExcessWorkload())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalExcess", totalExcess);

        // 绩效酬金总额
        BigDecimal totalPay = summaries.stream()
                .map(s -> s.getPerformancePay() == null ? BigDecimal.ZERO : s.getPerformancePay())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalPay", totalPay);

        return success(stats);
    }

    /**
     * 教师仪表盘统计（仅返回当前登录用户本人数据）
     */
    @PreAuthorize("@ss.hasPermi('system:workloadSummary:query')")
    @GetMapping("/teacherStats")
    public AjaxResult teacherStats(@RequestParam(required = false) String semester)
    {
        // 强制只返回当前用户数据（userId 取自登录态，忽略任何外部入参）
        Long userId = getUserId();
        Map<String, Object> stats = new HashMap<>();

        // 本学期承担课程数
        BizTeachingTask taskQuery = new BizTeachingTask();
        taskQuery.setUserId(userId);
        if (semester != null && !semester.isEmpty())
        {
            taskQuery.setSemester(semester);
        }
        List<BizTeachingTask> tasks = bizTeachingTaskService.selectBizTeachingTaskList(taskQuery);
        stats.put("courseCount", tasks.size());

        // 本学期工作量明细数
        BizWorkloadItem itemQuery = new BizWorkloadItem();
        itemQuery.setUserId(userId);
        if (semester != null && !semester.isEmpty())
        {
            itemQuery.setSemester(semester);
        }
        List<BizWorkloadItem> items = bizWorkloadItemService.selectBizWorkloadItemList(itemQuery);
        stats.put("itemCount", items.size());

        // 已核算总工作量
        BigDecimal totalWorkload = items.stream()
                .map(i -> i.getCalculatedWorkload() == null ? BigDecimal.ZERO : i.getCalculatedWorkload())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalWorkload", totalWorkload);

        // 个人学期汇总
        BizWorkloadSummary summaryQuery = new BizWorkloadSummary();
        summaryQuery.setUserId(userId);
        if (semester != null && !semester.isEmpty())
        {
            summaryQuery.setSemester(semester);
        }
        List<BizWorkloadSummary> summaries = bizWorkloadSummaryService.selectBizWorkloadSummaryList(summaryQuery);
        if (summaries.isEmpty())
        {
            stats.put("excessWorkload", BigDecimal.ZERO);
            stats.put("performancePay", BigDecimal.ZERO);
            stats.put("ratedWorkload", BigDecimal.ZERO);
            stats.put("summaryStatus", 0);
            stats.put("isCapped", 0);
            stats.put("basicTeachingMet", 0);
        }
        else
        {
            BizWorkloadSummary s = summaries.get(0);
            stats.put("excessWorkload", s.getExcessWorkload() == null ? BigDecimal.ZERO : s.getExcessWorkload());
            stats.put("performancePay", s.getPerformancePay() == null ? BigDecimal.ZERO : s.getPerformancePay());
            stats.put("ratedWorkload", s.getRatedWorkload() == null ? BigDecimal.ZERO : s.getRatedWorkload());
            stats.put("summaryStatus", s.getStatus() == null ? 0 : s.getStatus());
            stats.put("isCapped", s.getIsCapped() == null ? 0 : s.getIsCapped());
            stats.put("basicTeachingMet", s.getBasicTeachingMet() == null ? 0 : s.getBasicTeachingMet());
        }

        // 申诉中明细数
        long appealCount = items.stream().filter(i -> i.getAppealStatus() != null && i.getAppealStatus() == 1).count();
        stats.put("appealCount", appealCount);

        return success(stats);
    }

    /**
     * 各学院教学任务概况（柱图/折线图用）
     */
    @PreAuthorize("@ss.hasPermi('system:dashboard:collegeStats')")
    @GetMapping("/collegeStats")
    public AjaxResult collegeStats(@RequestParam(required = false) String semester)
    {
        // 1. 所有教师 → userId → deptId 映射
        List<BizTeacherProfile> profiles = bizTeacherProfileService.selectBizTeacherProfileList(new BizTeacherProfile());
        Map<Long, Long> userDeptMap = profiles.stream()
                .filter(p -> p.getUserId() != null && p.getDeptId() != null)
                .collect(Collectors.toMap(BizTeacherProfile::getUserId, BizTeacherProfile::getDeptId, (a, b) -> a));

        // 2. 所有教学任务
        BizTeachingTask taskQuery = new BizTeachingTask();
        if (semester != null && !semester.isEmpty())
        {
            taskQuery.setSemester(semester);
        }
        List<BizTeachingTask> tasks = bizTeachingTaskService.selectBizTeachingTaskList(taskQuery);

        // 3. 按 deptId 分组统计
        Map<Long, Long> deptTaskCount = tasks.stream()
                .filter(t -> userDeptMap.containsKey(t.getUserId()))
                .collect(Collectors.groupingBy(t -> userDeptMap.get(t.getUserId()), Collectors.counting()));

        // 4. 所有工作量明细 → 按 deptId 分组
        BizWorkloadItem itemQuery = new BizWorkloadItem();
        if (semester != null && !semester.isEmpty())
        {
            itemQuery.setSemester(semester);
        }
        List<BizWorkloadItem> items = bizWorkloadItemService.selectBizWorkloadItemList(itemQuery);
        Map<Long, Long> deptItemCount = items.stream()
                .filter(i -> userDeptMap.containsKey(i.getUserId()))
                .collect(Collectors.groupingBy(i -> userDeptMap.get(i.getUserId()), Collectors.counting()));

        // 5. 所有出现的 deptId 取并集并查部门名
        java.util.Set<Long> allDeptIds = new java.util.HashSet<>();
        allDeptIds.addAll(deptTaskCount.keySet());
        allDeptIds.addAll(deptItemCount.keySet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long deptId : allDeptIds)
        {
            SysDept dept = sysDeptService.selectDeptById(deptId);
            String deptName = dept != null ? dept.getDeptName() : "未知学院(" + deptId + ")";
            Map<String, Object> row = new HashMap<>();
            row.put("deptId", deptId);
            row.put("deptName", deptName);
            row.put("taskCount", deptTaskCount.getOrDefault(deptId, 0L));
            row.put("itemCount", deptItemCount.getOrDefault(deptId, 0L));
            result.add(row);
        }

        return success(result);
    }
}
