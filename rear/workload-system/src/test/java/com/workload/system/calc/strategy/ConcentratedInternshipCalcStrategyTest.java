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
import com.workload.system.domain.BizWlConcentratedInternship;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlConcentratedInternshipMapper;

/**
 * G6 集中实习（现场跟班指导）策略单元测试。
 * <p>
 * 依据《办法》第十四条6(3) 及注4：
 * <ul>
 *   <li>{@code G6 = W × R6 × 0.4}</li>
 *   <li><b>注4：「指导实习的学生人数不超过20人，超出部分不计算工作量及酬金。」</b>
 *       —— 这是办法中<b>明确写了截断</b>的一处（对比 G4/G5 均未写），故此处截断有依据</li>
 * </ul>
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("G6 集中实习策略（办法第十四条6）")
class ConcentratedInternshipCalcStrategyTest
{
    @InjectMocks
    private ConcentratedInternshipCalcStrategy strategy;

    @Mock
    private BizWlConcentratedInternshipMapper mapper;

    @Mock
    private RuleParamService ruleParamService;

    @BeforeEach
    void setUp()
    {
        lenient().when(ruleParamService.get(anyString(), any(BigDecimal.class)))
                .thenAnswer(inv -> {
                    String code = inv.getArgument(0);
                    if ("CAP_R6_MAX".equals(code))
                    {
                        return new BigDecimal("20");
                    }
                    if ("CONST_COURSE_DESIGN".equals(code))
                    {
                        return new BigDecimal("0.4");
                    }
                    return inv.getArgument(1);
                });
    }

    @Test
    @DisplayName("基本公式：W×R6×0.4 —— 4 周 ×18 人 ×0.4 = 28.80")
    void shouldComputeBasicFormula()
    {
        when(mapper.selectBizWlConcentratedInternshipByItemId(1L)).thenReturn(detail("4", "18"));

        assertThat(strategy.calculate(item(1L))).isEqualByComparingTo("28.80");
    }

    @Test
    @DisplayName("R6 恰好 20：4×20×0.4 = 32.00")
    void shouldComputeAtCap()
    {
        when(mapper.selectBizWlConcentratedInternshipByItemId(2L)).thenReturn(detail("4", "20"));

        assertThat(strategy.calculate(item(2L))).isEqualByComparingTo("32.00");
    }

    @Test
    @DisplayName("R6 超 20（25 人）：注4 明确超出部分不计 → 截断为 4×20×0.4 = 32.00")
    void shouldTruncateAboveCap()
    {
        when(mapper.selectBizWlConcentratedInternshipByItemId(3L)).thenReturn(detail("4", "25"));

        assertThat(strategy.calculate(item(3L)))
                .as("第十四条6注4 明写「超出部分不计算工作量及酬金」，此处截断有依据")
                .isEqualByComparingTo("32.00");
    }

    @Test
    @DisplayName("R6 超 20 需置 is_over_limit=1（截断必须同时告警，防教师无声少拿钱）")
    void shouldFlagWhenTruncated()
    {
        when(mapper.selectBizWlConcentratedInternshipByItemId(4L)).thenReturn(detail("4", "25"));
        BizWorkloadItem target = item(4L);

        strategy.afterCalculated(target, new BigDecimal("32.00"));

        assertThat(target.getIsOverLimit()).isEqualTo(1);
    }

    @Test
    @DisplayName("R6 未超 20 不置超标标记")
    void shouldNotFlagUnderCap()
    {
        when(mapper.selectBizWlConcentratedInternshipByItemId(5L)).thenReturn(detail("4", "20"));
        BizWorkloadItem target = item(5L);

        strategy.afterCalculated(target, new BigDecimal("32.00"));

        assertThat(target.getIsOverLimit()).isEqualTo(0);
    }

    @Test
    @DisplayName("类别代码为 G6")
    void typeCodeIsG6()
    {
        assertThat(strategy.getTypeCode()).isEqualTo("G6");
    }

    private BizWorkloadItem item(long id)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(id);
        return item;
    }

    private BizWlConcentratedInternship detail(String w, String r6)
    {
        BizWlConcentratedInternship detail = new BizWlConcentratedInternship();
        detail.setW(new BigDecimal(w));
        // R6 在实体中是 Long（人数为整数人）
        detail.setR6(Long.valueOf(r6));
        return detail;
    }
}
