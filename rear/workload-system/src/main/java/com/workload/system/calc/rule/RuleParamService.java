package com.workload.system.calc.rule;

import java.math.BigDecimal;

/**
 * 工作量规则参数读取服务（带缓存，按有效期取当期值）
 *
 * @author wflg
 * @date 2026-07-21
 */
public interface RuleParamService
{
    /**
     * 取规则参数当期生效值，查不到抛 ServiceException
     *
     * @param ruleCode 规则编码
     * @return 参数值
     */
    public BigDecimal get(String ruleCode);

    /**
     * 取规则参数当期生效值，查不到返回默认值
     *
     * @param ruleCode 规则编码
     * @param defaultValue 默认值
     * @return 参数值
     */
    public BigDecimal get(String ruleCode, BigDecimal defaultValue);

    /**
     * 使指定规则编码缓存失效
     *
     * @param ruleCode 规则编码
     */
    public void evict(String ruleCode);
}
