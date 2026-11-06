package com.workload.framework.mybatis;

import com.workload.system.domain.*;
import com.workload.system.mapper.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Real production plugin + real mapper XML; only owns a freshly random schema on guarded 13306. */
class BizAuditFieldMysqlIT {
    String schema;
    JdbcTemplate admin, jdbc;
    SqlSessionFactory factory;
    static java.time.LocalDateTime sqlTime(Date date) { return date==null?null:java.time.LocalDateTime.ofInstant(date.toInstant(),ZoneId.systemDefault()); }
    final Date now=Date.from(Instant.parse("2026-11-06T00:00:00Z"));
    @BeforeEach void setup() throws Exception {
        String url=System.getenv("WFIT_TEST_DB_URL");
        assertNotNull(url,"Explicit isolated MySQL required"); assertTrue(url.startsWith("jdbc:mysql://127.0.0.1:13306/"));
        String user=System.getenv("WFIT_TEST_DB_USER"), password=System.getenv("WFIT_TEST_DB_PASSWORD");
        admin=new JdbcTemplate(new DriverManagerDataSource(url,user,password));
        schema="wfit_it_"+UUID.randomUUID().toString().replace("-","");
        admin.execute("CREATE DATABASE `"+schema+"` CHARACTER SET utf8mb4");
        String base=url.substring(0,url.indexOf('/',"jdbc:mysql://".length())+1);
        var ds=new DriverManagerDataSource(base+schema+(url.contains("?")?url.substring(url.indexOf('?')):""),user,password);
        jdbc=new JdbcTemplate(ds);
        try {
            for(String table:List.of("biz_pay_rate","biz_audit_log","biz_workload_summary","biz_pay_record","biz_workload_item","biz_wl_theory","biz_workload_calc_snapshot","biz_workload_settlement_snapshot")) {
                String ddl=admin.queryForObject("SHOW CREATE TABLE "+table,(rs,n)->rs.getString(2));
                jdbc.execute(ddl);
            }
            Configuration config=new Configuration(new Environment("audit-real-mysql",new JdbcTransactionFactory(),ds));
            config.getTypeAliasRegistry().registerAliases("com.workload.system.domain");
            config.addInterceptor(new BizAuditFieldInterceptor(()->"trusted",Clock.fixed(now.toInstant(),ZoneOffset.UTC)));
            for(String mapper:List.of("BizPayRateMapper","BizAuditLogMapper","BizWorkloadSummaryMapper","BizPayRecordMapper","BizWorkloadItemMapper","BizWlTheoryMapper","BizWorkloadCalcSnapshotMapper","BizWorkloadSettlementSnapshotMapper")) {
                String resource="mapper/system/"+mapper+".xml";
                try(var input=getClass().getClassLoader().getResourceAsStream(resource)) {
                    new XMLMapperBuilder(input,config,resource,config.getSqlFragments()).parse();
                }
            }
            factory=new SqlSessionFactoryBuilder().build(config);
        } catch(Exception | AssertionError failure) { cleanup(); throw failure; }
    }
    @AfterEach void cleanup() {
        if(schema!=null && schema.matches("wfit_it_[a-f0-9]{32}")) { admin.execute("DROP DATABASE `"+schema+"`"); schema=null; }
    }
    @Test void directInsertAndUpdateKeepNullableAndFixedCreationHistory() {
        try(SqlSession session=factory.openSession(true)) {
            BizPayRate rate=new BizPayRate(); rate.setTitle("audit"); rate.setRate(BigDecimal.TEN); rate.setCreateBy("forged"); rate.setCreateTime(new Date(0));
            rate.setEffectiveFrom(LocalDate.of(2000,1,2)); session.getMapper(BizPayRateMapper.class).insertBizPayRate(rate);
            var row=jdbc.queryForMap("select * from biz_pay_rate where id=?",rate.getId()); assertEquals("trusted",row.get("create_by")); assertEquals(sqlTime(now),row.get("create_time")); assertEquals(sqlTime(now),row.get("update_time"));
            for(boolean nullHistory:List.of(false,true)) {
                jdbc.update("update biz_pay_rate set create_by=?,create_time=? where id=?",nullHistory?null:"history",nullHistory?null:new Date(100000),rate.getId());
                rate.setCreateBy("forged"); rate.setCreateTime(new Date(0)); rate.setUpdateBy("forged");
                session.getMapper(BizPayRateMapper.class).updateBizPayRate(rate);
                row=jdbc.queryForMap("select * from biz_pay_rate where id=?",rate.getId());
                assertEquals(nullHistory?null:"history",row.get("create_by")); assertEquals(nullHistory?null:sqlTime(new Date(100000)),row.get("create_time")); assertEquals("trusted",row.get("update_by"));
                assertEquals(LocalDate.of(2000,1,2),session.getMapper(BizPayRateMapper.class).selectBizPayRateById(rate.getId()).getEffectiveFrom());
            }
        }
    }
    @Test void scalarForgedUpdateByAndAllScalarSqlActuallyPersistBothAuditFields() {
        jdbc.update("insert into biz_workload_summary(id,user_id,semester,status) values(1,1003,'2097-2098-1',0)");
        try(SqlSession session=factory.openSession(true)) {
            var mapper=session.getMapper(BizWorkloadSummaryMapper.class);
            assertEquals(1,mapper.updateStatusIf(1L,0,1,"forged"));
            var row=jdbc.queryForMap("select * from biz_workload_summary where id=1"); assertEquals("trusted",row.get("update_by")); assertEquals(sqlTime(now),row.get("update_time"));
            assertEquals(1,mapper.teacherConfirmById(1L,1003L,"business-sign"));
            row=jdbc.queryForMap("select * from biz_workload_summary where id=1"); assertEquals("business-sign",row.get("teacher_sign")); assertEquals("trusted",row.get("update_by")); assertNotEquals(sqlTime(now),row.get("teacher_sign_time"));
            Map<String,Object> forged=new HashMap<>(Map.of("id",1L,"snapshotId",4L,"_auditFields",Map.of("by","forged")));
            assertThrows(Exception.class,()->session.update("com.workload.system.mapper.BizWorkloadSummaryMapper.updateSettlementPointer",forged));
            assertEquals(1,mapper.updateSettlementPointer(1L,4L));
            assertEquals("trusted",jdbc.queryForObject("select update_by from biz_workload_summary where id=1",String.class));
        }
    }
    @Test void namedEntityAndCoefficientUpdateUseRealPlugin() {
        jdbc.update("insert into biz_workload_summary(id,user_id,semester,status) values(1,1003,'2097-2098-1',0)");
        jdbc.update("insert into biz_pay_record(id,user_id,semester,summary_id) values(1,1003,'2097-2098-1',1)");
        jdbc.update("insert into biz_workload_item(id,user_id,semester,item_type,source_type,calculation_version,calculated_workload) values(1,1003,'2097-2098-1','G1','TASK',0,0)");
        jdbc.update("insert into biz_wl_theory(item_id,J1) values(1,32)");
        try(SqlSession session=factory.openSession(true)) {
            BizPayRecord record=new BizPayRecord(); record.setId(1L);record.setUserId(1003L);record.setSemester("2097-2098-1");record.setSummaryId(1L);record.setTotalPay(12L);record.setCreateBy("forged");
            assertEquals(1,session.getMapper(BizPayRecordMapper.class).updateIfSummaryDraft(record));
            assertEquals("trusted",jdbc.queryForObject("select update_by from biz_pay_record where id=1",String.class));
            assertEquals(1,session.getMapper(BizWlTheoryMapper.class).updateC1IfVersion(1L,BigDecimal.TEN,0L));
            assertEquals("trusted",jdbc.queryForObject("select update_by from biz_wl_theory where item_id=1",String.class)); assertEquals(now,jdbc.queryForObject("select update_time from biz_wl_theory where item_id=1",Date.class));
        }
    }
    @Test void auditLogMapsFourAuditFieldsAndDoesNotRewriteBusinessOperator() {
        try(SqlSession session=factory.openSession(true)) {
            BizAuditLog log=new BizAuditLog();log.setSummaryId(1L);log.setAction("submit");log.setOperatorId(77L);log.setOperatorName("trusted-business-operator");log.setCreateTime(new Date(0));
            session.getMapper(BizAuditLogMapper.class).insertBizAuditLog(log);
            var row=jdbc.queryForMap("select * from biz_audit_log where id=?",log.getId());
            assertEquals("trusted-business-operator",row.get("operator_name")); assertEquals("trusted",row.get("create_by")); assertEquals("trusted",row.get("update_by")); assertEquals(sqlTime(now),row.get("create_time"));assertEquals(sqlTime(now),row.get("update_time"));
        }
    }

    @Test void standaloneScalarXmlUsesServerFactoryAndRejectsReservedForgery() {
        var config=new Configuration(factory.getConfiguration().getEnvironment());
        config.getTypeAliasRegistry().registerAliases("com.workload.system.domain");
        String resource="mapper/system/BizWorkloadSummaryMapper.xml";
        try(var input=getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input,config,resource,config.getSqlFragments()).parse();
        } catch(java.io.IOException e) { throw new RuntimeException(e); }
        jdbc.update("insert into biz_workload_summary(id,user_id,semester,status) values(1,1003,'2097-2098-1',0)");
        try(var session=new SqlSessionFactoryBuilder().build(config).openSession(true)) {
            assertEquals(1,session.getMapper(BizWorkloadSummaryMapper.class).updateStatusIf(1L,0,1,"forged"));
            assertEquals("SYSTEM",jdbc.queryForObject("select update_by from biz_workload_summary where id=1",String.class));
            assertThrows(Exception.class,()->session.update("com.workload.system.mapper.BizWorkloadSummaryMapper.updateSettlementPointer",Map.of("id",1L,"snapshotId",1L,"_auditFields",Map.of("by","forged"))));
        }
    }
    @Test void existingCalculationAndSettlementEvidenceKeepTheirDedicatedHistoricalCreator() {
        try(SqlSession session=factory.openSession(true)) {
            Date historical=new Date(100000);
            BizWorkloadCalcSnapshot calc=new BizWorkloadCalcSnapshot();calc.setItemId(1L);calc.setCalculationVersion(1L);calc.setFormulaExpression("saved");calc.setFactorJson("{}");calc.setSourceJson("{}");calc.setContextJson("{}");calc.setRuleVersion("saved");calc.setResult(BigDecimal.ONE);calc.setSnapshotHash("a".repeat(64));calc.setCreatedBy("original-source");calc.setCreatedAt(historical);
            session.getMapper(BizWorkloadCalcSnapshotMapper.class).insertBizWorkloadCalcSnapshot(calc);
            assertEquals("original-source",session.getMapper(BizWorkloadCalcSnapshotMapper.class).selectLatestByItemId(1L).getCreatedBy());
            assertEquals(historical,session.getMapper(BizWorkloadCalcSnapshotMapper.class).selectLatestByItemId(1L).getCreatedAt());
            BizWorkloadSettlementSnapshot settlement=new BizWorkloadSettlementSnapshot();settlement.setSummaryId(1L);settlement.setUserId(1003L);settlement.setSemester("2097-2098-1");settlement.setSettlementVersion(1L);settlement.setFormatVersion(1);settlement.setPayloadJson("{}");settlement.setHash("b".repeat(64));settlement.setCreatedBy("original-source");settlement.setCreatedAt(historical);
            session.getMapper(BizWorkloadSettlementSnapshotMapper.class).insert(settlement);
            assertEquals("original-source",session.getMapper(BizWorkloadSettlementSnapshotMapper.class).selectById(settlement.getId()).getCreatedBy());
            assertEquals(historical,session.getMapper(BizWorkloadSettlementSnapshotMapper.class).selectById(settlement.getId()).getCreatedAt());
        }
    }
}
