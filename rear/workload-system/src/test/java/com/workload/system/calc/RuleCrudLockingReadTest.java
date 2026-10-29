package com.workload.system.calc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizWorkloadRule;
import com.workload.system.mapper.BizWorkloadRuleMapper;
import com.workload.system.service.impl.BizWorkloadRuleServiceImpl;

class RuleCrudLockingReadTest {
    @ParameterizedTest @ValueSource(strings={"update","delete"})
    void singleWriteInvalidatesCodeFromCurrentLockingRead(String operation) {
        var rules=mock(RuleParamService.class);
        var mapper=mock(BizWorkloadRuleMapper.class,inv -> {
            if(inv.getMethod().getName().equals("selectBizWorkloadRuleById"))
                throw new AssertionError("规则变更须先当前锁读，不能用旧事务快照取code");
            if(inv.getMethod().getName().equals("selectBizWorkloadRuleByIdForUpdate")) {var r=new BizWorkloadRule();r.setRuleCode("CURRENT_B");return r;}
            if(inv.getMethod().getReturnType().equals(int.class))return 1;
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(inv);
        });
        var service=service(mapper,rules);var update=new BizWorkloadRule();update.setId(7L);update.setRuleCode("NEW_C");
        if("update".equals(operation))service.updateBizWorkloadRule(update);else service.deleteBizWorkloadRuleById(7L);
        verify(rules).evict("CURRENT_B");
        if("update".equals(operation))verify(rules).evict("NEW_C");
    }
    @Test void batchLockOrderIsSortedWithoutMutatingClientArray() {
        var rules=mock(RuleParamService.class);List<Long> locked=new ArrayList<>();
        var mapper=mock(BizWorkloadRuleMapper.class,inv -> {
            if(inv.getMethod().getName().startsWith("selectBizWorkloadRuleById")) {locked.add(inv.getArgument(0));var r=new BizWorkloadRule();r.setRuleCode("R"+inv.getArgument(0));return r;}
            if(inv.getMethod().getReturnType().equals(int.class))return 3;
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(inv);
        });
        Long[] ids={3L,1L,2L};service(mapper,rules).deleteBizWorkloadRuleByIds(ids);
        assertThat(locked).as("批量旧行须按稳定次序锁定，避免反序批次死锁").containsExactly(1L,2L,3L);
        assertThat(ids).containsExactly(3L,1L,2L);verify(rules).evict("R1");verify(rules).evict("R2");verify(rules).evict("R3");
    }
    @Test void lockingStatementFlushesMybatisSessionCacheAndUsesForUpdate() throws Exception {
        var config=new Configuration();config.getTypeAliasRegistry().registerAlias(BizWorkloadRule.class);
        String resource="mapper/system/BizWorkloadRuleMapper.xml";
        try(var input=getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input,config,resource,config.getSqlFragments()).parse();
        }
        String id=BizWorkloadRuleMapper.class.getName()+".selectBizWorkloadRuleByIdForUpdate";
        assertThat(config.hasStatement(id)).as("规则写入前需要专用当前锁读语句").isTrue();
        var statement=config.getMappedStatement(id);assertThat(statement.isFlushCacheRequired()).isTrue();
        assertThat(statement.getBoundSql(7L).getSql().toLowerCase(Locale.ROOT)).contains("for update");
    }
    private BizWorkloadRuleServiceImpl service(BizWorkloadRuleMapper mapper,RuleParamService rules) {
        var s=new BizWorkloadRuleServiceImpl();ReflectionTestUtils.setField(s,"bizWorkloadRuleMapper",mapper);ReflectionTestUtils.setField(s,"ruleParamService",rules);return s;
    }
}
