package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWlInternshipTraining;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlInternshipTrainingMapper;

/**
 * G3 教学实习/实训计算策略：G3 = T × D × K × Q1 × Q2 × Q3
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component("internshipTrainingCalcStrategy")
public class InternshipTrainingCalcStrategy extends AbstractWorkloadCalcStrategy
{
    @Autowired
    private BizWlInternshipTrainingMapper bizWlInternshipTrainingMapper;

    @Override
    public String getTypeCode()
    {
        return "G3";
    }

    @Override
    public BigDecimal calculate(BizWorkloadItem item)
    {
        BizWlInternshipTraining detail = bizWlInternshipTrainingMapper.selectBizWlInternshipTrainingByItemId(item.getId());
        if (detail == null)
        {
            throw new ServiceException("G3教学实习实训明细缺失, itemId=" + item.getId());
        }
        return scale(mul(num(detail.getT()), coef(detail.getD()), coef(detail.getK()),
                coef(detail.getQ1()), coef(detail.getQ2()), coef(detail.getQ3())));
    }
}
