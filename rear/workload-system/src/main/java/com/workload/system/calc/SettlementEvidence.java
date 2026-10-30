package com.workload.system.calc;

import java.math.*;
import java.util.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.workload.system.calc.rule.*;
import com.workload.system.calc.allowance.AllowanceEvidence;
import com.workload.system.domain.*;
import com.workload.system.service.SnapshotIntegrity;

/** Self-contained historical evidence. Replay never queries current profiles or policies. */
public final class SettlementEvidence {
 private static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
 public static final String ALGORITHM="G7=sum(G1..G6);G10=G7+G8+G9;G11=min(raw,CAP_G11_SEMESTER);total=G10+G11;excess=max(total-rated,0);performance=explicitFullTime?max(min(total,CAP_200PCT)-rated,0)*rate:0;money=scale2/HALF_UP;totalPay=scale0/HALF_UP";
 private static final Set<String> PERSON_FIELDS=Set.of("title","nature","specialStatus","specialStart","specialEnd","enterpriseEvalResult");
 private static final Set<String> RATE_FIELDS=Set.of("id","title","rate","effectiveFrom","effectiveTo","status","createBy","createTime","updateBy","updateTime","remark");
 private static final Set<String> SELECTION_FIELDS=Set.of("semester","selectionMode","asOfDate","calendarJson");
 private static final Set<String> RESULT_FIELDS=Set.of("g7","g8","g9","g10","g11","total","rated","excess","rate","performance","isCapped","basicStandard","basicMet","cap");
 private static final Set<String> CALENDAR_FIELDS=Set.of("autumnStart","autumnEnd","springStart","springEnd");
 private SettlementEvidence(){}
 public record Item(Long id,String type,Integer status,String sourceType,Long taskId,Long assignmentId,String courseName,String educationLevel,Long calculationVersion,String result,String manualReason,String factorProvenanceJson,BizWorkloadCalcSnapshot snapshot){}
 public record SummaryEvidence(Long summaryId,Long userId,String semester,String mode,Map<String,String> selection,Map<String,String> person,String eligibilityReason,String profileBoundary,List<Item> items,List<RuleResolution> rules,Map<String,String> rate,String rateHash,Map<String,String> results,boolean threeTheoryCourses,List<String> warnings,String hash){}
 public record AllowanceRow(Long id,Long userId,String semester,Integer status,String feeType,String feeSubtype,Long studentCount,BigDecimal durationHours,BigDecimal days,Long classCount,BigDecimal workloadUnits,BigDecimal amount,String calculationEvidenceJson) {
  public static AllowanceRow copy(BizAllowanceItem row){return new AllowanceRow(row.getId(),row.getUserId(),row.getSemester(),row.getStatus(),row.getFeeType(),row.getFeeSubtype(),row.getStudentCount(),row.getDurationHours(),row.getDays(),row.getClassCount(),row.getWorkloadUnits(),row.getAmount(),row.getCalculationEvidenceJson());}
  public BizAllowanceItem materialize(){var row=new BizAllowanceItem();row.setId(id);row.setUserId(userId);row.setSemester(semester);row.setStatus(status);row.setFeeType(feeType);row.setFeeSubtype(feeSubtype);row.setStudentCount(studentCount);row.setDurationHours(durationHours);row.setDays(days);row.setClassCount(classCount);row.setWorkloadUnits(workloadUnits);row.setAmount(amount);row.setCalculationEvidenceJson(calculationEvidenceJson);return row;}
 }
 public record Allowance(Long id,String amount,AllowanceRow row){}
 public record PayEvidence(Long payId,String summaryEvidenceHash,List<Allowance> allowances,String courseHourPay,String otherPayTotal,Long totalPay,String rounding,String hash){}
 public record Payload(int formatVersion,Long settlementVersion,String algorithm,SummaryEvidence summary,PayEvidence pay){}
 public static String eligibility(String nature){return "专任".equals(nature)?"明确专任；旧档案可能由历史默认产生，无法证明人工确认":"人员性质不能确认专任绩效资格："+String.valueOf(nature);}
 public static boolean eligible(String nature){return "专任".equals(nature);}
 public static String encode(Object v){try{return JSON.writeValueAsString(v);}catch(Exception e){throw SnapshotIntegrity.failure("结算证据序列化失败");}}
 public static String hash(Object v){return PolicyHash.hash(v);}
 public static SummaryEvidence seal(SummaryEvidence e){return new SummaryEvidence(e.summaryId(),e.userId(),e.semester(),e.mode(),e.selection(),e.person(),e.eligibilityReason(),e.profileBoundary(),e.items(),e.rules(),e.rate(),e.rateHash(),e.results(),e.threeTheoryCourses(),e.warnings(),summaryHash(e));}
 private static String summaryHash(SummaryEvidence e){return hash(Arrays.asList(e.summaryId(),e.userId(),e.semester(),e.mode(),e.selection(),e.person(),e.eligibilityReason(),e.profileBoundary(),e.items(),e.rules(),e.rate(),e.rateHash(),e.results(),e.threeTheoryCourses(),e.warnings()));}
 public static PayEvidence seal(PayEvidence p){return new PayEvidence(p.payId(),p.summaryEvidenceHash(),p.allowances(),p.courseHourPay(),p.otherPayTotal(),p.totalPay(),p.rounding(),payHash(p));}
 private static String payHash(PayEvidence p){return hash(Arrays.asList(p.payId(),p.summaryEvidenceHash(),p.allowances(),p.courseHourPay(),p.otherPayTotal(),p.totalPay(),p.rounding()));}
 public static Payload decode(String json,String outerHash){try{
  var node=JSON.readTree(json);
  if(node==null||node.get("formatVersion")==null||!node.get("formatVersion").isIntegralNumber()||!node.get("formatVersion").canConvertToInt()
      ||node.get("settlementVersion")==null||!node.get("settlementVersion").isIntegralNumber()||!node.get("settlementVersion").canConvertToLong())throw new IllegalArgumentException("格式版本及结算版本必须为显式整数");
  Payload p=JSON.readValue(json,Payload.class);
  if(p.formatVersion()!=1 || p.settlementVersion()==null || p.settlementVersion()<1 || !ALGORITHM.equals(p.algorithm()) || !Objects.equals(hash(p),outerHash))throw new IllegalArgumentException("未知结算格式/算法或外层摘要不符");
  verifySummary(p.summary());verifyPay(p.summary(),p.pay());return p;
 }catch(Exception e){throw SnapshotIntegrity.failure("结算证据无效："+e.getMessage());}}
 public static void verifySummary(SummaryEvidence e){try{
  if(e==null || e.summaryId()==null || e.userId()==null || e.warnings()==null || !Objects.equals(summaryHash(e),e.hash()) || !Set.of("FORMAL","ISOLATED_VERIFICATION_ONLY").contains(e.mode()))throw new IllegalArgumentException("汇总证据绑定/摘要缺失");
  requireFields(e.person(),PERSON_FIELDS,"人员");
  requireFields(e.rate(),RATE_FIELDS,"费率整行");
  requireFields(e.selection(),SELECTION_FIELDS,"适用依据");
  requireFields(e.results(),RESULT_FIELDS,"汇总结果");
  verifyCalendar(e.selection().get("calendarJson"));
  FactorProvenance.verifySelection(e.selection());
  if(!Objects.equals(e.semester(),e.selection().get("semester")) || !eligible(e.person().get("nature")) || !Objects.equals(eligibility(e.person().get("nature")),e.eligibilityReason()) || e.profileBoundary()==null)throw new IllegalArgumentException("人员资格证据不符");
  Map<String,RuleResolution> rules=new TreeMap<>();String last=null;
  for(var r:e.rules()){r.verify();if(!e.selection().get("asOfDate").equals(r.asOfDate()) || last!=null&&last.compareTo(r.requestedCode())>=0)throw new IllegalArgumentException("规则集合绑定不符");rules.put(r.requestedCode(),r);last=r.requestedCode();}
  Map<String,BigDecimal> sums=new HashMap<>();Set<Long> ids=new HashSet<>();Set<String> theory=new HashSet<>();
  for(var i:e.items()){
   if(i.id()==null||!ids.add(i.id())||i.type()==null||!Set.of("G1","G2","G3","G4","G5","G6","G8","G9","G11").contains(i.type())||(i.status()!=null&&!Set.of(0,1,2,3).contains(i.status())))throw new IllegalArgumentException("明细集合不完整");
   if(Integer.valueOf(3).equals(i.status())){if(i.manualReason()==null||!i.manualReason().contains("status=3"))throw new IllegalArgumentException("已驳回明细缺排除说明");continue;}
   if(i.result()==null)throw new IllegalArgumentException("有效明细缺少实际结果");
   var provenance=FactorProvenance.read(i.factorProvenanceJson(),i.id(),i.type());
   if(Set.of("G8","G9").contains(i.type())){
    var proof=provenance.get("WORKLOAD");
    if(i.manualReason()==null||i.sourceType()==null||proof==null||provenance.size()!=1||!"MANUAL_INPUT".equals(proof.kind())||!"FACTOR".equals(proof.role())||!FactorProvenance.same(i.result(),proof.value()))throw new IllegalArgumentException("手工明细来源缺失或实际值/actor/角色不符");
   }
   else {if(i.snapshot()==null)throw new IllegalArgumentException("计算明细缺快照");SnapshotIntegrity.verify(i.snapshot());
    if(!Objects.equals(i.id(),i.snapshot().getItemId())||!Objects.equals(i.calculationVersion(),i.snapshot().getCalculationVersion())||num(i.result()).compareTo(i.snapshot().getResult())!=0)throw new IllegalArgumentException("明细计算版本/结果不符");
    var context=i.snapshot().getContextJson()==null?null:com.workload.system.service.SnapshotContextCodec.decode(i.snapshot().getContextJson());
    if(context==null||!i.type().equals(context.formulaType())||context.formatVersion()!=4||!"COMPLETE".equals(context.evidenceStatus())||!context.reproducible()||context.missingEvidenceReasons()==null||!context.missingEvidenceReasons().isEmpty()||context.persistedFactorSources()==null)throw new IllegalArgumentException("计算明细缺完整实算来源，请重新计算");
    for(var proof:context.persistedFactorSources())if(!Objects.equals(proof,provenance.get(proof.key())))throw new IllegalArgumentException("明细来源文档与实算快照不符");
   }
   sums.merge(i.type(),num(i.result()),BigDecimal::add);
   if("G1".equals(i.type())&&i.courseName()!=null&&!i.courseName().isBlank())theory.add(i.courseName());
  }
  if(e.threeTheoryCourses()!=(theory.size()>=3))throw new IllegalArgumentException("三门课程去重依据不符");
  BigDecimal g7=BigDecimal.ZERO;for(int n=1;n<=6;n++)g7=g7.add(sums.getOrDefault("G"+n,BigDecimal.ZERO));
  BigDecimal g8=sums.getOrDefault("G8",BigDecimal.ZERO),g9=sums.getOrDefault("G9",BigDecimal.ZERO),g10=g7.add(g8).add(g9),g11=sums.getOrDefault("G11",BigDecimal.ZERO).min(value(rules,"CAP_G11_SEMESTER"));
  BigDecimal total=g10.add(g11),rated=value(rules,"RATED_WORKLOAD"),cap=value(rules,"CAP_200PCT");
  for(var v:Map.of("g7",g7,"g8",g8,"g9",g9,"g10",g10,"g11",g11,"total",total,"rated",rated,"excess",total.subtract(rated).max(BigDecimal.ZERO),"cap",cap).entrySet())same(e.results().get(v.getKey()),v.getValue());
  if(!Objects.equals(e.rateHash(),hash(e.rate())) || e.rate().get("id")==null || !Objects.equals(e.person().get("title"),e.rate().get("title")) || !"1".equals(e.rate().get("status")))throw new IllegalArgumentException("费率整行证据不符");
  var date=java.time.LocalDate.parse(e.selection().get("asOfDate"));
  if(e.rate().get("effectiveFrom")!=null&&java.time.LocalDate.parse(e.rate().get("effectiveFrom")).isAfter(date)||e.rate().get("effectiveTo")!=null&&java.time.LocalDate.parse(e.rate().get("effectiveTo")).isBefore(date))throw new IllegalArgumentException("费率适用日期不符");
  same(e.results().get("rate"),num(e.rate().get("rate")));same(e.results().get("performance"),total.min(cap).subtract(rated).max(BigDecimal.ZERO).multiply(num(e.rate().get("rate"))));
  if(!Objects.equals(e.results().get("isCapped"),total.compareTo(cap)>0?"1":"0"))throw new IllegalArgumentException("封顶结果不符");
  String title=e.person().get("title");String basic=title!=null&&title.contains("教授")&&!title.contains("副")?"BASIC_TEACH_PROF":title!=null&&title.contains("副教授")?"BASIC_TEACH_APROF":title!=null&&title.contains("讲师")?"BASIC_TEACH_LECT":"BASIC_TEACH_ASSIST";
  Set<String> used=new TreeSet<>(Set.of("CAP_G11_SEMESTER","RATED_WORKLOAD","CAP_200PCT",basic));
  BigDecimal standard=value(rules,basic).divide(new BigDecimal("2"),2,RoundingMode.HALF_UP);boolean deemed=false;
  boolean active=specialActive(e);
  if(active){if("产假".equals(e.person().get("specialStatus"))){used.add("FACTOR_MATERNITY");standard=standard.multiply(value(rules,"FACTOR_MATERNITY"));}
   else if("在职读博".equals(e.person().get("specialStatus"))){used.add("BASIC_TEACH_PHD");standard=value(rules,"BASIC_TEACH_PHD").divide(new BigDecimal("2"),2,RoundingMode.HALF_UP);}else if("访学".equals(e.person().get("specialStatus")))deemed=true;}
  same(e.results().get("basicStandard"),standard);
  if(!Objects.equals(e.results().get("basicMet"),deemed||e.threeTheoryCourses()||g10.compareTo(standard)>=0?"1":"0")||!rules.keySet().equals(used))throw new IllegalArgumentException("达标结果/实际政策集合不符");
 }catch(Exception x){throw SnapshotIntegrity.failure("汇总证据无效："+x.getMessage());}}
 private static void requireFields(Map<String,String> map,Set<String> fields,String label) {
  if(map==null||!map.keySet().equals(fields))throw new IllegalArgumentException(label+"字段集合不完整或含未知字段；可空值也必须保留键");
 }
 private static void verifyCalendar(String json)throws Exception {
  var calendar=JSON.readTree(json);
  if(calendar==null||!calendar.isObject())throw new IllegalArgumentException("实际校历必须为完整对象");
  Set<String> fields=new HashSet<>();calendar.fieldNames().forEachRemaining(fields::add);
  if(!fields.equals(CALENDAR_FIELDS))throw new IllegalArgumentException("实际校历四字段不完整或含未知字段");
  for(String key:CALENDAR_FIELDS){
   var value=calendar.get(key);
   if(value==null||!value.isTextual()||!value.textValue().matches("[0-9]{2}-[0-9]{2}"))throw new IllegalArgumentException("实际校历字段必须为MM-dd文本："+key);
   java.time.MonthDay.parse("--"+value.textValue());
  }
 }
 private static boolean specialActive(SummaryEvidence e)throws Exception{
  SemesterCalendar c=JSON.readValue(e.selection().get("calendarJson"),SemesterCalendar.class);var range=c.rangeOf(e.semester());
  String start=e.person().get("specialStart"),end=e.person().get("specialEnd");
  return (start==null||!java.time.LocalDate.parse(start).isAfter(range[1]))&&(end==null||!java.time.LocalDate.parse(end).isBefore(range[0]));
 }
 public static void verifyPay(SummaryEvidence s,PayEvidence p){try{
  if(p==null||p.payId()==null||!Objects.equals(p.summaryEvidenceHash(),s.hash())||!Objects.equals(p.hash(),payHash(p))||!"course/other:scale2;total:scale0;HALF_UP".equals(p.rounding()))throw new IllegalArgumentException("酬金证据摘要/汇总引用不符");
  BigDecimal other=BigDecimal.ZERO;Set<Long> ids=new HashSet<>();
  for(var a:p.allowances()){
   if(a.id()==null||!ids.add(a.id())||a.row()==null||!a.id().equals(a.row().id())||!s.userId().equals(a.row().userId())||!s.semester().equals(a.row().semester())||!Integer.valueOf(1).equals(a.row().status())||a.row().calculationEvidenceJson()==null)throw new IllegalArgumentException("其他酬金来源/集合不完整");
   AllowanceEvidence.verifyFor(a.row().materialize());same(a.amount(),a.row().amount());other=other.add(num(a.amount()));
  }
  same(p.courseHourPay(),num(s.results().get("performance")));same(p.otherPayTotal(),other);
  if(p.totalPay()!=num(p.courseHourPay()).add(other).setScale(0,RoundingMode.HALF_UP).longValueExact())throw new IllegalArgumentException("酬金舍入输出不符");
 }catch(Exception x){throw SnapshotIntegrity.failure("酬金证据无效："+x.getMessage());}}
 private static BigDecimal value(Map<String,RuleResolution> r,String code){if(!r.containsKey(code))throw new IllegalArgumentException("缺少实际规则："+code);return r.get(code).value();}
 public static BigDecimal num(String v){return new BigDecimal(v);}
 private static void same(String actual,BigDecimal expected){if(actual==null||num(actual).compareTo(expected.setScale(2,RoundingMode.HALF_UP))!=0)throw new IllegalArgumentException("结算金额或结果不符");}
}
