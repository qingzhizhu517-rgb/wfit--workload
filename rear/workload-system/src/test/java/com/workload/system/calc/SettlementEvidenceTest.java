package com.workload.system.calc;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
class SettlementEvidenceTest {
 @Test void formalScopeIsExplicitAndQaIsNeverFormal() {
  var scope = new SettlementScope();
  assertThat(scope.mode("2097-2098-1")).isEqualTo("LEGACY_NOT_ENABLED");
  scope.setVerificationSemesters(java.util.List.of("2097-2098-1"));
  assertThat(scope.mode("2097-2098-1")).isEqualTo("ISOLATED_VERIFICATION_ONLY");
  scope.setFormalStartSemester("2027-2028-1");
  assertThat(scope.mode("2027-2028-1")).isEqualTo("FORMAL");
 }
 @Test void unknownNatureNeverQualifiesAndLegacyBoundaryIsExplicit() {
  assertThat(SettlementEvidence.eligibility(null)).contains("不能确认");
  assertThat(SettlementEvidence.eligibility("行政")).contains("不能确认");
  assertThat(SettlementEvidence.eligibility("专任")).contains("历史默认");
 }
 @Test void unknownAndBrokenPayloadFailWith50017() {
  assertThatThrownBy(() -> SettlementEvidence.decode("{}", "bad")).isInstanceOf(com.workload.common.exception.ServiceException.class)
   .extracting("code").isEqualTo(50017);
 }

 @Test void rejectedUncalculatedItemIsExcludedAndRetainsItsExplanation() {
  var old=new SettlementTestData().evidence();
  var rejected=new SettlementEvidence.Item(9L,"G1",3,"IMPORT",null,null,"拒绝的未算课程","本科",0L,null,"status=3驳回排除，不计入任何汇总",null,null);
  var evidence=SettlementEvidence.seal(new SettlementEvidence.SummaryEvidence(old.summaryId(),old.userId(),old.semester(),old.mode(),old.selection(),old.person(),old.eligibilityReason(),old.profileBoundary(),java.util.List.of(rejected),old.rules(),old.rate(),old.rateHash(),old.results(),old.threeTheoryCourses(),old.warnings(),null));
  assertThatCode(()->SettlementEvidence.verifySummary(evidence)).doesNotThrowAnyException();
 }
 @Test void resealingAnInconsistentPerformanceCannotHideAlgorithmMismatch() {
  var old=new SettlementTestData().evidence();var results=new java.util.TreeMap<>(old.results());results.put("performance","999.00");
  var evidence=SettlementEvidence.seal(new SettlementEvidence.SummaryEvidence(old.summaryId(),old.userId(),old.semester(),old.mode(),old.selection(),old.person(),old.eligibilityReason(),old.profileBoundary(),old.items(),old.rules(),old.rate(),old.rateHash(),results,old.threeTheoryCourses(),old.warnings(),null));
  assertThatThrownBy(()->SettlementEvidence.verifySummary(evidence)).isInstanceOf(com.workload.common.exception.ServiceException.class).extracting("code").isEqualTo(50017);
 }
 @Test void resealedUnknownPayloadFormatIsRejected()throws Exception {
  var row=new SettlementTestData().row();var json=new com.fasterxml.jackson.databind.ObjectMapper();var tree=json.readTree(row.getPayloadJson());((com.fasterxml.jackson.databind.node.ObjectNode)tree).put("formatVersion",2);
  assertThatThrownBy(()->SettlementEvidence.decode(tree.toString(),com.workload.system.calc.rule.PolicyHash.hash(tree))).hasMessageContaining("未知结算格式");
 }
 @Test void unknownFieldsRemainRejectedEvenWithMatchingOuterHash()throws Exception {
  var row=new SettlementTestData().row();var json=new com.fasterxml.jackson.databind.ObjectMapper();var tree=json.readTree(row.getPayloadJson());((com.fasterxml.jackson.databind.node.ObjectNode)tree).put("unknownMixedVersionField","x");
  assertThatThrownBy(()->SettlementEvidence.decode(tree.toString(),com.workload.system.calc.rule.PolicyHash.hash(tree))).hasMessageContaining("unknownMixedVersionField");
 }

 @Test void productionJackson3RejectsClientOwnedSettlementFieldsButSerializesServerValues() {
  var json=tools.jackson.databind.json.JsonMapper.builder().build();String input="{\"id\":1,\"settlementSnapshotId\":999,\"settlementRequired\":false,\"settlementSubmittable\":true,\"teacherNature\":\"专任\",\"eligibilityReason\":\"forged\",\"settlementEvidenceStatus\":\"VERIFIED\"}";
  var summary=json.readValue(input,com.workload.system.domain.BizWorkloadSummary.class);var pay=json.readValue(input,com.workload.system.domain.BizPayRecord.class);
  assertThat(summary.getSettlementSnapshotId()).isNull();assertThat(summary.getSettlementRequired()).isNull();assertThat(summary.getSettlementSubmittable()).isNull();assertThat(summary.getTeacherNature()).isNull();assertThat(pay.getSettlementSnapshotId()).isNull();assertThat(pay.getEligibilityReason()).isNull();assertThat(pay.getSettlementEvidenceStatus()).isNull();
  summary.setSettlementSnapshotId(7L);summary.setSettlementSubmittable(true);assertThat(json.writeValueAsString(summary)).contains("\"settlementSnapshotId\":7","\"settlementSubmittable\":true");
 }

 @Test void numericStringVersionCannotMasqueradeAsExplicitIntegerFormat()throws Exception {
  var row=new SettlementTestData().row();var json=new com.fasterxml.jackson.databind.ObjectMapper();var tree=json.readTree(row.getPayloadJson());((com.fasterxml.jackson.databind.node.ObjectNode)tree).put("formatVersion","1");
  assertThatThrownBy(()->SettlementEvidence.decode(tree.toString(),row.getHash())).hasMessageContaining("格式版本");
 }
 @Test void nullWarningsCannotBecomeCompleteEvidenceAfterResealing() {
  var old=new SettlementTestData().evidence();var broken=SettlementEvidence.seal(new SettlementEvidence.SummaryEvidence(old.summaryId(),old.userId(),old.semester(),old.mode(),old.selection(),old.person(),old.eligibilityReason(),old.profileBoundary(),old.items(),old.rules(),old.rate(),old.rateHash(),old.results(),old.threeTheoryCourses(),null,null));
  assertThatThrownBy(()->SettlementEvidence.verifySummary(broken)).isInstanceOf(com.workload.common.exception.ServiceException.class);
 }

 @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
 void missingOrNullThreeCourseFlagCannotPreserveTheOriginalHash(boolean explicitNull)throws Exception {
  var original=new SettlementTestData().row();var json=new com.fasterxml.jackson.databind.ObjectMapper();var tree=json.readTree(original.getPayloadJson());var summary=(com.fasterxml.jackson.databind.node.ObjectNode)tree.path("summary");if(explicitNull)summary.putNull("threeTheoryCourses");else summary.remove("threeTheoryCourses");
  assertThatThrownBy(()->SettlementEvidence.decode(tree.toString(),original.getHash())).isInstanceOf(com.workload.common.exception.ServiceException.class).extracting("code").isEqualTo(50017);
 }
 @Test void nullableRecordFieldMustBePresentRatherThanSilentlyRecreated()throws Exception {
  var old=new SettlementTestData().evidence();var rejected=new SettlementEvidence.Item(9L,"G1",3,"IMPORT",null,null,"已驳回","本科",0L,null,"status=3驳回排除",null,null);
  var evidence=SettlementEvidence.seal(new SettlementEvidence.SummaryEvidence(old.summaryId(),old.userId(),old.semester(),old.mode(),old.selection(),old.person(),old.eligibilityReason(),old.profileBoundary(),java.util.List.of(rejected),old.rules(),old.rate(),old.rateHash(),old.results(),false,old.warnings(),null));
  var pay=SettlementEvidence.seal(new SettlementEvidence.PayEvidence(2L,evidence.hash(),java.util.List.of(),"0.00","0.00",0L,"course/other:scale2;total:scale0;HALF_UP",null));var payload=new SettlementEvidence.Payload(1,1L,SettlementEvidence.ALGORITHM,evidence,pay);var json=new com.fasterxml.jackson.databind.ObjectMapper();var tree=json.readTree(SettlementEvidence.encode(payload));((com.fasterxml.jackson.databind.node.ObjectNode)tree.path("summary").path("items").get(0)).remove("assignmentId");
  assertThatThrownBy(()->SettlementEvidence.decode(tree.toString(),SettlementEvidence.hash(payload))).isInstanceOf(com.workload.common.exception.ServiceException.class).extracting("code").isEqualTo(50017);
 }
}
