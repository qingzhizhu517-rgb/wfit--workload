package com.workload.framework.mybatis;

import com.workload.framework.config.MyBatisConfig;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BizAuditCoverageTest {
    @Test void everyWriteInTheTwentyEntitiesAndAuditLogHasExactContractAndNoCreationHistorySet() throws Exception {
        Set<String> writes=new HashSet<>();
        Path root=Path.of("../workload-system/src/main/resources/mapper/system");
        int mappers=0;
        try(var files=Files.list(root)) {
            for(Path path:files.filter(p->p.getFileName().toString().startsWith("Biz")).toList()) {
                String name=path.getFileName().toString().replace(".xml","");
                if(Set.of("BizWorkloadCalcSnapshotMapper","BizWorkloadSettlementSnapshotMapper").contains(name)) continue;
                String xml=Files.readString(path);var matcher=Pattern.compile("<(insert|update)\\b[^>]*id=\"([^\"]+)\"[^>]*>(.*?)</\\1>",Pattern.DOTALL).matcher(xml);
                boolean covered=false;
                while(matcher.find()) {
                    String id=matcher.group(2);if(id.equals("lockPeriod")) continue;
                    covered=true;writes.add("com.workload.system.mapper."+name+"."+id);
                    if(matcher.group(1).equals("insert")) {
                        for(String column:List.of("create_by","create_time","update_by","update_time")) assertTrue(matcher.group(3).contains(column),name+"."+id+" "+column);
                    }
                    if(matcher.group(1).equals("update")) {
                        String body=matcher.group(3);
                        assertFalse(Pattern.compile("create_(by|time)\\s*=",Pattern.CASE_INSENSITIVE).matcher(body).find(),name+"."+id);
                        assertTrue(body.contains("update_by"),name+"."+id);assertTrue(body.contains("update_time"),name+"."+id);
                    }
                }
                if(covered)mappers++;
            }
        }
        assertEquals(21,mappers);assertEquals(writes,BizAuditFieldInterceptor.statementIds());
    }
    @Test void productionFactoryRegistersRealPluginAndPreservesOthers() throws Exception {
        var config=new MyBatisConfig();
        var environment=new org.springframework.core.env.StandardEnvironment();
        environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("test",Map.of(
            "mybatis.typeAliasesPackage","com.workload.system.domain,com.workload.common.core.domain.entity",
            "mybatis.mapperLocations","classpath*:mapper/**/*Mapper.xml",
            "mybatis.configLocation",Path.of("../workload-admin/src/main/resources/mybatis/mybatis-config.xml").toUri().toString())));
        var field=MyBatisConfig.class.getDeclaredField("env");field.setAccessible(true);field.set(config,environment);
        var beans=new org.springframework.beans.factory.support.StaticListableBeanFactory();beans.addBean("audit",config.bizAuditFieldInterceptor());
        var other=new org.apache.ibatis.plugin.Interceptor(){public Object intercept(org.apache.ibatis.plugin.Invocation i)throws Throwable{return i.proceed();}};
        beans.addBean("other",other);
        var factory=config.sqlSessionFactory(new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:mysql://127.0.0.1:13306/unused"),beans.getBeanProvider(org.apache.ibatis.plugin.Interceptor.class));
        assertEquals(1,factory.getConfiguration().getInterceptors().stream().filter(BizAuditFieldInterceptor.class::isInstance).count());
        assertTrue(factory.getConfiguration().getInterceptors().contains(other));
    }
}
