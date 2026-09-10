package com.workload.system.calc.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizWlCourseDesign;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlCourseDesignMapper;

/**
 * G4 课程设计策略单元测试。
 * <p>
 * 依据：《办法》第十四条4 —— {@code G4 = J4 × R4 × 0.4}，{@code R4≤60}。
 * <b>条文只说「R4≤60」，未写「超出部分不计算」</b>——办法需要截断时会明写
 * （对比第十四条6注4「指导实习的学生人数不超过20人，超出部分不计算工作量及酬金」）。
 * 故 R4&gt;60 应当<b>照实际人数计算</b>，仅作超标提示，而非静默截断。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("G4 课程设计策略（办法第十四条4）")
class CourseDesignCalcStrategyTest
{
    @InjectMocks
    private CourseDesignCalcStrategy strategy;

    @Mock
    private BizWlCourseDesignMapper bizWlCourseDesignMapper;

    @Mock
    private RuleParamService ruleParamService;

    @BeforeEach
    void setUp()
    {
        // lenient：部分用例不触及规则读取
        lenient().when(ruleParamService.get(anyString(), any(BigDecimal.class)))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    @DisplayName("基本公式：J4×R4×0.4 —— 2×50×0.4 = 40.00")
    void shouldComputeBasicFormula()
    {
        when(bizWlCourseDesignMapper.selectBizWlCourseDesignByItemId(1L)).thenReturn(detail("2", "50"));

        assertThat(strategy.calculate(item(1L))).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("R4 达上限 60：2×60×0.4 = 48.00")
    void shouldComputeAtCap()
    {
        when(bizWlCourseDesignMapper.selectBizWlCourseDesignByItemId(2L)).thenReturn(detail("2", "60"));

        assertThat(strategy.calculate(item(2L))).isEqualByComparingTo("48.00");
    }

    @Test
    @DisplayName("R4 超 60（80 人）：办法未规定截断，应按实际人数 2×80×0.4 = 64.00")
    void shouldNotTruncateAboveCap()
    {
        when(bizWlCourseDesignMapper.selectBizWlCourseDesignByItemId(3L)).thenReturn(detail("2", "80"));

        assertThat(strategy.calculate(item(3L)))
                .as("第十四条4 未写「超出部分不计算」，截断会擅自减少教师工作量")
                .isEqualByComparingTo("64.00");
    }

    @Test
    @DisplayName("R4 为空按 0 处理")
    void nullR4TreatedAsZero()
    {
        when(bizWlCourseDesignMapper.selectBizWlCourseDesignByItemId(4L)).thenReturn(detail("2", null));

        assertThat(strategy.calculate(item(4L))).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("超限回调：R4=80 超告警阈值 60 → is_over_limit=1（不减量）")
    void overLimitFlagWhenR4AboveThreshold()
    {
        when(bizWlCourseDesignMapper.selectBizWlCourseDesignByItemId(5L)).thenReturn(detail("2", "80"));

        BizWorkloadItem item = item(5L);
        strategy.calculate(item);
        strategy.afterCalculated(item, new BigDecimal("64.00"));

        assertThat(item.getIsOverLimit()).isEqualTo(1);
    }

    @Test
    @DisplayName("超限回调：R4=60 未超阈值 → is_over_limit=0")
    void noOverLimitFlagAtThreshold()
    {
        when(bizWlCourseDesignMapper.selectBizWlCourseDesignByItemId(6L)).thenReturn(detail("2", "60"));

        BizWorkloadItem item = item(6L);
        strategy.calculate(item);
        strategy.afterCalculated(item, new BigDecimal("48.00"));

        assertThat(item.getIsOverLimit()).isEqualTo(0);
    }

    @Test
    @DisplayName("类别代码为 G4")
    void typeCodeIsG4()
    {
        assertThat(strategy.getTypeCode()).isEqualTo("G4");
    }

    private BizWorkloadItem item(long id)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(id);
        return item;
    }

    private BizWlCourseDesign detail(String j4, String r4)
    {
        BizWlCourseDesign detail = new BizWlCourseDesign();
        detail.setJ4(j4 == null ? null : new BigDecimal(j4));
        // R4 在实体中是 Long（人数为整数人）
        detail.setR4(r4 == null ? null : Long.valueOf(r4));
        return detail;
    }
}
