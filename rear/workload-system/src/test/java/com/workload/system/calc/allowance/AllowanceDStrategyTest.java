package com.workload.system.calc.allowance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

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
 * D 代阅卷酬金策略单元测试。
 * <p>
 * 依据《办法》第十五条5 五档阶梯，重点覆盖四条档位边界。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("D 代阅卷酬金策略（办法第十五条5）")
class AllowanceDStrategyTest
{
    @InjectMocks
    private AllowanceDStrategy strategy;

    @Mock
    private RuleParamService ruleParamService;

    @BeforeEach
    void setUp()
    {
        // 未命中 mock 桩的键回落到默认值（与生产兜底一致）
        lenient().when(ruleParamService.get(anyString(), any(BigDecimal.class)))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    @DisplayName("19 人（<20 下边界外）→ 0 元")
    void nineteenReturnsZero()
    {
        assertThat(strategy.calculate(item(19L))).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("20 人（[20,60) 下边界）→ 30 元")
    void twentyReturns30()
    {
        assertThat(strategy.calculate(item(20L))).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("59 人（[20,60) 上边界）→ 30 元")
    void fiftyNineReturns30()
    {
        assertThat(strategy.calculate(item(59L))).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("60 人（[60,120) 下边界）→ 80 元")
    void sixtyReturns80()
    {
        assertThat(strategy.calculate(item(60L))).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("119 人（[60,120) 上边界）→ 80 元")
    void hundredNineteenReturns80()
    {
        assertThat(strategy.calculate(item(119L))).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("120 人（[120,200) 下边界）→ 100 元")
    void hundredTwentyReturns100()
    {
        assertThat(strategy.calculate(item(120L))).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("199 人（[120,200) 上边界）→ 100 元")
    void hundredNinetyNineReturns100()
    {
        assertThat(strategy.calculate(item(199L))).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("200 人（≥200 下边界）→ 150 元")
    void twoHundredReturns150()
    {
        assertThat(strategy.calculate(item(200L))).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("人数为空按 0 人处理 → 0 元")
    void nullCountTreatedAsZero()
    {
        assertThat(strategy.calculate(item(null))).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("酬金类型为 D")
    void feeTypeIsD()
    {
        assertThat(strategy.getFeeType()).isEqualTo("D");
    }

    private BizAllowanceItem item(Long count)
    {
        BizAllowanceItem item = new BizAllowanceItem();
        item.setFeeType("D");
        item.setStudentCount(count);
        return item;
    }
}
