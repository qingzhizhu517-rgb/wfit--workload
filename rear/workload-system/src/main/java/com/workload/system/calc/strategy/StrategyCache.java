package com.workload.system.calc.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * 策略缓存管理器：缓存计算策略实例，避免重复解析
 * 
 * @author MiMo
 * @date 2026-08-18
 */
@Component
public class StrategyCache {
    
    /** 缓存映射：typeCode -> 策略实例（NULL_PLACEHOLDER 表示无策略） */
    private final Map<String, WorkloadCalcStrategy> cache = new ConcurrentHashMap<>();
    
    /** 空策略占位符，用于缓存 null 值 */
    private static final WorkloadCalcStrategy NULL_PLACEHOLDER = new WorkloadCalcStrategy() {
        @Override
        public String getTypeCode() {
            return "";
        }
        
        @Override
        public java.math.BigDecimal calculate(com.workload.system.domain.BizWorkloadItem item) {
            return null;
        }
    };
    
    /**
     * 获取策略实例，如果缓存未命中则通过加载器加载
     * 
     * @param typeCode 类别代码
     * @param loader 策略加载器（当缓存未命中时调用）
     * @return 策略实例，如果无策略则返回 null
     */
    public WorkloadCalcStrategy get(String typeCode, Supplier<WorkloadCalcStrategy> loader) {
        WorkloadCalcStrategy cached = cache.get(typeCode);
        if (cached != null) {
            return cached == NULL_PLACEHOLDER ? null : cached;
        }
        
        // 缓存未命中，通过加载器加载
        WorkloadCalcStrategy strategy = loader.get();
        
        // 存入缓存（null 值用占位符表示）
        cache.put(typeCode, strategy == null ? NULL_PLACEHOLDER : strategy);
        
        return strategy;
    }
    
    /**
     * 驱逐指定类别的缓存
     * 
     * @param typeCode 类别代码
     */
    public void evict(String typeCode) {
        cache.remove(typeCode);
    }
    
    /**
     * 清空所有缓存
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * 获取缓存大小（用于监控）
     * 
     * @return 缓存条目数
     */
    public int size() {
        return cache.size();
    }
}