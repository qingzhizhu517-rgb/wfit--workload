package com.workload.system.calc.allowance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizAllowanceItem;

/**
 * 其他酬金 A–G 策略验收：用真实策略类断言各档金额与关键边界。
 * <p>
 * 只 Mock {@link RuleParamService} 并令其对任意规则编码返回传入的 fallback，
 * 确保当前 03_calc_rules.sql / 19_enable_allowance_d.sql 的默认规则矩阵可复现；
 * 计算结果全部来自真实策略实现，绝不 Mock 金额。
 * <p>
 * 期望金额直接对齐规则矩阵（不迁就实现）：
 * <ul>
 *   <li>A 自学 &lt;6 人 → PAY_A_SELF_LT6=120；[6,20) → PAY_A_SELF_6_20=260；≥20 → 0（第十五条1(2)）</li>
 *   <li>B 分散 → PAY_B_DISPERSED=10/人；集中(不跟班) → PAY_B_CONCENTRATED=15/人</li>
 *   <li>C → PAY_C_THESIS=120/人</li>
 *   <li>D 五档：&lt;20→0、[20,60)→30、[60,120)→80、[120,200)→100、≥200→150</li>
 *   <li>E → PAY_E_HOURLY=60/小时</li>
 *   <li>F → (天数×PAY_F_DAY_UNITS=6 + 体测班数×PAY_F_CLASS_UNITS=1) × PAY_UNIT_FEE=30</li>
 *   <li>G → 工作量单位 × PAY_UNIT_FEE=30</li>
 * </ul>
 *
 * @author wflg
 * @date 2026-09-12
 */
@DisplayName("其他酬金 A–G 策略验收（对齐 03_calc_rules.sql 规则矩阵）")
class AllowanceStrategyAcceptanceTest
{
    /** fee_type -> 真实策略（RuleParamService 被 Mock 为返回 fallback） */
    private Map<String, AllowanceCalcStrategy> strategies;

    @BeforeEach
    void setUp()
    {
        RuleParamService rule = mock(RuleParamService.class);
        when(rule.get(anyString(), any(BigDecimal.class)))
                .thenAnswer(inv -> inv.getArgument(1));

        strategies = new HashMap<>();
        for (AllowanceCalcStrategy strategy : List.of(
                new AllowanceAStrategy(), new AllowanceBStrategy(), new AllowanceCStrategy(),
                new AllowanceDStrategy(), new AllowanceEStrategy(), new AllowanceFStrategy(),
                new AllowanceGStrategy()))
        {
            ReflectionTestUtils.setField(strategy, "ruleParamService", rule);
            strategies.put(strategy.getFeeType(), strategy);
        }
    }

    @ParameterizedTest(name = "{0} {1} => {3}")
    @MethodSource("cases")
    @DisplayName("A–G 各档金额与真实策略一致")
    void allowanceStrategiesMatchAcceptanceFixture(String type, String label,
            BizAllowanceItem item, String expected)
    {
        AllowanceCalcStrategy strategy = strategies.get(type);
        assertThat(strategy.calculate(item)).isEqualByComparingTo(expected);
    }

    static Stream<Arguments> cases()
    {
        return Stream.of(
                args("A", "自学5人", item("A").subtype("自学").students(5).build(), "120.00"),
                args("A", "自学20人归零", item("A").subtype("自学").students(20).build(), "0.00"),
                args("B", "分散10人", item("B").subtype("分散").students(10).build(), "100.00"),
                args("B", "集中不跟班10人", item("B").subtype("集中不跟班").students(10).build(), "150.00"),
                args("C", "2人", item("C").students(2).build(), "240.00"),
                args("D", "60人", item("D").students(60).build(), "80.00"),
                args("E", "1.5小时", item("E").hours("1.5").build(), "90.00"),
                args("F", "2天3班", item("F").days("2").classes(3).build(), "450.00"),
                args("G", "4单位", item("G").units("4").build(), "120.00"));
    }

    @ParameterizedTest(name = "D {0}人 => {1}")
    @MethodSource("dBoundaryCases")
    @DisplayName("D 代阅卷五档边界（第十五条5）")
    void allowanceDBoundaries(long students, String expected)
    {
        BizAllowanceItem item = item("D").students(students).build();
        assertThat(strategies.get("D").calculate(item)).isEqualByComparingTo(expected);
    }

    static Stream<Arguments> dBoundaryCases()
    {
        return Stream.of(
                args(19L, "0.00"), args(20L, "30.00"), args(59L, "30.00"), args(60L, "80.00"),
                args(119L, "80.00"), args(120L, "100.00"), args(199L, "100.00"), args(200L, "150.00"));
    }

    @ParameterizedTest(name = "A 自学{0}人 => {1}")
    @MethodSource("aBoundaryCases")
    @DisplayName("A 自学辅导档位边界（第十五条1）")
    void allowanceASelfStudyBoundaries(long students, String expected)
    {
        BizAllowanceItem item = item("A").subtype("自学加辅导").students(students).build();
        assertThat(strategies.get("A").calculate(item)).isEqualByComparingTo(expected);
    }

    static Stream<Arguments> aBoundaryCases()
    {
        return Stream.of(
                args(5L, "120.00"), args(6L, "260.00"), args(19L, "260.00"), args(20L, "0.00"));
    }

    private static Arguments args(Object... values)
    {
        return Arguments.of(values);
    }

    private static ItemBuilder item(String feeType)
    {
        return new ItemBuilder(feeType);
    }

    /** 流式构造 {@link BizAllowanceItem} 验收输入，只填各策略实际读取的字段。 */
    private static final class ItemBuilder
    {
        private final BizAllowanceItem item = new BizAllowanceItem();

        private ItemBuilder(String feeType)
        {
            item.setFeeType(feeType);
        }

        ItemBuilder subtype(String subtype)
        {
            item.setFeeSubtype(subtype);
            return this;
        }

        ItemBuilder students(long count)
        {
            item.setStudentCount(count);
            return this;
        }

        ItemBuilder hours(String hours)
        {
            item.setDurationHours(new BigDecimal(hours));
            return this;
        }

        ItemBuilder days(String days)
        {
            item.setDays(new BigDecimal(days));
            return this;
        }

        ItemBuilder classes(long classCount)
        {
            item.setClassCount(classCount);
            return this;
        }

        ItemBuilder units(String workloadUnits)
        {
            item.setWorkloadUnits(new BigDecimal(workloadUnits));
            return this;
        }

        BizAllowanceItem build()
        {
            return item;
        }
    }
}
