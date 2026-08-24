package com.workload.system.calc.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.workload.system.domain.BizWorkloadCategoryDict;
import com.workload.system.mapper.BizWorkloadCategoryDictMapper;

/**
 * CalcStrategyFactory 单元测试
 * 
 * @author MiMo
 * @date 2026-08-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CalcStrategyFactory 单元测试")
class CalcStrategyFactoryTest {

    @InjectMocks
    private CalcStrategyFactory calcStrategyFactory;

    @Mock
    private StrategyCache strategyCache;

    @Mock
    private BizWorkloadCategoryDictMapper categoryDictMapper;

    @Mock
    private Map<String, WorkloadCalcStrategy> strategyMap;

    @BeforeEach
    void setUp() {
        // 初始化策略映射
        strategyMap = new HashMap<>();
        
        // 注入模拟的 strategyMap
        try {
            java.lang.reflect.Field field = CalcStrategyFactory.class.getDeclaredField("strategyMap");
            field.setAccessible(true);
            field.set(calcStrategyFactory, strategyMap);
        } catch (Exception e) {
            throw new RuntimeException("注入 strategyMap 失败", e);
        }
    }

    @Test
    @DisplayName("应该通过缓存获取策略")
    void shouldGetStrategyThroughCache() {
        // Given
        String typeCode = "G1";
        WorkloadCalcStrategy expectedStrategy = createMockStrategy(typeCode);
        
        when(strategyCache.get(eq(typeCode), any())).thenReturn(expectedStrategy);

        // When
        WorkloadCalcStrategy result = calcStrategyFactory.get(typeCode);

        // Then
        assertThat(result).isEqualTo(expectedStrategy);
        verify(strategyCache).get(eq(typeCode), any());
    }

    @Test
    @DisplayName("应该返回 null 当类别无策略时")
    void shouldReturnNullWhenNoStrategy() {
        // Given
        String typeCode = "G7";
        
        when(strategyCache.get(eq(typeCode), any())).thenReturn(null);

        // When
        WorkloadCalcStrategy result = calcStrategyFactory.get(typeCode);

        // Then
        assertThat(result).isNull();
        verify(strategyCache).get(eq(typeCode), any());
    }

    @Test
    @DisplayName("应该从数据库解析策略")
    void shouldResolveStrategyFromDatabase() {
        // Given
        String typeCode = "G1";
        String strategyBeanName = "theoryCalcStrategy";
        WorkloadCalcStrategy expectedStrategy = createMockStrategy(typeCode);
        
        BizWorkloadCategoryDict categoryDict = new BizWorkloadCategoryDict();
        categoryDict.setCalcStrategy(strategyBeanName);
        
        when(categoryDictMapper.selectBizWorkloadCategoryDictByTypeCode(typeCode))
            .thenReturn(categoryDict);
        
        strategyMap.put(strategyBeanName, expectedStrategy);

        // When - 调用私有方法 resolve 通过策略缓存
        // 由于我们模拟了 strategyCache，我们需要测试 resolve 方法的逻辑
        // 这里我们通过反射调用私有方法进行测试
        try {
            java.lang.reflect.Method resolveMethod = CalcStrategyFactory.class.getDeclaredMethod("resolve", String.class);
            resolveMethod.setAccessible(true);
            
            WorkloadCalcStrategy result = (WorkloadCalcStrategy) resolveMethod.invoke(calcStrategyFactory, typeCode);
            
            // Then
            assertThat(result).isEqualTo(expectedStrategy);
            verify(categoryDictMapper).selectBizWorkloadCategoryDictByTypeCode(typeCode);
        } catch (Exception e) {
            throw new RuntimeException("测试 resolve 方法失败", e);
        }
    }

    @Test
    @DisplayName("应该返回 null 当数据库中无类别配置时")
    void shouldReturnNullWhenNoCategoryInDatabase() {
        // Given
        String typeCode = "G1";
        
        when(categoryDictMapper.selectBizWorkloadCategoryDictByTypeCode(typeCode))
            .thenReturn(null);

        // When
        try {
            java.lang.reflect.Method resolveMethod = CalcStrategyFactory.class.getDeclaredMethod("resolve", String.class);
            resolveMethod.setAccessible(true);
            
            WorkloadCalcStrategy result = (WorkloadCalcStrategy) resolveMethod.invoke(calcStrategyFactory, typeCode);
            
            // Then
            assertThat(result).isNull();
            verify(categoryDictMapper).selectBizWorkloadCategoryDictByTypeCode(typeCode);
        } catch (Exception e) {
            throw new RuntimeException("测试 resolve 方法失败", e);
        }
    }

    @Test
    @DisplayName("应该返回 null 当策略 Bean 名称为空时")
    void shouldReturnNullWhenStrategyBeanNameIsEmpty() {
        // Given
        String typeCode = "G1";
        
        BizWorkloadCategoryDict categoryDict = new BizWorkloadCategoryDict();
        categoryDict.setCalcStrategy(""); // 空策略名称
        
        when(categoryDictMapper.selectBizWorkloadCategoryDictByTypeCode(typeCode))
            .thenReturn(categoryDict);

        // When
        try {
            java.lang.reflect.Method resolveMethod = CalcStrategyFactory.class.getDeclaredMethod("resolve", String.class);
            resolveMethod.setAccessible(true);
            
            WorkloadCalcStrategy result = (WorkloadCalcStrategy) resolveMethod.invoke(calcStrategyFactory, typeCode);
            
            // Then
            assertThat(result).isNull();
            verify(categoryDictMapper).selectBizWorkloadCategoryDictByTypeCode(typeCode);
        } catch (Exception e) {
            throw new RuntimeException("测试 resolve 方法失败", e);
        }
    }

    @Test
    @DisplayName("应该抛出异常当配置的策略 Bean 不存在时")
    void shouldThrowWhenStrategyBeanNotFound() {
        // Given
        String typeCode = "G1";
        String strategyBeanName = "nonExistentStrategy";

        BizWorkloadCategoryDict categoryDict = new BizWorkloadCategoryDict();
        categoryDict.setCalcStrategy(strategyBeanName);

        when(categoryDictMapper.selectBizWorkloadCategoryDictByTypeCode(typeCode))
            .thenReturn(categoryDict);

        // strategyMap 中没有这个 Bean —— 属配置错误，必须抛异常而非静默返回 null

        // When / Then：通过反射调用私有 resolve，期望其内部抛出 ServiceException
        java.lang.reflect.Method resolveMethod;
        try {
            resolveMethod = CalcStrategyFactory.class.getDeclaredMethod("resolve", String.class);
            resolveMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("找不到 resolve 方法", e);
        }

        assertThatThrownBy(() -> resolveMethod.invoke(calcStrategyFactory, typeCode))
            .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
            .hasCauseInstanceOf(com.workload.common.exception.ServiceException.class);
        verify(categoryDictMapper).selectBizWorkloadCategoryDictByTypeCode(typeCode);
    }

    @Test
    @DisplayName("应该处理多个类别的策略解析")
    void shouldHandleMultipleTypeCodes() {
        // Given
        String typeCode1 = "G1";
        String typeCode2 = "G2";
        WorkloadCalcStrategy strategy1 = createMockStrategy(typeCode1);
        WorkloadCalcStrategy strategy2 = createMockStrategy(typeCode2);
        
        when(strategyCache.get(eq(typeCode1), any())).thenReturn(strategy1);
        when(strategyCache.get(eq(typeCode2), any())).thenReturn(strategy2);

        // When
        WorkloadCalcStrategy result1 = calcStrategyFactory.get(typeCode1);
        WorkloadCalcStrategy result2 = calcStrategyFactory.get(typeCode2);

        // Then
        assertThat(result1).isEqualTo(strategy1);
        assertThat(result2).isEqualTo(strategy2);
        verify(strategyCache).get(eq(typeCode1), any());
        verify(strategyCache).get(eq(typeCode2), any());
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