package com.workload.system.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workload.common.exception.ServiceException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ReviewCommandsTest {
    final ObjectMapper json=new ObjectMapper();
    @Test void reviewOnlyAcceptsExplicitIntegralVersionAndReason() throws Exception {
        assertThat(ReviewCommands.review(json.readTree("{\"version\":0}"),"PASS").version()).isZero();
        for(String body: new String[]{"{}","{\"version\":\"0\"}","{\"version\":0.1}","{\"version\":-1}","{\"version\":0,\"reviewerId\":7}","{\"version\":0,\"unknown\":null}"})
            assertThatThrownBy(()->ReviewCommands.review(json.readTree(body),"PASS")).isInstanceOf(ServiceException.class);
        assertThatThrownBy(()->ReviewCommands.review(json.readTree("{\"version\":0,\"reason\":\" \"}"),"WAIVE")).hasMessageContaining("原因");
    }
    @Test void reasonsAndNoteTextKeepVerbatimWhitespaceAndCheckRawLength() {
        String reason="  原文原因 \n";var review=ReviewSnapshotCodec.object();review.put("version",0);review.put("reason",reason);
        assertThat(ReviewCommands.review(review,"REJECT").reason()).isEqualTo(reason);
        review.put("reason"," "+"理".repeat(499)+" ");assertThatThrownBy(()->ReviewCommands.review(review,"WAIVE")).isInstanceOf(ServiceException.class);
        var note=ReviewSnapshotCodec.object();note.put("adjustmentAmount",1);note.put("reason",reason);note.put("handledAt","2026-11-06");note.put("noteText","  原文备注 \n");
        assertThat(ReviewCommands.note(note,"verbatim").reason()).isEqualTo(reason);assertThat(ReviewCommands.note(note,"verbatim").noteText()).isEqualTo("  原文备注 \n");
        note.put("reason"," "+"理".repeat(499)+" ");assertThatThrownBy(()->ReviewCommands.note(note,"verbatim")).isInstanceOf(ServiceException.class);
        note.put("reason","有效");note.put("noteText"," "+"注".repeat(9999)+" ");assertThatThrownBy(()->ReviewCommands.note(note,"verbatim")).isInstanceOf(ServiceException.class);
    }
    @Test void paginationIsBoundedAndRejectsUnknownSortOrInvalidNumber() {
        assertThat(ReviewCommands.page(Map.of()).size()).isEqualTo(10);
        assertThat(ReviewCommands.page(Map.of("pageSize","100")).size()).isEqualTo(100);
        for(Map<String,String> q:java.util.List.of(Map.of("pageNum","0"),Map.of("pageSize","101"),Map.of("pageSize","1.0"),Map.of("orderByColumn","id desc")))
            assertThatThrownBy(()->ReviewCommands.page(q)).isInstanceOf(ServiceException.class);
    }
    @Test void adjustmentRejectsRoundingForgedFieldsBadDateAndUnsafeAttachment() throws Exception {
        String valid="{\"adjustmentAmount\":-12.30,\"reason\":\"补差\",\"handledAt\":\"2026-11-06\"}";
        assertThat(ReviewCommands.note(json.readTree(valid),"req-1").amount()).isEqualByComparingTo("-12.30");
        for(String body:new String[]{valid.replace("-12.30","1.001"),valid.replace("-12.30","10000000000"),valid.replace("2026-11-06","2026-02-30"),valid.replace("}",",\"handledById\":9}"),valid.replace("}",",\"attachmentRef\":\"/profile/upload/../secret\"}"),valid.replace("}",",\"attachmentRef\":\"https://example.com/a\"}")})
            assertThatThrownBy(()->ReviewCommands.note(json.readTree(body),"req-1")).isInstanceOf(ServiceException.class);
        assertThatThrownBy(()->ReviewCommands.note(json.readTree(valid)," ")).isInstanceOf(ServiceException.class);
    }
    @Test void controlledUploadReferenceSupportsTheSystemsChineseFilenameConvention() throws Exception {
        var body=ReviewCommands.parse("{\"adjustmentAmount\":1,\"reason\":\"补差\",\"handledAt\":\"2026-11-06\",\"attachmentRef\":\"/profile/upload/2026/11/06/线下补差 (复核)_123.pdf\"}");
        assertThat(ReviewCommands.note(body,"upload-1").attachmentRef()).contains("线下补差");
    }
    @Test void batchRejectsDuplicateAndMoreThanHundredCommands() throws Exception {
        assertThatThrownBy(()->ReviewCommands.batch(json.readTree("{\"items\":[{\"id\":1,\"version\":0},{\"id\":1,\"version\":0}]}"),"PASS")).isInstanceOf(ServiceException.class);
        String item="{\"id\":1,\"version\":0}";
        assertThatThrownBy(()->ReviewCommands.batch(json.readTree("{\"items\":["+String.join(",",java.util.Collections.nCopies(101,item))+"]}"),"PASS")).isInstanceOf(ServiceException.class);
    }
}
