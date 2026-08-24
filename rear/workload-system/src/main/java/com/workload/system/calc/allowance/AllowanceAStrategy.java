package com.workload.system.calc.allowance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;
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
            // <6 人 120 元；6~20 人（不含 20）260 元；≥20 人单独开班，按手工金额
            if (count < 6)
            {
                return scale(ruleParamService.get("PAY_A_SELF_LT6", new BigDecimal("120")));
            }
            if (count < 20)
            {
                return scale(ruleParamService.get("PAY_A_SELF_6_20", new BigDecimal("260")));
            }
            // ≥20 人：单独开班按基本工作量标准，取录入的手工金额；
            // 未录入金额属数据缺失，抛异常而非静默按 0 发放（原按 260 计算的自动兜底已失效）
            if (item.getAmount() == null)
            {
                throw new ServiceException("自学辅导人数≥20需单独开班并录入酬金金额，当前金额为空 (feeSubtype=" + subtype + ")");
            }
            return scale(item.getAmount());
        }
        // 跟班/单独开班：手工金额
        return scale(item.getAmount() == null ? BigDecimal.ZERO : item.getAmount());
    }

    private BigDecimal scale(BigDecimal value)
    {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
