package com.workload.system.calc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import com.workload.common.core.redis.RedisCache;
import com.workload.system.calc.rule.*;
import com.workload.system.domain.*;
import com.workload.system.mapper.*;

class HistoricalSummaryDateTest {
    private SummaryCalcServiceImpl summary;
    private BizPayRateMapper rateMapper;
    private BizTeacherProfile profile;
    @BeforeEach void setup() {
        summary=new SummaryCalcServiceImpl();
        var items=mock(BizWorkloadItemMapper.class);var summaries=mock(BizWorkloadSummaryMapper.class);
        var teachers=mock(BizTeacherProfileMapper.class);rateMapper=mock(BizPayRateMapper.class);
        var compliance=mock(ComplianceChecker.class);var rulesMapper=mock(BizWorkloadRuleMapper.class);
        var rules=new RuleParamServiceImpl();ReflectionTestUtils.setField(rules,"bizWorkloadRuleMapper",rulesMapper);ReflectionTestUtils.setField(rules,"redisCache",mock(RedisCache.class));
        when(rulesMapper.selectBizWorkloadRuleList(any())).thenAnswer(i -> {
            BizWorkloadRule q=i.getArgument(0);
            return switch(q.getRuleCode()) {
                case "RATED_WORKLOAD" -> List.of(rule("100","2025-09-01","2026-01-31"),rule("150","2026-02-01",null));
                case "BASIC_TEACH_PROF" -> List.of(rule("400","2025-09-01","2026-01-31"),rule("600","2026-02-01",null));
                case "FACTOR_MATERNITY" -> List.of(rule("0.4","2025-09-01","2026-01-31"),rule("0.6","2026-02-01",null));
                case "BASIC_TEACH_PHD" -> List.of(rule("128","2025-09-01","2026-01-31"),rule("160","2026-02-01",null));
                default -> List.of();
            };
        });
        profile=new BizTeacherProfile();profile.setTitle("教授");profile.setTeacherNature("专任");profile.setSpecialStatus("正常");
        when(teachers.selectBizTeacherProfileByUserId(1L)).thenReturn(profile);
        when(summaries.selectBizWorkloadSummaryList(any())).thenReturn(List.of());
        var item=new BizWorkloadItem();item.setItemType("G1");item.setCalculatedWorkload(new BigDecimal("240"));
        when(items.selectBizWorkloadItemList(any())).thenReturn(List.of(item));
        when(compliance.check(any(),anyString(),any())).thenReturn(new ComplianceChecker.Result());
        when(rateMapper.selectBizPayRateList(any())).thenReturn(List.of(rate("11","2025-09-01","2026-01-31"),rate("22","2026-02-01","2026-07-15"),rate("33","2026-07-16",null)));
        ReflectionTestUtils.setField(summary,"bizWorkloadItemMapper",items);ReflectionTestUtils.setField(summary,"bizWorkloadSummaryMapper",summaries);
        ReflectionTestUtils.setField(summary,"bizTeacherProfileMapper",teachers);ReflectionTestUtils.setField(summary,"bizPayRateMapper",rateMapper);
        ReflectionTestUtils.setField(summary,"ruleParamService",rules);ReflectionTestUtils.setField(summary,"complianceChecker",compliance);
    }
    @Test void historicalSemestersUseDifferentRulesAndRatesEvenWhenRecalculatedOnSameDay() {
        var autumn=summary.recalcSummary(1L,"2025-2026-1",false);var spring=summary.recalcSummary(1L,"2025-2026-2",false);
        assertThat(autumn.getRatedWorkload()).isEqualByComparingTo("100");assertThat(autumn.getPayRate()).isEqualByComparingTo("11");
        assertThat(autumn.getPerformancePay()).isEqualByComparingTo("1540");assertThat(autumn.getBasicTeachingStandard()).isEqualByComparingTo("200");
        assertThat(spring.getRatedWorkload()).isEqualByComparingTo("150");assertThat(spring.getPayRate()).isEqualByComparingTo("22");
        assertThat(spring.getPerformancePay()).isEqualByComparingTo("1980");assertThat(spring.getBasicTeachingStandard()).isEqualByComparingTo("300");
    }
    @Test void specialStatusStandardsUseSameSemesterDate() {
        profile.setSpecialStatus("产假");
        assertThat(summary.recalcSummary(1L,"2025-2026-1",false).getBasicTeachingStandard()).isEqualByComparingTo("80");
        assertThat(summary.recalcSummary(1L,"2025-2026-2",false).getBasicTeachingStandard()).isEqualByComparingTo("180");
        profile.setSpecialStatus("在职读博");
        assertThat(summary.recalcSummary(1L,"2025-2026-1",false).getBasicTeachingStandard()).isEqualByComparingTo("64");
        assertThat(summary.recalcSummary(1L,"2025-2026-2",false).getBasicTeachingStandard()).isEqualByComparingTo("80");
    }
    @Test void missingHistoricalRateDoesNotFallForwardToCurrentRate() {
        when(rateMapper.selectBizPayRateList(any())).thenReturn(List.of(rate("33","2026-07-16",null)));
        var result=summary.recalcSummary(1L,"2025-2026-1",false);
        assertThat(result.getPayRate()).isNull();assertThat(result.getPerformancePay()).isEqualByComparingTo("0");
    }
    @Test void duplicateLatestRateStartIsRejected() {
        when(rateMapper.selectBizPayRateList(any())).thenReturn(List.of(rate("11","2025-09-01",null),rate("22","2025-09-01",null)));
        assertThatThrownBy(() -> summary.recalcSummary(1L,"2025-2026-1",false)).hasMessageContaining("生效版本冲突").hasMessageContaining("职称费率");
    }
    @Test void configuredEndControlsRulesAndRatesTogether() {
        var calendar=new SemesterCalendar();calendar.setAutumnEnd("02-01");ReflectionTestUtils.setField(summary,"semesterCalendar",calendar);
        var result=summary.recalcSummary(1L,"2025-2026-1",false);
        assertThat(result.getPayRate()).isEqualByComparingTo("22");assertThat(result.getRatedWorkload()).isEqualByComparingTo("150");assertThat(result.getPerformancePay()).isEqualByComparingTo("1980");
    }
    private BizWorkloadRule rule(String value,String from,String to) {var r=new BizWorkloadRule();r.setRuleValue(new BigDecimal(value));r.setEffectiveFrom(LocalDate.parse(from));r.setEffectiveTo(to==null?null:LocalDate.parse(to));return r;}
    private BizPayRate rate(String value,String from,String to) {var r=new BizPayRate();r.setRate(new BigDecimal(value));r.setEffectiveFrom(LocalDate.parse(from));r.setEffectiveTo(to==null?null:LocalDate.parse(to));return r;}
}
