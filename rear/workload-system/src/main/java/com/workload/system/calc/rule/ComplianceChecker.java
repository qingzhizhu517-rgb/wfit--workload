package com.workload.system.calc.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWlPractice;
import com.workload.system.domain.BizWlTheory;
import com.workload.system.mapper.BizRoleAssignmentMapper;
import com.workload.system.mapper.BizWlPracticeMapper;
import com.workload.system.mapper.BizWlTheoryMapper;

/**
 * 制度性合规校验（第六、八、九、十条）——产出告警，不改变任何计算结果。
 * <p>
 * 三项校验：
 * <ol>
 *   <li><b>三门理论课视同完成基本教学量</b>（第六条）：独立完成三门理论课视同达标。
 *       「独立完成」无法从数据核验，按 G1 明细去重课程名 ≥3 判定，
 *       由汇总层据此置 {@code basic_teaching_met=1}（欠额告警降级）。</li>
 *   <li><b>课程数 ≤3</b>（第六条）：「原则上不超过 3 门」，重复课（同名）算一门，
 *       不含实践类（G2/G3/G6），超限告警。</li>
 *   <li><b>周学时上限</b>（第八/九/十条）：原始学时 (J1+J2) ÷ 16 周（不带任何系数）。
 *       行政岗/校领导 6；其余教师 16；仅承担专科课程 18。
 *       超限 ≤2 学时按第十条提示「须审批后方可排课」，超过则明确违规。</li>
 * </ol>
 * 周学时 6/12/16/18 与办法「即 96/192/256/288 学时/学期」的 16 周换算一致。
 * 系统岗位枚举无「院长」类角色，第八条 12 学时/周档暂无对应数据源，不校验。
 *
 * @author wflg
 * @date 2026-09-10
 */
@Component
public class ComplianceChecker
{
    /** 每学期教学周数（第八/九条「即 Y 学时/学期」按 16 周换算） */
    private static final BigDecimal WEEKS_PER_SEMESTER = new BigDecimal("16");

    /** 实践类环节：不计入「课程数」与「三门理论课」（第六条） */
    private static final Set<String> PRACTICE_TYPES = Set.of("G2", "G3", "G6");

    /** 行政/校领导岗（第八条 6 学时/周）——对应系统岗位枚举 */
    private static final Set<String> ADMIN_ROLE_TYPES = Set.of("中层副职", "心理中心");

    @Autowired
    private BizWlTheoryMapper bizWlTheoryMapper;

    @Autowired
    private BizWlPracticeMapper bizWlPracticeMapper;

    @Autowired
    private BizRoleAssignmentMapper bizRoleAssignmentMapper;

    /**
     * 校验结果：warnings 为告警文案（空=无）；threeTheoryCourses 供汇总层降级欠额判定
     */
    public static class Result
    {
        private final List<String> warnings = new ArrayList<>();

        private boolean threeTheoryCourses;

        public List<String> getWarnings()
        {
            return warnings;
        }

        public boolean isThreeTheoryCourses()
        {
            return threeTheoryCourses;
        }

        public void setThreeTheoryCourses(boolean threeTheoryCourses)
        {
            this.threeTheoryCourses = threeTheoryCourses;
        }
    }

    /**
     * 对一名教师一学期的全部明细做制度性校验
     *
     * @param userId   教师ID
     * @param semester 学年学期
     * @param items    该教师该学期全部明细（已驳回的会被本方法忽略）
     * @return 校验结果（永不返回 null）
     */
    public Result check(Long userId, String semester, List<BizWorkloadItem> items)
    {
        Result result = new Result();
        List<BizWorkloadItem> valid = new ArrayList<>();
        for (BizWorkloadItem item : items)
        {
            if (item.getStatus() == null || item.getStatus() != 3)
            {
                valid.add(item);
            }
        }
        if (valid.isEmpty())
        {
            return result;
        }

        // 1. 三门理论课（第六条）：G1 去重课程名 ≥3
        Set<String> theoryCourses = distinctCourses(valid, true);
        result.threeTheoryCourses = theoryCourses.size() >= 3;

        // 2. 课程数 ≤3（第六条）：非实践类去重课程名
        Set<String> courses = distinctCourses(valid, false);
        if (courses.size() > 3)
        {
            result.warnings.add(String.format("承担课程数 %d 门，超第六条 3 门上限（原则上）", courses.size()));
        }

        // 3. 周学时上限（第八/九/十条）：(J1+J2)÷16，不带系数
        checkWeeklyHours(userId, semester, valid, result);
        return result;
    }

    /**
     * 去重课程名：theoryOnly=true 只数 G1，否则数非实践类全部
     */
    private Set<String> distinctCourses(List<BizWorkloadItem> items, boolean theoryOnly)
    {
        Set<String> courses = new HashSet<>();
        for (BizWorkloadItem item : items)
        {
            if (theoryOnly ? "G1".equals(item.getItemType()) : !PRACTICE_TYPES.contains(item.getItemType()))
            {
                if (StringUtils.hasText(item.getCourseName()))
                {
                    courses.add(item.getCourseName());
                }
            }
        }
        return courses;
    }

    /**
     * 周学时 = (ΣJ1 + ΣJ2) ÷ 16；上限按岗位/课程层次取 6/16/18
     */
    private void checkWeeklyHours(Long userId, String semester, List<BizWorkloadItem> items, Result result)
    {
        BigDecimal rawHours = BigDecimal.ZERO;
        boolean anyJuniorOnly = true;
        boolean anyCourse = false;
        for (BizWorkloadItem item : items)
        {
            BigDecimal hours = null;
            if ("G1".equals(item.getItemType()))
            {
                BizWlTheory detail = bizWlTheoryMapper.selectBizWlTheoryByItemId(item.getId());
                hours = detail == null ? null : detail.getJ1();
            }
            else if ("G2".equals(item.getItemType()))
            {
                BizWlPractice detail = bizWlPracticeMapper.selectBizWlPracticeByItemId(item.getId());
                hours = detail == null ? null : detail.getJ2();
            }
            if (hours != null)
            {
                rawHours = rawHours.add(hours);
                anyCourse = true;
                // 只承担专科课程 → 18；出现任一非专科 → 16。
                // 「本科(含专升本)」不含「专科」子串，须用全词匹配而非单字「专」
                if (item.getEducationLevel() == null || !item.getEducationLevel().contains("专科"))
                {
                    anyJuniorOnly = false;
                }
            }
        }
        if (!anyCourse)
        {
            return;
        }
        BigDecimal weekly = rawHours.divide(WEEKS_PER_SEMESTER, 2, RoundingMode.HALF_UP);

        BigDecimal cap;
        String article;
        if (hasAdminRole(userId, semester))
        {
            cap = new BigDecimal("6");
            article = "第八条";
        }
        else if (anyJuniorOnly)
        {
            cap = new BigDecimal("18");
            article = "第九条(仅专科)";
        }
        else
        {
            cap = new BigDecimal("16");
            article = "第九条";
        }
        if (weekly.compareTo(cap) > 0)
        {
            BigDecimal excess = weekly.subtract(cap).setScale(2, RoundingMode.HALF_UP);
            String excessNote = excess.compareTo(new BigDecimal("2")) <= 0
                    ? String.format("超出 %s 学时，第十条：经审批后方可排课", excess.stripTrailingZeros().toPlainString())
                    : String.format("超出 %s 学时，已超过第十条 2 学时审批范围", excess.stripTrailingZeros().toPlainString());
            result.warnings.add(String.format("周学时 %s 超%s %s 学时上限（原始学时÷16 周，不带系数），%s",
                    weekly.stripTrailingZeros().toPlainString(), article,
                    cap.stripTrailingZeros().toPlainString(), excessNote));
        }
    }

    /**
     * 本学期是否有行政/校领导类岗位任职（第八条 6 学时/周）
     */
    private boolean hasAdminRole(Long userId, String semester)
    {
        BizRoleAssignment query = new BizRoleAssignment();
        query.setUserId(userId);
        query.setSemester(semester);
        query.setStatus(1);
        List<BizRoleAssignment> assignments = bizRoleAssignmentMapper.selectBizRoleAssignmentList(query);
        for (BizRoleAssignment assignment : assignments)
        {
            if (ADMIN_ROLE_TYPES.contains(assignment.getRoleType()))
            {
                return true;
            }
        }
        return false;
    }
}
