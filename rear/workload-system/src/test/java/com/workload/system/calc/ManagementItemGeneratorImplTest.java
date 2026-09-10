package com.workload.system.calc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.domain.BizWlManagement;
import com.workload.system.mapper.BizRoleAssignmentMapper;
import com.workload.system.mapper.BizWlManagementMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;

/**
 * G11 管理服务生成器单元测试。
 * <p>
 * 依据《办法》第十六条/十七条（2026-09-10 统一口径）：
 * {@code allowance_rate} 存<b>学年值</b>，引擎 ÷2 折学期标准；
 * 「督导」例外——第十七条 15 学时/学期本身即学期值，不折半。
 * <p>
 * 学期区间取 2025-09-01 ~ 2026-01-15（共 137 天）。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("G11 管理服务生成器（办法第十六/十七条）")
class ManagementItemGeneratorImplTest
{
    private static final String SEMESTER = "2025-2026-1";

    private static final LocalDate SEM_START = LocalDate.of(2025, 9, 1);

    private static final LocalDate SEM_END = LocalDate.of(2026, 1, 15);

    @InjectMocks
    private ManagementItemGeneratorImpl generator;

    @Mock
    private BizRoleAssignmentMapper bizRoleAssignmentMapper;

    @Mock
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    @Mock
    private BizWlManagementMapper bizWlManagementMapper;

    @Mock
    private SemesterCalendar semesterCalendar;

    @Mock
    private WorkloadCalcService workloadCalcService;

    @Test
    @DisplayName("班主任 180 学时/学年，全学期任职 → 学期 90.00")
    void yearRateHalvedForFullSemester()
    {
        stubCalendar();
        stubAssignments(assignment("班主任", "180", null));
        stubNoExistingItem();

        generator.generate(1L, SEMESTER);

        assertThat(capturedDetail().getProratedAmount()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("督导 15 学时/学期（第十七条），全学期任职 → 15.00，不折半")
    void supervisorRateNotHalved()
    {
        stubCalendar();
        stubAssignments(assignment("督导", "15", null));
        stubNoExistingItem();

        generator.generate(1L, SEMESTER);

        assertThat(capturedDetail().getProratedAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("班主任 180/学年，2025-10-01 起任职 → 90 × 107/137 = 70.29")
    void yearRateProratedByOverlapDays()
    {
        stubCalendar();
        // 2025-10-01 ~ 2026-01-15 与学期重叠 107 天（学期共 137 天）
        stubAssignments(assignment("班主任", "180", LocalDate.of(2025, 10, 1)));
        stubNoExistingItem();

        generator.generate(1L, SEMESTER);

        assertThat(capturedDetail().getProratedAmount()).isEqualByComparingTo("70.29");
    }

    @Test
    @DisplayName("任职区间与学期无交集 → 不生成明细")
    void noOverlapNoGeneration()
    {
        stubCalendar();
        // 任职 2024-02-01 ~ 2024-07-01，与学期 2025-09-01 起无交集
        BizRoleAssignment past = assignment("班主任", "180", LocalDate.of(2024, 2, 1));
        past.setEndDate(java.sql.Date.valueOf(LocalDate.of(2024, 7, 1)));
        stubAssignments(past);

        int count = generator.generate(1L, SEMESTER);

        assertThat(count).isZero();
        verify(bizWlManagementMapper, org.mockito.Mockito.never()).insertBizWlManagement(any());
    }

    @Test
    @DisplayName("allowance_rate 为空按 0 处理 → 0.00")
    void nullRateTreatedAsZero()
    {
        stubCalendar();
        stubAssignments(assignment("系主任", null, null));
        stubNoExistingItem();

        generator.generate(1L, SEMESTER);

        assertThat(capturedDetail().getProratedAmount()).isEqualByComparingTo("0.00");
    }

    private void stubCalendar()
    {
        when(semesterCalendar.rangeOf(SEMESTER)).thenReturn(new LocalDate[] { SEM_START, SEM_END });
    }

    private void stubAssignments(BizRoleAssignment assignment)
    {
        when(bizRoleAssignmentMapper.selectBizRoleAssignmentList(any()))
                .thenReturn(Collections.singletonList(assignment));
    }

    private void stubNoExistingItem()
    {
        when(bizWorkloadItemMapper.selectBizWorkloadItemList(any())).thenReturn(Collections.emptyList());
    }

    private BizWlManagement capturedDetail()
    {
        ArgumentCaptor<BizWlManagement> captor = ArgumentCaptor.forClass(BizWlManagement.class);
        verify(bizWlManagementMapper).insertBizWlManagement(captor.capture());
        return captor.getValue();
    }

    /** 起始日为 null 时按学期首日起算；结束日为 null 视为任职至学期末 */
    private BizRoleAssignment assignment(String roleType, String rate, LocalDate start)
    {
        BizRoleAssignment assignment = new BizRoleAssignment();
        assignment.setId(100L);
        assignment.setUserId(1L);
        assignment.setRoleType(roleType);
        assignment.setAllowanceRate(rate == null ? null : new BigDecimal(rate));
        assignment.setSemester(SEMESTER);
        assignment.setStatus(1);
        assignment.setAcademicYear("2025-2026");
        assignment.setStartDate(start == null ? null : java.sql.Date.valueOf(start));
        return assignment;
    }
}
