package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 计算策略基类：空值安全与统一舍入
 *
 * @author wflg
 * @date 2026-07-21
 */
public abstract class AbstractWorkloadCalcStrategy implements WorkloadCalcStrategy
{
    /** 系数空值按 1 处理（不参与缩放） */
    protected BigDecimal coef(BigDecimal value)
    {
        return value == null ? BigDecimal.ONE : value;
    }

    /** 数量空值按 0 处理 */
    protected BigDecimal num(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 数量空值按 0 处理（Long 转 BigDecimal） */
    protected BigDecimal num(Long value)
    {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value);
    }

    /** 中间结果连乘 */
    protected BigDecimal mul(BigDecimal first, BigDecimal... rest)
    {
        BigDecimal result = first;
        for (BigDecimal factor : rest)
        {
            result = result.multiply(factor);
        }
        return result;
    }

    /** 最终结果统一 scale=2 HALF_UP */
    protected BigDecimal scale(BigDecimal value)
    {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
