package com.workload.system.calc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.workload.system.calc.rule.*;
import com.workload.system.domain.BizWorkloadRule;
import com.workload.system.mapper.BizWorkloadRuleMapper;
import org.springframework.test.util.ReflectionTestUtils;

class PolicyResolutionTest {
    private final LocalDate date = LocalDate.of(2026, 1, 31);
    private BizWorkloadRule row() {
        BizWorkloadRule r = new BizWorkloadRule(); r.setId(1L); r.setRuleCode("X");
        r.setRuleValue(new BigDecimal("0.4")); r.setStatus(1); r.setRuleDesc("原文");
        r.setEffectiveFrom(LocalDate.of(2025, 1, 1)); return r;
    }
    @Test void immutableContentTracksDatesAndDescriptionRatherThanIdOrValue() {
        BizWorkloadRule row = row(); RuleResolution first = RuleResolution.database(row, date);
        row.setRuleDesc("修订说明"); RuleResolution next = RuleResolution.database(row, date);
        assertThat(first.content().description()).isEqualTo("原文");
        assertThat(first.contentHash()).isNotEqualTo(next.contentHash());
        row.setEffectiveTo(date); assertThat(RuleResolution.database(row,date).contentHash()).isNotEqualTo(next.contentHash());
        first.verify();
    }
    @Test void contextResolvesOnceAndRejectsConflictingDefaults() {
        AtomicInteger calls = new AtomicInteger();
        CalculationPolicyContext c = new CalculationPolicyContext(date, (code,d,f,p) -> {
            calls.incrementAndGet(); return RuleResolution.fallback(code, d, f, p, "NO_EFFECTIVE_RULE");
        });
        assertThat(c.value("X", BigDecimal.ONE)).isEqualByComparingTo("1");
        assertThat(c.value("X", new BigDecimal("1.0"))).isEqualByComparingTo("1");
        assertThat(calls).hasValue(1);
        assertThatThrownBy(() -> c.value("X", BigDecimal.TEN)).isInstanceOf(IllegalArgumentException.class);
        assertThat(c.policySetHash()).matches("[0-9a-f]{64}");
        assertThat(c.sources()).hasSize(1);
        assertThatThrownBy(() -> c.sources().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void actualResolverCopiesDatabaseRowAndExplicitlyRecordsFallback() {
        BizWorkloadRuleMapper mapper = mock(BizWorkloadRuleMapper.class);
        RuleParamServiceImpl service = new RuleParamServiceImpl();
        ReflectionTestUtils.setField(service,"bizWorkloadRuleMapper",mapper);
        when(mapper.selectBizWorkloadRuleList(any())).thenReturn(List.of(row()));
        RuleResolution db = service.resolve("X",date,BigDecimal.ONE,"计算默认");
        assertThat(db.sourceKind()).isEqualTo("DB_RULE");
        assertThat(db.content().id()).isEqualTo(1L); assertThat(db.value()).isEqualByComparingTo("0.4");
        when(mapper.selectBizWorkloadRuleList(any())).thenReturn(List.of());
        RuleResolution fallback = service.resolve("X",date,BigDecimal.ONE,"计算默认");
        assertThat(fallback.sourceKind()).isEqualTo("CODE_FALLBACK");
        assertThat(fallback.requestedCode()).isEqualTo("X");
        assertThat(fallback.missingReason()).isNotBlank(); assertThat(fallback.fallbackPurpose()).isEqualTo("计算默认");
        assertThatThrownBy(() -> service.resolve("X",date)).hasMessageContaining("缺失");
    }
}
