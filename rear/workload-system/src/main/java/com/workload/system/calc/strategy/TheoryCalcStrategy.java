package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWlTheory;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlTheoryMapper;

/**
 * G1 理论课计算策略：G1 = J1 × C1 × K1 × Q1 × Q2 × N
 * <p>
 * 依据《办法》第十四条1，公式不含 Q3；附件1「-新」模板 L 列公式同为
 * {@code G1(J1*C1*K1*Q1*Q2*N)}。Q3（非语言类全外文课程系数）为表内保留列，
 * 字段保留展示但不参与计算。
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
        // 第十四条1：G1 = J1×C1×K1×Q1×Q2×N，Q3 是保留列不参与（2026-09-10 按办法原文修正）
        return scale(mul(num(detail.getJ1()), coef(detail.getC1()), coef(detail.getK1()),
                coef(detail.getQ1()), coef(detail.getQ2()), coef(detail.getN())));
    }
}
