package com.workload.system.calc.allowance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizAllowanceItem;

/**
 * B 毕业实习酬金：分散 10 元/人；集中(不现场跟班) 15 元/人
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component
public class AllowanceBStrategy implements AllowanceCalcStrategy
{
    @Autowired
    private RuleParamService ruleParamService;

    @Override
    public String getFeeType()
    {
        return "B";
    }

    @Override
    public BigDecimal calculate(BizAllowanceItem item)
    {
        long count = item.getStudentCount() == null ? 0 : item.getStudentCount();
        String subtype = item.getFeeSubtype();
        boolean concentrated = subtype != null && subtype.contains("集中");
        // 兜底默认值须与档位对应：集中 15 / 分散 10。原先两档共用 10，
        // 一旦 PAY_B_CONCENTRATED 规则行被删或停用，集中实习会按分散价少发 1/3
        String ruleCode = concentrated ? "PAY_B_CONCENTRATED" : "PAY_B_DISPERSED";
        BigDecimal fallback = concentrated ? new BigDecimal("15") : new BigDecimal("10");
        BigDecimal perHead = ruleParamService.get(ruleCode, fallback);
        return perHead.multiply(new BigDecimal(count)).setScale(2, RoundingMode.HALF_UP);
    }
}
