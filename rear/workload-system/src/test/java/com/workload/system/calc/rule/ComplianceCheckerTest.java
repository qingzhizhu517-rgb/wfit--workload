package com.workload.system.calc.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWlPractice;
import com.workload.system.domain.BizWlTheory;
import com.workload.system.mapper.BizRoleAssignmentMapper;
import com.workload.system.mapper.BizWlPracticeMapper;
import com.workload.system.mapper.BizWlTheoryMapper;

/**
 * 制度性合规校验单元测试（第六/八/九/十条）。
 * <p>
 * 三门理论课 / 课程数≤3 / 周学时上限三项，重点覆盖边界与档次判定。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("制度性合规校验（办法第六/八/九/十条）")
class ComplianceCheckerTest
{
    private static final Long USER = 1L;

    private static final String SEMESTER = "2025-2026-1";

    @InjectMocks
    private ComplianceChecker checker;

    @Mock
    private BizWlTheoryMapper bizWlTheoryMapper;

    @Mock
    private BizWlPracticeMapper bizWlPracticeMapper;

    @Mock
    private BizRoleAssignmentMapper bizRoleAssignmentMapper;

    @BeforeEach
    void setUp()
    {
        lenient().when(bizRoleAssignmentMapper.selectBizRoleAssignmentList(any()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("三门理论课：3 门不同课程名 → threeTheoryCourses=true")
    void threeDistinctTheoryCoursesMet()
    {
        ComplianceChecker.Result result = checker.check(USER, SEMESTER, Arrays.asList(
                g1(1L, "高等数学A", "本科", "64"),
                g1(2L, "高等数学A（重修班）", "本科", "32"),
                g1(3L, "线性代数", "本科", "48"),
                g1(4L, "概率论", "本科", "48")));

        assertThat(result.isThreeTheoryCourses()).isTrue();
    }

    @Test
    @DisplayName("三门理论课：重复课算一门，2 门 → false；实践类不计入")
    void repeatedCourseCountsAsOne()
    {
        ComplianceChecker.Result result = checker.check(USER, SEMESTER, Arrays.asList(
                g1(1L, "高等数学A", "本科", "64"),
                g1(2L, "高等数学A", "本科", "64"),
                g1(3L, "线性代数", "本科", "48"),
                g2(4L, "线性代数实验", "本科", "16")));

        assertThat(result.isThreeTheoryCourses()).isFalse();
    }

    @Test
    @DisplayName("课程数：4 门非实践类 → 超第六条告警")
    void moreThanThreeCoursesWarns()
    {
        ComplianceChecker.Result result = checker.check(USER, SEMESTER, Arrays.asList(
                g1(1L, "课程一", "本科", "32"),
                g1(2L, "课程二", "本科", "32"),
                g1(3L, "课程三", "本科", "32"),
                g1(4L, "课程四", "本科", "32")));

        assertThat(result.getWarnings())
                .anyMatch(w -> w.contains("课程数 4 门") && w.contains("第六条"));
    }

    @Test
    @DisplayName("课程数：实践类不计入，3 门理论 + 实践类 → 不告警")
    void practiceTypesExcludedFromCourseCount()
    {
        ComplianceChecker.Result result = checker.check(USER, SEMESTER, Arrays.asList(
                g1(1L, "课程一", "本科", "32"),
                g1(2L, "课程二", "本科", "32"),
                g1(3L, "课程三", "本科", "32"),
                g2(4L, "课程一实验", "本科", "16")));

        assertThat(result.getWarnings()).noneMatch(w -> w.contains("课程数"));
    }

    @Test
    @DisplayName("周学时：288÷16=18 达专科上限 → 不告警")
    void juniorCapExactlyMetNoWarning()
    {
        ComplianceChecker.Result result = checker.check(USER, SEMESTER, Collections.singletonList(
                g1(1L, "专科课程", "专科", "288")));

        assertThat(result.getWarnings()).noneMatch(w -> w.contains("周学时"));
    }

    @Test
    @DisplayName("周学时：304÷16=19 超专科 18 上限，超 1 学时 → 第十条审批提示")
    void juniorCapExceededWithinApprovalRange()
    {
        ComplianceChecker.Result result = checker.check(USER, SEMESTER, Collections.singletonList(
                g1(1L, "专科课程", "专科", "304")));

        assertThat(result.getWarnings())
                .anyMatch(w -> w.contains("周学时 19") && w.contains("第九条(仅专科)") && w.contains("第十条：经审批后方可排课"));
    }

    @Test
    @DisplayName("周学时：本科 288÷16=18 超 16 上限，超 2 学时（含2）→ 审批提示")
    void bachelorCapExceeded()
    {
        ComplianceChecker.Result result = checker.check(USER, SEMESTER, Arrays.asList(
                g1(1L, "本科课程", "本科", "192"),
                g2(2L, "本科实验", "本科", "96")));

        assertThat(result.getWarnings())
                .anyMatch(w -> w.contains("周学时 18") && w.contains("第九条") && w.contains("第十条：经审批后方可排课"));
    }

    @Test
    @DisplayName("周学时：336÷16=21 超 16 上限 5 学时 → 超第十条审批范围")
    void exceedBeyondApprovalRange()
    {
        ComplianceChecker.Result result = checker.check(USER, SEMESTER, Collections.singletonList(
                g1(1L, "本科课程", "本科", "336")));

        assertThat(result.getWarnings())
                .anyMatch(w -> w.contains("已超过第十条 2 学时审批范围"));
    }

    @Test
    @DisplayName("周学时：行政岗（中层副职）112÷16=7 超第八条 6 上限 → 告警")
    void adminRoleCapSix()
    {
        BizRoleAssignment assignment = new BizRoleAssignment();
        assignment.setUserId(USER);
        assignment.setSemester(SEMESTER);
        assignment.setRoleType("中层副职");
        assignment.setStatus(1);
        when(bizRoleAssignmentMapper.selectBizRoleAssignmentList(any()))
                .thenReturn(Collections.singletonList(assignment));

        ComplianceChecker.Result result = checker.check(USER, SEMESTER, Collections.singletonList(
                g1(1L, "课程", "本科", "112")));

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("第八条") && w.contains("6"));
    }

    @Test
    @DisplayName("已驳回明细（status=3）不参与任何校验")
    void rejectedItemsIgnored()
    {
        List<BizWorkloadItem> items = new ArrayList<>();
        items.add(g1(1L, "课程一", "本科", "32"));
        BizWorkloadItem rejected = g1(2L, "课程二", "本科", "32");
        rejected.setStatus(3);
        items.add(rejected);

        ComplianceChecker.Result result = checker.check(USER, SEMESTER, items);

        assertThat(result.isThreeTheoryCourses()).isFalse();
        assertThat(result.getWarnings()).noneMatch(w -> w.contains("周学时"));
    }

    @Test
    @DisplayName("「本科(含专升本)」按本科判 16 上限，不误入专科 18 档")
    void zsbLevelTreatedAsBachelor()
    {
        // 304 学时：按专科档(18)不告警，按本科档(16)应告警
        ComplianceChecker.Result result = checker.check(USER, SEMESTER, Collections.singletonList(
                g1(1L, "专升本课程", "本科(含专升本)", "304")));

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("周学时 19") && w.contains("16"));
    }

    private BizWorkloadItem g1(long id, String course, String level, String j1)
    {
        BizWorkloadItem item = base(id, course, level);
        item.setItemType("G1");
        BizWlTheory detail = new BizWlTheory();
        detail.setJ1(new BigDecimal(j1));
        lenient().when(bizWlTheoryMapper.selectBizWlTheoryByItemId(id)).thenReturn(detail);
        return item;
    }

    private BizWorkloadItem g2(long id, String course, String level, String j2)
    {
        BizWorkloadItem item = base(id, course, level);
        item.setItemType("G2");
        BizWlPractice detail = new BizWlPractice();
        detail.setJ2(new BigDecimal(j2));
        lenient().when(bizWlPracticeMapper.selectBizWlPracticeByItemId(id)).thenReturn(detail);
        return item;
    }

    private BizWorkloadItem base(long id, String course, String level)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(id);
        item.setUserId(USER);
        item.setSemester(SEMESTER);
        item.setCourseName(course);
        item.setEducationLevel(level);
        item.setStatus(0);
        item.setCalculatedWorkload(BigDecimal.TEN);
        return item;
    }
}
