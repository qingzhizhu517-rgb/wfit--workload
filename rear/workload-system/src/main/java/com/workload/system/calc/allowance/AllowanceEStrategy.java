package com.workload.system.calc.allowance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizAllowanceItem;

/**
 * E 讲座酬金：时长(小时) × 60 元/小时
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component
public class AllowanceEStrategy implements AllowanceCalcStrategy
{
    @Autowired
    private RuleParamService ruleParamService;

    @Override
    public String getFeeType()
    {
        return "E";
    }

    @Override
    public BigDecimal calculate(BizAllowanceItem item)
    {
        BigDecimal hours = item.getDurationHours() == null ? BigDecimal.ZERO : item.getDurationHours();
        return hours.multiply(ruleParamService.get("PAY_E_HOURLY", new BigDecimal("60")))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
