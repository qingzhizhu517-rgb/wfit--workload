package com.workload.system.calc;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import com.workload.system.calc.rule.*;
import com.workload.system.domain.*;
import com.workload.system.mapper.*;
import com.workload.system.service.SnapshotIntegrity;
import static com.workload.system.calc.SettlementEvidence.*;

/** Coordinates append-only evidence and server-owned pointers inside the calculation transaction. */
@Service
public class SettlementSnapshotService {
 private final BizWorkloadSettlementSnapshotMapper snapshots;
 private final BizWorkloadCalcSnapshotMapper itemSnapshots;
 private final BizWorkloadSummaryMapper summaries;
 private final BizPayRecordMapper pays;
 private final BizWorkloadItemMapper items;
 private final BizAllowanceItemMapper allowances;
 private final SettlementScope scope;
 public SettlementSnapshotService(BizWorkloadSettlementSnapshotMapper s,BizWorkloadCalcSnapshotMapper i,BizWorkloadSummaryMapper m,BizPayRecordMapper p,BizWorkloadItemMapper w,BizAllowanceItemMapper a,SettlementScope scope){snapshots=s;itemSnapshots=i;summaries=m;pays=p;items=w;allowances=a;this.scope=scope;}
 public SummaryEvidence summary(BizWorkloadSummary s,BizTeacherProfile profile,List<BizWorkloadItem> rows,CalculationPolicyContext context,BizPayRate rate,BigDecimal cap,boolean three,List<String> warnings){
  Map<String,String> person=new TreeMap<>();person.put("title",profile.getTitle());person.put("nature",profile.getTeacherNature());person.put("specialStatus",profile.getSpecialStatus());person.put("specialStart",date(profile.getSpecialStatusStart()));person.put("specialEnd",date(profile.getSpecialStatusEnd()));person.put("enterpriseEvalResult",profile.getEnterpriseEvalResult());
  List<Item> evidence=new ArrayList<>();
  for(var i:rows){
   boolean manual=Set.of("G8","G9").contains(i.getItemType());
   BizWorkloadCalcSnapshot snapshot=manual||Integer.valueOf(3).equals(i.getStatus())?null:itemSnapshots.selectLatestByItemIdCurrent(i.getId());
   evidence.add(new Item(i.getId(),i.getItemType(),i.getStatus(),i.getSourceType(),i.getTaskId(),i.getAssignmentId(),i.getCourseName(),i.getEducationLevel(),i.getCalculationVersion(),plain(i.getCalculatedWorkload()),Integer.valueOf(3).equals(i.getStatus())?"status=3驳回排除，不计入任何汇总":manual?"人工申报；createBy="+i.getCreateBy()+"；description="+i.getDescription():null,i.getFactorProvenanceJson(),snapshot));
  }
  evidence.sort(Comparator.comparing(Item::id));
  Map<String,String> rateMap=new TreeMap<>();if(rate!=null){rateMap.put("id",plain(rate.getId()));rateMap.put("title",rate.getTitle());rateMap.put("rate",plain(rate.getRate()));rateMap.put("effectiveFrom",plain(rate.getEffectiveFrom()));rateMap.put("effectiveTo",plain(rate.getEffectiveTo()));rateMap.put("status",plain(rate.getStatus()));rateMap.put("createBy",rate.getCreateBy());rateMap.put("createTime",plain(rate.getCreateTime()));rateMap.put("updateBy",rate.getUpdateBy());rateMap.put("updateTime",plain(rate.getUpdateTime()));rateMap.put("remark",rate.getRemark());}
  var e=seal(new SummaryEvidence(s.getId(),s.getUserId(),s.getSemester(),scope.mode(s.getSemester()),context.selectionEvidence(),person,eligibility(profile.getTeacherNature()),"旧专任档案可能由导入默认产生，现有字段无法区分人工确认；本次仅记录现存档案，不追改历史金额",List.copyOf(evidence),context.sources(),rateMap,hash(rateMap),results(s,cap),three,List.copyOf(warnings),null));
  verifySummary(e);return e;
 }
 public static Map<String,String> results(BizWorkloadSummary s,BigDecimal cap){Map<String,String> m=new TreeMap<>();m.put("g7",plain(s.getG7()));m.put("g8",plain(s.getG8()));m.put("g9",plain(s.getG9()));m.put("g10",plain(s.getG10()));m.put("g11",plain(s.getG11()));m.put("total",plain(s.getTotalWorkload()));m.put("rated",plain(s.getRatedWorkload()));m.put("excess",plain(s.getExcessWorkload()));m.put("rate",plain(s.getPayRate()));m.put("performance",plain(s.getPerformancePay()));m.put("isCapped",plain(s.getIsCapped()));m.put("basicStandard",plain(s.getBasicTeachingStandard()));m.put("basicMet",plain(s.getBasicTeachingMet()));m.put("cap",plain(cap));return m;}
 @Transactional(propagation=Propagation.MANDATORY)
 public void append(BizWorkloadSummary s,BizPayRecord p,SummaryEvidence evidence,List<BizAllowanceItem> rows){
  assertSummaryBinding(s,evidence);
  List<Allowance> a=rows.stream().sorted(Comparator.comparing(BizAllowanceItem::getId)).map(row->new Allowance(row.getId(),plain(row.getAmount()),AllowanceRow.copy(row))).toList();
  var pay=seal(new PayEvidence(p.getId(),evidence.hash(),a,plain(p.getCourseHourPay()),plain(p.getOtherPayTotal()),p.getTotalPay(),"course/other:scale2;total:scale0;HALF_UP",null));
  verifyPay(evidence,pay);
  Long previous=snapshots.selectMaxVersionCurrent(s.getId());long version=previous==null?1:previous+1;
  Payload payload=new Payload(1,version,ALGORITHM,evidence,pay);
  BizWorkloadSettlementSnapshot row=new BizWorkloadSettlementSnapshot();row.setSummaryId(s.getId());row.setUserId(s.getUserId());row.setSemester(s.getSemester());row.setSettlementVersion(version);row.setFormatVersion(1);row.setPayloadJson(encode(payload));row.setHash(hash(payload));row.setCreatedBy(WorkloadCalcServiceImpl.sourceActor().get("actor"));row.setCreatedAt(new Date());
  if(snapshots.insert(row)!=1||row.getId()==null)throw SnapshotIntegrity.failure("结算证据写入失败");
  if(summaries.updateSettlementPointer(s.getId(),row.getId())!=1||pays.updateSettlementPointer(p.getId(),row.getId())!=1)throw SnapshotIntegrity.failure("结算证据指针写入失败");
  s.setSettlementSnapshotId(row.getId());p.setSettlementSnapshotId(row.getId());
 }
 /** Must run after the caller holds the teacher-period and summary locks, before any recalculation writes.
  * A missing main pointer does not erase append-only history or the paired pay pointer. */
 @Transactional(propagation=Propagation.MANDATORY)
 public void assertRecalculationBoundary(BizWorkloadSummary summary){
  if(summary==null)return;
  Long version=snapshots.selectMaxVersionCurrent(summary.getId());
  // Preserve period -> summary -> items/allowances -> pay even when inspecting legacy escape paths.
  BizWorkloadItem itemQuery=new BizWorkloadItem();itemQuery.setUserId(summary.getUserId());itemQuery.setSemester(summary.getSemester());itemQuery.getParams().put("currentRead",true);
  items.selectBizWorkloadItemList(itemQuery);
  BizAllowanceItem allowanceQuery=new BizAllowanceItem();allowanceQuery.setUserId(summary.getUserId());allowanceQuery.setSemester(summary.getSemester());allowanceQuery.setStatus(1);allowanceQuery.getParams().put("currentRead",true);
  allowances.selectBizAllowanceItemList(allowanceQuery);
  BizPayRecord query=new BizPayRecord();query.setUserId(summary.getUserId());query.setSemester(summary.getSemester());query.getParams().put("currentRead",true);
  var records=pays.selectBizPayRecordList(query);
  boolean hasPayPointer=records.stream().anyMatch(pay->pay.getSettlementSnapshotId()!=null);
  boolean hasHistory=summary.getSettlementSnapshotId()!=null||version!=null||hasPayPointer;
  if(hasHistory&&summary.getSettlementSnapshotId()==null)throw SnapshotIntegrity.failure("已有结算历史或酬金指针，但汇总缺少结算证据指针；禁止降级重建");
  if(hasHistory&&!scope.required(summary.getSemester()))throw new com.workload.common.exception.ServiceException("已有结算历史而当前作用域未启用，禁止降级为legacy重算");
  if(summary.getSettlementSnapshotId()!=null){
   var payload=read(summary,true);
   if(records.size()!=1)throw SnapshotIntegrity.failure("已有结算历史但配对酬金缺失或重复");
   assertPay(summary,payload,records.get(0));
  }
 }
 public SummaryEvidence savedSummary(BizWorkloadSummary s){Payload p=read(s,true);assertSummaryBinding(s,p.summary());assertItems(s,p.summary(),true);return p.summary();}
 public void assertPreviousPay(BizWorkloadSummary s,BizPayRecord p){assertPay(s,read(s,true),p);}
 private Payload read(BizWorkloadSummary s,boolean currentRead){
  if(s.getSettlementSnapshotId()==null)throw SnapshotIntegrity.failure("汇总缺少结算证据，请重新核算");
  var row=currentRead?snapshots.selectByIdCurrent(s.getSettlementSnapshotId()):snapshots.selectById(s.getSettlementSnapshotId());
  if(row==null||!Objects.equals(row.getSummaryId(),s.getId())||!Objects.equals(row.getUserId(),s.getUserId())||!Objects.equals(row.getSemester(),s.getSemester())||!Integer.valueOf(1).equals(row.getFormatVersion()))throw SnapshotIntegrity.failure("结算快照归属/格式不符");
  var p=decode(row.getPayloadJson(),row.getHash());if(!Objects.equals(p.settlementVersion(),row.getSettlementVersion()))throw SnapshotIntegrity.failure("结算版本不符");return p;
 }
 private void assertSummaryBinding(BizWorkloadSummary s,SummaryEvidence e){verifySummary(e);if(!Objects.equals(s.getId(),e.summaryId())||!Objects.equals(s.getUserId(),e.userId())||!Objects.equals(s.getSemester(),e.semester())||!Objects.equals(s.getTitle(),e.person().get("title"))||!results(s,num(e.results().get("cap"))).equals(e.results()))throw SnapshotIntegrity.failure("汇总人员/金额与保存结算证据不符");}
 private void assertItems(BizWorkloadSummary s,SummaryEvidence e,boolean currentRead){
  BizWorkloadItem q=new BizWorkloadItem();q.setUserId(s.getUserId());q.setSemester(s.getSemester());q.getParams().put("currentRead",currentRead);
  var current=items.selectBizWorkloadItemList(q);Map<Long,Item> saved=new HashMap<>();e.items().forEach(i->saved.put(i.id(),i));
  if(current.size()!=saved.size())throw stale("结算明细集合已变化，请重新核算");
  for(var i:current){var old=saved.get(i.getId());if(old==null||!Objects.equals(old.type(),i.getItemType())||!Objects.equals(old.calculationVersion(),i.getCalculationVersion())||!numericNullable(old.result(),i.getCalculatedWorkload())||!Objects.equals(old.sourceType(),i.getSourceType())||!Objects.equals(old.taskId(),i.getTaskId())||!Objects.equals(old.assignmentId(),i.getAssignmentId())||!Objects.equals(old.courseName(),i.getCourseName())||!Objects.equals(old.educationLevel(),i.getEducationLevel())||!sameReviewGroup(old.status(),i.getStatus()))throw stale("结算明细版本/结果/集合已变化，请重新核算");
   com.workload.system.calc.rule.FactorProvenance.read(i.getFactorProvenanceJson(),i.getId(),i.getItemType());
   if(!Objects.equals(old.factorProvenanceJson(),i.getFactorProvenanceJson()))throw stale("明细来源已变化，请重新核算");
   if(old.snapshot()!=null){var latest=currentRead?itemSnapshots.selectLatestByItemIdCurrent(i.getId()):itemSnapshots.selectLatestByItemId(i.getId());if(latest==null||!Objects.equals(latest.getId(),old.snapshot().getId())||!Objects.equals(latest.getSnapshotHash(),old.snapshot().getSnapshotHash()))throw SnapshotIntegrity.failure("结算明细快照指针/摘要已变化");SnapshotIntegrity.verify(latest);}
  }
 }
 // Review confirmation does not change a calculation: 0 -> 1 is permitted; disputed/rejected membership is exact.
 private boolean sameReviewGroup(Integer a,Integer b){return (a==null||a==0||a==1)&&(b==null||b==0||b==1)||Objects.equals(a,b);}
 public void assertSubmittable(BizWorkloadSummary s){assertComplete(s,true);}
 private void assertComplete(BizWorkloadSummary s,boolean currentRead){
  if(s.getSettlementSnapshotId()==null && (!scope.required(s.getSemester()) || Integer.valueOf(1).equals(s.getStatus())) && genuineLegacy(s,currentRead)) return;
  var payload=read(s,currentRead);assertSummaryBinding(s,payload.summary());assertItems(s,payload.summary(),currentRead);
  BizAllowanceItem aq=new BizAllowanceItem();aq.setUserId(s.getUserId());aq.setSemester(s.getSemester());aq.setStatus(1);aq.getParams().put("currentRead",currentRead);var current=allowances.selectBizAllowanceItemList(aq);
  Map<Long,Allowance> old=new HashMap<>();payload.pay().allowances().forEach(a->old.put(a.id(),a));
  if(current.size()!=old.size())throw stale("其他酬金集合已变化，请重新核算酬金");
  for(var a:current){com.workload.system.calc.allowance.AllowanceEvidence.verifyFor(a);var v=old.get(a.getId());if(v==null||!numeric(v.amount(),a.getAmount())||!Objects.equals(v.row().calculationEvidenceJson(),a.getCalculationEvidenceJson()))throw stale("其他酬金金额/来源已变化，请重新核算酬金");com.workload.system.calc.allowance.AllowanceEvidence.verifyFor(a);}
  assertPairedPay(s,payload,currentRead);
 }
 private void assertPairedPay(BizWorkloadSummary s,Payload payload,boolean currentRead){
  BizPayRecord pq=new BizPayRecord();pq.setUserId(s.getUserId());pq.setSemester(s.getSemester());pq.getParams().put("currentRead",currentRead);var ps=pays.selectBizPayRecordList(pq);
  if(ps.size()!=1)throw SnapshotIntegrity.failure("酬金记录缺失或重复");var p=ps.get(0);
  assertPay(s,payload,p);
 }
 private void assertPay(BizWorkloadSummary s,Payload payload,BizPayRecord p){
  if(p==null)throw SnapshotIntegrity.failure("酬金记录缺失");
  if(!Objects.equals(p.getUserId(),s.getUserId())||!Objects.equals(p.getSemester(),s.getSemester())||!Objects.equals(p.getSettlementSnapshotId(),s.getSettlementSnapshotId())||!Objects.equals(p.getSummaryId(),s.getId())||!Objects.equals(p.getId(),payload.pay().payId())||!numeric(payload.pay().courseHourPay(),p.getCourseHourPay())||!numeric(payload.pay().otherPayTotal(),p.getOtherPayTotal())||!Objects.equals(payload.pay().totalPay(),p.getTotalPay()))throw SnapshotIntegrity.failure("汇总与酬金结算指针/金额不一致");
 }
 public void decorate(BizWorkloadSummary s){if(s==null)return;s.setSettlementRequired(s.getSettlementSnapshotId()!=null||scope.required(s.getSemester()));s.setSettlementMode(scope.mode(s.getSemester()));s.setSettlementSubmittable(false);s.setWorkloadCap(null);s.setTeacherNature(null);
  if(s.getSettlementSnapshotId()==null){if(!genuineLegacy(s,false))throw SnapshotIntegrity.failure("已有结算证据但汇总指针缺失");s.setSettlementEvidenceStatus("HISTORICAL_MISSING");s.setSettlementMissingReason("历史记录未保存结算证据，人员性质及具体封顶阈值未知；禁止按当前资料补造");return;}
  var p=read(s,false);assertSummaryBinding(s,p.summary());s.setWorkloadCap(num(p.summary().results().get("cap")));s.setTeacherNature(p.summary().person().get("nature"));s.setEligibilityReason(p.summary().eligibilityReason());s.setSettlementMode(p.summary().mode());
  assertPairedPay(s,p,false);
  boolean stale=Integer.valueOf(1).equals(s.getNeedsRecalculation());
  if(!stale)try {assertComplete(s,false);}catch(com.workload.common.exception.ServiceException x){if(!Integer.valueOf(409).equals(x.getCode()))throw x;stale=true;s.setSettlementMissingReason(x.getMessage());}
  s.setSettlementEvidenceStatus(stale?"STALE":"VERIFIED");s.setSettlementSubmittable(!stale);
 }
 public void decorate(BizPayRecord p){if(p==null)return;var s=summaries.selectBizWorkloadSummaryById(p.getSummaryId());decorate(s);if(s==null)return;
  if(!Objects.equals(p.getSettlementSnapshotId(),s.getSettlementSnapshotId()))throw SnapshotIntegrity.failure("酬金与汇总结算指针不符");
  if(s.getSettlementSnapshotId()!=null)assertPay(s,read(s,false),p);
  p.setSettlementRequired(s.getSettlementRequired());p.setSettlementSubmittable(s.getSettlementSubmittable());p.setSettlementEvidenceStatus(s.getSettlementEvidenceStatus());p.setSettlementMode(s.getSettlementMode());p.setTeacherNature(s.getTeacherNature());p.setEligibilityReason(s.getEligibilityReason());p.setSettlementMissingReason(s.getSettlementMissingReason());p.setWorkloadCap(s.getWorkloadCap());
 }
 /** Deletion must preserve evidence even if either main-table pointer was cleared.
  * Callers already hold the period lock; every decision below uses locking/current reads. */
 @Transactional(propagation=Propagation.MANDATORY)
 public void assertSummaryDeletionAllowed(BizWorkloadSummary initial){
  var current=summaries.selectByUserSemesterForUpdate(initial.getUserId(),initial.getSemester());
  if(current==null||!Objects.equals(current.getId(),initial.getId()))throw new com.workload.common.exception.ServiceException("汇总记录已变化，请刷新后重试");
  var records=lockPayRows(initial.getUserId(),initial.getSemester());
  if(hasSettlementHistory(current,records))throw protectedDeletion();
 }
 @Transactional(propagation=Propagation.MANDATORY)
 public void assertPayDeletionAllowed(BizPayRecord initial){
  var summary=summaries.selectByUserSemesterForUpdate(initial.getUserId(),initial.getSemester());
  var records=lockPayRows(initial.getUserId(),initial.getSemester());
  if(records.stream().noneMatch(row->Objects.equals(row.getId(),initial.getId())))throw new com.workload.common.exception.ServiceException("酬金记录已变化，请刷新后重试");
  if(hasSettlementHistory(summary,records))throw protectedDeletion();
 }
 private List<BizPayRecord> lockPayRows(Long userId,String semester){
  var query=new BizPayRecord();query.setUserId(userId);query.setSemester(semester);query.getParams().put("currentRead",true);
  return pays.selectBizPayRecordList(query);
 }
 private boolean hasSettlementHistory(BizWorkloadSummary summary,List<BizPayRecord> records){
  Set<Long> summaryIds=new TreeSet<>();
  if(summary!=null){if(summary.getSettlementSnapshotId()!=null)return true;summaryIds.add(summary.getId());}
  for(var record:records){if(record.getSettlementSnapshotId()!=null)return true;if(record.getSummaryId()!=null)summaryIds.add(record.getSummaryId());}
  return summaryIds.stream().anyMatch(id->snapshots.selectMaxVersionCurrent(id)!=null);
 }
 private static com.workload.common.exception.ServiceException protectedDeletion(){return new com.workload.common.exception.ServiceException("已有结算证据的汇总或酬金记录禁止通用删除，请保留结算链",409);}
 private boolean genuineLegacy(BizWorkloadSummary s,boolean currentRead){
  if((currentRead?snapshots.selectMaxVersionCurrent(s.getId()):snapshots.selectMaxVersion(s.getId()))!=null)return false;
  BizPayRecord query=new BizPayRecord();query.setUserId(s.getUserId());query.setSemester(s.getSemester());query.getParams().put("currentRead",currentRead);
  return pays.selectBizPayRecordList(query).stream().noneMatch(pay->pay.getSettlementSnapshotId()!=null);
 }
 private static boolean numericNullable(String actual,BigDecimal expected){return actual==null?expected==null:numeric(actual,expected);}
 private static com.workload.common.exception.ServiceException stale(String message){return new com.workload.common.exception.ServiceException(message,409);}
 private static boolean numeric(String a,BigDecimal b){return a!=null&&b!=null&&num(a).compareTo(b)==0;}
 private static String plain(Object o){return FactorProvenance.plain(o);}
 private static String date(Date d){if(d==null)return null;if(d instanceof java.sql.Date q)return q.toLocalDate().toString();return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();}
}
