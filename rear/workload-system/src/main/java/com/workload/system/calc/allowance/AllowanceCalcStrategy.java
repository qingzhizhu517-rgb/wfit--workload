package com.workload.system.calc.allowance;

import java.math.BigDecimal;
import com.workload.system.domain.BizAllowanceItem;

/**
 * 其他酬金（A~G）计算策略，与 G 类别策略同模式
 *
 * @author wflg
 * @date 2026-07-21
 */
public interface AllowanceCalcStrategy
{
    /**
     * 酬金类型（A/B/C/E/F/G）
     *
     * @return 类型代码
     */
    public String getFeeType();

    /**
     * 计算酬金金额（scale=2 HALF_UP）
     *
     * @param item 其他酬金明细
     * @return 金额
     */
    public BigDecimal calculate(BizAllowanceItem item);
}
