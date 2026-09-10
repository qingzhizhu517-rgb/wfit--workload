package com.workload.system.calc.allowance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 其他酬金策略工厂：fee_type -> 策略 bean（A~G 全量注册；
 * 未注册类型返回 null，由调用方决定是否视为未启用）
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component
public class AllowanceStrategyFactory
{
    @Autowired
    private List<AllowanceCalcStrategy> strategies;

    private final Map<String, AllowanceCalcStrategy> byFeeType = new HashMap<>();

    @PostConstruct
    public void init()
    {
        for (AllowanceCalcStrategy strategy : strategies)
        {
            byFeeType.put(strategy.getFeeType(), strategy);
        }
    }

    /**
     * 取 fee_type 对应策略；未注册类型返回 null
     *
     * @param feeType 酬金类型
     * @return 策略或 null
     */
    public AllowanceCalcStrategy get(String feeType)
    {
        return byFeeType.get(feeType);
    }
}
