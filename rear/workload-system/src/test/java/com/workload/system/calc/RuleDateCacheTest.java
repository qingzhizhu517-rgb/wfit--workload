package com.workload.system.calc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.workload.common.core.redis.RedisCache;
import com.workload.system.calc.rule.RuleParamServiceImpl;
import com.workload.system.domain.BizWorkloadRule;
import com.workload.system.mapper.BizWorkloadRuleMapper;
import com.workload.system.service.impl.BizWorkloadRuleServiceImpl;

class RuleDateCacheTest {
    private final LocalDate jan = LocalDate.of(2026,1,31), jul = LocalDate.of(2026,7,15);
    private RuleParamServiceImpl service;
    private BizWorkloadRuleMapper mapper;
    private RedisCache redis;
    private Map<String,Object> cache;
    @BeforeEach void setup() {
        service=new RuleParamServiceImpl(); mapper=mock(BizWorkloadRuleMapper.class); redis=mock(RedisCache.class);
        cache=new HashMap<>();
        when(redis.getCacheObject(anyString())).thenAnswer(i -> cache.get(i.getArgument(0)));
        doAnswer(i -> {cache.put(i.getArgument(0),i.getArgument(1));return null;}).when(redis).setCacheObject(anyString(),any());
        doAnswer(i -> {cache.put(i.getArgument(0),i.getArgument(1));return null;}).when(redis).setCacheObject(anyString(),any(),anyInt(),any());
        ReflectionTestUtils.setField(service,"bizWorkloadRuleMapper",mapper);
        ReflectionTestUtils.setField(service,"redisCache",redis);
    }
    @AfterEach void cleanup() { if(TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization(); TransactionSynchronizationManager.setActualTransactionActive(false); }
    @Test void cachesEachDateAndEvictsAllDates() {
        when(mapper.selectBizWorkloadRuleList(any())).thenReturn(List.of(rule("R","1","2026-01-01",null)));
        assertThat(service.get("R",jan)).isEqualByComparingTo("1");
        assertThat(service.get("R",jul)).isEqualByComparingTo("1");
        when(mapper.selectBizWorkloadRuleList(any())).thenReturn(List.of(rule("R","2","2026-01-01",null)));
        service.evict("R");
        assertThat(service.get("R",jan)).isEqualByComparingTo("2");
        assertThat(service.get("R",jul)).isEqualByComparingTo("2");
        verify(mapper,times(4)).selectBizWorkloadRuleList(any());
    }
    @Test void expiryHasInclusiveDayAndMissingHistoricalRuleDoesNotUseCurrentVersion() {
        when(mapper.selectBizWorkloadRuleList(any())).thenReturn(List.of(rule("R","1","2026-01-01","2026-01-31"),rule("R","9","2026-02-01",null)));
        assertThat(service.get("R",jan)).isEqualByComparingTo("1");
        assertThat(service.get("R",new BigDecimal("3"),LocalDate.of(2025,12,31))).isEqualByComparingTo("3");
        assertThatThrownBy(() -> service.get("R",LocalDate.of(2025,12,31))).hasMessageContaining("缺失");
        assertThat(service.get("R",LocalDate.of(2026,2,1))).isEqualByComparingTo("9");
    }
    @Test void rejectsAmbiguousLatestStartEvenIfValuesMatchAndMapperOrderChanges() {
        var a=rule("R","1","2026-01-01",null); var b=rule("R","1","2026-01-01",null);
        when(mapper.selectBizWorkloadRuleList(any())).thenReturn(List.of(a,b),List.of(b,a));
        assertThatThrownBy(() -> service.get("R",jan)).hasMessageContaining("冲突");
        assertThatThrownBy(() -> service.get("R",jan)).hasMessageContaining("冲突");
    }
    @Test void doesNotRejectDuplicateOlderStartsWhenLatestVersionIsUnambiguous() {
        when(mapper.selectBizWorkloadRuleList(any())).thenReturn(List.of(rule("R","1","2025-01-01",null),rule("R","2","2025-01-01",null),rule("R","3","2026-01-01",null)));
        assertThat(service.get("R",jan)).isEqualByComparingTo("3");
    }
    @Test void transactionReadsBypassSharedCacheAndNeverPublishUncommittedValues() {
        when(mapper.selectBizWorkloadRuleList(any())).thenReturn(List.of(rule("R","1","2026-01-01",null)));
        service.get("R",jan); clearInvocations(redis);
        begin();
        when(mapper.selectBizWorkloadRuleList(any())).thenReturn(List.of(rule("R","2","2026-01-01",null)));
        assertThat(service.get("R",jan)).isEqualByComparingTo("2");
        verifyNoInteractions(redis);
        cleanup();
        assertThat(service.get("R",jan)).isEqualByComparingTo("1");
    }
    @Test void evictionRunsOnlyAfterCommitAndRollbackKeepsPriorCache() {
        when(mapper.selectBizWorkloadRuleList(any())).thenReturn(List.of(rule("R","1","2026-01-01",null)));
        service.get("R",jan); clearInvocations(redis); begin(); service.evict("R");
        verifyNoInteractions(redis);
        cleanup();
        assertThat(service.get("R",jan)).isEqualByComparingTo("1");
        begin(); service.evict("R"); commit();
        verify(redis).setCacheObject(eq("wl_rule:R:generation"),anyString());
    }
    @Test void staleFillStartedBeforeEvictionCannotPoisonNewGeneration() {
        AtomicInteger reads=new AtomicInteger();
        when(mapper.selectBizWorkloadRuleList(any())).thenAnswer(i -> {
            if(reads.getAndIncrement()==0) {service.evict("R"); return List.of(rule("R","1","2026-01-01",null));}
            return List.of(rule("R","2","2026-01-01",null));
        });
        assertThat(service.get("R",jan)).isEqualByComparingTo("1");
        assertThat(service.get("R",jan)).isEqualByComparingTo("2");
        assertThat(service.get("R",jan)).isEqualByComparingTo("2");
        assertThat(reads.get()).isEqualTo(2);
    }
    @Test void crudCodeRenameInvalidatesOldAndNewCodesOnlyAfterCommit() {
        var crud=new BizWorkloadRuleServiceImpl();
        ReflectionTestUtils.setField(crud,"bizWorkloadRuleMapper",mapper); ReflectionTestUtils.setField(crud,"ruleParamService",service);
        var old=rule("OLD","1","2026-01-01",null);old.setId(1L);
        var updated=rule("NEW","2","2026-01-01",null);updated.setId(1L);
        when(mapper.selectBizWorkloadRuleByIdForUpdate(1L)).thenReturn(old);
        when(mapper.updateBizWorkloadRule(updated)).thenReturn(1);
        begin(); crud.updateBizWorkloadRule(updated);
        verify(redis,never()).setCacheObject(anyString(),any());
        commit();
        verify(redis).setCacheObject(eq("wl_rule:OLD:generation"),anyString());
        verify(redis).setCacheObject(eq("wl_rule:NEW:generation"),anyString());
    }
    @Test void insertAndBothDeletePathsInvalidateAllCodeDatesAfterSuccessfulWrite() {
        var crud=new BizWorkloadRuleServiceImpl();
        ReflectionTestUtils.setField(crud,"bizWorkloadRuleMapper",mapper); ReflectionTestUtils.setField(crud,"ruleParamService",service);
        var old=rule("R","1","2026-01-01",null);old.setId(1L);
        when(mapper.selectBizWorkloadRuleByIdForUpdate(1L)).thenReturn(old);
        when(mapper.insertBizWorkloadRule(old)).thenReturn(1); when(mapper.deleteBizWorkloadRuleById(1L)).thenReturn(1);
        when(mapper.deleteBizWorkloadRuleByIds(any())).thenReturn(1);
        begin();crud.insertBizWorkloadRule(old);crud.deleteBizWorkloadRuleById(1L);crud.deleteBizWorkloadRuleByIds(new Long[]{1L});
        verify(redis,never()).setCacheObject(anyString(),any());commit();
        verify(redis,times(3)).setCacheObject(eq("wl_rule:R:generation"),anyString());
    }
    private void begin() {TransactionSynchronizationManager.initSynchronization();TransactionSynchronizationManager.setActualTransactionActive(true);}
    private void commit() {var callbacks=TransactionSynchronizationManager.getSynchronizations();cleanup();callbacks.forEach(c -> c.afterCommit());}
    private BizWorkloadRule rule(String code,String value,String from,String to) {
        var r=new BizWorkloadRule(); r.setRuleCode(code);r.setRuleValue(new BigDecimal(value));r.setEffectiveFrom(LocalDate.parse(from));r.setEffectiveTo(to==null?null:LocalDate.parse(to));return r;
    }
}
