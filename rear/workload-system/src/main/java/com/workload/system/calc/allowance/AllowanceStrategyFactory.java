package com.workload.system.calc.allowance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 其他酬金策略工厂：fee_type -> 策略 bean（D 代阅卷首期未注册，取不到即未启用）
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
     * 取 fee_type 对应策略；未启用类型（如 D）返回 null
     *
     * @param feeType 酬金类型
     * @return 策略或 null
     */
    public AllowanceCalcStrategy get(String feeType)
    {
        return byFeeType.get(feeType);
    }
}
