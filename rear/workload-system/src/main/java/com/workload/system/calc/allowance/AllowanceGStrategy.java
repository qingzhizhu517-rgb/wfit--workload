package com.workload.system.calc.allowance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizAllowanceItem;

/**
 * G 夜间值班酬金：工作量 × 30 元/单位（以签字为据）
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component
public class AllowanceGStrategy implements AllowanceCalcStrategy
{
    @Autowired
    private RuleParamService ruleParamService;

    @Override
    public String getFeeType()
    {
        return "G";
    }

    @Override
    public BigDecimal calculate(BizAllowanceItem item)
    {
        BigDecimal units = item.getWorkloadUnits() == null ? BigDecimal.ZERO : item.getWorkloadUnits();
        return units.multiply(ruleParamService.get("PAY_UNIT_FEE", new BigDecimal("30")))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
