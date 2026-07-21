package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWlTheory;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlTheoryMapper;

/**
 * G1 理论课计算策略：G1 = J1 × C1 × K1 × Q1 × Q2 × Q3 × N
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component("theoryCalcStrategy")
public class TheoryCalcStrategy extends AbstractWorkloadCalcStrategy
{
    @Autowired
    private BizWlTheoryMapper bizWlTheoryMapper;

    @Override
    public String getTypeCode()
    {
        return "G1";
    }

    @Override
    public BigDecimal calculate(BizWorkloadItem item)
    {
        BizWlTheory detail = bizWlTheoryMapper.selectBizWlTheoryByItemId(item.getId());
        if (detail == null)
        {
            throw new ServiceException("G1理论课明细缺失, itemId=" + item.getId());
        }
        return scale(mul(num(detail.getJ1()), coef(detail.getC1()), coef(detail.getK1()),
                coef(detail.getQ1()), coef(detail.getQ2()), coef(detail.getQ3()), coef(detail.getN())));
    }
}
