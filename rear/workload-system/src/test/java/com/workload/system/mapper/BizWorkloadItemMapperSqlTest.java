package com.workload.system.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BizWorkloadItemMapperSqlTest
{
    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void disputedAndRejectedItemsRemainEligibleForConditionalCalculationUpdate(int itemStatus)
            throws Exception
    {
        String resource = "/mapper/system/BizWorkloadItemMapper.xml";
        try (InputStream stream = getClass().getResourceAsStream(resource))
        {
            assertThat(stream).as(resource).isNotNull();
            String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(itemStatus).isNotEqualTo(1);
            assertThat(xml).contains("ifnull(i.status, 0) &lt;&gt; #{confirmedStatus}");
            assertThat(xml).doesNotContain("ifnull(i.status, 0) = #{draftStatus}");
        }
    }
}
