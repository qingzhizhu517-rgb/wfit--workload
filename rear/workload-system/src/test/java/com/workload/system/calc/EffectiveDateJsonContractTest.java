package com.workload.system.calc;

import static org.assertj.core.api.Assertions.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.TimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import com.workload.system.domain.BizWorkloadRule;
import com.workload.system.domain.BizPayRate;

class EffectiveDateJsonContractTest {
    @ParameterizedTest @ValueSource(strings={"rule","rate"})
    void datePickerPayloadParsesWithProductionGlobalDateTimeFormat(String kind) {
        var format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); format.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        var mapper=JsonMapper.builder().defaultDateFormat(format).defaultTimeZone(TimeZone.getTimeZone("GMT+8")).build();
        String json="{\"effectiveFrom\":\"2025-10-01\",\"effectiveTo\":\"2026-01-31\"}";
        Class<?> type = "rule".equals(kind) ? BizWorkloadRule.class : BizPayRate.class;
        assertThatCode(() -> mapper.readValue(json, type))
                .doesNotThrowAnyException();
        if("rule".equals(kind)) {
            var row=mapper.readValue(json,BizWorkloadRule.class);
            assertThat(row.getEffectiveFrom()).isEqualTo(LocalDate.of(2025,10,1));
            assertThat(row.getEffectiveTo()).isEqualTo(LocalDate.of(2026,1,31));
        } else {
            var row=mapper.readValue(json,BizPayRate.class);
            assertThat(row.getEffectiveFrom()).isEqualTo(LocalDate.of(2025,10,1));
            assertThat(row.getEffectiveTo()).isEqualTo(LocalDate.of(2026,1,31));
        }
    }
}
