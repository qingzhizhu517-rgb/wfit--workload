package com.workload.system.calc.allowance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizAllowanceItem;

/**
 * A 重修辅导酬金：自学辅导 &lt;6 人 120 元、6~19 人 260 元；
 * ≥20 人按第十五条1(2) 应单独开班、按教师基本工作量标准（G1 路线）计，
 * 本项不再计绩效酬金，金额归零（防与理论课重复计酬）；
 * 跟班重修按手工金额（办法未给自动档位）。
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
            // <6 人 120 元；6~19 人 260 元（第十五条1(3)）
            if (count < 6)
            {
                return scale(ruleParamService.get("PAY_A_SELF_LT6", new BigDecimal("120")));
            }
            if (count < 20)
            {
                return scale(ruleParamService.get("PAY_A_SELF_6_20", new BigDecimal("260")));
            }
            // ≥20 人：第十五条1(2) 应单独开班、按基本工作量标准计算，
            // 工作量与酬金走 G1 那条路，本项归零防重复计酬（2026-09-10 按办法原文修正，
            // 原「按手工金额」无条文依据，且可绕过 260 元档位上限）
            return BigDecimal.ZERO;
        }
        // 跟班/单独开班：手工金额
        return scale(item.getAmount() == null ? BigDecimal.ZERO : item.getAmount());
    }

    private BigDecimal scale(BigDecimal value)
    {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
