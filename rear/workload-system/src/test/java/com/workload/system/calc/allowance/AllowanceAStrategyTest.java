package com.workload.system.calc.allowance;

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
import com.workload.system.domain.BizAllowanceItem;

/**
 * 其他酬金 A 项（单独重修辅导）策略单元测试。
 * <p>
 * 依据《办法》第十五条1：
 * <ul>
 *   <li>1(3) 自学加辅导重修：① &lt;6 人 → 120 元；② [6,20) 人 → 260 元</li>
 *   <li>1(2) <b>「学生人数大于等于 20 人单独开班，按照教师基本工作量标准计算」</b>
 *       —— 即 ≥20 人时本项<b>不再计绩效酬金</b>，金额应由 G1（单独开班）那条路产生</li>
 * </ul>
 * 因此 ≥20 人应归零并告警，而不是沿用 260 元或取手工金额
 * （静默沿用会让教师重复拿钱；静默归零则教师无从判断是漏报还是规则如此）。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("其他酬金 A 项（办法第十五条1）")
class AllowanceAStrategyTest
{
    @InjectMocks
    private AllowanceAStrategy strategy;

    @Mock
    private RuleParamService ruleParamService;

    @BeforeEach
    void setUp()
    {
        lenient().when(ruleParamService.get(anyString(), any(BigDecimal.class)))
                .thenAnswer(inv -> {
                    String code = inv.getArgument(0);
                    if ("PAY_A_SELF_LT6".equals(code))
                    {
                        return new BigDecimal("120");
                    }
                    if ("PAY_A_SELF_6_20".equals(code))
                    {
                        return new BigDecimal("260");
                    }
                    return inv.getArgument(1);
                });
    }

    @Test
    @DisplayName("自学辅导 5 人 → 120 元")
    void belowSixReturns120()
    {
        assertThat(strategy.calculate(selfItem(5L, null))).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("自学辅导 6 人（下边界）→ 260 元")
    void atSixReturns260()
    {
        assertThat(strategy.calculate(selfItem(6L, null))).isEqualByComparingTo("260.00");
    }

    @Test
    @DisplayName("自学辅导 19 人（上边界）→ 260 元")
    void atNineteenReturns260()
    {
        assertThat(strategy.calculate(selfItem(19L, null))).isEqualByComparingTo("260.00");
    }

    @Test
    @DisplayName("自学辅导 20 人 → 0 元（第十五条1(2) 应单独开班，酬金在 G1 那条路）")
    void atTwentyReturnsZero()
    {
        assertThat(strategy.calculate(selfItem(20L, null)))
                .as("≥20 人须单独开班按基本工作量计，本项不计酬金")
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("自学辅导 20 人且录了手工金额也不发（防重复计酬）")
    void atTwentyIgnoresManualAmount()
    {
        assertThat(strategy.calculate(selfItem(20L, new BigDecimal("260"))))
                .as("手工金额不得覆盖「应单独开班」的规则")
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("自学辅导 35 人 → 0 元")
    void wellAboveTwentyReturnsZero()
    {
        assertThat(strategy.calculate(selfItem(35L, null))).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("学生人数为空按 0 人处理 → 走 <6 档 120 元")
    void nullCountTreatedAsZero()
    {
        BizAllowanceItem item = new BizAllowanceItem();
        item.setFeeType("A");
        item.setFeeSubtype("自学加辅导");

        assertThat(strategy.calculate(item)).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("跟班重修：按录入金额（本办法未给自动档位）")
    void followUpUsesManualAmount()
    {
        BizAllowanceItem item = new BizAllowanceItem();
        item.setFeeType("A");
        item.setFeeSubtype("跟班重修");
        item.setAmount(new BigDecimal("300"));

        assertThat(strategy.calculate(item)).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("酬金类型为 A")
    void feeTypeIsA()
    {
        assertThat(strategy.getFeeType()).isEqualTo("A");
    }

    private BizAllowanceItem selfItem(long count, BigDecimal manualAmount)
    {
        BizAllowanceItem item = new BizAllowanceItem();
        item.setFeeType("A");
        item.setFeeSubtype("自学加辅导");
        item.setStudentCount(count);
        item.setAmount(manualAmount);
        return item;
    }
}
