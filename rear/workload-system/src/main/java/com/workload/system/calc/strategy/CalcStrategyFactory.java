package com.workload.system.calc.strategy;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(CalcStrategyFactory.class);
    /** bean 名 -> 策略实例（Spring 按 bean 名注入） */
    @Autowired
    private Map<String, WorkloadCalcStrategy> strategyMap;

    @Autowired
    private BizWorkloadCategoryDictMapper categoryDictMapper;

    @Autowired
    private StrategyCache strategyCache;



    /**
     * 取类别对应策略；无策略类别（G7/G8/G9/G10）返回 null
     *
     * @param typeCode 类别代码
     * @return 策略或 null
     */
    /**
     * 获取指定类别代码的计算策略
     * 
     * @param typeCode 类别代码（如 G1、G2 等）
     * @return 计算策略实例，如果该类别无策略则返回 null
     */
    public WorkloadCalcStrategy get(String typeCode)
    {
        return strategyCache.get(typeCode, () -> resolve(typeCode));
    }

    private WorkloadCalcStrategy resolve(String typeCode)
    {
        log.debug("解析类别 {} 的计算策略", typeCode);
        
        BizWorkloadCategoryDict dict = categoryDictMapper.selectBizWorkloadCategoryDictByTypeCode(typeCode);
        if (dict == null) {
            log.warn("未找到类别配置: {}", typeCode);
            return null;
        }
        
        String strategyBeanName = dict.getCalcStrategy();
        if (!StringUtils.hasText(strategyBeanName)) {
            log.debug("类别 {} 未配置计算策略", typeCode);
            return null;
        }
        
        WorkloadCalcStrategy strategy = strategyMap.get(strategyBeanName);
        if (strategy == null) {
            log.error("未找到策略Bean: {} (类别: {})", strategyBeanName, typeCode);
        }
        
        return strategy;
    }
}
