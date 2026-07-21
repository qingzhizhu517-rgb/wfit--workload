package com.workload.system.calc.allowance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizAllowanceItem;

/**
 * C 论文重修酬金：120 元/人
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component
public class AllowanceCStrategy implements AllowanceCalcStrategy
{
    @Autowired
    private RuleParamService ruleParamService;

    @Override
    public String getFeeType()
    {
        return "C";
    }

    @Override
    public BigDecimal calculate(BizAllowanceItem item)
    {
        long count = item.getStudentCount() == null ? 0 : item.getStudentCount();
        return ruleParamService.get("PAY_C_THESIS", new BigDecimal("120"))
                .multiply(new BigDecimal(count)).setScale(2, RoundingMode.HALF_UP);
    }
}
