package com.workload.system.calc.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.workload.system.domain.BizWorkloadCategoryDict;
import com.workload.system.mapper.BizWorkloadCategoryDictMapper;

/**
 * 计算策略工厂：按类别字典 calc_strategy 配置解析 Spring bean（结果缓存）
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component
public class CalcStrategyFactory
{
    /** bean 名 -> 策略实例（Spring 按 bean 名注入） */
    @Autowired
    private Map<String, WorkloadCalcStrategy> strategyMap;

    @Autowired
    private BizWorkloadCategoryDictMapper categoryDictMapper;

    /** typeCode -> 策略（null 值用 NULL 占位表示该类别无策略） */
    private final Map<String, WorkloadCalcStrategy> resolvedCache = new ConcurrentHashMap<>();

    private static final WorkloadCalcStrategy NULL = new WorkloadCalcStrategy()
    {
        @Override
        public String getTypeCode()
        {
            return "";
        }

        @Override
        public java.math.BigDecimal calculate(com.workload.system.domain.BizWorkloadItem item)
        {
            return null;
        }
    };

    /**
     * 取类别对应策略；无策略类别（G7/G8/G9/G10）返回 null
     *
     * @param typeCode 类别代码
     * @return 策略或 null
     */
    public WorkloadCalcStrategy get(String typeCode)
    {
        WorkloadCalcStrategy cached = resolvedCache.get(typeCode);
        if (cached != null)
        {
            return cached == NULL ? null : cached;
        }
        WorkloadCalcStrategy resolved = resolve(typeCode);
        resolvedCache.put(typeCode, resolved == null ? NULL : resolved);
        return resolved;
    }

    private WorkloadCalcStrategy resolve(String typeCode)
    {
        BizWorkloadCategoryDict dict = categoryDictMapper.selectBizWorkloadCategoryDictByTypeCode(typeCode);
        if (dict == null || !StringUtils.hasText(dict.getCalcStrategy()))
        {
            return null;
        }
        return strategyMap.get(dict.getCalcStrategy());
    }
}
