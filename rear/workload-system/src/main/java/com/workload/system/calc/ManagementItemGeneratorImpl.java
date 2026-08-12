package com.workload.system.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.common.utils.DateUtils;
import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.domain.BizWlManagement;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizRoleAssignmentMapper;
import com.workload.system.mapper.BizWlManagementMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;

/**
 * G11 管理服务工作量生成器实现
 *
 * 折算：prorated = 标准学时/学年 ÷ 2 × (任职区间 ∩ 学期区间 天数 / 学期总天数)，
 * end_date 为 NULL 视为任职至学期末；多岗叠加与 180 封顶在汇总层处理
 *
 * @author wflg
 * @date 2026-07-21
 */
@Service
public class ManagementItemGeneratorImpl implements ManagementItemGenerator
{
    @Autowired
    private BizRoleAssignmentMapper bizRoleAssignmentMapper;

    @Autowired
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    @Autowired
    private BizWlManagementMapper bizWlManagementMapper;

    @Autowired
    private WorkloadCalcService workloadCalcService;

    @Autowired
    private SemesterCalendar semesterCalendar;

    @Override
    @Transactional
    public int generate(Long userId, String semester)
    {
        BizRoleAssignment query = new BizRoleAssignment();
        query.setUserId(userId);
        query.setSemester(semester);
        query.setStatus(1);
        List<BizRoleAssignment> assignments = bizRoleAssignmentMapper.selectBizRoleAssignmentList(query);
        int count = 0;
        for (BizRoleAssignment assignment : assignments)
        {
            if (generateOne(assignment, semester))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    @Transactional
    public int generateForSemester(String semester)
    {
        BizRoleAssignment query = new BizRoleAssignment();
        query.setSemester(semester);
        query.setStatus(1);
        List<BizRoleAssignment> assignments = bizRoleAssignmentMapper.selectBizRoleAssignmentList(query);
        Set<Long> userIds = new LinkedHashSet<>();
        for (BizRoleAssignment assignment : assignments)
        {
            userIds.add(assignment.getUserId());
        }
        int count = 0;
        for (Long userId : userIds)
        {
            count += generate(userId, semester);
        }
        return count;
    }

    /**
     * 单条任职 -> G11 明细；任职区间与学期无交集返回 false
     */
    private boolean generateOne(BizRoleAssignment assignment, String semester)
    {
        LocalDate[] range = semesterCalendar.rangeOf(semester);
        LocalDate semStart = range[0];
        LocalDate semEnd = range[1];
        LocalDate assignStart = toLocalDate(assignment.getStartDate());
        LocalDate assignEnd = assignment.getEndDate() == null ? semEnd : toLocalDate(assignment.getEndDate());
        if (assignStart == null)
        {
            assignStart = semStart;
        }
        LocalDate overlapStart = assignStart.isAfter(semStart) ? assignStart : semStart;
        LocalDate overlapEnd = assignEnd.isBefore(semEnd) ? assignEnd : semEnd;
        if (overlapStart.isAfter(overlapEnd))
        {
            return false;
        }
        long overlapDays = ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
        long semesterDays = ChronoUnit.DAYS.between(semStart, semEnd) + 1;

        // 标准学时/学年 ÷ 2 = 满学期量，再按任职天数占比折算
        BigDecimal rate = assignment.getAllowanceRate() == null ? BigDecimal.ZERO : assignment.getAllowanceRate();
        BigDecimal prorated = rate.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(overlapDays))
                .divide(new BigDecimal(semesterDays), 2, RoundingMode.HALF_UP);
        String basis = String.format("任职 %s 至 %s，学期 %s 至 %s，折算 %d/%d 天",
                assignStart, assignEnd, semStart, semEnd, overlapDays, semesterDays);

        BizWorkloadItem item = findG11Item(assignment.getUserId(), semester, assignment.getId());
        if (item == null)
        {
            item = new BizWorkloadItem();
            item.setUserId(assignment.getUserId());
            item.setSemester(semester);
            item.setAcademicYear(assignment.getAcademicYear());
            item.setItemType("G11");
            item.setSourceType("IMPORT");
            item.setAssignmentId(assignment.getId());
            // G11 明细同步写入岗位类型（与 biz_role_assignment.role_type 同枚举口径，P3-05）
            item.setRoleType(assignment.getRoleType());
            item.setCalculatedWorkload(BigDecimal.ZERO);
            item.setStatus(0);
            item.setCreateTime(DateUtils.getNowDate());
            bizWorkloadItemMapper.insertBizWorkloadItem(item);

            BizWlManagement detail = new BizWlManagement();
            detail.setItemId(item.getId());
            detail.setAssignmentId(assignment.getId());
            // role_type 优先取明细自带值（含教师自报申报），为空回退岗位任职解析
            detail.setRoleType(item.getRoleType() != null && !item.getRoleType().isEmpty()
                    ? item.getRoleType() : assignment.getRoleType());
            detail.setProratedAmount(prorated);
            detail.setProrationBasis(basis);
            detail.setCreateTime(DateUtils.getNowDate());
            bizWlManagementMapper.insertBizWlManagement(detail);
        }
        else
        {
            BizWlManagement detail = bizWlManagementMapper.selectBizWlManagementByItemId(item.getId());
            if (detail != null)
            {
                // role_type 优先取明细自带值（含教师自报申报），为空回退岗位任职解析
                detail.setRoleType(item.getRoleType() != null && !item.getRoleType().isEmpty()
                        ? item.getRoleType() : assignment.getRoleType());
                detail.setProratedAmount(prorated);
                detail.setProrationBasis(basis);
                detail.setUpdateTime(DateUtils.getNowDate());
                bizWlManagementMapper.updateBizWlManagement(detail);
            }
        }
        // 已核对明细保持冻结值；未核对的回写最新折算
        if (item.getStatus() == null || item.getStatus() != 1)
        {
            workloadCalcService.recalcItem(item.getId());
        }
        return true;
    }

    private BizWorkloadItem findG11Item(Long userId, String semester, Long assignmentId)
    {
        BizWorkloadItem query = new BizWorkloadItem();
        query.setUserId(userId);
        query.setSemester(semester);
        query.setItemType("G11");
        query.setAssignmentId(assignmentId);
        List<BizWorkloadItem> items = bizWorkloadItemMapper.selectBizWorkloadItemList(query);
        return items.isEmpty() ? null : items.get(0);
    }

    private LocalDate toLocalDate(Date date)
    {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
