package com.workload.system.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
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
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.SecurityUtils;
import com.workload.system.calc.WorkloadCalcService;
import com.workload.system.domain.*;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.mapper.*;
import com.workload.system.service.*;
import com.workload.system.service.impl.CoefficientAdjustmentServiceImpl;

/**
 * 显式以-Dtest=CoefficientApprovalTransactionIT运行；需要WFIT_TEST_DB_URL/USER/PASSWORD。
 * 所有写入只触及本连接临时表；默认单元测试不运行此IT，不静默跳过连接失败。
 * 核算替身仅用于在真实SQL写入后注入失败，不代替完整核算端到端验收。
 */
class CoefficientApprovalTransactionIT
{
    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbc;
    private WorkloadCalcService calc;
    private ICoefficientAdjustmentService service;
    private WorkloadSnapshotService snapshots;
    private SqlSessionTemplate sessions;

    @BeforeEach
    void setup() throws Exception
    {
        String url = System.getenv("WFIT_TEST_DB_URL");
        assertThat(url).as("必须显式配置本地测试数据库URL").startsWith("jdbc:mysql://127.0.0.1:").doesNotContain(":3306/");
        dataSource = new SingleConnectionDataSource(url, System.getenv("WFIT_TEST_DB_USER"),
                System.getenv("WFIT_TEST_DB_PASSWORD"), true);
        jdbc = new JdbcTemplate(dataSource);
        // 先建立所有临时影子表，随后才允许任何写入；连接关闭时MySQL自动删除。
        for (String table : List.of("biz_workload_item", "biz_wl_theory", "biz_workload_summary",
                "biz_coefficient_adjustment", "biz_workload_calc_snapshot", "biz_workload_period_lock"))
        {
            String ddl = jdbc.queryForObject("SHOW CREATE TABLE " + table, (rs, n) -> rs.getString(2));
            jdbc.execute(ddl.replaceFirst("CREATE TABLE", "CREATE TEMPORARY TABLE"));
        }
        jdbc.update("INSERT INTO biz_workload_item(id,user_id,semester,item_type,source_type,calculated_workload,status,calculation_version) VALUES(9,1003,'2025-2026-1','G1','IMPORT',32,0,3)");
        jdbc.update("INSERT INTO biz_wl_theory(item_id,J1,Q2) VALUES(9,32,1)");
        jdbc.update("INSERT INTO biz_coefficient_adjustment(id,user_id,semester,item_id,category,factor_code,requested_value,reason,status,base_calculation_version) VALUES(5,1003,'2025-2026-1',9,'G1','Q2',1.5,'test',0,3)");
        Configuration config = new Configuration(new Environment("temporary-mysql",
                new SpringManagedTransactionFactory(), dataSource));
        config.getTypeAliasRegistry().registerAliases("com.workload.system.domain");
        for (String name : List.of("BizCoefficientAdjustmentMapper", "BizWlTheoryMapper", "BizWorkloadCalcSnapshotMapper", "BizWorkloadSummaryMapper", "BizWorkloadItemMapper"))
        {
            String resource = "mapper/system/" + name + ".xml";
            try (var in = getClass().getClassLoader().getResourceAsStream(resource))
            {
                new XMLMapperBuilder(in, config, resource, config.getSqlFragments()).parse();
            }
        }
        sessions = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(config));
        snapshots = new WorkloadSnapshotService(sessions.getMapper(BizWorkloadCalcSnapshotMapper.class));
        calc = mock(WorkloadCalcService.class);
        var target = new CoefficientAdjustmentServiceImpl();
        ReflectionTestUtils.setField(target, "adjustmentMapper", sessions.getMapper(BizCoefficientAdjustmentMapper.class));
        ReflectionTestUtils.setField(target, "itemMapper", sessions.getMapper(BizWorkloadItemMapper.class));
        ReflectionTestUtils.setField(target, "workloadWriteGuard", new com.workload.system.calc.WorkloadWriteGuard(sessions.getMapper(BizWorkloadSummaryMapper.class)));
        ReflectionTestUtils.setField(target, "theoryMapper", sessions.getMapper(BizWlTheoryMapper.class));
        ReflectionTestUtils.setField(target, "workloadCalcService", calc);
        TransactionInterceptor advice = new TransactionInterceptor();
        advice.setTransactionManager(new DataSourceTransactionManager(dataSource));
        advice.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(advice);
        service = (ICoefficientAdjustmentService) proxy.getProxy();
    }

    @AfterEach
    void close()
    {
        if (dataSource != null) dataSource.destroy();
    }

    private void simulateCalculation(boolean fail)
    {
        when(calc.recalcItem(9L)).thenAnswer(invocation -> {
            assertThat(jdbc.queryForObject("SELECT status FROM biz_coefficient_adjustment WHERE id=5", Integer.class)).isEqualTo(1);
            BizWorkloadItem item = new BizWorkloadItem(); item.setId(9L);
            var factor = new FactorFormulaVo.FactorVo("Q2", new BigDecimal("1.5"), "APPROVED_OVERRIDE", "质量", "APPROVED");
            factor.setSourceRef("coefficient-adjustment:5");
            snapshots.capture(item, new FactorFormulaVo("G1", "Q2", new BigDecimal("1.5"), "测试", List.of(factor)), "test-v1");
            jdbc.update("UPDATE biz_workload_item SET calculation_version=4 WHERE id=9");
            if (fail) throw new ServiceException("injected calculation failure");
            return new BigDecimal("1.5");
        });
    }

    private void approve()
    {
        try (var security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUserId).thenReturn(1004L);
            service.approve(5L, "test approval");
        }
    }

    @Test
    void failureRollsBackFactorApprovalVersionAndSnapshot()
    {
        simulateCalculation(true);
        assertThatThrownBy(this::approve).isInstanceOf(ServiceException.class).hasMessageContaining("injected");
        assertThat(jdbc.queryForObject("SELECT Q2 FROM biz_wl_theory WHERE item_id=9", BigDecimal.class)).isEqualByComparingTo("1");
        assertThat(jdbc.queryForObject("SELECT status FROM biz_coefficient_adjustment WHERE id=5", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT calculation_version FROM biz_workload_item WHERE id=9", Long.class)).isEqualTo(3L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM biz_workload_calc_snapshot", Integer.class)).isZero();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {1, 2})
    void frozenSummaryRejectsApprovalBeforeAnyCalculation(int status)
    {
        jdbc.update("INSERT INTO biz_workload_summary(user_id,semester,status) VALUES(1003,'2025-2026-1',?)", status);
        assertThatThrownBy(this::approve).isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
        assertThat(jdbc.queryForObject("SELECT Q2 FROM biz_wl_theory WHERE item_id=9", BigDecimal.class)).isEqualByComparingTo("1");
        assertThat(jdbc.queryForObject("SELECT status FROM biz_coefficient_adjustment WHERE id=5", Integer.class)).isZero();
        verifyNoInteractions(calc);
    }

    @Test
    void staleVersionRejectsApprovalWithoutChangingFactor()
    {
        jdbc.update("UPDATE biz_workload_item SET calculation_version=4 WHERE id=9");
        assertThatThrownBy(this::approve).isInstanceOf(ServiceException.class).hasMessageContaining("并发冲突");
        assertThat(jdbc.queryForObject("SELECT Q2 FROM biz_wl_theory WHERE item_id=9", BigDecimal.class)).isEqualByComparingTo("1");
        assertThat(jdbc.queryForObject("SELECT status FROM biz_coefficient_adjustment WHERE id=5", Integer.class)).isZero();
        verifyNoInteractions(calc);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"0,true", "1,true", "0,false", "1,false"})
    void pendingApplicationOrUnreviewedItemBlocksSummaryTransition(int status, boolean pending)
    {
        if (!pending) jdbc.update("UPDATE biz_coefficient_adjustment SET status=2 WHERE id=5");
        if (pending) jdbc.update("UPDATE biz_workload_item SET status=1 WHERE id=9");
        jdbc.update("INSERT INTO biz_workload_summary(id,user_id,semester,status,needs_recalculation) VALUES(7,1003,'2025-2026-1',?,0)", status);
        var target = new com.workload.system.service.impl.BizAuditServiceImpl();
        ReflectionTestUtils.setField(target, "writeGuard", new com.workload.system.calc.WorkloadWriteGuard(sessions.getMapper(BizWorkloadSummaryMapper.class)));
        ReflectionTestUtils.setField(target, "bizWorkloadSummaryMapper", sessions.getMapper(BizWorkloadSummaryMapper.class));
        ReflectionTestUtils.setField(target, "adjustmentMapper", sessions.getMapper(BizCoefficientAdjustmentMapper.class));
        ReflectionTestUtils.setField(target, "itemMapper", sessions.getMapper(BizWorkloadItemMapper.class));
        TransactionInterceptor advice = new TransactionInterceptor();
        advice.setTransactionManager(new DataSourceTransactionManager(dataSource));
        advice.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(target); proxy.addAdvice(advice);
        BizAuditService audit = (BizAuditService) proxy.getProxy();
        try (var scope = mockStatic(com.workload.common.utils.DataScopeUtil.class))
        {
            assertThatThrownBy(() -> { if (status == 0) audit.submit(7L); else audit.approve(7L); })
                    .isInstanceOf(ServiceException.class).hasMessageContaining(pending ? "待审系数" : "未核对");
        }
        assertThat(jdbc.queryForObject("SELECT status FROM biz_workload_summary WHERE id=7", Integer.class)).isEqualTo(status);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"0,true", "1,false", "2,true", "3,false"})
    void reviewReadMatchesIncludedItemStates(int status, boolean blocked)
    {
        jdbc.update("UPDATE biz_workload_item SET status=? WHERE id=9", status);
        var mapper = sessions.getMapper(BizWorkloadItemMapper.class);
        assertThat(mapper.selectUnreviewedIdsForUpdate(1003L, "2025-2026-1").contains(9L)).isEqualTo(blocked);
        assertThat(mapper.selectUnreviewedIdsForUpdate(1004L, "2025-2026-1")).isEmpty();
        assertThat(mapper.selectUnreviewedIdsForUpdate(1003L, "2025-2026-2")).isEmpty();
    }

    @Test
    void confirmationSqlRejectsPendingChangedAndRepeatedResults()
    {
        BizWorkloadItemMapper mapper = sessions.getMapper(BizWorkloadItemMapper.class);
        assertThat(mapper.confirmIfUnchanged(9L, 3L, new BigDecimal("32"), "qa")).isZero();
        jdbc.update("UPDATE biz_coefficient_adjustment SET status=2 WHERE id=5");
        assertThat(mapper.confirmIfUnchanged(9L, 2L, new BigDecimal("32"), "qa")).isZero();
        assertThat(mapper.confirmIfUnchanged(9L, 3L, new BigDecimal("33"), "qa")).isZero();
        assertThat(mapper.confirmIfUnchanged(9L, 3L, new BigDecimal("32"), "qa")).isEqualTo(1);
        assertThat(mapper.confirmIfUnchanged(9L, 3L, new BigDecimal("32"), "qa")).isZero();
        assertThat(jdbc.queryForObject("SELECT update_by FROM biz_workload_item WHERE id=9", String.class)).isEqualTo("qa");
    }

    @Test
    void successCommitsAllAndRepeatedApprovalCannotCreateAnotherSnapshot()
    {
        simulateCalculation(false);
        approve();
        assertThat(jdbc.queryForObject("SELECT Q2 FROM biz_wl_theory WHERE item_id=9", BigDecimal.class)).isEqualByComparingTo("1.5");
        assertThat(jdbc.queryForObject("SELECT status FROM biz_coefficient_adjustment WHERE id=5", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT calculation_version FROM biz_workload_item WHERE id=9", Long.class)).isEqualTo(4L);
        assertThatThrownBy(this::approve).isInstanceOf(ServiceException.class).hasMessageContaining("不是待审");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM biz_workload_calc_snapshot", Integer.class)).isEqualTo(1);
        verify(calc, times(1)).recalcItem(9L);
    }
}
