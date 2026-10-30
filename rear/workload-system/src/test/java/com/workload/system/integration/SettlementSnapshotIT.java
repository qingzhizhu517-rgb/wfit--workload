package com.workload.system.integration;
import static org.assertj.core.api.Assertions.*;
import java.math.*;import java.util.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.*;
import org.springframework.aop.framework.Advised;import org.springframework.test.util.ReflectionTestUtils;
import com.workload.system.domain.dto.CalculationRunRequest;import com.workload.system.calc.*;import com.workload.system.calc.rule.*;import com.workload.system.calc.allowance.*;import com.workload.system.domain.*;import com.workload.system.mapper.*;
/** Strict scope is explicitly enabled in every new-engine test; random schemas only on 13306. */
class SettlementSnapshotIT {
 static final String SEM="2097-2098-1";
 FactorProvenanceIT source;WorkloadMysqlFixture f;SettlementSnapshotService evidence;SettlementScope scope;
 @BeforeEach void setup()throws Exception{
  source=new FactorProvenanceIT();source.setup();f=source.f;
  scope=(SettlementScope)f.beans.get(SettlementScope.class);scope.setVerificationSemesters(List.of(SEM));
  evidence=new SettlementSnapshotService(f.mapper(BizWorkloadSettlementSnapshotMapper.class),f.mapper(BizWorkloadCalcSnapshotMapper.class),f.mapper(BizWorkloadSummaryMapper.class),f.mapper(BizPayRecordMapper.class),f.mapper(BizWorkloadItemMapper.class),f.mapper(BizAllowanceItemMapper.class),scope);f.beans.put(SettlementSnapshotService.class,evidence);
  for(Object proxy:List.of(f.summary,f.pay,f.audit))f.wire(((Advised)proxy).getTargetSource().getTarget());
  f.jdbc.update("INSERT INTO biz_pay_rate(title,rate,effective_from,status) VALUES('讲师',30,'2025-01-01',1)");
 }
 @AfterEach void close(){if(source!=null)source.close();}
 BizWorkloadItem imported()throws Exception{var d=source.row("G1");d.setSemester(SEM);d.setPlannedHours(new BigDecimal("300"));return source.imported(d);}
 BizWorkloadSummary calculate()throws Exception{imported();return f.summary.recalcSummary(1003L,SEM,true);}
 BizWorkloadSummary summary(){return f.mapper(BizWorkloadSummaryMapper.class).selectByUserSemesterForUpdate(1003L,SEM);}
 BizPayRecord pay(){return f.pay.currentPay(1003L,SEM);}
 @Test void strictQaCapturesOneImmutableSettlementAndConfirmationStillSubmits()throws Exception{
  var i=imported();var run=f.calc.run(new CalculationRunRequest(1003L,SEM,false));var s=run.getSummary();assertThat(s.getSettlementSnapshotId()).isNotNull();assertThat(f.count("biz_workload_settlement_snapshot")).isEqualTo(1);assertThat(pay().getSettlementSnapshotId()).isEqualTo(s.getSettlementSnapshotId());
  evidence.decorate(s);assertThat(s.getSettlementMode()).isEqualTo("ISOLATED_VERIFICATION_ONLY");assertThat(s.getSettlementSubmittable()).isTrue();
  var now=f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(i.getId());WorkloadMysqlFixture.authenticated(()->{f.items.confirm(i.getId(),now.getCalculationVersion(),now.getCalculatedWorkload());f.audit.submit(s.getId());f.audit.approve(s.getId());return null;});assertThat(summary().getStatus()).isEqualTo(2);assertThat(f.count("biz_workload_settlement_snapshot")).isEqualTo(1);
 }
 @ParameterizedTest @NullSource @ValueSource(strings={"行政","兼岗","unknown","外聘","校企"}) void nonExplicitFullTimeCannotPersist(String nature)throws Exception{
  imported();f.jdbc.update("UPDATE biz_teacher_profile SET teacher_nature=? WHERE user_id=1003",nature);
  assertThatThrownBy(()->f.summary.recalcSummary(1003L,SEM,true)).hasMessageContaining("不能确认");assertThat(f.count("biz_workload_settlement_snapshot")).isZero();assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM biz_workload_summary WHERE semester=?",Long.class,SEM)).isZero();
  var preview=f.summary.recalcSummary(1003L,SEM,false);assertThat(preview.getPerformancePay()).isEqualByComparingTo("0");assertThat(preview.getEligibilityReason()).contains("不能确认");
 }
 @Test void format4PartialSourceCannotBecomeCompleteSettlement()throws Exception {
  f.jdbc.update("UPDATE biz_workload_item SET semester=? WHERE id=9",SEM);
  f.jdbc.update("UPDATE biz_workload_item SET factor_provenance_json=? WHERE id=9",FactorProvenance.encode(9L,"G1",Map.of()));
  WorkloadMysqlFixture.authenticated(()->f.calc.recalcItem(9L));
  assertThat(f.mapper(BizWorkloadCalcSnapshotMapper.class).selectLatestByItemId(9L).getContextJson()).contains("PARTIAL","\"formatVersion\":4");
  assertThatThrownBy(()->f.summary.recalcSummary(1003L,SEM,true)).hasMessageContaining("完整实算来源");
 }
 @Test void oldManualValueWithoutPersistedActorProofCannotSettle()throws Exception {
  var id=imported().getId();f.jdbc.update("INSERT INTO biz_workload_item(user_id,semester,item_type,source_type,calculated_workload,status,create_by) VALUES(1003,?,'G8','MANUAL',12,0,'legacy')",SEM);
  assertThatThrownBy(()->f.summary.recalcSummary(1003L,SEM,true)).hasMessageContaining("手工明细来源");
 }
 @Test void pendingWithClearedSummaryPointerCannotUseLegacyEscape()throws Exception {
  var s=calculate();f.jdbc.update("UPDATE biz_workload_summary SET status=1,settlement_snapshot_id=NULL WHERE id=?",s.getId());
  assertThatThrownBy(()->f.tx.execute(status->{evidence.assertSubmittable(summary());return null;})).hasMessageContaining("缺少结算证据");
 }
 @Test void missingRateBlocksInsteadOfZero()throws Exception{imported();f.jdbc.update("DELETE FROM biz_pay_rate");assertThatThrownBy(()->f.summary.recalcSummary(1003L,SEM,true)).hasMessageContaining("费率");assertThat(f.count("biz_workload_settlement_snapshot")).isZero();}
 @Test void savedHistoryAndIndependentPayIgnoreCurrentProfileRulesAndRate()throws Exception{
  var s=calculate();var old=pay();var row=f.mapper(BizWorkloadSettlementSnapshotMapper.class).selectById(s.getSettlementSnapshotId());String json=row.getPayloadJson(),hash=row.getHash();
  f.jdbc.update("UPDATE biz_teacher_profile SET title='教授',teacher_nature='行政',special_status='产假'");f.jdbc.update("UPDATE biz_pay_rate SET rate=999");f.jdbc.update("DELETE FROM biz_workload_rule");
  var p=f.pay.recalcPay(1003L,SEM);assertThat(p.getCourseHourPay()).isEqualByComparingTo(old.getCourseHourPay());assertThat(f.count("biz_workload_settlement_snapshot")).isEqualTo(2);var original=f.mapper(BizWorkloadSettlementSnapshotMapper.class).selectById(row.getId());assertThat(original.getPayloadJson()).isEqualTo(json);assertThat(original.getHash()).isEqualTo(hash);evidence.decorate(summary());assertThat(summary().getTitle()).isEqualTo("讲师");
 }
 @Test void independentPayUsesCurrentAllowanceAndAppendsOnlyOnce()throws Exception{
  calculate();var a=new BizAllowanceItem();a.setUserId(1003L);a.setSemester(SEM);a.setFeeType("E");a.setDurationHours(new BigDecimal("1.25"));a.setStatus(1);WorkloadMysqlFixture.authenticated(()->source.allowances.insertBizAllowanceItem(a));
  assertThat(summary().getNeedsRecalculation()).isZero();
  var stale=summary();evidence.decorate(stale);assertThat(stale.getSettlementEvidenceStatus()).isEqualTo("STALE");assertThat(stale.getSettlementSubmittable()).isFalse();
  assertThatThrownBy(()->f.tx.execute(status->{evidence.assertSubmittable(summary());return null;})).hasMessageContaining("其他酬金集合");
  var original=f.mapper(BizWorkloadSettlementSnapshotMapper.class).selectById(stale.getSettlementSnapshotId());var summaryHash=SettlementEvidence.decode(original.getPayloadJson(),original.getHash()).summary().hash();
  var p=f.pay.recalcPay(1003L,SEM);assertThat(p.getOtherPayTotal()).isEqualByComparingTo("75");
  var next=f.mapper(BizWorkloadSettlementSnapshotMapper.class).selectById(p.getSettlementSnapshotId());assertThat(SettlementEvidence.decode(next.getPayloadJson(),next.getHash()).summary().hash()).isEqualTo(summaryHash);assertThat(f.count("biz_workload_settlement_snapshot")).isEqualTo(2);evidence.assertSubmittable(summary());
 }
 @Test void mismatchedPointerFailsSubmitAndIndependentPay()throws Exception{
  var s=calculate();f.jdbc.update("UPDATE biz_pay_record SET settlement_snapshot_id=NULL WHERE semester=?",SEM);
  assertThatThrownBy(()->f.tx.execute(status->{evidence.assertSubmittable(summary());return null;})).hasMessageContaining("指针");assertThatThrownBy(()->f.pay.recalcPay(1003L,SEM)).hasMessageContaining("指针");assertThat(f.count("biz_workload_settlement_snapshot")).isEqualTo(1);
 }
 @Test void missingPointerDraftInActiveQaCannotSubmit()throws Exception{
  var s=calculate();f.jdbc.update("UPDATE biz_workload_summary SET settlement_snapshot_id=NULL WHERE id=?",s.getId());
  assertThatThrownBy(()->f.tx.execute(status->{evidence.assertSubmittable(summary());return null;})).hasMessageContaining("缺少结算证据");
 }
 @Test void itemVersionOrResultMutationFailsWithoutCurrentRuleReconstruction()throws Exception{
  calculate();f.jdbc.update("UPDATE biz_workload_item SET calculation_version=calculation_version+1 WHERE semester=?",SEM);
  assertThatThrownBy(()->f.tx.execute(status->{evidence.assertSubmittable(summary());return null;})).hasMessageContaining("版本");
 }
 @ParameterizedTest @ValueSource(strings={"snapshot","summaryPointer","payPointer"}) void postPayFailureRollsBackEveryWrite(String stage)throws Exception{
  imported();String table=stage.equals("snapshot")?"biz_workload_settlement_snapshot":stage.equals("summaryPointer")?"biz_workload_summary":"biz_pay_record";String event=stage.equals("snapshot")?"INSERT":"UPDATE";
  f.jdbc.execute("CREATE TRIGGER fail_settlement BEFORE "+event+" ON "+table+" FOR EACH ROW "+(event.equals("UPDATE")?"BEGIN IF NEW.settlement_snapshot_id IS NOT NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected settlement failure'; END IF; END":"SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected settlement failure'"));
  assertThatThrownBy(()->f.summary.recalcSummary(1003L,SEM,true)).hasMessageContaining("injected settlement");assertThat(f.count("biz_workload_settlement_snapshot")).isZero();assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM biz_workload_summary WHERE semester=?",Long.class,SEM)).isZero();assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM biz_pay_record WHERE semester=?",Long.class,SEM)).isZero();
 }
 @Test void legacyFinishedPolicyChangesNeverBackfillOrRewriteHistory()throws Exception{
  f.calc.recalcAll(1003L,WorkloadMysqlFixture.SEM);f.jdbc.update("UPDATE biz_workload_summary SET status=2,teacher_sign='original',academic_assistant_sign='signed',lock_time='2026-01-01'");var before=f.jdbc.queryForMap("SELECT * FROM biz_workload_summary WHERE semester=?",WorkloadMysqlFixture.SEM);f.jdbc.update("UPDATE biz_pay_rate SET rate=999");scope.setFormalStartSemester(WorkloadMysqlFixture.SEM);
  var old=f.mapper(BizWorkloadSummaryMapper.class).selectByUserSemesterForUpdate(1003L,WorkloadMysqlFixture.SEM);evidence.decorate(old);assertThat(old.getSettlementEvidenceStatus()).isEqualTo("HISTORICAL_MISSING");assertThat(old.getWorkloadCap()).isNull();assertThat(old.getTeacherNature()).isNull();assertThatThrownBy(()->f.summary.recalcSummary(1003L,WorkloadMysqlFixture.SEM,true)).hasMessageContaining("审批");assertThat(f.jdbc.queryForMap("SELECT * FROM biz_workload_summary WHERE semester=?",WorkloadMysqlFixture.SEM)).isEqualTo(before);assertThat(f.count("biz_workload_settlement_snapshot")).isZero();
 }

 void review()throws Exception {
  BizWorkloadItem query=new BizWorkloadItem();query.setUserId(1003L);query.setSemester(SEM);
  for(var row:f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemList(query))if(Integer.valueOf(0).equals(row.getStatus()))WorkloadMysqlFixture.authenticated(()->f.items.confirm(row.getId(),row.getCalculationVersion(),row.getCalculatedWorkload()));
 }
 @Test void submitWaitsForStrictCalculationCommitAndUsesTheNewVersion()throws Exception {
  calculate();review();long id=summary().getId();
  var calculated=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
  try {
   var first=pool.submit(()->f.tx.execute(status->{f.summary.recalcSummary(1003L,SEM,true);calculated.countDown();WorkloadConcurrencyIT.await(release);return null;}));
   assertThat(calculated.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
   var second=pool.submit(()->WorkloadMysqlFixture.authenticated(()->{f.audit.submit(id);return null;}));
   try {
    long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);boolean observed=false;
    while(System.nanoTime()<deadline&&!second.isDone()) {
     var count=f.admin.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l ON l.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID AND l.ENGINE=w.ENGINE WHERE l.OBJECT_SCHEMA=?",Integer.class,f.schema);
     if(count!=null&&count>0){observed=true;break;}Thread.sleep(20);
    }
    assertThat(observed).as("必须观察到随机schema内真实数据库锁等待").isTrue();assertThat(second.isDone()).isFalse();
   }finally{release.countDown();}
   first.get(10,java.util.concurrent.TimeUnit.SECONDS);second.get(10,java.util.concurrent.TimeUnit.SECONDS);
   assertThat(summary().getStatus()).isEqualTo(1);assertThat(f.count("biz_workload_settlement_snapshot")).isEqualTo(2);
   f.tx.execute(status->{evidence.assertSubmittable(summary());return null;});
  }finally{release.countDown();pool.shutdownNow();}
 }
 @ParameterizedTest @ValueSource(strings={"snapshot","summaryPointer","payPointer"})
 void failedNextVersionPreservesAllPriorAmountsPointersAndHashes(String stage)throws Exception {
  var s=calculate();var beforeSummary=f.jdbc.queryForMap("SELECT * FROM biz_workload_summary WHERE id=?",s.getId());var beforePay=f.jdbc.queryForMap("SELECT * FROM biz_pay_record WHERE summary_id=?",s.getId());var beforeEvidence=f.jdbc.queryForList("SELECT * FROM biz_workload_settlement_snapshot");
  f.jdbc.update("UPDATE biz_pay_rate SET rate=40");String table=stage.equals("snapshot")?"biz_workload_settlement_snapshot":stage.equals("summaryPointer")?"biz_workload_summary":"biz_pay_record";
  String body=stage.equals("snapshot")?"SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected next settlement failure'":"BEGIN IF NOT(NEW.settlement_snapshot_id <=> OLD.settlement_snapshot_id) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected next settlement failure'; END IF; END";
  f.jdbc.execute("CREATE TRIGGER fail_next_settlement BEFORE "+(stage.equals("snapshot")?"INSERT":"UPDATE")+" ON "+table+" FOR EACH ROW "+body);
  assertThatThrownBy(()->f.summary.recalcSummary(1003L,SEM,true)).hasMessageContaining("injected next settlement");
  assertThat(f.jdbc.queryForMap("SELECT * FROM biz_workload_summary WHERE id=?",s.getId())).isEqualTo(beforeSummary);assertThat(f.jdbc.queryForMap("SELECT * FROM biz_pay_record WHERE summary_id=?",s.getId())).isEqualTo(beforePay);assertThat(f.jdbc.queryForList("SELECT * FROM biz_workload_settlement_snapshot")).isEqualTo(beforeEvidence);
 }
 @ParameterizedTest @CsvSource({"产假,2097-09-01,2098-01-31,48.00,0","产假,2097-01-01,2097-08-31,96.00,0","在职读博,2097-09-01,2098-01-31,64.00,0","访学,2097-09-01,2098-01-31,96.00,1"})
 void specialStatusAndCalendarBoundariesAreFrozenAndReplayable(String special,String start,String end,String expected,int met)throws Exception {
  var d=source.row("G1");d.setSemester(SEM);source.imported(d);
  f.jdbc.update("UPDATE biz_teacher_profile SET special_status=?,special_status_start=?,special_status_end=?,enterprise_eval_result='优秀' WHERE user_id=1003",special,start,end);
  var s=f.summary.recalcSummary(1003L,SEM,true);assertThat(s.getBasicTeachingStandard()).isEqualByComparingTo(expected);assertThat(s.getBasicTeachingMet()).isEqualTo(met);
  var row=f.mapper(BizWorkloadSettlementSnapshotMapper.class).selectById(s.getSettlementSnapshotId());var saved=SettlementEvidence.decode(row.getPayloadJson(),row.getHash()).summary();
  assertThat(saved.person()).containsEntry("specialStatus",special).containsEntry("specialStart",start).containsEntry("specialEnd",end).containsEntry("enterpriseEvalResult","优秀");assertThat(saved.selection()).containsEntry("selectionMode","SEMESTER_END").containsEntry("asOfDate","2098-01-31");
  f.jdbc.update("UPDATE biz_teacher_profile SET special_status='正常',special_status_start=NULL,special_status_end=NULL");f.pay.recalcPay(1003L,SEM);assertThat(summary().getBasicTeachingStandard()).isEqualByComparingTo(expected);
 }
 @Test void completeManualG8G9ProofsSettleAndRejectedItemIsExplained()throws Exception {
  for(String type:List.of("G8","G9")){var input=new BizWorkloadItem();input.setUserId(1003L);input.setSemester(SEM);input.setItemType(type);input.setCalculatedWorkload(new BigDecimal("12.50"));input.setCourseName(type);WorkloadMysqlFixture.authenticated(()->f.items.insertBizWorkloadItem(input));}
  f.jdbc.update("INSERT INTO biz_workload_item(user_id,semester,item_type,source_type,calculated_workload,status) VALUES(1003,?,'G1','IMPORT',0,3)",SEM);
  var s=f.summary.recalcSummary(1003L,SEM,true);assertThat(s.getG8()).isEqualByComparingTo("12.50");assertThat(s.getG9()).isEqualByComparingTo("12.50");assertThat(s.getG7()).isEqualByComparingTo("0");review();WorkloadMysqlFixture.authenticated(()->{f.audit.submit(s.getId());return null;});
 }
 @Test void trueLegacyPendingWithoutAnySettlementHistoryCanFinishAfterActivation()throws Exception {
  f.calc.recalcAll(1003L,WorkloadMysqlFixture.SEM);var old=f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(9L);WorkloadMysqlFixture.authenticated(()->{f.items.confirm(9L,old.getCalculationVersion(),old.getCalculatedWorkload());f.audit.submit(f.summaryId());return null;});
  scope.setFormalStartSemester(WorkloadMysqlFixture.SEM);WorkloadMysqlFixture.authenticated(()->{f.audit.approve(f.summaryId());return null;});assertThat(f.jdbc.queryForObject("SELECT status FROM biz_workload_summary WHERE id=?",Integer.class,f.summaryId())).isEqualTo(2);assertThat(f.count("biz_workload_settlement_snapshot")).isZero();
 }
 @Test void corruptedInnerHashAndResignedWrongAmountFailWithoutDatabaseReconstruction()throws Exception {
  var s=calculate();review();var mapper=f.mapper(BizWorkloadSettlementSnapshotMapper.class);var original=mapper.selectById(s.getSettlementSnapshotId());var json=new com.fasterxml.jackson.databind.ObjectMapper();var tree=json.readTree(original.getPayloadJson());
  ((com.fasterxml.jackson.databind.node.ObjectNode)tree.path("summary").path("results")).put("performance","999.00");
  f.jdbc.update("UPDATE biz_workload_settlement_snapshot SET payload_json=?,hash=? WHERE id=?",tree.toString(),PolicyHash.hash(tree),original.getId());
  assertThatThrownBy(()->WorkloadMysqlFixture.authenticated(()->{f.audit.submit(s.getId());return null;})).hasMessageContaining("摘要");
  var payload=SettlementEvidence.decode(original.getPayloadJson(),original.getHash());var old=payload.summary();var results=new TreeMap<>(old.results());results.put("performance","999.00");
  var bad=SettlementEvidence.seal(new SettlementEvidence.SummaryEvidence(old.summaryId(),old.userId(),old.semester(),old.mode(),old.selection(),old.person(),old.eligibilityReason(),old.profileBoundary(),old.items(),old.rules(),old.rate(),old.rateHash(),results,old.threeTheoryCourses(),old.warnings(),null));
  var replaced=new SettlementEvidence.Payload(1,payload.settlementVersion(),SettlementEvidence.ALGORITHM,bad,payload.pay());
  f.jdbc.update("UPDATE biz_workload_settlement_snapshot SET payload_json=?,hash=? WHERE id=?",SettlementEvidence.encode(replaced),PolicyHash.hash(replaced),original.getId());
  assertThatThrownBy(()->WorkloadMysqlFixture.authenticated(()->{f.audit.submit(s.getId());return null;})).hasMessageContaining("金额或结果");assertThat(summary().getStatus()).isZero();
 }

 @ParameterizedTest @NullSource @ValueSource(strings={""," "})
 void newProfileCannotAcquireFullTimeThroughDatabaseDefault(String nature)throws Exception {
  var service=f.wire(new com.workload.system.service.impl.BizTeacherProfileServiceImpl());var profile=new BizTeacherProfile();profile.setUserId(2001L);profile.setTitle("讲师");profile.setTeacherNature(nature);
  assertThatThrownBy(()->WorkloadMysqlFixture.authenticated(()->service.insertBizTeacherProfile(profile))).hasMessageContaining("人员性质");
  assertThat(f.mapper(BizTeacherProfileMapper.class).selectBizTeacherProfileByUserId(2001L)).isNull();
 }
 @Test void omittedNatureOnPartialProfileUpdatePreservesExistingValue() {
  var service=f.wire(new com.workload.system.service.impl.BizTeacherProfileServiceImpl());var update=new BizTeacherProfile();update.setUserId(1003L);update.setTitle("副教授");service.updateBizTeacherProfile(update);
  assertThat(f.mapper(BizTeacherProfileMapper.class).selectBizTeacherProfileByUserId(1003L).getTeacherNature()).isEqualTo("专任");
 }

 @Test void genericSummaryAndPayEditsCannotInjectPointersMoneyOrEligibility()throws Exception {
  var s=calculate();var pay=pay();var snapshot=f.mapper(BizWorkloadSettlementSnapshotMapper.class).selectById(s.getSettlementSnapshotId());
  var summaryService=f.proxy(f.wire(new com.workload.system.service.impl.BizWorkloadSummaryServiceImpl()));var edit=new BizWorkloadSummary();edit.setId(s.getId());edit.setSettlementSnapshotId(999999L);edit.setPerformancePay(new BigDecimal("99999"));edit.setTeacherNature("行政");edit.setWorkloadCap(new BigDecimal("1"));edit.setSettlementSubmittable(true);edit.setRemark("合法备注");
  ((com.workload.system.service.IBizWorkloadSummaryService)summaryService).updateBizWorkloadSummary(edit);
  var payService=f.proxy(f.wire(new com.workload.system.service.impl.BizPayRecordServiceImpl()));var payEdit=new BizPayRecord();payEdit.setId(pay.getId());payEdit.setSettlementSnapshotId(999999L);payEdit.setCourseHourPay(new BigDecimal("99999"));payEdit.setTotalPay(99999L);payEdit.setTeacherNature("行政");payEdit.setSettlementEvidenceStatus("VERIFIED");payEdit.setRemark("酬金备注");
  ((com.workload.system.service.IBizPayRecordService)payService).updateBizPayRecord(payEdit);
  var after=summary();assertThat(after.getSettlementSnapshotId()).isEqualTo(s.getSettlementSnapshotId());assertThat(after.getPerformancePay()).isEqualByComparingTo(s.getPerformancePay());assertThat(pay().getTotalPay()).isEqualTo(pay.getTotalPay());evidence.decorate(after);assertThat(after.getTeacherNature()).isEqualTo("专任");assertThat(after.getWorkloadCap()).isEqualByComparingTo("540");assertThat(f.mapper(BizWorkloadSettlementSnapshotMapper.class).selectById(snapshot.getId()).getHash()).isEqualTo(snapshot.getHash());assertThat(f.count("biz_workload_settlement_snapshot")).isEqualTo(1);
 }
 @ParameterizedTest @ValueSource(ints={2,3})
 void disputedOrRejectedMembershipChangeBlocksRealSubmit(int status)throws Exception {
  var s=calculate();review();f.jdbc.update("UPDATE biz_workload_item SET status=? WHERE semester=?",status,SEM);
  assertThatThrownBy(()->WorkloadMysqlFixture.authenticated(()->{f.audit.submit(s.getId());return null;})).isInstanceOf(com.workload.common.exception.ServiceException.class);assertThat(summary().getStatus()).isZero();
  assertThatThrownBy(()->f.tx.execute(tx->{evidence.assertSubmittable(summary());return null;})).hasMessageContaining("集合已变化");
 }
 @Test void teachingSourceDirtyStillBlocksIndependentPay()throws Exception {
  calculate();var update=new BizTeachingTask();update.setUserId(1003L);update.setSemester(SEM);update.setCourseName("新增未生成教学源");update.setStatus(1);f.tasks.insertBizTeachingTask(update);
  assertThat(summary().getNeedsRecalculation()).isEqualTo(1);assertThatThrownBy(()->f.pay.recalcPay(1003L,SEM)).hasMessageContaining("源数据已变更");assertThat(f.count("biz_workload_settlement_snapshot")).isEqualTo(1);
 }
 @Test void scopeDeactivationCannotSilentlyDowngradeExistingSettlement()throws Exception {
  calculate();var before=f.jdbc.queryForMap("SELECT * FROM biz_pay_record WHERE semester=?",SEM);scope.setVerificationSemesters(List.of());
  assertThatThrownBy(()->f.pay.recalcPay(1003L,SEM)).hasMessageContaining("禁止降级");assertThatThrownBy(()->f.summary.recalcSummary(1003L,SEM,true)).hasMessageContaining("禁止降级");assertThat(f.jdbc.queryForMap("SELECT * FROM biz_pay_record WHERE semester=?",SEM)).isEqualTo(before);
  f.jdbc.update("UPDATE biz_workload_summary SET needs_recalculation=1 WHERE semester=?",SEM);var stale=summary();evidence.decorate(stale);assertThat(stale.getSettlementRequired()).isTrue();assertThat(stale.getSettlementSubmittable()).isFalse();
 }

 @Test void readOnlyHistoricalViewRemainsConsistentAcrossConcurrentNewCalculation()throws Exception {
  var old=calculate();var pool=java.util.concurrent.Executors.newSingleThreadExecutor();var read=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
  try {
   var reader=pool.submit(()->{var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(f.ds));tx.setReadOnly(true);return tx.execute(status->{var row=f.mapper(BizWorkloadSummaryMapper.class).selectBizWorkloadSummaryById(old.getId());read.countDown();WorkloadConcurrencyIT.await(release);evidence.decorate(row);return row;});});
   assertThat(read.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();f.calc.run(new CalculationRunRequest(1003L,SEM,false));release.countDown();
   var displayed=reader.get(10,java.util.concurrent.TimeUnit.SECONDS);assertThat(displayed.getSettlementSnapshotId()).isEqualTo(old.getSettlementSnapshotId());assertThat(displayed.getSettlementEvidenceStatus()).isEqualTo("VERIFIED");assertThat(summary().getSettlementSnapshotId()).isNotEqualTo(old.getSettlementSnapshotId());
  }finally{release.countDown();pool.shutdownNow();}
 }
 @Test void actualAttachment2UsesFrozenNatureCapAndReadableQaAndLegacyMissingLabels()throws Exception {
  for(String table:List.of("sys_user","sys_dept"))f.jdbc.execute((String)f.admin.queryForObject("SHOW CREATE TABLE "+table,(rs,n)->rs.getString(2)));
  f.jdbc.update("INSERT INTO sys_dept(dept_id,dept_name) VALUES(100,'隔离学院')");f.jdbc.update("INSERT INTO sys_user(user_id,user_name,nick_name,dept_id) VALUES(1003,'T1003','隔离教师',100)");
  String resource="mapper/system/BizExportMapper.xml";try(var in=getClass().getClassLoader().getResourceAsStream(resource)){new org.apache.ibatis.builder.xml.XMLMapperBuilder(in,f.sessions.getConfiguration(),resource,f.sessions.getConfiguration().getSqlFragments()).parse();}
  var current=calculate();var changedAllowance=new BizAllowanceItem();changedAllowance.setUserId(1003L);changedAllowance.setSemester(SEM);changedAllowance.setFeeType("E");changedAllowance.setDurationHours(new BigDecimal("1.25"));changedAllowance.setStatus(1);WorkloadMysqlFixture.authenticated(()->source.allowances.insertBizAllowanceItem(changedAllowance));f.calc.recalcAll(1003L,WorkloadMysqlFixture.SEM);f.jdbc.update("UPDATE biz_teacher_profile SET teacher_nature='行政'");f.jdbc.update("DELETE FROM biz_workload_rule");f.jdbc.update("UPDATE biz_pay_rate SET rate=999");
  var service=f.proxy(f.wire(new com.workload.system.service.impl.BizWorkloadSummaryServiceImpl()));var controller=new com.workload.system.controller.BizExportController();ReflectionTestUtils.setField(controller,"exportMapper",f.mapper(BizExportMapper.class));ReflectionTestUtils.setField(controller,"summaries",service);
  for(String semester:List.of(SEM,WorkloadMysqlFixture.SEM)){
   var response=new org.springframework.mock.web.MockHttpServletResponse();WorkloadMysqlFixture.authenticated(()->{controller.exportPaySummary(semester,response);return null;});
   try(var workbook=org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(response.getContentAsByteArray()))){var sheet=workbook.getSheetAt(0);Map<String,Integer> header=new HashMap<>();for(var cell:sheet.getRow(0))header.put(cell.getStringCellValue(),cell.getColumnIndex());var row=sheet.getRow(1);String notes=row.getCell(header.get("备注")).getStringCellValue();
    if(semester.equals(SEM)){assertThat(row.getCell(header.get("人员性质")).getStringCellValue()).isEqualTo("专任");assertThat(notes).contains("隔离验收","历史默认","需重新结算","已保存旧版","其他酬金集合已变化");}
    else {assertThat(notes).contains("历史结算证据缺失","人员性质","具体封顶阈值未知");var nature=row.getCell(header.get("人员性质"));assertThat(nature==null||nature.toString().isBlank()).isTrue();}
   }
  }
 }

 @Test void strictPersistAndPreviewApplyZeroFloorWhenCapIsBelowRated()throws Exception {
  imported();f.jdbc.update("INSERT INTO biz_workload_rule(rule_code,rule_value,rule_desc,effective_from,status) VALUES('CAP_200PCT',60,'低上限边界','2025-01-01',1)");
  assertThat(f.summary.recalcSummary(1003L,SEM,false).getPerformancePay()).isEqualByComparingTo("0");var s=f.summary.recalcSummary(1003L,SEM,true);assertThat(s.getPerformancePay()).isEqualByComparingTo("0");assertThat(pay().getCourseHourPay()).isEqualByComparingTo("0");evidence.assertSubmittable(summary());
 }
 @Test void threeDistinctTheoryCourseNamesUseFrozenDeduplicationBasis()throws Exception {
  for(int index=0;index<3;index++){var row=source.row("G1");row.setSemester(SEM);row.setCourseName("去重课程"+index);source.imported(row);}
  var s=f.summary.recalcSummary(1003L,SEM,true);assertThat(s.getTotalWorkload()).isLessThan(s.getBasicTeachingStandard());assertThat(s.getBasicTeachingMet()).isEqualTo(1);var snapshot=f.mapper(BizWorkloadSettlementSnapshotMapper.class).selectById(s.getSettlementSnapshotId());var saved=SettlementEvidence.decode(snapshot.getPayloadJson(),snapshot.getHash()).summary();assertThat(saved.threeTheoryCourses()).isTrue();assertThat(saved.items()).extracting(SettlementEvidence.Item::courseName).containsExactly("去重课程0","去重课程1","去重课程2");
 }

 @ParameterizedTest @CsvSource({"summary,false","summary,true","pay,false","pay,true"})
 void clearingPointersAndRemovingScopeCannotDowngradeExistingHistory(String entry,boolean clearPay)throws Exception {
  var s=calculate();f.jdbc.update("UPDATE biz_workload_summary SET settlement_snapshot_id=NULL WHERE id=?",s.getId());if(clearPay)f.jdbc.update("UPDATE biz_pay_record SET settlement_snapshot_id=NULL WHERE semester=?",SEM);scope.setVerificationSemesters(List.of());f.jdbc.update("UPDATE biz_pay_rate SET rate=999");
  var beforeSummary=f.jdbc.queryForMap("SELECT * FROM biz_workload_summary WHERE id=?",s.getId());var beforePay=f.jdbc.queryForMap("SELECT * FROM biz_pay_record WHERE semester=?",SEM);var beforeEvidence=f.jdbc.queryForList("SELECT * FROM biz_workload_settlement_snapshot");
  assertThatThrownBy(()->{if(entry.equals("summary"))f.summary.recalcSummary(1003L,SEM,true);else f.pay.recalcPay(1003L,SEM);}).isInstanceOf(com.workload.common.exception.ServiceException.class).extracting("code").isEqualTo(50017);
  assertThat(f.jdbc.queryForMap("SELECT * FROM biz_workload_summary WHERE id=?",s.getId())).isEqualTo(beforeSummary);assertThat(f.jdbc.queryForMap("SELECT * FROM biz_pay_record WHERE semester=?",SEM)).isEqualTo(beforePay);assertThat(f.jdbc.queryForList("SELECT * FROM biz_workload_settlement_snapshot")).isEqualTo(beforeEvidence);
 }
 @Test void missingRatePreviewExplainsWhyItCannotComplete()throws Exception {
  imported();f.jdbc.update("DELETE FROM biz_pay_rate");var preview=f.summary.recalcSummary(1003L,SEM,false);assertThat(preview.getPerformancePay()).isEqualByComparingTo("0");assertThat(preview.getSettlementEvidenceStatus()).isEqualTo("PREVIEW_INELIGIBLE");assertThat(preview.getEligibilityReason()).contains("费率");assertThat(preview.getSettlementSubmittable()).isFalse();
 }

 com.workload.system.service.IBizWorkloadSummaryService summaryCrud(){return f.proxy(f.wire(new com.workload.system.service.impl.BizWorkloadSummaryServiceImpl()));}
 com.workload.system.service.IBizPayRecordService payCrud(){return f.proxy(f.wire(new com.workload.system.service.impl.BizPayRecordServiceImpl()));}
 @ParameterizedTest @CsvSource({"summary,false,false","summary,false,true","summary,true,false","summary,true,true","pay,false,false","pay,false,true","pay,true,false","pay,true,true"})
 void genericDeleteCannotDestroySettlementOrPartiallyDeleteMixedBatch(String entity,boolean batch,boolean clearPointers)throws Exception {
  var current=calculate();var currentPay=pay();f.calc.recalcAll(1003L,WorkloadMysqlFixture.SEM);var legacyId=f.jdbc.queryForObject("SELECT id FROM "+(entity.equals("summary")?"biz_workload_summary":"biz_pay_record")+" WHERE semester=?",Long.class,WorkloadMysqlFixture.SEM);
  if(clearPointers){f.jdbc.update("UPDATE biz_workload_summary SET settlement_snapshot_id=NULL WHERE semester=?",SEM);f.jdbc.update("UPDATE biz_pay_record SET settlement_snapshot_id=NULL WHERE semester=?",SEM);}
  var beforeSummary=f.jdbc.queryForList("SELECT * FROM biz_workload_summary ORDER BY id");var beforePay=f.jdbc.queryForList("SELECT * FROM biz_pay_record ORDER BY id");var beforeSnapshots=f.jdbc.queryForList("SELECT * FROM biz_workload_settlement_snapshot ORDER BY id");
  assertThatThrownBy(()->{if(entity.equals("summary")){if(batch)summaryCrud().deleteBizWorkloadSummaryByIds(new Long[]{legacyId,current.getId()});else summaryCrud().deleteBizWorkloadSummaryById(current.getId());}else{if(batch)payCrud().deleteBizPayRecordByIds(new Long[]{legacyId,currentPay.getId()});else payCrud().deleteBizPayRecordById(currentPay.getId());}}).isInstanceOf(com.workload.common.exception.ServiceException.class).hasMessageContaining("结算证据");
  assertThat(f.jdbc.queryForList("SELECT * FROM biz_workload_summary ORDER BY id")).isEqualTo(beforeSummary);assertThat(f.jdbc.queryForList("SELECT * FROM biz_pay_record ORDER BY id")).isEqualTo(beforePay);assertThat(f.jdbc.queryForList("SELECT * FROM biz_workload_settlement_snapshot ORDER BY id")).isEqualTo(beforeSnapshots);
 }
 @ParameterizedTest @ValueSource(strings={"summary","pay"})
 void trueLegacyDeleteSemanticsRemainAvailableForSingleAndBatch(String entity)throws Exception {
  f.calc.recalcAll(1003L,WorkloadMysqlFixture.SEM);String table=entity.equals("summary")?"biz_workload_summary":"biz_pay_record";Long id=f.jdbc.queryForObject("SELECT id FROM "+table+" WHERE semester=?",Long.class,WorkloadMysqlFixture.SEM);
  assertThat(entity.equals("summary")?summaryCrud().deleteBizWorkloadSummaryById(id):payCrud().deleteBizPayRecordById(id)).isEqualTo(1);
  f.calc.recalcAll(1003L,WorkloadMysqlFixture.SEM);Long next=f.jdbc.queryForObject("SELECT id FROM "+table+" WHERE semester=?",Long.class,WorkloadMysqlFixture.SEM);
  assertThat(entity.equals("summary")?summaryCrud().deleteBizWorkloadSummaryByIds(new Long[]{next}):payCrud().deleteBizPayRecordByIds(new Long[]{next})).isEqualTo(1);assertThat(f.count("biz_workload_settlement_snapshot")).isZero();
 }
 @ParameterizedTest @ValueSource(strings={"summary","pay"})
 void deleteWaitingOnPeriodMustSeeNewSettlementInsteadOfInitialNullPointer(String entity)throws Exception {
  imported();scope.setVerificationSemesters(List.of());var legacy=f.summary.recalcSummary(1003L,SEM,true);var legacyPay=f.pay.currentPay(1003L,SEM);assertThat(legacy.getSettlementSnapshotId()).isNull();scope.setVerificationSemesters(List.of(SEM));
  var locked=new java.util.concurrent.CountDownLatch(1);var recalc=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
  try {
   var first=pool.submit(()->f.tx.execute(status->{f.guard.lockDraftOrAbsent(1003L,SEM);locked.countDown();WorkloadConcurrencyIT.await(recalc);f.summary.recalcSummary(1003L,SEM,true);return null;}));assertThat(locked.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
   var second=pool.submit(()->entity.equals("summary")?summaryCrud().deleteBizWorkloadSummaryById(legacy.getId()):payCrud().deleteBizPayRecordById(legacyPay.getId()));
   try{long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);boolean observed=false;while(System.nanoTime()<deadline&&!second.isDone()){var count=f.admin.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l ON l.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID AND l.ENGINE=w.ENGINE WHERE l.OBJECT_SCHEMA=?",Integer.class,f.schema);if(count!=null&&count>0){observed=true;break;}Thread.sleep(20);}assertThat(observed).as("必须观察到随机schema内删除等待period的真实锁等待").isTrue();}finally{recalc.countDown();}
   first.get(10,java.util.concurrent.TimeUnit.SECONDS);assertThatThrownBy(()->second.get(10,java.util.concurrent.TimeUnit.SECONDS)).hasCauseInstanceOf(com.workload.common.exception.ServiceException.class).hasStackTraceContaining("结算证据");assertThat(summary().getSettlementSnapshotId()).isNotNull();assertThat(pay().getSettlementSnapshotId()).isEqualTo(summary().getSettlementSnapshotId());assertThat(f.count("biz_workload_settlement_snapshot")).isEqualTo(1);
  }finally{recalc.countDown();pool.shutdownNow();}
 }
}
