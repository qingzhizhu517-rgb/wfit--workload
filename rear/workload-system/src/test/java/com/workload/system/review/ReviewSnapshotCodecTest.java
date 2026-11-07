package com.workload.system.review;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ReviewSnapshotCodecTest {
    ObjectNode item(){
        ObjectNode n=ReviewSnapshotCodec.object();n.put("formatVersion",1);n.put("summaryId",1);n.put("userId",2);n.put("semester","2025-2026-1");n.put("itemId",3);n.put("reviewBatchNo","batch-1");n.put("reviewType","MANUAL_DECLARATION");
        ObjectNode v=n.putObject("values");for(String key:ReviewSnapshotCodec.ITEM_KEYS)v.putNull(key);
        v.put("itemType","G8");v.put("sourceType","SELF");v.put("calculatedWorkload","12.3");v.put("status","1");v.put("calculationVersion","0");v.put("isOverLimit","0");v.put("evidenceStatus","HISTORICAL_MISSING");return n;
    }
    ObjectNode batch(){
        var n=ReviewSnapshotCodec.object();n.put("formatVersion",1);n.put("summaryId",1);n.put("userId",2);n.put("semester","2025-2026-1");n.put("reviewBatchNo","batch-1");n.put("dataVersion",1);
        var summary=n.putObject("summary");for(String key:ReviewSnapshotCodec.SUMMARY_KEYS)summary.putNull(key);summary.put("needsRecalculation","0");summary.put("isCapped","0");summary.put("basicTeachingMet","0");summary.put("lastCalculatedAt","1762489200000");
        var pay=n.putObject("pay");pay.put("id","1");pay.put("summaryId","1");pay.put("courseHourPay","0");pay.put("otherPayTotal","0");pay.put("totalPay","0");pay.put("status","0");pay.putNull("settlementSnapshotId");
        n.putArray("allowances");var seal=ReviewSnapshotCodec.sealItem(item());var member=n.putArray("items").addObject();member.put("json",seal.json());member.put("hash",seal.hash());
        var counts=n.putObject("sourceCounts");for(String key:java.util.List.of("includedReviewItemCount","excludedRejectedItemCount","distinctTeachingTaskReferenceCount","distinctRoleAssignmentReferenceCount","manualDeclarationItemCount"))counts.put(key,"0");counts.put("includedReviewItemCount","1");counts.put("manualDeclarationItemCount","1");counts.putNull("importRawRowCount");counts.put("importRawMissingReason","历史缺失");n.putNull("settlementSnapshotId");n.putNull("settlementSnapshotHash");n.put("settlementEvidenceStatus","HISTORICAL_MISSING");return n;
    }
    void assertResignedRejected(ObjectNode n){String hash=com.workload.system.calc.rule.PolicyHash.hash(n);assertThatThrownBy(()->ReviewSnapshotCodec.readBatch(n.toString(),hash)).isInstanceOf(com.workload.common.exception.ServiceException.class);}
    @Test void resignedBatchCannotInventEnumsTimesOrErasePayOutputs(){
        var valid=batch();var seal=ReviewSnapshotCodec.sealBatch(valid);assertThat(seal.hash()).isEqualTo(com.workload.system.calc.rule.PolicyHash.hash(valid));assertThatCode(()->ReviewSnapshotCodec.readBatch(seal.json(),seal.hash())).doesNotThrowAnyException();
        for(String key:java.util.List.of("isCapped","basicTeachingMet","needsRecalculation")){var n=batch();n.withObject("/summary").put(key,"2");assertResignedRejected(n);}
        var dirty=batch();dirty.withObject("/summary").put("needsRecalculation","1");assertResignedRejected(dirty);
        for(String key:java.util.List.of("lastCalculatedAt","teacherSignTime","deptLeaderSignTime","academicAssistantSignTime")){var n=batch();n.withObject("/summary").put(key,"not-a-time");assertResignedRejected(n);}
        var missingTime=batch();missingTime.withObject("/summary").putNull("lastCalculatedAt");assertResignedRejected(missingTime);
        var noPay=batch();noPay.putNull("pay");assertResignedRejected(noPay);var invalidStatus=batch();invalidStatus.withObject("/pay").put("status","2");assertResignedRejected(invalidStatus);
        var ref=batch();ref.withObject("/pay").put("settlementSnapshotId","99");assertResignedRejected(ref);
    }
    @Test void nullableHistoricalFlagsAndSignatureTimesStayExplicitNullAndLegalEnumsRead(){
        var n=batch();n.withObject("/summary").putNull("isCapped");n.withObject("/summary").putNull("basicTeachingMet");n.withObject("/pay").putNull("status");var seal=ReviewSnapshotCodec.sealBatch(n);assertThat(ReviewSnapshotCodec.readBatch(seal.json(),seal.hash()).path("summary").path("isCapped").isNull()).isTrue();
        n.withObject("/summary").put("isCapped","1");n.withObject("/summary").put("basicTeachingMet","1");n.withObject("/pay").put("status","1");n.withObject("/summary").put("teacherSignTime","0");assertThatCode(()->ReviewSnapshotCodec.sealBatch(n)).doesNotThrowAnyException();
        var nullFresh=batch();nullFresh.withObject("/summary").putNull("needsRecalculation");assertResignedRejected(nullFresh);
    }
    @Test void canonicalHashIgnoresObjectKeyOrderAndPreservesExplicitNull(){
        var seal=ReviewSnapshotCodec.sealItem(item());assertThat(ReviewSnapshotCodec.readItem(seal.json(),seal.hash())).isEqualTo(item());
        var changed=item();changed.withObject("/values").put("calculatedWorkload","15");
        assertThatThrownBy(()->ReviewSnapshotCodec.readItem(changed.toString(),seal.hash())).hasMessageContaining("hash");
    }
    @Test void resignedPayloadStillRejectsMissingKeysWrongTypesEnumsAndImplicitNull(){
        var missing=item();missing.withObject("/values").remove("taskId");assertThatThrownBy(()->ReviewSnapshotCodec.sealItem(missing)).hasMessageContaining("字段");
        var wrong=item();wrong.put("itemId","3");assertThatThrownBy(()->ReviewSnapshotCodec.sealItem(wrong)).hasMessageContaining("整数");
        var type=item();type.put("reviewType","ALLOWANCE");assertThatThrownBy(()->ReviewSnapshotCodec.sealItem(type)).hasMessageContaining("类型");
        var n=item();n.withObject("/values").put("calculationSnapshotHash","0".repeat(64));assertThatThrownBy(()->ReviewSnapshotCodec.sealItem(n)).hasMessageContaining("引用");
    }
    @Test void resignedClassificationAndSourceReferenceMustStillFollowContract(){
        var wrong=item();wrong.put("reviewType","TEACHING_TASK");assertThatThrownBy(()->ReviewSnapshotCodec.sealItem(wrong)).hasMessageContaining("分类");
        var source=item();source.withObject("/values").put("sourceType","IMPORT");source.put("reviewType","TEACHING_TASK");assertThatThrownBy(()->ReviewSnapshotCodec.sealItem(source)).hasMessageContaining("来源");
        var date=item();date.withObject("/values").put("lastCalculatedAt","not-a-time");assertThatThrownBy(()->ReviewSnapshotCodec.sealItem(date)).hasMessageContaining("整数");
    }
    @Test void jsonDuplicateKeysAreRejectedEvenWhenTextIsResigned(){
        String s=item().toString().replace("\"formatVersion\":1","\"formatVersion\":1,\"formatVersion\":1");
        assertThatThrownBy(()->ReviewSnapshotCodec.readItem(s,"0".repeat(64))).hasMessageContaining("证据");
    }
}
