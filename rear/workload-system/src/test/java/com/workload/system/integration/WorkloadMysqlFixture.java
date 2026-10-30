package com.workload.system.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import com.workload.common.utils.SecurityUtils;
import com.workload.common.utils.DataScopeUtil;
import com.workload.system.calc.*;
import com.workload.system.calc.rule.*;
import com.workload.system.calc.strategy.*;
import com.workload.system.mapper.*;
import com.workload.system.service.*;
import com.workload.system.service.impl.*;

/** Dedicated random schema on the explicitly configured sandbox; never modifies the source schema. */
class WorkloadMysqlFixture implements AutoCloseable {
    final String schema = "wfit_it_" + UUID.randomUUID().toString().replace("-", "");
    final JdbcTemplate admin;
    final JdbcTemplate jdbc;
    final DriverManagerDataSource ds;
    final TransactionTemplate tx;
    final SqlSessionTemplate sessions;
    final Map<Class<?>, Object> beans = new HashMap<>();
    final WorkloadWriteGuard guard;
    final WorkloadCalcService calc;
    final SummaryCalcService summary;
    final PayCalcService pay;
    final IBizWlTheoryService theory;
    final IBizWorkloadItemService items;
    final IBizTeachingTaskService tasks;
    final ICoefficientAdjustmentService adjustments;
    final BizAuditService audit;
    final IWorkloadFactorFormulaService formulas;
    static final String SEM = "2025-2026-1";

    WorkloadMysqlFixture() throws Exception {
        String url = System.getenv("WFIT_TEST_DB_URL");
        assertThat(url).as("必须显式配置隔离实例，禁止默认端口3306").startsWith("jdbc:mysql://127.0.0.1:13306/").doesNotContain(":3306/");
        String user = System.getenv("WFIT_TEST_DB_USER"), password = System.getenv("WFIT_TEST_DB_PASSWORD");
        admin = new JdbcTemplate(new DriverManagerDataSource(url, user, password));
        admin.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4");
        String base = url.substring(0, url.indexOf('/', "jdbc:mysql://".length()) + 1);
        String params = url.contains("?") ? url.substring(url.indexOf('?')) : "";
        ds = new DriverManagerDataSource(base + schema + params, user, password);
        jdbc = new JdbcTemplate(ds);
        try {
        for (String table : List.of("biz_workload_item", "biz_wl_theory", "biz_wl_practice", "biz_wl_internship_training", "biz_wl_course_design", "biz_wl_thesis", "biz_wl_concentrated_internship", "biz_wl_management", "biz_workload_summary", "biz_coefficient_adjustment", "biz_workload_calc_snapshot", "biz_teacher_profile", "biz_teaching_task", "biz_workload_category_dict", "biz_pay_rate", "biz_pay_record", "biz_allowance_item", "biz_role_assignment", "biz_audit_log")) {
            String ddl = admin.queryForObject("SHOW CREATE TABLE " + table, (rs,n) -> rs.getString(2));
            jdbc.execute(ddl);
        }
        // A migration-added lock table is copied only when present in the verified source schema.
        if (!admin.queryForList("SHOW TABLES LIKE 'biz_workload_period_lock'").isEmpty()) {
            String lockDdl = admin.queryForObject("SHOW CREATE TABLE biz_workload_period_lock", (rs,n) -> rs.getString(2));
            jdbc.execute(lockDdl);
        }
        Configuration config = new Configuration(new Environment("isolated-mysql", new SpringManagedTransactionFactory(), ds));
        config.getTypeAliasRegistry().registerAliases("com.workload.system.domain");
        // Default SESSION cache matches production; lock reads must flush stale entries.
        for (String name : List.of("BizWorkloadItemMapper", "BizWlTheoryMapper", "BizWlPracticeMapper", "BizWlInternshipTrainingMapper", "BizWlCourseDesignMapper", "BizWlThesisMapper", "BizWlConcentratedInternshipMapper", "BizWlManagementMapper", "BizWorkloadSummaryMapper", "BizCoefficientAdjustmentMapper", "BizWorkloadCalcSnapshotMapper", "BizTeacherProfileMapper", "BizTeachingTaskMapper", "BizWorkloadCategoryDictMapper", "BizPayRateMapper", "BizPayRecordMapper", "BizAllowanceItemMapper", "BizRoleAssignmentMapper", "BizAuditLogMapper", "BizImportBatchMapper", "BizImportRowMapper")) {
            String resource = "mapper/system/" + name + ".xml";
            try (var in = getClass().getClassLoader().getResourceAsStream(resource)) { new XMLMapperBuilder(in, config, resource, config.getSqlFragments()).parse(); }
        }
        sessions = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(config));
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        guard = new WorkloadWriteGuard(mapper(BizWorkloadSummaryMapper.class)); beans.put(WorkloadWriteGuard.class, guard);
        RuleParamService rules = mock(RuleParamService.class);
        when(rules.get(anyString(), any(BigDecimal.class), org.mockito.ArgumentMatchers.eq(java.time.LocalDate.of(2026, 1, 31)))).thenAnswer(i -> i.getArgument(1));
        when(rules.resolve(anyString(), org.mockito.ArgumentMatchers.eq(java.time.LocalDate.of(2026,1,31)), any(BigDecimal.class), anyString()))
                .thenAnswer(i -> RuleResolution.fallback(i.getArgument(0),i.getArgument(1),i.getArgument(2),i.getArgument(3),"NO_EFFECTIVE_RULE"));
        beans.put(RuleParamService.class, rules);
        beans.put(SemesterCalendar.class, new SemesterCalendar());
        beans.put(ComplianceChecker.class, wire(new ComplianceChecker()));
        TheoryCalcStrategy strategy = wire(new TheoryCalcStrategy());
        CalcStrategyFactory factory = new CalcStrategyFactory();
        ReflectionTestUtils.setField(factory, "strategyMap", Map.of("theoryCalcStrategy", strategy));
        ReflectionTestUtils.setField(factory, "categoryDictMapper", mapper(BizWorkloadCategoryDictMapper.class));
        ReflectionTestUtils.setField(factory, "strategyCache", new StrategyCache());
        beans.put(CalcStrategyFactory.class, factory);
        beans.put(WorkloadSnapshotService.class, new WorkloadSnapshotService(mapper(BizWorkloadCalcSnapshotMapper.class)));
        formulas = wire(new WorkloadFactorFormulaServiceImpl()); beans.put(IWorkloadFactorFormulaService.class, formulas);
        pay = proxy(wire(new PayCalcServiceImpl())); beans.put(PayCalcService.class, pay);
        summary = proxy(wire(new SummaryCalcServiceImpl())); beans.put(SummaryCalcService.class, summary);
        calc = proxy(wire(new WorkloadCalcServiceImpl())); beans.put(WorkloadCalcService.class, calc);
        theory = proxy(wire(new BizWlTheoryServiceImpl()));
        items = proxy(wire(new BizWorkloadItemServiceImpl()));
        tasks = proxy(wire(new BizTeachingTaskServiceImpl()));
        adjustments = proxy(wire(new CoefficientAdjustmentServiceImpl()));
        audit = proxy(wire(new BizAuditServiceImpl()));
        seed();
        } catch (Exception | AssertionError failure) { admin.execute("DROP DATABASE `" + schema + "`"); throw failure; }
    }
    <T> T mapper(Class<T> type) { return sessions.getMapper(type); }
    <T> T wire(T target) {
        for (Field f : target.getClass().getDeclaredFields()) {
            if (!f.isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class)) continue;
            Object bean = beans.get(f.getType());
            if (f.getType().getPackageName().equals("com.workload.system.mapper")) bean = mapper(f.getType());
            // Non-invoked orchestration dependencies only; all invoked calculation/mapper services are real.
            if (bean == null) bean = mock(f.getType());
            ReflectionTestUtils.setField(target, f.getName(), bean);
        }
        return target;
    }
    @SuppressWarnings("unchecked") <T> T proxy(Object target) {
        TransactionInterceptor advice = new TransactionInterceptor();
        advice.setTransactionManager(new DataSourceTransactionManager(ds));
        advice.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory pf = new ProxyFactory(target); pf.addAdvice(advice); return (T) pf.getProxy();
    }
    void seed() {
        jdbc.update("INSERT INTO biz_teacher_profile(user_id,title,teacher_nature) VALUES(1003,'讲师','专任')");
        jdbc.update("INSERT INTO biz_workload_category_dict(type_code,type_name,parent_group,calc_strategy) VALUES('G1','理论','G7','theoryCalcStrategy')");
        jdbc.update("INSERT INTO biz_workload_item(id,user_id,semester,item_type,source_type,course_name,calculated_workload,status,calculation_version) VALUES(9,1003,?,'G1','IMPORT','数学',32,0,0)", SEM);
        jdbc.update("INSERT INTO biz_wl_theory(item_id,J1,C1,K1,Q1,Q2,N) VALUES(9,32,1,1,1,1,1)");
    }
    long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    long summaryId() { return jdbc.queryForObject("SELECT id FROM biz_workload_summary WHERE user_id=1003", Long.class); }
    static <T> T authenticated(Callable<T> work) throws Exception {
        try (var security = mockStatic(SecurityUtils.class); var scope = mockStatic(DataScopeUtil.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(1004L); security.when(SecurityUtils::getUsername).thenReturn("integration");
            security.when(() -> SecurityUtils.hasPermi(anyString())).thenReturn(true);
            scope.when(() -> DataScopeUtil.resolveUserId(any())).thenAnswer(i -> i.getArgument(0));
            return work.call();
        }
    }
    public void close() { admin.execute("DROP DATABASE `" + schema + "`"); }
}
