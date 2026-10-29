package com.workload.system.calc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.WebDataBinder;
import tools.jackson.databind.json.JsonMapper;
import com.workload.common.core.controller.BaseController;
import com.workload.system.calc.rule.EffectiveDateResolver;
import com.workload.system.domain.BizPayRate;
import com.workload.system.domain.BizWorkloadRule;

/** 同时覆盖 HTTP JSON、GET 参数、真实 MyBatis 映射及 JDBC DATE 边界。 */
class EffectiveDateTimezoneBoundaryTest {
    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);
    private static final String JSON = "{\"effectiveFrom\":\"2026-01-01\",\"effectiveTo\":\"2026-01-31\"}";
    private final TimeZone originalZone = TimeZone.getDefault();

    @AfterEach void restoreTimezone() { TimeZone.setDefault(originalZone); }

    static Stream<Arguments> timezonesAndDomains() {
        return Stream.of("UTC", "Asia/Tokyo", "America/Los_Angeles", "Asia/Shanghai")
                .flatMap(zone -> Stream.of(BizWorkloadRule.class, BizPayRate.class)
                        .map(type -> Arguments.of(zone, type)));
    }

    @ParameterizedTest @MethodSource("timezonesAndDomains")
    void jsonInputRetainsInclusiveEffectiveToDay(String zone, Class<?> type) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        Object row = jsonMapper().readValue(JSON, type);
        Object selected;
        if (row instanceof BizWorkloadRule rule) {
            selected = EffectiveDateResolver.select(List.of(rule), TO,
                    BizWorkloadRule::getEffectiveFrom, BizWorkloadRule::getEffectiveTo, "R");
        } else {
            BizPayRate rate = (BizPayRate) row;
            selected = EffectiveDateResolver.select(List.of(rate), TO,
                    BizPayRate::getEffectiveFrom, BizPayRate::getEffectiveTo, "费率");
        }
        assertThat(selected).as("JSON 有效期终止日包含当天，不受 JVM 时区影响").isSameAs(row);
    }

    @ParameterizedTest @MethodSource("timezonesAndDomains")
    void jdbcDateMappingSerializesOriginalCalendarDays(String zone, Class<?> type) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        Configuration config = mybatis(type);
        var mapped = config.getMappedStatement(namespace(type) + ".select" + type.getSimpleName() + "List");
        var rs = mock(ResultSet.class);
        var metadata = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnCount()).thenReturn(2);
        when(metadata.getColumnLabel(1)).thenReturn("effective_from");
        when(metadata.getColumnLabel(2)).thenReturn("effective_to");
        for (int column = 1; column <= 2; column++) {
            when(metadata.getColumnType(column)).thenReturn(Types.DATE);
            when(metadata.getColumnClassName(column)).thenReturn(java.sql.Date.class.getName());
        }
        // 旧 DateTypeHandler 正是 getTimestamp：Connector/J 对 DATE 按 JVM 区构造午夜。
        when(rs.getTimestamp("effective_from")).thenReturn(Timestamp.valueOf(FROM.atStartOfDay()));
        when(rs.getTimestamp("effective_to")).thenReturn(Timestamp.valueOf(TO.atStartOfDay()));
        // JDBC 4.2 原生 LocalDate 路径直接保留 SQL DATE，无毫秒/时区中转。
        when(rs.getObject("effective_from", LocalDate.class)).thenReturn(FROM);
        when(rs.getObject("effective_to", LocalDate.class)).thenReturn(TO);
        when(rs.next()).thenReturn(true, false);
        var statement = mock(Statement.class);
        when(statement.getResultSet()).thenReturn(rs);
        when(statement.getUpdateCount()).thenReturn(-1);
        Object query = type.getConstructor().newInstance();
        var handler = new DefaultResultSetHandler(mock(Executor.class), mapped, null, null,
                mapped.getBoundSql(query), RowBounds.DEFAULT);
        var rows = handler.handleResultSets(statement);
        assertThat(rows).hasSize(1);
        assertDaysInJson(rows.get(0));
    }

    @ParameterizedTest @MethodSource("timezonesAndDomains")
    void jdbcWritesAndDateFiltersUseCalendarDateParameters(String zone, Class<?> type) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        Object row = jsonMapper().readValue(JSON, type);
        var config = mybatis(type);
        for (String operation : List.of("insert", "update", "select")) {
            String suffix = operation.equals("select") ? "List" : "";
            var mapped = config.getMappedStatement(namespace(type) + "." + operation + type.getSimpleName() + suffix);
            var sql = mapped.getBoundSql(row);
            var statement = mock(PreparedStatement.class);
            new DefaultParameterHandler(mapped, row, sql).setParameters(statement);
            for (int i = 0; i < sql.getParameterMappings().size(); i++) {
                String property = sql.getParameterMappings().get(i).getProperty();
                if (property.equals("effectiveFrom")) verify(statement).setObject(i + 1, FROM);
                if (property.equals("effectiveTo")) verify(statement).setObject(i + 1, TO);
            }
        }
    }

    @ParameterizedTest @MethodSource("timezonesAndDomains")
    void getDateFiltersUseSameDateContractAsJson(String zone, Class<?> type) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        Object row = type.getConstructor().newInstance();
        var binder = new WebDataBinder(row);
        binder.setConversionService(new DefaultFormattingConversionService());
        new BaseController().initBinder(binder);
        binder.bind(new MutablePropertyValues(Map.of("effectiveFrom", FROM.toString(), "effectiveTo", TO.toString())));
        assertThat(binder.getBindingResult().getAllErrors()).isEmpty();
        assertDaysInJson(row);
    }

    private void assertDaysInJson(Object row) {
        var node = jsonMapper().readTree(jsonMapper().writeValueAsString(row));
        assertThat(node.get("effectiveFrom").asText()).isEqualTo(FROM.toString());
        assertThat(node.get("effectiveTo").asText()).isEqualTo(TO.toString());
    }

    private JsonMapper jsonMapper() {
        var format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return JsonMapper.builder().defaultDateFormat(format)
                .defaultTimeZone(TimeZone.getTimeZone("GMT+8")).build();
    }

    private Configuration mybatis(Class<?> type) throws Exception {
        var config = new Configuration(); config.getTypeAliasRegistry().registerAlias(type);
        String resource = "mapper/system/" + type.getSimpleName() + "Mapper.xml";
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
        }
        return config;
    }

    private String namespace(Class<?> type) {
        return "com.workload.system.mapper." + type.getSimpleName() + "Mapper";
    }
}
