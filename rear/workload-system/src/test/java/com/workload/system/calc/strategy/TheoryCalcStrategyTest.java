package com.workload.system.calc.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWlTheory;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlTheoryMapper;

/**
 * G1 理论课策略单元测试。
 * <p>
 * 依据：《潍坊理工学院教师工作量管理办法》第十四条1 ——
 * {@code G1 = J1 × C1 × K1 × Q1 × Q2 × N}；
 * 附件1「-新」模板 L 列公式同为 {@code G1(J1*C1*K1*Q1*Q2*N)}。
 * <b>两处均不含 Q3</b>——Q3（非语言类全外文课程系数）是表内保留列，未进入公式。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("G1 理论课策略（办法第十四条1）")
class TheoryCalcStrategyTest
{
    private static final BigDecimal ONE = new BigDecimal("1");

    @InjectMocks
    private TheoryCalcStrategy strategy;

    @Mock
    private BizWlTheoryMapper bizWlTheoryMapper;

    @Test
    @DisplayName("基本公式：J1×C1×K1×Q1×Q2×N —— 32×1×1.1×1×1×1.1 = 38.72")
    void shouldComputeBasicFormula()
    {
        when(bizWlTheoryMapper.selectBizWlTheoryByItemId(1L)).thenReturn(
                theory("32", "1", "1.1", "1", "1", "1", "1.1"));

        assertThat(strategy.calculate(item(1L))).isEqualByComparingTo("38.72");
    }

    @Test
    @DisplayName("Q3 不参与计算：填 1.5 也不改变结果（否则 38.72 会变成 58.08）")
    void q3MustNotAffectResult()
    {
        when(bizWlTheoryMapper.selectBizWlTheoryByItemId(2L)).thenReturn(
                theory("32", "1", "1.1", "1", "1", "1.5", "1.1"));

        assertThat(strategy.calculate(item(2L)))
                .as("办法与附件1 公式均不含 Q3，Q3 只是保留列")
                .isEqualByComparingTo("38.72");
    }

    @Test
    @DisplayName("C1 三档：第二次 0.9 → 32×0.9×1.1×1×1×1.1 = 34.848 → 34.85")
    void shouldApplyC1SecondRound()
    {
        when(bizWlTheoryMapper.selectBizWlTheoryByItemId(3L)).thenReturn(
                theory("32", "0.9", "1.1", "1", "1", "1", "1.1"));

        assertThat(strategy.calculate(item(3L))).isEqualByComparingTo("34.85");
    }

    @Test
    @DisplayName("N 合堂 1.2 与 Q1 不合格 0.8 同时生效：48×1×1×0.8×1×1×1.2 = 46.08")
    void shouldApplyNandQ1()
    {
        when(bizWlTheoryMapper.selectBizWlTheoryByItemId(4L)).thenReturn(
                theory("48", "1", "1", "0.8", "1", "1", "1.2"));

        assertThat(strategy.calculate(item(4L))).isEqualByComparingTo("46.08");
    }

    @Test
    @DisplayName("结果保留 2 位小数 HALF_UP")
    void shouldRoundHalfUpToTwoDecimals()
    {
        // 33×1×1×1×1×1×1 = 33 精确；换 33.333 的三位输入验证四舍五入
        when(bizWlTheoryMapper.selectBizWlTheoryByItemId(5L)).thenReturn(
                theory("33.333", "1", "1", "1", "1", "1", "1"));

        assertThat(strategy.calculate(item(5L))).isEqualByComparingTo("33.33");
    }

    @Test
    @DisplayName("系数为 null 时按 1 处理（不缩放）")
    void nullCoefficientsDefaultToOne()
    {
        when(bizWlTheoryMapper.selectBizWlTheoryByItemId(6L)).thenReturn(
                theory("40", null, null, null, null, null, null));

        assertThat(strategy.calculate(item(6L))).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("明细缺失时抛 ServiceException（不静默返回 0）")
    void shouldThrowWhenDetailMissing()
    {
        when(bizWlTheoryMapper.selectBizWlTheoryByItemId(7L)).thenReturn(null);

        assertThatThrownBy(() -> strategy.calculate(item(7L)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("G1理论课明细缺失");
    }

    @Test
    @DisplayName("类别代码为 G1")
    void typeCodeIsG1()
    {
        assertThat(strategy.getTypeCode()).isEqualTo("G1");
    }

    private BizWorkloadItem item(long id)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(id);
        return item;
    }

    private BizWlTheory theory(String j1, String c1, String k1, String q1, String q2, String q3, String n)
    {
        BizWlTheory detail = new BizWlTheory();
        detail.setJ1(dec(j1));
        detail.setC1(dec(c1));
        detail.setK1(dec(k1));
        detail.setQ1(dec(q1));
        detail.setQ2(dec(q2));
        detail.setQ3(dec(q3));
        detail.setN(dec(n));
        return detail;
    }

    private BigDecimal dec(String value)
    {
        return value == null ? null : new BigDecimal(value);
    }
}
