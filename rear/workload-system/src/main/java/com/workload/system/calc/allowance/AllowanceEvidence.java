package com.workload.system.calc.allowance;

import java.math.*;
import java.util.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.workload.system.calc.rule.*;
import com.workload.system.domain.BizAllowanceItem;
import com.workload.system.service.SnapshotIntegrity;

/** Amount and evidence are produced by the same branch before the amount is persisted. */
public final class AllowanceEvidence {
 private static final ObjectMapper JSON=new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
 private AllowanceEvidence() {}
 public record Evidence(int formatVersion,Long allowanceId,Long userId,String feeType,String semester,String asOfDate,Map<String,String> selection,String branch,String branchContent,
   Map<String,String> inputs,Map<String,String> persistedInputs,List<RuleResolution> rules,String unrounded,String rounding,String amount,Map<String,String> actor,String hash) {}
 private record Result(String branch,BigDecimal unrounded) {}
 public static BigDecimal calculate(BizAllowanceItem item,CalculationPolicyContext context) {
  Map<String,String> inputs=inputs(item);
  Result result=compute(item.getFeeType(),inputs,context);
  BigDecimal amount=result.unrounded().setScale(2,RoundingMode.HALF_UP);
  var actor=com.workload.system.calc.WorkloadCalcServiceImpl.sourceActor();
  Evidence evidence=new Evidence(1,item.getId(),item.getUserId(),item.getFeeType(),item.getSemester(),context.asOfDate().toString(),context.selectionEvidence(),result.branch(),content(result.branch()),inputs,null,context.sources(),result.unrounded().toPlainString(),"scale=2;HALF_UP",amount.toPlainString(),actor,null);
  evidence=withHash(evidence,hash(evidence));
  try { item.setCalculationEvidenceJson(JSON.writeValueAsString(evidence)); }
  catch(Exception e) { throw SnapshotIntegrity.failure("其他酬金实算证据保存失败"); }
  return amount;
 }
 private static Evidence withHash(Evidence e,String hash) { return new Evidence(e.formatVersion(),e.allowanceId(),e.userId(),e.feeType(),e.semester(),e.asOfDate(),e.selection(),e.branch(),e.branchContent(),e.inputs(),e.persistedInputs(),e.rules(),e.unrounded(),e.rounding(),e.amount(),e.actor(),hash); }
 private static String hash(Evidence e) { return PolicyHash.hash(Arrays.asList(e.formatVersion(),e.allowanceId(),e.userId(),e.feeType(),e.semester(),e.asOfDate(),e.selection(),e.branch(),e.branchContent(),e.inputs(),e.persistedInputs(),e.rules(),e.unrounded(),e.rounding(),e.amount(),e.actor())); }
 public static Evidence verify(String json) {
  if(json==null) return null; // Legacy amount has no historical calculation evidence.
  try {
   Evidence e=JSON.readValue(json,Evidence.class);
   if(e.formatVersion()!=1 || !Objects.equals(e.hash(),hash(e)) || !Objects.equals(e.branchContent(),content(e.branch())) || !"scale=2;HALF_UP".equals(e.rounding())
      || e.actor()==null || e.actor().get("actor")==null || e.actor().get("recordedAt")==null) throw new IllegalArgumentException("实算来源摘要或角色缺失");
   FactorProvenance.verifySelection(e.selection());
   if(!Objects.equals(e.semester(),e.selection().get("semester")) || !Objects.equals(e.asOfDate(),e.selection().get("asOfDate"))) throw new IllegalArgumentException("金额来源校历绑定不符");
   Map<String,RuleResolution> rules=new TreeMap<>();String previous=null;
   for(var r:e.rules()) {r.verify();if(!e.asOfDate().equals(r.asOfDate()) || (previous!=null&&previous.compareTo(r.requestedCode())>=0) || rules.put(r.requestedCode(),r)!=null) throw new IllegalArgumentException("实算政策集合不符");previous=r.requestedCode();}
   CalculationPolicyContext replay=new CalculationPolicyContext(java.time.LocalDate.parse(e.asOfDate()),(code,date,fallback,purpose)-> {
    var r=rules.get(code);if(r==null)throw new IllegalArgumentException("缺少实算政策"); return r;
   });
   Result r=compute(e.feeType(),e.inputs(),replay);
   if(!r.branch().equals(e.branch()) || !FactorProvenance.same(e.unrounded(),r.unrounded().toPlainString()) || !FactorProvenance.same(e.amount(),r.unrounded().setScale(2,RoundingMode.HALF_UP).toPlainString())
      || !replay.sources().equals(e.rules())) throw new IllegalArgumentException("实算输入、分支、规则或金额不符");
   if(e.persistedInputs()!=null) {
    verifyStorageInputs(e.inputs(),e.persistedInputs());
    if(!FactorProvenance.same(e.persistedInputs().get("inputAmount"),e.amount())) throw SnapshotIntegrity.failure("持久化金额与实算输出不符");
   }
   return e;
  } catch(Exception e) {throw SnapshotIntegrity.failure("其他酬金实算证据不符："+e.getMessage());}
 }
 public static void verifyFor(BizAllowanceItem item) {
  Evidence e=verify(item.getCalculationEvidenceJson());
  if(e!=null && (!Objects.equals(e.allowanceId(),item.getId()) || !Objects.equals(e.userId(),item.getUserId()) || !Objects.equals(e.feeType(),item.getFeeType()) || !Objects.equals(e.semester(),item.getSemester()) || !FactorProvenance.same(e.amount(),FactorProvenance.plain(item.getAmount())))) throw SnapshotIntegrity.failure("其他酬金实算证据与记录绑定不符");
  if(e!=null) {
   Map<String,String> current=inputs(item);
   if(e.persistedInputs()==null) throw SnapshotIntegrity.failure("金额来源未绑定实际持久化输入");
   for(String key:List.of("feeSubtype","studentCount","durationHours","days","classCount","workloadUnits")) {
    boolean same="feeSubtype".equals(key)?Objects.equals(current.get(key),e.persistedInputs().get(key)):num(current,key).compareTo(num(e.persistedInputs(),key))==0;
    if(!same) throw SnapshotIntegrity.failure("其他酬金实算输入与记录绑定不符");
   }
  }
 }
 public static void bindInserted(BizAllowanceItem item,BizAllowanceItem persisted) {
  try {
   Evidence e=verify(item.getCalculationEvidenceJson());
   if(e==null) throw new IllegalArgumentException("缺少本次金额来源");
   if(persisted==null || persisted.getId()==null) throw new IllegalArgumentException("金额来源缺少实际持久化行");
   Evidence bound=new Evidence(e.formatVersion(),persisted.getId(),e.userId(),e.feeType(),e.semester(),e.asOfDate(),e.selection(),e.branch(),e.branchContent(),e.inputs(),inputs(persisted),e.rules(),e.unrounded(),e.rounding(),e.amount(),e.actor(),null);
   item.setCalculationEvidenceJson(JSON.writeValueAsString(withHash(bound,hash(bound))));
   persisted.setCalculationEvidenceJson(item.getCalculationEvidenceJson()); verifyFor(persisted);
  }catch(Exception e){throw SnapshotIntegrity.failure("绑定金额来源失败："+e.getMessage());}
 }
 private static void verifyStorageInputs(Map<String,String> raw,Map<String,String> persisted) {
  Set<String> keys=Set.of("feeSubtype","studentCount","durationHours","days","classCount","workloadUnits","inputAmount");
  if(!raw.keySet().equals(keys) || !persisted.keySet().equals(keys)) throw SnapshotIntegrity.failure("金额实际输入集合不完整");
  if(!Objects.equals(raw.get("feeSubtype"),persisted.get("feeSubtype"))) throw SnapshotIntegrity.failure("金额实际输入分支不符");
  for(String key:List.of("studentCount","classCount","durationHours","days","workloadUnits")) {
   BigDecimal expected=num(raw,key);
   if(!Set.of("studentCount","classCount").contains(key)) expected=expected.setScale(2,RoundingMode.HALF_UP);
   if(expected.compareTo(num(persisted,key))!=0) throw SnapshotIntegrity.failure("金额计算输入与持久化输入不一致");
  }
 }
 private static Map<String,String> inputs(BizAllowanceItem i) {
  Map<String,String> m=new TreeMap<>();m.put("feeSubtype",i.getFeeSubtype());m.put("studentCount",FactorProvenance.plain(i.getStudentCount()));
  m.put("durationHours",FactorProvenance.plain(i.getDurationHours()));m.put("days",FactorProvenance.plain(i.getDays()));m.put("classCount",FactorProvenance.plain(i.getClassCount()));
  m.put("workloadUnits",FactorProvenance.plain(i.getWorkloadUnits()));m.put("inputAmount",FactorProvenance.plain(i.getAmount()));
  return Collections.unmodifiableMap(m);
 }
 private static BigDecimal num(Map<String,String> i,String key){String value=i.get(key);return value==null?BigDecimal.ZERO:new BigDecimal(value);}
 private static BigDecimal rule(CalculationPolicyContext c,String code,String fallback){return c.value(code,new BigDecimal(fallback));}
 private static Result compute(String type,Map<String,String> i,CalculationPolicyContext c) {
  BigDecimal count=num(i,"studentCount");String subtype=i.get("feeSubtype");
  return switch(type) {
   case "A" -> subtype!=null&&subtype.contains("自学") ? count.compareTo(new BigDecimal("6"))<0 ? new Result("A_SELF_LT6",rule(c,"PAY_A_SELF_LT6","120"))
     :count.compareTo(new BigDecimal("20"))<0 ? new Result("A_SELF_6_20",rule(c,"PAY_A_SELF_6_20","260")) :new Result("A_SELF_GE20",BigDecimal.ZERO) :new Result("A_MANUAL",num(i,"inputAmount"));
   case "B" -> {boolean concentrated=subtype!=null&&subtype.contains("集中");yield new Result(concentrated?"B_CONCENTRATED":"B_DISPERSED",count.multiply(rule(c,concentrated?"PAY_B_CONCENTRATED":"PAY_B_DISPERSED",concentrated?"15":"10")));}
   case "C" -> new Result("C_THESIS",count.multiply(rule(c,"PAY_C_THESIS","120")));
   case "D" -> {String branch=count.compareTo(new BigDecimal("20"))<0?"LT20":count.compareTo(new BigDecimal("60"))<0?"20_60":count.compareTo(new BigDecimal("120"))<0?"60_120":count.compareTo(new BigDecimal("200"))<0?"120_200":"GE200";String fallback=switch(branch){case "LT20"->"0";case "20_60"->"30";case "60_120"->"80";case "120_200"->"100";default->"150";};yield new Result("D_"+branch,rule(c,"PAY_D_MARKING_"+branch,fallback));}
   case "E" -> new Result("E_HOURLY",num(i,"durationHours").multiply(rule(c,"PAY_E_HOURLY","60")));
   case "F" -> new Result("F_SPORT",num(i,"days").multiply(rule(c,"PAY_F_DAY_UNITS","6")).add(num(i,"classCount").multiply(rule(c,"PAY_F_CLASS_UNITS","1"))).multiply(rule(c,"PAY_UNIT_FEE","30")));
   case "G" -> new Result("G_NIGHT",num(i,"workloadUnits").multiply(rule(c,"PAY_UNIT_FEE","30")));
   default -> throw new IllegalArgumentException("未启用酬金类别");
  };
 }
 private static String content(String branch) {return switch(branch) {
  case "A_SELF_LT6"->"feeSubtype contains 自学; studentCount null->0; count<6; amount=PAY_A_SELF_LT6(default 120)";
  case "A_SELF_6_20"->"feeSubtype contains 自学; 6<=count<20; amount=PAY_A_SELF_6_20(default 260)";
  case "A_SELF_GE20"->"feeSubtype contains 自学; count>=20; amount=0; separate teaching class follows G1";
  case "A_MANUAL"->"feeSubtype does not contain 自学; amount=inputAmount; null->0";
  case "B_CONCENTRATED"->"feeSubtype contains 集中; amount=studentCount(null->0)*PAY_B_CONCENTRATED(default 15)";
  case "B_DISPERSED"->"feeSubtype does not contain 集中; amount=studentCount(null->0)*PAY_B_DISPERSED(default 10)";
  case "C_THESIS"->"amount=studentCount(null->0)*PAY_C_THESIS(default 120)";
  case "D_LT20"->"studentCount(null->0)<20; amount=PAY_D_MARKING_LT20(default 0)";
  case "D_20_60"->"20<=studentCount<60; amount=PAY_D_MARKING_20_60(default 30)";
  case "D_60_120"->"60<=studentCount<120; amount=PAY_D_MARKING_60_120(default 80)";
  case "D_120_200"->"120<=studentCount<200; amount=PAY_D_MARKING_120_200(default 100)";
  case "D_GE200"->"studentCount>=200; amount=PAY_D_MARKING_GE200(default 150)";
  case "E_HOURLY"->"amount=durationHours(null->0)*PAY_E_HOURLY(default 60)";
  case "F_SPORT"->"amount=(days(null->0)*PAY_F_DAY_UNITS(default 6)+classCount(null->0)*PAY_F_CLASS_UNITS(default 1))*PAY_UNIT_FEE(default 30)";
  case "G_NIGHT"->"amount=workloadUnits(null->0)*PAY_UNIT_FEE(default 30); signed workload input";
  default->throw new IllegalArgumentException("未知实算分支");
 };}
}
