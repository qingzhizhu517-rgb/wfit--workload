package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWlPractice;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlPracticeMapper;

/**
 * G2 课内实践/实验/实训计算策略：G2 = J2 × K × C2 × Q1 × Q2 × Q3
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
        return scale(mul(num(detail.getJ2()), coef(detail.getK()), coef(detail.getC2()),
                coef(detail.getQ1()), coef(detail.getQ2()), coef(detail.getQ3())));
    }
}
