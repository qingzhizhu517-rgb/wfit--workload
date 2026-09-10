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
import com.workload.system.domain.BizWlPractice;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlPracticeMapper;

/**
 * G2 课内实践策略单元测试。
 * <p>
 * 依据：《潍坊理工学院教师工作量管理办法》第十四条2 ——
 * {@code G2 = J2 × K × C2 × Q1 × Q2}；
 * 附件1「-新」模板 P 列公式同为 {@code G2(J2*K*C2*Q1*Q2)}。
 * <b>两处均不含 Q3</b>——Q3 是表内保留列，未进入公式。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("G2 课内实践策略（办法第十四条2）")
class PracticeCalcStrategyTest
{
    @InjectMocks
    private PracticeCalcStrategy strategy;

    @Mock
    private BizWlPracticeMapper bizWlPracticeMapper;

    @Test
    @DisplayName("基本公式：J2×K×C2×Q1×Q2 —— 32×1×0.9×1×1 = 28.80")
    void shouldComputeBasicFormula()
    {
        when(bizWlPracticeMapper.selectBizWlPracticeByItemId(1L)).thenReturn(
                practice("32", "1", "0.9", "1", "1", "1"));

        assertThat(strategy.calculate(item(1L))).isEqualByComparingTo("28.80");
    }

    @Test
    @DisplayName("Q3 不参与计算：填 1.5 也不改变结果（否则 28.8 会变成 43.2）")
    void q3MustNotAffectResult()
    {
        when(bizWlPracticeMapper.selectBizWlPracticeByItemId(2L)).thenReturn(
                practice("32", "1", "0.9", "1", "1", "1.5"));

        assertThat(strategy.calculate(item(2L)))
                .as("办法与附件1 公式均不含 Q3，Q3 只是保留列")
                .isEqualByComparingTo("28.80");
    }

    @Test
    @DisplayName("系数为 null 时按 1 处理（不缩放）")
    void nullCoefficientsDefaultToOne()
    {
        when(bizWlPracticeMapper.selectBizWlPracticeByItemId(3L)).thenReturn(
                practice("40", null, null, null, null, null));

        assertThat(strategy.calculate(item(3L))).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("明细缺失时抛 ServiceException（不静默返回 0）")
    void shouldThrowWhenDetailMissing()
    {
        when(bizWlPracticeMapper.selectBizWlPracticeByItemId(4L)).thenReturn(null);

        assertThatThrownBy(() -> strategy.calculate(item(4L)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("G2课内实践明细缺失");
    }

    @Test
    @DisplayName("类别代码为 G2")
    void typeCodeIsG2()
    {
        assertThat(strategy.getTypeCode()).isEqualTo("G2");
    }

    private BizWorkloadItem item(long id)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(id);
        return item;
    }

    private BizWlPractice practice(String j2, String k, String c2, String q1, String q2, String q3)
    {
        BizWlPractice detail = new BizWlPractice();
        detail.setJ2(dec(j2));
        detail.setK(dec(k));
        detail.setC2(dec(c2));
        detail.setQ1(dec(q1));
        detail.setQ2(dec(q2));
        detail.setQ3(dec(q3));
        return detail;
    }

    private BigDecimal dec(String value)
    {
        return value == null ? null : new BigDecimal(value);
    }
}
