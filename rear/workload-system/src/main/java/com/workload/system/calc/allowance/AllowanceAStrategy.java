package com.workload.system.calc.allowance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizAllowanceItem;

/**
 * A 重修辅导酬金：自学辅导 &lt;6 人 120 元、6-20 人 260 元；
 * 跟班/单独开班首期按手工金额（待正式文件，策略原样返回录入值）
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component
public class AllowanceAStrategy implements AllowanceCalcStrategy
{
    @Autowired
    private RuleParamService ruleParamService;

    @Override
    public String getFeeType()
    {
        return "A";
    }

    @Override
    public BigDecimal calculate(BizAllowanceItem item)
    {
        String subtype = item.getFeeSubtype();
        if (subtype != null && subtype.contains("自学"))
        {
            long count = item.getStudentCount() == null ? 0 : item.getStudentCount();
            String ruleCode = count < 6 ? "PAY_A_SELF_LT6" : "PAY_A_SELF_6_20";
            return scale(ruleParamService.get(ruleCode, new BigDecimal(count < 6 ? "120" : "260")));
        }
        // 跟班/单独开班：手工金额
        return scale(item.getAmount() == null ? BigDecimal.ZERO : item.getAmount());
    }

    private BigDecimal scale(BigDecimal value)
    {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
