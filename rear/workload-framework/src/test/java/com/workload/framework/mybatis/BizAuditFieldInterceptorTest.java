package com.workload.framework.mybatis;

import com.workload.system.domain.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.builder.StaticSqlSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BizAuditFieldInterceptorTest {
    final Date now = Date.from(Instant.parse("2026-11-06T00:00:00Z"));
    final BizAuditFieldInterceptor audit = new BizAuditFieldInterceptor(() -> "trusted", Clock.fixed(now.toInstant(), ZoneOffset.UTC));
    Object call(String name, Object parameter, boolean fail) throws Throwable {
        return callUsing(audit,name,parameter,fail);
    }
    Object callUsing(BizAuditFieldInterceptor plugin, String name, Object parameter, boolean fail) throws Throwable {
        MappedStatement ms = new MappedStatement.Builder(new Configuration(), "com.workload.system.mapper." + name,
                new StaticSqlSource(new Configuration(), "update t set a=1"), name.contains("insert") ? SqlCommandType.INSERT : SqlCommandType.UPDATE).build();
        Executor executor = (Executor) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Executor.class}, (p,m,a) -> {
            if (fail) throw new IllegalStateException("db failure");
            return 1;
        });
        return plugin.intercept(new Invocation(executor, Executor.class.getMethod("update", MappedStatement.class, Object.class), new Object[]{ms,parameter}));
    }
    @Test void insertOverridesFourFieldsAndKeepsBusinessDates() throws Throwable {
        BizTeachingTask item = new BizTeachingTask(); item.setCreateBy("forged"); item.setCreateTime(new Date(0)); item.setImportTime(new Date(1234));
        call("BizTeachingTaskMapper.insertBizTeachingTask", item, false);
        assertEquals("trusted", item.getCreateBy()); assertEquals(now, item.getCreateTime());
        assertEquals("trusted", item.getUpdateBy()); assertEquals(now,item.getUpdateTime()); assertEquals(new Date(1234),item.getImportTime());
    }
    @Test void updateKeepsNullAndFixedCreationHistory() throws Throwable {
        BizWorkloadItem item = new BizWorkloadItem(); item.setCreateBy("old"); item.setCreateTime(new Date(0));
        call("BizWorkloadItemMapper.updateBizWorkloadItem",item,false);
        assertEquals("old",item.getCreateBy()); assertEquals(new Date(0),item.getCreateTime()); assertEquals("trusted",item.getUpdateBy());
        BizWorkloadItem nullHistory=new BizWorkloadItem(); call("BizWorkloadItemMapper.updateBizWorkloadItem",nullHistory,false); assertNull(nullHistory.getCreateTime());
    }
    @Test void excludedStatementDoesNotMutate() throws Throwable {
        BizWorkloadItem item = new BizWorkloadItem(); call("OtherMapper.insertBizWorkloadItem",item,false); assertNull(item.getCreateTime());
    }
    @Test void namedEntityGetsAuditedAndReservedInputRejected() throws Throwable {
        BizPayRecord record=new BizPayRecord(); Map<String,Object> map=new HashMap<>(); map.put("record",record); map.put("param1",record);
        call("BizPayRecordMapper.updateIfSummaryDraft",map,false); assertEquals("trusted",record.getUpdateBy()); assertFalse(map.containsKey("_auditFields"));
        map.put("_auditFields","forged"); assertThrows(IllegalArgumentException.class,()->call("BizPayRecordMapper.updateIfSummaryDraft",map,false));
    }
    @Test void unknownMapAndWrongEntityFailClosed() {
        assertThrows(IllegalArgumentException.class,()->call("BizPayRecordMapper.updateIfSummaryDraft",Map.of("anything",new BizPayRecord()),false));
        assertThrows(IllegalArgumentException.class,()->call("BizPayRateMapper.insertBizPayRate",new BizTeachingTask(),false));
        assertThrows(IllegalArgumentException.class,()->call("BizPayRecordMapper.updateSettlementPointer",Map.of("id",1L,"snapshotId",2L,"list",List.of(new BizPayRecord())),false));
    }
    @Test void failureRemovesOnlyOwnedBindingAndNextCallUsesFreshUser() throws Throwable {
        Map<String,Object> map=new HashMap<>(Map.of("id",1L,"snapshotId",2L));
        assertThrows(Throwable.class,()->call("BizPayRecordMapper.updateSettlementPointer",map,true));
        assertFalse(map.containsKey("_auditFields")); call("BizPayRecordMapper.updateSettlementPointer",map,false); assertFalse(map.containsKey("_auditFields"));
    }
    @Test void collectionArrayAndIdentityCyclesAreBounded() throws Throwable {
        BizPayRate first=new BizPayRate(),second=new BizPayRate();
        List<Object> list=new ArrayList<>();list.add(first);list.add(new BizPayRate[]{second,first});list.add(list);
        call("BizPayRateMapper.insertBizPayRate",list,false);
        assertEquals("trusted",first.getCreateBy());assertEquals("trusted",second.getUpdateBy());
    }
    @Test void snapshotAndPeriodStatementsRemainOutsideAllowlist() {
        assertFalse(BizAuditFieldInterceptor.statementIds().contains("com.workload.system.mapper.BizWorkloadSummaryMapper.lockPeriod"));
        assertTrue(BizAuditFieldInterceptor.statementIds().stream().noneMatch(id->id.contains("Snapshot")||id.contains("ReviewBatch")||id.contains("Offline")));
    }
    @Test void sharedFactoryRejectsFakeReservedMapAndDefendsDateMutability() {
        assertThrows(IllegalArgumentException.class,()->com.workload.common.core.domain.audit.AuditFields.resolve(Map.of("_auditFields",Map.of("by","forged"))));
        assertThrows(IllegalArgumentException.class,()->com.workload.common.core.domain.audit.AuditFields.resolve(Map.of("_auditBy","forged")));
        var stamp=com.workload.common.core.domain.audit.AuditFields.of("trusted",Clock.fixed(now.toInstant(),ZoneOffset.UTC));
        stamp.getTime().setTime(0);assertEquals(now,stamp.getTime());
    }
    @Test void reusedThreadHasNoAuditStateAndCallerSecurityContextIsPreserved() throws Exception {
        var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            assertEquals("alice",executor.submit(()->{
                var user=new com.workload.common.core.domain.entity.SysUser();user.setUserName("alice");
                var login=new com.workload.common.core.domain.model.LoginUser(7L,1L,user,Set.of());
                var context=org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login,null,List.of()));
                org.springframework.security.core.context.SecurityContextHolder.setContext(context);
                try {
                    var plugin=new BizAuditFieldInterceptor();
                    BizPayRate inserted=new BizPayRate();
                    assertThrows(Throwable.class,()->callUsing(plugin,"BizPayRateMapper.insertBizPayRate",inserted,true));
                    assertEquals("alice",inserted.getCreateBy());
                    assertEquals("alice",com.workload.common.core.domain.audit.AuditFields.current().getBy());
                    assertSame(context,org.springframework.security.core.context.SecurityContextHolder.getContext());
                    return com.workload.common.core.domain.audit.AuditFields.current().getBy();
                } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
            }).get());
            assertEquals("SYSTEM",executor.submit(()->{
                BizPayRate next=new BizPayRate();
                try { callUsing(new BizAuditFieldInterceptor(),"BizPayRateMapper.insertBizPayRate",next,false); }
                catch(Throwable error) { throw new RuntimeException(error); }
                return next.getCreateBy();
            }).get());
        } finally { executor.shutdownNow(); }
    }
}
