package com.workload.system.calc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import com.workload.system.calc.rule.RuleParamServiceImpl;
import com.workload.system.domain.BizWorkloadRule;
import com.workload.system.mapper.BizWorkloadRuleMapper;
import com.workload.common.core.redis.RedisCache;

class SemesterRuleDateTest {
    @ParameterizedTest
    @ValueSource(strings={"2025-2027-1", "2025-2026-3", "2025-2026-0", "2025-2026-1-extra", "2025-2026-2x"})
    void rejectsInvalidAcademicYearOrTerm(String semester) {
        assertThatThrownBy(() -> new SemesterCalendar().rangeOf(semester)).hasMessageContaining("学期");
    }
    @Test void configuredSemesterEndIsUsed() {
        SemesterCalendar calendar = new SemesterCalendar(); calendar.setAutumnEnd("02-10");
        assertThat(calendar.rangeOf("2025-2026-1")[1]).isEqualTo(LocalDate.of(2026,2,10));
    }
    @Test void historicalDateSeparatesCacheAndIncludesEffectiveToDay() throws Exception {
        var service = new RuleParamServiceImpl();
        var mapper = mock(BizWorkloadRuleMapper.class);
        var redis = mock(RedisCache.class);
        Map<String,Object> cache = new HashMap<>();
        when(redis.getCacheObject(anyString())).thenAnswer(i -> cache.get(i.getArgument(0)));
        doAnswer(i -> { cache.put(i.getArgument(0),i.getArgument(1)); return null; }).when(redis).setCacheObject(anyString(),any(),anyInt(),any());
        when(mapper.selectBizWorkloadRuleList(any())).thenReturn(List.of(rule("1","2026-01-01","2026-01-31"),rule("2","2026-02-01",null)));
        ReflectionTestUtils.setField(service,"bizWorkloadRuleMapper",mapper);
        ReflectionTestUtils.setField(service,"redisCache",redis);
        assertThat(Arrays.stream(service.getClass().getMethods()).anyMatch(m -> m.getName().equals("get") && Arrays.equals(m.getParameterTypes(),new Class[]{String.class,LocalDate.class})))
            .as("显式日期API必须存在，不能让历史学期使用运行当天规则").isTrue();
        var method = service.getClass().getMethod("get",String.class,LocalDate.class);
        assertThat((BigDecimal)method.invoke(service,"R",LocalDate.of(2026,1,31))).isEqualByComparingTo("1");
        assertThat((BigDecimal)method.invoke(service,"R",LocalDate.of(2026,2,1))).isEqualByComparingTo("2");
        assertThat((BigDecimal)method.invoke(service,"R",LocalDate.of(2026,1,31))).isEqualByComparingTo("1");
        verify(mapper,times(2)).selectBizWorkloadRuleList(any());
    }
    private BizWorkloadRule rule(String value,String from,String to) {
        var r=new BizWorkloadRule(); r.setRuleCode("R"); r.setRuleValue(new BigDecimal(value));
        r.setEffectiveFrom(LocalDate.parse(from)); r.setEffectiveTo(to==null?null:LocalDate.parse(to)); return r;
    }
}
