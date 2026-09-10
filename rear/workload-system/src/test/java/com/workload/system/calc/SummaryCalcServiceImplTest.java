package com.workload.system.calc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.system.calc.rule.ComplianceChecker;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.mapper.BizPayRateMapper;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

/**
 * 汇总服务制度性校验集成单元测试（第六条三门理论课视同达标 + remark 告警）。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("汇总服务·三门理论课与告警集成（办法第六条）")
class SummaryCalcServiceImplTest
{
    private static final Long USER = 1L;

    private static final String SEMESTER = "2025-2026-1";

    @InjectMocks
    private SummaryCalcServiceImpl service;

    @Mock
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    @Mock
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    @Mock
    private BizTeacherProfileMapper bizTeacherProfileMapper;

    @Mock
    private BizPayRateMapper bizPayRateMapper;

    @Mock
    private RuleParamService ruleParamService;

    @Mock
    private ComplianceChecker complianceChecker;

    @BeforeEach
    void setUp()
    {
        // 规则参数一律回落默认值；职称快照为空 → 绩效 0、达标线 BASIC_TEACH_ASSIST=192/2=96
        lenient().when(ruleParamService.get(anyString(), any(BigDecimal.class)))
                .thenAnswer(inv -> inv.getArgument(1));
        lenient().when(bizTeacherProfileMapper.selectBizTeacherProfileByUserId(USER))
                .thenReturn(new BizTeacherProfile());
        lenient().when(bizPayRateMapper.selectBizPayRateList(any())).thenReturn(Collections.emptyList());
        lenient().when(bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(any()))
                .thenReturn(Collections.emptyList());
        lenient().when(bizWorkloadItemMapper.selectBizWorkloadItemList(any()))
                .thenReturn(Arrays.asList(
                        item("G1", "10.00"), item("G1", "10.00"), item("G1", "10.00")));
    }

    @Test
    @DisplayName("三门理论课 → basicTeachingMet=1（G10=30 不足 96 也视同达标，第六条）")
    void threeTheoryCoursesDeemMet()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(true));

        BizWorkloadSummary summary = service.recalcSummary(USER, SEMESTER, false);

        assertThat(summary.getBasicTeachingMet()).isEqualTo(1);
        // 视同达标不改标准线本身，仍按职称标准展示
        assertThat(summary.getBasicTeachingStandard()).isEqualByComparingTo("96.00");
    }

    @Test
    @DisplayName("不足三门理论课且 G10=30 < 96 → basicTeachingMet=0（此前会误判欠额的项）")
    void notMetWithoutThreeTheory()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));

        BizWorkloadSummary summary = service.recalcSummary(USER, SEMESTER, false);

        assertThat(summary.getBasicTeachingMet()).isEqualTo(0);
    }

    @Test
    @DisplayName("制度性告警写入 remark；无告警时 remark 置空（保持与当前数据一致）")
    void warningsWrittenToRemark()
    {
        ComplianceChecker.Result warned = result(false);
        warned.getWarnings().add("承担课程数 4 门，超第六条 3 门上限（原则上）");
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(warned);

        BizWorkloadSummary summary = service.recalcSummary(USER, SEMESTER, false);

        assertThat(summary.getRemark()).isEqualTo("承担课程数 4 门，超第六条 3 门上限（原则上）");

        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        assertThat(service.recalcSummary(USER, SEMESTER, false).getRemark()).isEmpty();
    }

    @Test
    @DisplayName("G11 累计 200 超 180/学期 → 封顶为 180 且 remark 含封顶告警（第十六条注）")
    void g11CapWarningWritten()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        when(bizWorkloadItemMapper.selectBizWorkloadItemList(any())).thenReturn(Collections.singletonList(
                item("G11", "200.00")));

        BizWorkloadSummary summary = service.recalcSummary(USER, SEMESTER, false);

        assertThat(summary.getG11()).isEqualByComparingTo("180.00");
        assertThat(summary.getRemark()).contains("G11 管理服务累计 200").contains("180/学期封顶");
    }

    @Test
    @DisplayName("总工作量 580 超 CAP_200PCT=540 → isCapped=1 且 remark 含绩效封顶告警（第二十条）")
    void cap200WarningWritten()
    {
        when(complianceChecker.check(any(), anyString(), any())).thenReturn(result(false));
        when(bizWorkloadItemMapper.selectBizWorkloadItemList(any())).thenReturn(Collections.singletonList(
                item("G1", "580.00")));

        BizWorkloadSummary summary = service.recalcSummary(USER, SEMESTER, false);

        assertThat(summary.getIsCapped()).isEqualTo(1);
        assertThat(summary.getRemark()).contains("CAP_200PCT=540").contains("绩效酬金已按封顶值核算");
    }

    private ComplianceChecker.Result result(boolean threeTheory)
    {
        ComplianceChecker.Result result = new ComplianceChecker.Result();
        result.setThreeTheoryCourses(threeTheory);
        return result;
    }

    private BizWorkloadItem item(String type, String workload)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setUserId(USER);
        item.setSemester(SEMESTER);
        item.setItemType(type);
        item.setCalculatedWorkload(new BigDecimal(workload));
        item.setStatus(0);
        return item;
    }
}
