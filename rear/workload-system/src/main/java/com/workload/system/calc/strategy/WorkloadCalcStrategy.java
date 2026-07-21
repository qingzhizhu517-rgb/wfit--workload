package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import com.workload.system.domain.BizWorkloadItem;

/**
 * 工作量类别计算策略（G1~G6/G11，bean 名对应 biz_workload_category_dict.calc_strategy）
 *
 * @author wflg
 * @date 2026-07-21
 */
public interface WorkloadCalcStrategy
{
    /**
     * 类别代码（G1/G2/...）
     *
     * @return 类别代码
     */
    public String getTypeCode();

    /**
     * 按类别明细计算工作量（中间不截断，返回 scale=2 HALF_UP）
     *
     * @param item 工作量明细主表
     * @return 核算工作量
     */
    public BigDecimal calculate(BizWorkloadItem item);

    /**
     * 计算完成后的回调（如 G5/G6 置 is_over_limit 超标标记），默认空实现
     *
     * @param item 工作量明细主表
     * @param value 本次计算值
     */
    default void afterCalculated(BizWorkloadItem item, BigDecimal value)
    {
    }
}
