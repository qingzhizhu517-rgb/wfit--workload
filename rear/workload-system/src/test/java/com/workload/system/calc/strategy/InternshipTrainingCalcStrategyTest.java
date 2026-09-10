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
import com.workload.system.domain.BizWlInternshipTraining;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlInternshipTrainingMapper;

/**
 * G3 教学实习/实训策略单元测试。
 * <p>
 * 依据：《潍坊理工学院教师工作量管理办法》第十四条3 ——
 * {@code G3 = T × D × K × Q1 × Q2}；
 * 附件1「-新」模板 T 列公式同为 {@code G3(T*D*K*Q1*Q2)}。
 * <b>两处均不含 Q3</b>——Q3 是表内保留列，未进入公式。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("G3 教学实习实训策略（办法第十四条3）")
class InternshipTrainingCalcStrategyTest
{
    @InjectMocks
    private InternshipTrainingCalcStrategy strategy;

    @Mock
    private BizWlInternshipTrainingMapper bizWlInternshipTrainingMapper;

    @Test
    @DisplayName("基本公式：T×D×K×Q1×Q2 —— 10×4×1×1×1 = 40.00")
    void shouldComputeBasicFormula()
    {
        when(bizWlInternshipTrainingMapper.selectBizWlInternshipTrainingByItemId(1L)).thenReturn(
                internship("10", "4", "1", "1", "1", "1"));

        assertThat(strategy.calculate(item(1L))).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("Q3 不参与计算：填 1.5 也不改变结果（否则 40 会变成 60）")
    void q3MustNotAffectResult()
    {
        when(bizWlInternshipTrainingMapper.selectBizWlInternshipTrainingByItemId(2L)).thenReturn(
                internship("10", "4", "1", "1", "1", "1.5"));

        assertThat(strategy.calculate(item(2L)))
                .as("办法与附件1 公式均不含 Q3，Q3 只是保留列")
                .isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("系数为 null 时按 1 处理（T 为空按 0）")
    void nullCoefficientsDefaultToOne()
    {
        when(bizWlInternshipTrainingMapper.selectBizWlInternshipTrainingByItemId(3L)).thenReturn(
                internship(null, null, null, null, null, null));

        assertThat(strategy.calculate(item(3L))).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("明细缺失时抛 ServiceException（不静默返回 0）")
    void shouldThrowWhenDetailMissing()
    {
        when(bizWlInternshipTrainingMapper.selectBizWlInternshipTrainingByItemId(4L)).thenReturn(null);

        assertThatThrownBy(() -> strategy.calculate(item(4L)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("G3教学实习实训明细缺失");
    }

    @Test
    @DisplayName("类别代码为 G3")
    void typeCodeIsG3()
    {
        assertThat(strategy.getTypeCode()).isEqualTo("G3");
    }

    private BizWorkloadItem item(long id)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(id);
        return item;
    }

    private BizWlInternshipTraining internship(String t, String d, String k, String q1, String q2, String q3)
    {
        BizWlInternshipTraining detail = new BizWlInternshipTraining();
        detail.setT(dec(t));
        detail.setD(dec(d));
        detail.setK(dec(k));
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
