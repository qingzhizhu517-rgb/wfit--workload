package com.workload.system.calc.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * StrategyCache 单元测试
 * 
 * @author MiMo
 * @date 2026-08-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StrategyCache 单元测试")
class StrategyCacheTest {

    private StrategyCache strategyCache;

    @Mock
    private Supplier<WorkloadCalcStrategy> mockLoader;

    @BeforeEach
    void setUp() {
        strategyCache = new StrategyCache();
    }

    @Test
    @DisplayName("应该缓存策略实例")
    void shouldCacheStrategyInstance() {
        // Given
        WorkloadCalcStrategy mockStrategy = createMockStrategy("G1");
        when(mockLoader.get()).thenReturn(mockStrategy);

        // When - 第一次调用
        WorkloadCalcStrategy result1 = strategyCache.get("G1", mockLoader);
        
        // Then
        assertThat(result1).isEqualTo(mockStrategy);
        verify(mockLoader, times(1)).get();

        // When - 第二次调用（应该从缓存获取）
        WorkloadCalcStrategy result2 = strategyCache.get("G1", mockLoader);
        
        // Then
        assertThat(result2).isEqualTo(mockStrategy);
        verify(mockLoader, times(1)).get(); // 仍然只调用一次
    }

    @Test
    @DisplayName("应该缓存 null 值（使用占位符）")
    void shouldCacheNullValueWithPlaceholder() {
        // Given
        when(mockLoader.get()).thenReturn(null);

        // When - 第一次调用
        WorkloadCalcStrategy result1 = strategyCache.get("G7", mockLoader);
        
        // Then
        assertThat(result1).isNull();
        verify(mockLoader, times(1)).get();

        // When - 第二次调用（应该从缓存获取 null）
        WorkloadCalcStrategy result2 = strategyCache.get("G7", mockLoader);
        
        // Then
        assertThat(result2).isNull();
        verify(mockLoader, times(1)).get(); // 仍然只调用一次
    }

    @Test
    @DisplayName("应该支持不同类别代码")
    void shouldSupportDifferentTypeCodes() {
        // Given
        WorkloadCalcStrategy strategyG1 = createMockStrategy("G1");
        WorkloadCalcStrategy strategyG2 = createMockStrategy("G2");
        
        when(mockLoader.get()).thenReturn(strategyG1).thenReturn(strategyG2);

        // When
        WorkloadCalcStrategy result1 = strategyCache.get("G1", mockLoader);
        WorkloadCalcStrategy result2 = strategyCache.get("G2", mockLoader);

        // Then
        assertThat(result1).isEqualTo(strategyG1);
        assertThat(result2).isEqualTo(strategyG2);
        verify(mockLoader, times(2)).get();
    }

    @Test
    @DisplayName("应该支持驱逐缓存")
    void shouldSupportEviction() {
        // Given
        WorkloadCalcStrategy mockStrategy = createMockStrategy("G1");
        when(mockLoader.get()).thenReturn(mockStrategy);

        // When - 第一次调用
        strategyCache.get("G1", mockLoader);
        
        // 驱逐缓存
        strategyCache.evict("G1");
        
        // When - 第二次调用（应该重新加载）
        strategyCache.get("G1", mockLoader);

        // Then
        verify(mockLoader, times(2)).get();
    }

    @Test
    @DisplayName("应该支持清空所有缓存")
    void shouldSupportClearAll() {
        // Given
        WorkloadCalcStrategy strategyG1 = createMockStrategy("G1");
        WorkloadCalcStrategy strategyG2 = createMockStrategy("G2");
        
        when(mockLoader.get()).thenReturn(strategyG1).thenReturn(strategyG2);

        // When - 缓存两个策略
        strategyCache.get("G1", mockLoader);
        strategyCache.get("G2", mockLoader);
        
        // 清空缓存
        strategyCache.clear();
        
        // When - 再次获取（应该重新加载）
        strategyCache.get("G1", mockLoader);
        strategyCache.get("G2", mockLoader);

        // Then
        verify(mockLoader, times(4)).get();
    }

    @Test
    @DisplayName("应该返回正确的缓存大小")
    void shouldReturnCorrectSize() {
        // Given
        WorkloadCalcStrategy strategyG1 = createMockStrategy("G1");
        WorkloadCalcStrategy strategyG2 = createMockStrategy("G2");
        
        when(mockLoader.get()).thenReturn(strategyG1).thenReturn(strategyG2);

        // When
        strategyCache.get("G1", mockLoader);
        assertThat(strategyCache.size()).isEqualTo(1);
        
        strategyCache.get("G2", mockLoader);
        assertThat(strategyCache.size()).isEqualTo(2);
        
        strategyCache.evict("G1");
        assertThat(strategyCache.size()).isEqualTo(1);
        
        strategyCache.clear();
        assertThat(strategyCache.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("应该处理加载器抛出异常的情况")
    void shouldHandleLoaderException() {
        // Given
        when(mockLoader.get()).thenThrow(new RuntimeException("加载失败"));

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            strategyCache.get("G1", mockLoader);
        });
        
        // 验证缓存为空（异常不应该被缓存）
        assertThat(strategyCache.size()).isEqualTo(0);
    }

    /**
     * 创建模拟策略对象
     */
    private WorkloadCalcStrategy createMockStrategy(String typeCode) {
        return new WorkloadCalcStrategy() {
            @Override
            public String getTypeCode() {
                return typeCode;
            }

            @Override
            public BigDecimal calculate(com.workload.system.domain.BizWorkloadItem item) {
                return BigDecimal.ONE;
            }
        };
    }
}