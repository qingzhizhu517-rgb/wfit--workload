package com.workload.system.review;

import com.workload.system.domain.BizWorkloadItemReview;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ReviewRowIntegrityTest {
    @Test void terminalResultRequiresReviewerTimeVersionAndReason(){
        var seal=ReviewSnapshotCodec.sealItem(new ReviewSnapshotCodecTest().item());var row=new BizWorkloadItemReview();row.setSummaryId(1L);row.setItemId(3L);row.setReviewBatchNo("batch-1");row.setReviewType("MANUAL_DECLARATION");row.setFormatVersion(1);row.setSnapshotJson(seal.json());row.setDataHash(seal.hash());row.setResult("WAIVE");row.setVersion(0L);
        assertThatThrownBy(()->new ReviewBatchService().verifyReviewRow(row)).hasMessageContaining("结果");
    }
    @Test void summaryVersionAndBatchFieldsAreReadOnlyUnderProductionJackson3(){
        var json=tools.jackson.databind.json.JsonMapper.builder().build();var summary=json.readValue("{\"dataVersion\":99,\"reviewBatchNo\":\"forged\",\"lastCalculatedAt\":1}",com.workload.system.domain.BizWorkloadSummary.class);assertThat(summary.getDataVersion()).isNull();assertThat(summary.getReviewBatchNo()).isNull();assertThat(summary.getLastCalculatedAt()).isNull();summary.setDataVersion(1L);assertThat(json.writeValueAsString(summary)).contains("\"dataVersion\":1");
    }
    @Test void transportPreservesNonZeroDigitsBeyondDoublePrecision(){
        var body=ReviewCommands.parse("{\"adjustmentAmount\":1.00000000000000001,\"reason\":\"精度\",\"handledAt\":\"2026-11-06\"}");
        assertThatThrownBy(()->ReviewCommands.note(body,"precision-1")).hasMessageContaining("DECIMAL");
    }
}
