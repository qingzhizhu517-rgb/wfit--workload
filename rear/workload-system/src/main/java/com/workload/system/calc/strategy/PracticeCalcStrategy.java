package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWlPractice;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlPracticeMapper;

/**
 * G2 课内实践/实验/实训计算策略：G2 = J2 × K × C2 × Q1 × Q2
 * <p>
 * 依据《办法》第十四条2，公式不含 Q3；附件1 模板 P 列同为
 * {@code G2(J2*K*C2*Q1*Q2)}。Q3 为保留列，字段保留但不参与计算。
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component("practiceCalcStrategy")
public class PracticeCalcStrategy extends AbstractWorkloadCalcStrategy
{
    @Autowired
    private BizWlPracticeMapper bizWlPracticeMapper;

    @Override
    public String getTypeCode()
    {
        return "G2";
    }

    @Override
    public BigDecimal calculate(BizWorkloadItem item)
    {
        BizWlPractice detail = bizWlPracticeMapper.selectBizWlPracticeByItemId(item.getId());
        if (detail == null)
        {
            throw new ServiceException("G2课内实践明细缺失, itemId=" + item.getId());
        }
        // 第十四条2：G2 = J2×K×C2×Q1×Q2，Q3 是保留列不参与（2026-09-10 按办法原文修正）
        return scale(mul(num(detail.getJ2()), coef(detail.getK()), coef(detail.getC2()),
                coef(detail.getQ1()), coef(detail.getQ2())));
    }
}
