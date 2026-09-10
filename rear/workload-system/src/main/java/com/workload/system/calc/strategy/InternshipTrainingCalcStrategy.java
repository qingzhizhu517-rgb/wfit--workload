package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWlInternshipTraining;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlInternshipTrainingMapper;

/**
 * G3 教学实习/实训计算策略：G3 = T × D × K × Q1 × Q2
 * <p>
 * 依据《办法》第十四条3，公式不含 Q3；附件1 模板 T 列同为
 * {@code G3(T*D*K*Q1*Q2)}。Q3 为保留列，字段保留但不参与计算。
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
        // 第十四条3：G3 = T×D×K×Q1×Q2，Q3 是保留列不参与（2026-09-10 按办法原文修正）
        return scale(mul(num(detail.getT()), coef(detail.getD()), coef(detail.getK()),
                coef(detail.getQ1()), coef(detail.getQ2())));
    }
}
