package com.workload.system.calc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.workload.system.calc.strategy.*;
import com.workload.system.domain.BizWorkloadCategoryDict;
import com.workload.system.mapper.BizWorkloadCategoryDictMapper;
import com.workload.system.service.impl.BizWorkloadCategoryDictServiceImpl;

class CategoryStrategyInvalidationTest {
    @AfterEach void cleanup() { if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.clearSynchronization();TransactionSynchronizationManager.setActualTransactionActive(false); }
    @ParameterizedTest @ValueSource(strings={"insert","update","delete","batchDelete"})
    void dictionaryWritesInvalidateBoundStrategyOnlyAfterCommit(String operation) {
        var mapper=mock(BizWorkloadCategoryDictMapper.class);var cache=new StrategyCache();var factory=new CalcStrategyFactory();var crud=new BizWorkloadCategoryDictServiceImpl();
        var old=mock(WorkloadCalcStrategy.class);var replacement=mock(WorkloadCalcStrategy.class);
        AtomicReference<BizWorkloadCategoryDict> row=new AtomicReference<>(dict("old"));
        when(mapper.selectBizWorkloadCategoryDictByTypeCode("G4")).thenAnswer(i -> row.get());
        ReflectionTestUtils.setField(factory,"categoryDictMapper",mapper);ReflectionTestUtils.setField(factory,"strategyMap",Map.of("old",old,"new",replacement));ReflectionTestUtils.setField(factory,"strategyCache",cache);
        ReflectionTestUtils.setField(crud,"bizWorkloadCategoryDictMapper",mapper);ReflectionTestUtils.setField(crud,"strategyCache",cache);
        assertThat(factory.get("G4")).isSameAs(old);
        TransactionSynchronizationManager.initSynchronization();TransactionSynchronizationManager.setActualTransactionActive(true);
        switch(operation) {
            case "insert" -> {when(mapper.insertBizWorkloadCategoryDict(any())).thenReturn(1);crud.insertBizWorkloadCategoryDict(dict("new"));}
            case "update" -> {when(mapper.updateBizWorkloadCategoryDict(any())).thenReturn(1);crud.updateBizWorkloadCategoryDict(dict("new"));}
            case "delete" -> {when(mapper.deleteBizWorkloadCategoryDictByTypeCode("G4")).thenReturn(1);crud.deleteBizWorkloadCategoryDictByTypeCode("G4");}
            default -> {when(mapper.deleteBizWorkloadCategoryDictByTypeCodes(any())).thenReturn(1);crud.deleteBizWorkloadCategoryDictByTypeCodes(new String[]{"G4"});}
        }
        assertThat(cache.size()).isEqualTo(1);
        var callbacks=TransactionSynchronizationManager.getSynchronizations();cleanup();
        row.set(operation.contains("elete")?null:dict("new"));callbacks.forEach(c -> c.afterCommit());
        assertThat(cache.size()).isZero();assertThat(factory.get("G4")).isSameAs(row.get()==null?null:replacement);
    }
    @Test void transactionReadsDoNotFillCacheAndRollbackKeepsOldBinding() {
        var cache=new StrategyCache();var old=mock(WorkloadCalcStrategy.class);var uncommitted=mock(WorkloadCalcStrategy.class);
        cache.get("G4",() -> old);
        TransactionSynchronizationManager.initSynchronization();TransactionSynchronizationManager.setActualTransactionActive(true);
        assertThat(cache.get("G4",() -> uncommitted)).isSameAs(uncommitted);
        cleanup();assertThat(cache.get("G4",() -> uncommitted)).isSameAs(old);
    }
    private BizWorkloadCategoryDict dict(String bean) {var d=new BizWorkloadCategoryDict();d.setTypeCode("G4");d.setCalcStrategy(bean);return d;}
}
