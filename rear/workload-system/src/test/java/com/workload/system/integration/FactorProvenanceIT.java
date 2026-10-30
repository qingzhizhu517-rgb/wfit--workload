package com.workload.system.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.aop.framework.Advised;
import com.workload.system.calc.*;
import com.workload.system.calc.rule.*;
import com.workload.system.calc.strategy.*;
import com.workload.system.calc.allowance.*;
import com.workload.system.domain.*;
import com.workload.system.domain.dto.*;
import com.workload.system.mapper.*;
import com.workload.system.service.*;
import com.workload.system.service.impl.*;
import com.workload.common.core.domain.entity.SysUser;

/** Real mapper + explicit resolver + strategy + transaction lifecycle in a disposable 13306 schema. */
class FactorProvenanceIT {
 WorkloadMysqlFixture f; ITeachingTaskImportService importer; IBizAllowanceItemService allowances;
 @BeforeEach void setup() throws Exception {
  f=new WorkloadMysqlFixture();
  for(String table:List.of("biz_workload_rule","biz_import_batch","biz_import_row")) f.jdbc.execute((String)f.admin.queryForObject("SHOW CREATE TABLE "+table,(rs,n)->rs.getString(2)));
  String resource="mapper/system/BizWorkloadRuleMapper.xml";
  try(var in=getClass().getClassLoader().getResourceAsStream(resource)){new org.apache.ibatis.builder.xml.XMLMapperBuilder(in,f.sessions.getConfiguration(),resource,f.sessions.getConfiguration().getSqlFragments()).parse();}
  RuleParamServiceImpl rules=new RuleParamServiceImpl();ReflectionTestUtils.setField(rules,"bizWorkloadRuleMapper",f.mapper(BizWorkloadRuleMapper.class));f.beans.put(RuleParamService.class,rules);
  Object calc=((Advised)f.calc).getTargetSource().getTarget();ReflectionTestUtils.setField(calc,"ruleParamService",rules);
  Map<String,WorkloadCalcStrategy> strategies=new LinkedHashMap<>();
  strategies.put("theoryCalcStrategy",f.wire(new TheoryCalcStrategy()));strategies.put("practiceCalcStrategy",f.wire(new PracticeCalcStrategy()));strategies.put("internshipTrainingCalcStrategy",f.wire(new InternshipTrainingCalcStrategy()));
  strategies.put("courseDesignCalcStrategy",f.wire(new CourseDesignCalcStrategy()));strategies.put("thesisCalcStrategy",f.wire(new ThesisCalcStrategy()));strategies.put("concentratedInternshipCalcStrategy",f.wire(new ConcentratedInternshipCalcStrategy()));strategies.put("managementCalcStrategy",f.wire(new ManagementCalcStrategy()));
  var factory=(CalcStrategyFactory)f.beans.get(CalcStrategyFactory.class);ReflectionTestUtils.setField(factory,"strategyMap",strategies);
  int index=2;for(String bean:List.of("practiceCalcStrategy","internshipTrainingCalcStrategy","courseDesignCalcStrategy","thesisCalcStrategy","concentratedInternshipCalcStrategy")) {f.jdbc.update("INSERT INTO biz_workload_category_dict(type_code,type_name,parent_group,calc_strategy) VALUES(?,?,?,?)","G"+index,"test","G7",bean);index++;}
  f.jdbc.update("INSERT INTO biz_workload_category_dict(type_code,type_name,parent_group,calc_strategy) VALUES('G11','管理','G11','managementCalcStrategy')");
  Map<String,String> values=new TreeMap<>(Map.of("COEF_REPEAT_1ST","1","COEF_REPEAT_2ND","0.9","COEF_REPEAT_3RD_UP","0.8","COEF_CLASS_151_UP","1.2","COEF_CLASS_120_150","1.1","COEF_PRACTICE_LG","1","COEF_PRACTICE_OTHER","0.9","COEF_TRAIN_D_LG","4","COEF_TRAIN_D_ART","3","COEF_TRAIN_D_HUM","2"));
  values.putAll(Map.of("COEF_THESIS_K5_LG_B","9","COEF_THESIS_K5_LG_C","5","COEF_THESIS_K5_HU_B","6","COEF_THESIS_K5_HU_C","4","CAP_R4_MAX","60","CAP_R6_MAX","20","CONST_COURSE_DESIGN","0.4","APPROVAL_R5_BACHELOR","8","APPROVAL_R5_JUNIOR","15","PAY_UNIT_FEE","30"));
  values.putAll(Map.of("PAY_A_SELF_LT6","120","PAY_A_SELF_6_20","260","PAY_B_DISPERSED","10","PAY_B_CONCENTRATED","15","PAY_C_THESIS","120","PAY_D_MARKING_60_120","80","PAY_E_HOURLY","60","PAY_F_DAY_UNITS","6","PAY_F_CLASS_UNITS","1"));
  values.forEach((code,value)->f.jdbc.update("INSERT INTO biz_workload_rule(rule_code,rule_value,rule_desc,effective_from,status) VALUES(?,?,?,'2025-01-01',1)",code,new BigDecimal(value),"原始政策 "+code));
  ISysUserService users=mock(ISysUserService.class);when(users.selectUserByUserName("T1003")).thenReturn(new SysUser(1003L));f.beans.put(ISysUserService.class,users);
  importer=f.proxy(f.wire(new TeachingTaskImportServiceImpl()));
  var allowanceFactory=new AllowanceStrategyFactory();ReflectionTestUtils.setField(allowanceFactory,"strategies",List.of(new AllowanceAStrategy(),new AllowanceBStrategy(),new AllowanceCStrategy(),new AllowanceDStrategy(),new AllowanceEStrategy(),new AllowanceFStrategy(),new AllowanceGStrategy()));allowanceFactory.init();f.beans.put(AllowanceStrategyFactory.class,allowanceFactory);
  allowances=f.proxy(f.wire(new BizAllowanceItemServiceImpl()));
 }
 @AfterEach void close(){if(f!=null)f.close();}
 TeachingTaskImportDTO row(String type){var d=new TeachingTaskImportDTO();d.setSemester(WorkloadMysqlFixture.SEM);d.setUserCode("T1003");d.setCourseName("来源测试"+UUID.randomUUID());d.setWorkloadType(type);d.setMajorCategory("理工类");d.setCourseNature("必修");d.setEducationLevel("本科");d.setStudentCount(10);d.setTeachingEval("不合格");d.setCourseLevel("省级一流");d.setCourseRole("主持人");d.setRepeatOrder(1);switch(type){case "G1","G2"->d.setPlannedHours(new BigDecimal("10"));case "G3"->d.setDays(new BigDecimal("2"));case "G4"->d.setCredits(new BigDecimal("2"));case "G6"->d.setWeeks(new BigDecimal("2"));}return d;}
 BizWorkloadItem imported(TeachingTaskImportDTO d)throws Exception {WorkloadMysqlFixture.authenticated(()->importer.processSingleRow(d,"IMP-REAL"));return f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(f.jdbc.queryForObject("SELECT id FROM biz_workload_item WHERE course_name=?",Long.class,d.getCourseName()));}
 Map<String,FactorProvenance.Proof> proofs(BizWorkloadItem i){return FactorProvenance.read(i.getFactorProvenanceJson(),i.getId(),i.getItemType());}
 @ParameterizedTest @ValueSource(strings={"G1","G2","G3","G4","G5","G6"})
 void actualImportSavesInputsBranchesRulesAndFormat4(String type)throws Exception {
  var d=row(type);if(type.equals("G1"))d.setStudentCount(160);
  var item=imported(d);var p=proofs(item);assertThat(p).isNotEmpty();
  p.values().forEach(proof->{assertThat(proof.references()).containsEntry("batch","IMP-REAL").containsKey("rawInputJson").containsKey("normalizedInputJson");FactorProvenance.verify(proof);});
  var snapshot=f.mapper(BizWorkloadCalcSnapshotMapper.class).selectLatestByItemId(item.getId());assertThat(snapshot.getContextJson()).contains("\"formatVersion\":4");SnapshotIntegrity.verify(snapshot);
  assertThat(f.formulas.build(item).getEvidenceStatus()).isEqualTo("COMPLETE");
  if(type.equals("G1")){assertThat(p.get("C1").kind()).isEqualTo("DB_RULE");assertThat(p.get("K1").branchContent()).contains("必修");assertThat(p.get("Q1").inputs()).containsEntry("teachingEval","不合格");assertThat(p.get("N").rules()).extracting(RuleResolution::requestedCode).containsExactly("COEF_CLASS_151_UP");}
  if(type.equals("G2"))assertThat(p.get("C2").branchContent()).contains("0.9");
  if(type.equals("G3"))assertThat(p.get("K").kind()).isEqualTo("CODE_POLICY");
 }
 @ParameterizedTest @CsvSource({"G2,K,1","G3,D,4","G5,K5,9"})
 void explicitImportOverrideEqualToPolicyStillOverrides(String type,String key,String value)throws Exception {var d=row(type);d.setCourseCoefficient(new BigDecimal(value));var i=imported(d);assertThat(proofs(i).get(key).kind()).isEqualTo("IMPORT_OVERRIDE");assertThat(proofs(i).get(key).rules()).isEmpty();}
 @Test void sourceMutationCannotReplaceSavedDerivedOriginOnRecalculation()throws Exception {
  var item=imported(row("G1"));String before=item.getFactorProvenanceJson();
  f.jdbc.update("UPDATE biz_workload_rule SET rule_code=CONCAT('RENAMED_',rule_code),rule_value=999,rule_desc='changed'");f.jdbc.update("DELETE FROM biz_workload_rule");
  WorkloadMysqlFixture.authenticated(()->f.calc.recalcItem(item.getId()));var now=f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(item.getId());assertThat(now.getFactorProvenanceJson()).isEqualTo(before);
  var v=f.formulas.build(now);assertThat(v.getEvidenceStatus()).isEqualTo("COMPLETE");assertThat(v.getPersistedFactorSources()).anyMatch(p->p.rules().stream().anyMatch(r->r.content().description().startsWith("原始政策")));
 }
 @Test void sameValueManualEditReplacesSourceAndRemarkOnlyPreservesOthers()throws Exception {
  var item=imported(row("G1"));var old=proofs(item);var edit=new BizWlTheory();edit.setItemId(item.getId());edit.setC1(new BigDecimal("1.00"));
  WorkloadMysqlFixture.authenticated(()->f.theory.updateBizWlTheory(edit));var after=f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(item.getId());var changed=proofs(after);assertThat(changed.get("C1").kind()).isEqualTo("MANUAL_INPUT");assertThat(changed.get("Q1")).isEqualTo(old.get("Q1"));
  var note=new BizWlTheory();note.setItemId(item.getId());note.setRemark("只改备注");WorkloadMysqlFixture.authenticated(()->f.theory.updateBizWlTheory(note));assertThat(f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(item.getId()).getFactorProvenanceJson()).isEqualTo(after.getFactorProvenanceJson());
 }
 @Test void oldEqualMaterializedCoefficientRemainsMissing()throws Exception {WorkloadMysqlFixture.authenticated(()->f.calc.recalcItem(9L));assertThat(f.formulas.build(f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(9L)).getEvidenceStatus()).isEqualTo("PARTIAL");}
 @Test void importFailureAfterSourceWriteRollsBackTaskDetailProvenanceSnapshotAndVersion()throws Exception {
  long items=f.count("biz_workload_item"),tasks=f.count("biz_teaching_task");
  f.jdbc.execute("CREATE TRIGGER fail_snapshot BEFORE INSERT ON biz_workload_calc_snapshot FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected post-source failure'");
  assertThatThrownBy(()->imported(row("G1"))).hasMessageContaining("injected post-source");assertThat(f.count("biz_workload_item")).isEqualTo(items);assertThat(f.count("biz_teaching_task")).isEqualTo(tasks);assertThat(f.count("biz_workload_calc_snapshot")).isZero();assertThat(f.count("biz_wl_theory")).isEqualTo(1);
 }
 @Test void approvingCapturesCompleteActualApprovalRowAndFailureRollsItAllBack()throws Exception {
  var item=imported(row("G1"));String initial=item.getFactorProvenanceJson();long version=item.getCalculationVersion();
  f.jdbc.update("INSERT INTO biz_coefficient_adjustment(id,user_id,semester,task_id,item_id,category,factor_code,old_value,requested_value,reason,attachment_url,status,base_calculation_version,create_by) VALUES(50,1003,?,?,?,'G1','Q1',0.8,1,'实际申请理由','/evidence.pdf',0,?,'actual-applicant')",item.getSemester(),item.getTaskId(),item.getId(),version);
  f.jdbc.execute("CREATE TRIGGER fail_snapshot BEFORE INSERT ON biz_workload_calc_snapshot FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected approval failure'");
  assertThatThrownBy(()->WorkloadMysqlFixture.authenticated(()->{f.adjustments.approve(50L,"批准意见");return null;})).hasMessageContaining("injected approval");
  assertThat(f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(item.getId()).getFactorProvenanceJson()).isEqualTo(initial);assertThat(f.jdbc.queryForObject("SELECT status FROM biz_coefficient_adjustment WHERE id=50",Integer.class)).isZero();
  f.jdbc.execute("DROP TRIGGER fail_snapshot");WorkloadMysqlFixture.authenticated(()->{f.adjustments.approve(50L,"批准意见");return null;});
  var after=f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(item.getId());var proof=proofs(after).get("Q1");assertThat(proof.kind()).isEqualTo("APPROVED_OVERRIDE");assertThat(proof.references().get("approvalJson")).contains("实际申请理由","actual-applicant","批准意见","0.80","/evidence.pdf");
  f.jdbc.update("DELETE FROM biz_coefficient_adjustment WHERE id=50");WorkloadMysqlFixture.authenticated(()->f.calc.recalcItem(item.getId()));assertThat(f.formulas.build(f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(item.getId())).getPersistedFactorSources()).contains(proof);
 }
 @ParameterizedTest @ValueSource(strings={"A","B","C","D","E","F","G"})
 void allowanceActualAmountAndEvidenceAreBoundAndIndependentOfRules(String type)throws Exception {
  var a=new BizAllowanceItem();a.setUserId(1003L);a.setSemester(WorkloadMysqlFixture.SEM);a.setFeeType(type);a.setFeeSubtype(type.equals("A")?"自学":"集中");a.setStudentCount(70L);a.setDurationHours(new BigDecimal("1.555"));a.setDays(new BigDecimal("2"));a.setClassCount(3L);a.setWorkloadUnits(new BigDecimal("4"));a.setAmount(new BigDecimal("5"));a.setStatus(1);
  WorkloadMysqlFixture.authenticated(()->allowances.insertBizAllowanceItem(a));var saved=allowances.selectBizAllowanceItemById(a.getId());var evidence=AllowanceEvidence.verify(saved.getCalculationEvidenceJson());assertThat(evidence.allowanceId()).isEqualTo(a.getId());assertThat(evidence.rounding()).isEqualTo("scale=2;HALF_UP");assertThat(evidence.inputs().get("durationHours")).isEqualTo("1.555");assertThat(evidence.persistedInputs().get("durationHours")).isEqualTo("1.56");
  f.jdbc.update("DELETE FROM biz_workload_rule");assertThat(allowances.selectBizAllowanceItemById(a.getId()).getCalculationEvidenceJson()).isEqualTo(saved.getCalculationEvidenceJson());
  f.jdbc.update("UPDATE biz_allowance_item SET user_id=1009 WHERE id=?",a.getId());assertThatThrownBy(()->allowances.selectBizAllowanceItemById(a.getId())).hasMessageContaining("绑定");
 }
 @Test void manualInputCannotLosePrecisionAndIsRolledBack()throws Exception {var d=row("G2");d.setCourseCoefficient(new BigDecimal("1.123"));assertThatThrownBy(()->imported(d)).hasMessageContaining("2位");var i=imported(row("G1"));var edit=new BizWlTheory();edit.setItemId(i.getId());edit.setC1(new BigDecimal("1.123"));assertThatThrownBy(()->WorkloadMysqlFixture.authenticated(()->f.theory.updateBizWlTheory(edit))).hasMessageContaining("DECIMAL");assertThat(proofs(f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(i.getId())).get("C1").kind()).isEqualTo("DB_RULE");}
 @ParameterizedTest @CsvSource({"G1,2,0.9","G1,3,0.8","G3,2,0.9","G3,3,0.9"})
 void explicitRepeatUsesActualSelectedPolicyAndKeepsOtherInputs(String type,int order,String expected)throws Exception {
  var d=row(type);d.setRepeatOrder(order);var item=imported(d);var p=proofs(item);String key=type.equals("G1")?"C1":"K";
  assertThat(new BigDecimal(p.get(key).value())).isEqualByComparingTo(expected);assertThat(p.get(key).inputs()).containsEntry("resolvedRepeatOrder",Integer.toString(order));
  assertThat(f.formulas.build(item).getEvidenceStatus()).isEqualTo("COMPLETE");
 }
 @Test void automaticallyResolvedSecondAndThirdRepeatAreCaptured()throws Exception {
  var a=row("G1");a.setRepeatOrder(null);var b=row("G1");b.setRepeatOrder(null);b.setCourseName(a.getCourseName());b.setClassName("second");var c=row("G1");c.setRepeatOrder(null);c.setCourseName(a.getCourseName());c.setClassName("third");
  imported(a);WorkloadMysqlFixture.authenticated(()->importer.processSingleRow(b,"IMP-2"));WorkloadMysqlFixture.authenticated(()->importer.processSingleRow(c,"IMP-3"));
  var ids=f.jdbc.queryForList("SELECT id FROM biz_workload_item WHERE course_name=? ORDER BY id",Long.class,a.getCourseName());assertThat(ids).hasSize(3);
  for(int n=0;n<ids.size();n++){var item=f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(ids.get(n));assertThat(proofs(item).get("C1").inputs()).containsEntry("resolvedRepeatOrder",Integer.toString(n+1));assertThat(f.formulas.build(item).getEvidenceStatus()).isEqualTo("COMPLETE");}
 }
 @ParameterizedTest @CsvSource({"G1,N,119,理工类,本科,1","G1,N,120,理工类,本科,1.1","G1,N,151,理工类,本科,1.2","G2,K,10,文史类,本科,0.9","G3,D,10,艺术类,本科,3","G3,D,10,文史类,本科,2","G5,K5,10,理工类,专科,5","G5,K5,10,文史类,本科,6","G5,K5,10,文史类,专科,4"})
 void conditionalInputsBindTheActualMaterializedBranch(String type,String key,int count,String major,String level,String value)throws Exception {
  var d=row(type);d.setStudentCount(count);d.setMajorCategory(major);d.setEducationLevel(level);var item=imported(d);assertThat(new BigDecimal(proofs(item).get(key).value())).isEqualByComparingTo(value);assertThat(f.formulas.build(item).getEvidenceStatus()).isEqualTo("COMPLETE");
 }
 @ParameterizedTest @CsvSource({"省级一流,主持人,1.5","省级一流,团队前3,1.2","校级精品,主持人,1.2","校级精品,团队前3,1.1","其他,独立,1"})
 void courseQualityAndDisplayOnlyBranchesAreSaved(String level,String role,String q2)throws Exception {var d=row("G1");d.setCourseLevel(level);d.setCourseRole(role);d.setCourseNature("选修");d.setTeachingEval("优秀");var p=proofs(imported(d));assertThat(new BigDecimal(p.get("Q2").value())).isEqualByComparingTo(q2);assertThat(new BigDecimal(p.get("K1").value())).isEqualByComparingTo("1");assertThat(new BigDecimal(p.get("Q1").value())).isEqualByComparingTo("1");assertThat(p.get("Q3").role()).isEqualTo("DISPLAY_ONLY");}
 @Test void generatedManagementCapturesActualRegistryAssignmentAndBatch()throws Exception {
  f.jdbc.update("INSERT INTO biz_role_assignment(id,user_id,role_type,start_date,semester,academic_year,allowance_rate,source_batch_id,status) VALUES(72,1003,'班主任','2025-09-01',?,'2025-2026',35,'REGISTRY-BATCH-72',1)",WorkloadMysqlFixture.SEM);
  ManagementItemGenerator generator=f.proxy(new ManagementItemGeneratorImpl(f.mapper(BizRoleAssignmentMapper.class),f.mapper(BizWorkloadItemMapper.class),f.mapper(BizWlManagementMapper.class),f.calc,f.guard));
  WorkloadMysqlFixture.authenticated(()->generator.generate(1003L,WorkloadMysqlFixture.SEM));var id=f.jdbc.queryForObject("SELECT id FROM biz_workload_item WHERE assignment_id=72",Long.class);var item=f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(id);var p=proofs(item).get("岗位减免");
  assertThat(p.kind()).isEqualTo("REGISTRY_INPUT");assertThat(new BigDecimal(p.value())).isEqualByComparingTo("35");assertThat(p.references()).containsEntry("assignmentId","72").containsEntry("batch","REGISTRY-BATCH-72").containsEntry("semester",WorkloadMysqlFixture.SEM).containsEntry("actor","1004:integration");
  f.jdbc.update("DELETE FROM biz_role_assignment WHERE id=72");assertThat(f.formulas.build(item).getPersistedFactorSources()).contains(p);
 }
 @ParameterizedTest @ValueSource(strings={"G8","G9"})
 void manualWorkloadUsesServerActorAndIgnoresClientProvenance(String type)throws Exception {
  var input=new BizWorkloadItem();input.setUserId(1003L);input.setSemester(WorkloadMysqlFixture.SEM);input.setItemType(type);input.setCalculatedWorkload(new BigDecimal("12"));input.setCreateBy("forged-user");input.setFactorProvenanceJson("{forged-source}");input.setCourseName("manual-"+type);
  WorkloadMysqlFixture.authenticated(()->f.items.insertBizWorkloadItem(input));var id=f.jdbc.queryForObject("SELECT id FROM biz_workload_item WHERE course_name=?",Long.class,input.getCourseName());var stored=f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(id);var proof=proofs(stored).get("WORKLOAD");assertThat(proof.kind()).isEqualTo("MANUAL_INPUT");assertThat(proof.references()).containsEntry("actor","1004:integration");assertThat(stored.getFactorProvenanceJson()).doesNotContain("forged");
  var edit=new BizWorkloadItem();edit.setId(id);edit.setCalculatedWorkload(new BigDecimal("12.00"));edit.setFactorProvenanceJson("forged-again");edit.setUpdateBy("forged-user");WorkloadMysqlFixture.authenticated(()->f.items.updateBizWorkloadItem(edit));var after=f.mapper(BizWorkloadItemMapper.class).selectBizWorkloadItemById(id);assertThat(proofs(after).get("WORKLOAD").references()).containsEntry("actor","1004:integration");assertThat(after.getFactorProvenanceJson()).doesNotContain("forged");
 }
 @Test void allowanceTypeSwitchAndRemarkOnlyUpdateBindTheActualPersistedRow()throws Exception {
  var a=new BizAllowanceItem();a.setUserId(1003L);a.setSemester(WorkloadMysqlFixture.SEM);a.setFeeType("A");a.setFeeSubtype("自学");a.setStudentCount(5L);a.setStatus(1);WorkloadMysqlFixture.authenticated(()->allowances.insertBizAllowanceItem(a));
  var edit=new BizAllowanceItem();edit.setId(a.getId());edit.setFeeType("B");WorkloadMysqlFixture.authenticated(()->allowances.updateBizAllowanceItem(edit));var stored=allowances.selectBizAllowanceItemById(a.getId());assertThat(stored.getFeeSubtype()).isNull();assertThat(stored.getAmount()).isEqualByComparingTo("50");
  var note=new BizAllowanceItem();note.setId(a.getId());note.setRemark("only note");WorkloadMysqlFixture.authenticated(()->allowances.updateBizAllowanceItem(note));assertThat(allowances.selectBizAllowanceItemList(new BizAllowanceItem())).hasSize(1);assertThat(allowances.selectBizAllowanceItemById(a.getId()).getAmount()).isEqualByComparingTo("50");
 }
 @Test void allowanceEvidencePostWriteFailureRollsBackRowAndFreshness()throws Exception {
  f.jdbc.execute("CREATE TRIGGER fail_evidence BEFORE UPDATE ON biz_allowance_item FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected evidence write failure'");
  var a=new BizAllowanceItem();a.setUserId(1003L);a.setSemester(WorkloadMysqlFixture.SEM);a.setFeeType("G");a.setWorkloadUnits(BigDecimal.ONE);a.setStatus(1);
  assertThatThrownBy(()->WorkloadMysqlFixture.authenticated(()->allowances.insertBizAllowanceItem(a))).hasMessageContaining("injected evidence");assertThat(f.count("biz_allowance_item")).isZero();assertThat(f.count("biz_workload_summary")).isZero();
 }
 @ParameterizedTest @ValueSource(strings={"content","reference","value","role","missing"})
 void format4RejectsReSignedInnerTampering(String mutation)throws Exception {
  var item=imported(row("G1"));var snapshot=f.mapper(BizWorkloadCalcSnapshotMapper.class).selectLatestByItemId(item.getId());var json=new com.fasterxml.jackson.databind.ObjectMapper();var tree=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(snapshot.getContextJson());
  switch(mutation){
   case "content"->((com.fasterxml.jackson.databind.node.ObjectNode)tree.path("persistedFactorSources").get(0)).put("branchContent","altered content");
   case "reference"->((com.fasterxml.jackson.databind.node.ObjectNode)tree.path("factorEvidence").get(0)).put("ruleRef","a".repeat(64));
   case "value"->snapshot.setFactorJson(snapshot.getFactorJson().replace("1.10","1.20"));
   case "role"->((com.fasterxml.jackson.databind.node.ObjectNode)tree.path("persistedFactorSources").get(0)).put("role","DISPLAY_ONLY");
   case "missing"->tree.putArray("persistedFactorSources");
  }
  snapshot.setContextJson(tree.toString());snapshot.setRuleVersion(PolicyHash.hash(Arrays.asList("PERSISTED_FACTOR_POLICY_V1",SnapshotContextCodec.decode(snapshot.getContextJson()).policySources(),SnapshotContextCodec.decode(snapshot.getContextJson()).persistedFactorSources())));snapshot.setSnapshotHash(SnapshotIntegrity.hashV4(snapshot));assertThatThrownBy(()->SnapshotIntegrity.verify(snapshot)).hasMessageContaining("完整性");
 }

 @ParameterizedTest @ValueSource(strings={"G1","G2","G3","G4","G5","G6"})
 void v1RawConversionAndV2DefaultsRemainAuditable(String type)throws Exception {
  var raw=new TeachingTaskImportV1DTO();raw.setSemester(WorkloadMysqlFixture.SEM);raw.setUserCode("T1003");raw.setCourseName("V1-"+UUID.randomUUID());raw.setWorkloadType(type);raw.setStudentCount(5);raw.setBaseValue(new BigDecimal("2"));raw.setCourseNature("  ");raw.setMajorCategory(null);raw.setEducationLevel("本科");raw.setRepeatOrder(2);
  var dto=TeachingTaskInputNormalizer.convertV1(raw);dto.setRawImportJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(raw));dto.setImportTemplateVersion("V1");var item=imported(dto);var p=proofs(item);p.values().forEach(proof->{assertThat(proof.references()).containsEntry("templateVersion","V1").containsEntry("inputTransformVersion",TeachingTaskInputNormalizer.VERSION);assertThat(proof.inputs()).containsEntry("courseNature","必修").containsEntry("majorCategory","理工类");});assertThat(f.formulas.build(item).getEvidenceStatus()).isEqualTo("COMPLETE");
 }

}
