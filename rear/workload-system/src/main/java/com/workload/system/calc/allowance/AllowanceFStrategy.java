package com.workload.system.calc.allowance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizAllowanceItem;

/**
 * F 运动会裁判酬金：(天数×6 + 体测班数×1) 工作量 × 30 元/单位
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component
public class AllowanceFStrategy implements AllowanceCalcStrategy
{
    @Autowired
    private RuleParamService ruleParamService;

    @Override
    public String getFeeType()
    {
        return "F";
    }

    @Override
    public BigDecimal calculate(BizAllowanceItem item)
    {
        BigDecimal days = item.getDays() == null ? BigDecimal.ZERO : item.getDays();
        BigDecimal classes = item.getClassCount() == null ? BigDecimal.ZERO : new BigDecimal(item.getClassCount());
        BigDecimal units = days.multiply(ruleParamService.get("PAY_F_DAY_UNITS", new BigDecimal("6")))
                .add(classes.multiply(ruleParamService.get("PAY_F_CLASS_UNITS", BigDecimal.ONE)));
        return units.multiply(ruleParamService.get("PAY_UNIT_FEE", new BigDecimal("30")))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
