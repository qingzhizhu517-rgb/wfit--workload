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
import com.workload.system.domain.BizWlThesis;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlThesisMapper;

/**
 * G5 毕业论文（设计）策略单元测试。
 * <p>
 * 依据《办法》第十四条5：
 * <ul>
 *   <li>{@code G5 = R5 × K5}，本科「R5≤10 时，<b>按实际人数计算</b>，当 R5＞8 时，须报院长批准，教务处备案」；
 *       专科「R5≤15 时，按实际人数计算，当 R5＞15 时，须报院长批准」</li>
 *   <li>K5：理工本科 9 / 专科 5；文史本科 6 / 专科 4</li>
 * </ul>
 * 条文<b>未规定「超出部分不计算」</b>，故人数一律不截断，超阈值只置 is_over_limit
 * （表示须报批，不是少算）。注意报批阈值本科为 <b>8</b>（不是 10）。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("G5 毕业论文策略（办法第十四条5）")
class ThesisCalcStrategyTest
{
    @InjectMocks
    private ThesisCalcStrategy strategy;

    @Mock
    private BizWlThesisMapper bizWlThesisMapper;

    @Mock
    private RuleParamService ruleParamService;

    @BeforeEach
    void setUp()
    {
        // CAP_R5_JUNIOR 默认 15、APPROVAL_R5_BACHELOR 默认 8
        lenient().when(ruleParamService.get(anyString(), any(BigDecimal.class)))
                .thenAnswer(inv -> {
                    String code = inv.getArgument(0);
                    if ("CAP_R5_JUNIOR".equals(code))
                    {
                        return new BigDecimal("15");
                    }
                    if ("APPROVAL_R5_BACHELOR".equals(code))
                    {
                        return new BigDecimal("8");
                    }
                    return inv.getArgument(1);
                });
    }

    @Test
    @DisplayName("G5 = R5×K5：本科理工 K5=9，6 人 → 54.00")
    void shouldComputeScitechUndergrad()
    {
        when(bizWlThesisMapper.selectBizWlThesisByItemId(1L)).thenReturn(detail("6", "9", "本科"));

        assertThat(strategy.calculate(item(1L))).isEqualByComparingTo("54.00");
    }

    @Test
    @DisplayName("文史本科 K5=6：6 人 → 36.00")
    void shouldComputeLiberalArtsUndergrad()
    {
        when(bizWlThesisMapper.selectBizWlThesisByItemId(2L)).thenReturn(detail("6", "6", "本科"));

        assertThat(strategy.calculate(item(2L))).isEqualByComparingTo("36.00");
    }

    @Test
    @DisplayName("文史专科 K5=4：8 人 → 32.00")
    void shouldComputeLiberalArtsJunior()
    {
        when(bizWlThesisMapper.selectBizWlThesisByItemId(3L)).thenReturn(detail("8", "4", "专科"));

        assertThat(strategy.calculate(item(3L))).isEqualByComparingTo("32.00");
    }

    @Test
    @DisplayName("R5=12 不截断：本科理工 12×9 = 108.00（条文只说须报批，未说封顶）")
    void shouldNotTruncateR5()
    {
        when(bizWlThesisMapper.selectBizWlThesisByItemId(4L)).thenReturn(detail("12", "9", "本科"));

        assertThat(strategy.calculate(item(4L)))
                .as("第十四条5 明写「按实际人数计算」，无封顶规则")
                .isEqualByComparingTo("108.00");
    }

    @Test
    @DisplayName("本科 R5=8 不触发报批（阈值为 >8）")
    void undergradAtThresholdNotFlagged()
    {
        when(bizWlThesisMapper.selectBizWlThesisByItemId(5L)).thenReturn(detail("8", "9", "本科"));
        BizWorkloadItem target = item(5L);

        strategy.afterCalculated(target, new BigDecimal("72"));

        assertThat(target.getIsOverLimit()).isEqualTo(0);
    }

    @Test
    @DisplayName("本科 R5=9 触发报批（>8）")
    void undergradAboveThresholdFlagged()
    {
        when(bizWlThesisMapper.selectBizWlThesisByItemId(6L)).thenReturn(detail("9", "9", "本科"));
        BizWorkloadItem target = item(6L);

        strategy.afterCalculated(target, new BigDecimal("81"));

        assertThat(target.getIsOverLimit()).isEqualTo(1);
    }

    @Test
    @DisplayName("专科 R5=15 不触发（阈值 >15）")
    void juniorAtThresholdNotFlagged()
    {
        when(bizWlThesisMapper.selectBizWlThesisByItemId(7L)).thenReturn(detail("15", "5", "专科"));
        BizWorkloadItem target = item(7L);

        strategy.afterCalculated(target, new BigDecimal("75"));

        assertThat(target.getIsOverLimit()).isEqualTo(0);
    }

    @Test
    @DisplayName("专科 R5=16 触发报批（>15）")
    void juniorAboveThresholdFlagged()
    {
        when(bizWlThesisMapper.selectBizWlThesisByItemId(8L)).thenReturn(detail("16", "5", "专科"));
        BizWorkloadItem target = item(8L);

        strategy.afterCalculated(target, new BigDecimal("80"));

        assertThat(target.getIsOverLimit()).isEqualTo(1);
    }

    @Test
    @DisplayName("类别代码为 G5")
    void typeCodeIsG5()
    {
        assertThat(strategy.getTypeCode()).isEqualTo("G5");
    }

    private BizWorkloadItem item(long id)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(id);
        return item;
    }

    private BizWlThesis detail(String r5, String k5, String level)
    {
        BizWlThesis detail = new BizWlThesis();
        // R5 在实体中是 Long（人数为整数人）
        detail.setR5(Long.valueOf(r5));
        detail.setK5(new BigDecimal(k5));
        detail.setEducationLevel(level);
        return detail;
    }
}
