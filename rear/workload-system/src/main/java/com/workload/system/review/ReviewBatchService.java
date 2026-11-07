package com.workload.system.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.workload.common.exception.ServiceException;
import com.workload.common.core.domain.audit.AuditFields;
import com.workload.system.calc.*;
import com.workload.system.domain.*;
import com.workload.system.mapper.*;
import com.workload.system.service.SnapshotIntegrity;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Lock-bound review evidence lifecycle. Historical reads use persisted evidence only. */
@Service
public class ReviewBatchService {
    @Autowired private WorkloadWriteGuard guard;
    @Autowired private BizWorkloadSummaryMapper summaries;
    @Autowired private BizWorkloadItemMapper items;
    @Autowired private BizAllowanceItemMapper allowances;
    @Autowired private BizPayRecordMapper pays;
    @Autowired private BizWorkloadCalcSnapshotMapper calculations;
    @Autowired private BizWorkloadSettlementSnapshotMapper settlements;
    @Autowired private BizWorkloadReviewBatchMapper batches;
    @Autowired private BizWorkloadItemReviewMapper reviews;
    @Autowired private SettlementSnapshotService settlementEvidence;

    @Transactional(propagation=Propagation.MANDATORY)
    public BizWorkloadSummary lock(Long id){
        BizWorkloadSummary initial=summaries.selectBizWorkloadSummaryById(id);
        if(initial==null)throw new ServiceException("汇总不存在",404);
        guard.lockPeriod(initial.getUserId(),initial.getSemester());
        var current=summaries.selectByUserSemesterForUpdate(initial.getUserId(),initial.getSemester());
        if(current==null||!Objects.equals(current.getId(),id))throw ReviewCommands.conflict("汇总已变化");return current;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public String create(BizWorkloadSummary s){
        if(!Integer.valueOf(0).equals(s.getStatus())||s.getDataVersion()==null||s.getDataVersion()<=0||s.getLastCalculatedAt()==null)throw ReviewCommands.conflict("请先重新核算生成新版汇总再提交");
        if(Integer.valueOf(1).equals(s.getNeedsRecalculation()))throw ReviewCommands.conflict("源数据已变化，请重新核算");
        String no=UUID.randomUUID().toString();ObjectNode payload=capture(s,no);
        settlementEvidence.assertSubmittable(s);
        var sealed=ReviewSnapshotCodec.sealBatch(payload);var row=new BizWorkloadReviewBatch();
        row.setSummaryId(s.getId());row.setReviewBatchNo(no);row.setSummaryDataVersion(s.getDataVersion());row.setUserId(s.getUserId());row.setSemester(s.getSemester());row.setFormatVersion(1);row.setSnapshotJson(sealed.json());row.setDataHash(sealed.hash());
        row.setSettlementSnapshotId(s.getSettlementSnapshotId());row.setSettlementSnapshotHash(nullable(payload.get("settlementSnapshotHash")));var audit=AuditFields.current();row.setCreatedBy(audit.getBy());row.setCreatedAt(audit.getTime());
        if(batches.insert(row)!=1)throw ReviewCommands.conflict("审核批次保存失败");
        for(JsonNode member:payload.get("items")){
            String json=member.get("json").textValue(),hash=member.get("hash").textValue();ObjectNode i=ReviewSnapshotCodec.readItem(json,hash);var review=new BizWorkloadItemReview();
            review.setSummaryId(s.getId());review.setItemId(i.get("itemId").longValue());review.setReviewBatchNo(no);review.setReviewType(i.get("reviewType").textValue());review.setFormatVersion(1);review.setSnapshotJson(json);review.setDataHash(hash);review.setResult("PENDING");review.setVersion(0L);
            review.setCreateBy(audit.getBy());review.setCreateTime(audit.getTime());review.setUpdateBy(audit.getBy());review.setUpdateTime(audit.getTime());
            if(reviews.insert(review)!=1)throw ReviewCommands.conflict("审核成员保存失败");
        }
        if(summaries.setReviewBatch(s.getId(),s.getDataVersion(),no)!=1)throw ReviewCommands.conflict("审核批次指针保存失败");s.setReviewBatchNo(no);return no;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public List<BizWorkloadItemReview> assertCurrent(BizWorkloadSummary s,boolean finalApproval){
        if(!Integer.valueOf(1).equals(s.getStatus()))throw ReviewCommands.conflict("汇总已不在待审状态");
        if(s.getReviewBatchNo()==null)throw ReviewCommands.conflict("历史待审缺少审核批次，请先驳回、重新核算再提交");
        if(s.getDataVersion()==null||s.getDataVersion()<=0||Integer.valueOf(1).equals(s.getNeedsRecalculation()))throw ReviewCommands.conflict("汇总数据已失效，请驳回后重新核算");
        // Capture locks items -> allowances -> pay before first batch/review current read.
        ObjectNode current=capture(s,s.getReviewBatchNo());settlementEvidence.assertSubmittable(s);
        var b=batches.selectBatch(s.getId(),s.getReviewBatchNo(),true);if(b==null)throw ReviewCommands.conflict("当前审核批次缺失，请先驳回、重新核算再提交");
        ObjectNode saved=verifyBatchRow(b);if(!Objects.equals(b.getSummaryDataVersion(),s.getDataVersion()))throw ReviewCommands.conflict("审核批次数据版本过期");
        ObjectNode expected=current.deepCopy(),original=saved.deepCopy();
        // A legal teacher signature is an independent event; do not re-sign old evidence with current signatures.
        for(String key:List.of("teacherSign","teacherSignTime")){expected.withObject("/summary").remove(key);original.withObject("/summary").remove(key);}
        if(!Objects.equals(expected.get("pay"),original.get("pay"))||!Objects.equals(expected.get("allowances"),original.get("allowances")))throw ReviewCommands.conflict("审核批次酬金输出或来源已变化");
        if(!ReviewSnapshotCodec.equivalent(expected,original))throw ReviewCommands.conflict("审核批次成员、数值、版本或引用已变化");
        List<BizWorkloadItemReview> rows=reviews.selectForBatch(s.getId(),s.getReviewBatchNo(),true);
        if(rows.isEmpty()||rows.size()!=saved.get("items").size())throw ReviewCommands.conflict("审核成员为空或不完整");
        Map<Long,JsonNode> memberById=new HashMap<>();for(JsonNode i:saved.get("items")){var item=ReviewSnapshotCodec.readItem(i.get("json").textValue(),i.get("hash").textValue());memberById.put(item.get("itemId").longValue(),i);}
        Set<Long> seen=new HashSet<>();
        for(var r:rows){ObjectNode i=verifyReviewRow(r);JsonNode m=memberById.get(r.getItemId());if(!seen.add(r.getItemId())||m==null||!Objects.equals(r.getSummaryId(),s.getId())||!Objects.equals(r.getReviewBatchNo(),s.getReviewBatchNo())||!Objects.equals(r.getDataHash(),m.get("hash").textValue())||!i.equals(ReviewSnapshotCodec.readItem(m.get("json").textValue(),m.get("hash").textValue())))throw ReviewCommands.conflict("审核成员全集或证据不一致");
            if(finalApproval&&!Set.of("PASS","WAIVE").contains(r.getResult()))throw ReviewCommands.conflict("存在未通过的审核明细，请完成逐项审核");}
        return rows;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void assertDeletionAllowed(BizWorkloadSummary s){if(batches.countHistoryCurrent(s.getId())>0)throw ReviewCommands.conflict("已有审核历史的汇总禁止删除");}
    public ObjectNode verifyBatchRow(BizWorkloadReviewBatch b){
        ObjectNode n=ReviewSnapshotCodec.readBatch(b.getSnapshotJson(),b.getDataHash());
        if(!Integer.valueOf(1).equals(b.getFormatVersion())||!Objects.equals(b.getSummaryId(),n.get("summaryId").longValue())||!Objects.equals(b.getSummaryDataVersion(),n.get("dataVersion").longValue())||!Objects.equals(b.getUserId(),n.get("userId").longValue())||!Objects.equals(b.getSemester(),n.get("semester").textValue())||!Objects.equals(b.getReviewBatchNo(),n.get("reviewBatchNo").textValue())||!Objects.equals(b.getSettlementSnapshotId(),n.get("settlementSnapshotId").isNull()?null:n.get("settlementSnapshotId").longValue())||!Objects.equals(b.getSettlementSnapshotHash(),nullable(n.get("settlementSnapshotHash"))))throw ReviewCommands.conflict("审核批次行与证据引用不符");return n;
    }
    public ObjectNode verifyReviewRow(BizWorkloadItemReview r){
        ObjectNode n=ReviewSnapshotCodec.readItem(r.getSnapshotJson(),r.getDataHash());
        if(!Integer.valueOf(1).equals(r.getFormatVersion())||!Objects.equals(r.getSummaryId(),n.get("summaryId").longValue())||!Objects.equals(r.getItemId(),n.get("itemId").longValue())||!Objects.equals(r.getReviewType(),n.get("reviewType").textValue())||!Objects.equals(r.getReviewBatchNo(),n.get("reviewBatchNo").textValue())||!Set.of("PENDING","PASS","REJECT","WAIVE").contains(r.getResult())||r.getVersion()==null||r.getVersion()<0)throw ReviewCommands.conflict("审核明细行与证据不符");
        boolean pending="PENDING".equals(r.getResult());
        if(pending&&(r.getVersion()!=0||r.getReviewerId()!=null||r.getReviewerName()!=null||r.getReviewedAt()!=null||r.getReason()!=null)
                ||!pending&&(r.getVersion()!=1||r.getReviewerId()==null||r.getReviewerName()==null||r.getReviewerName().isBlank()||r.getReviewedAt()==null)
                ||Set.of("REJECT","WAIVE").contains(r.getResult())&&(r.getReason()==null||r.getReason().trim().isEmpty())
                ||r.getReason()!=null&&r.getReason().length()>500)throw ReviewCommands.conflict("审核结果、版本、原因或审核人不完整");
        return n;
    }
    private ObjectNode capture(BizWorkloadSummary s,String batchNo){
        ObjectNode n=ReviewSnapshotCodec.object();n.put("formatVersion",1);n.put("summaryId",s.getId());n.put("userId",s.getUserId());n.put("semester",s.getSemester());n.put("reviewBatchNo",batchNo);n.put("dataVersion",s.getDataVersion());n.set("summary",summaryValues(s));
        var q=new BizWorkloadItem();q.setUserId(s.getUserId());q.setSemester(s.getSemester());q.getParams().put("currentRead",true);var source=items.selectBizWorkloadItemList(q);ArrayNode members=n.putArray("items");
        for(var i:source.stream().sorted(Comparator.comparing(BizWorkloadItem::getId)).toList()){if(Integer.valueOf(3).equals(i.getStatus()))continue;if(!Integer.valueOf(1).equals(i.getStatus()))throw ReviewCommands.conflict("存在未核对的明细");var seal=ReviewSnapshotCodec.sealItem(itemEvidence(s,i,batchNo));var m=members.addObject();m.put("json",seal.json());m.put("hash",seal.hash());}
        ObjectNode counts=n.putObject("sourceCounts");
        put(counts,"includedReviewItemCount",members.size());put(counts,"excludedRejectedItemCount",source.stream().filter(i->Integer.valueOf(3).equals(i.getStatus())).count());
        var included=source.stream().filter(i->!Integer.valueOf(3).equals(i.getStatus())).toList();
        put(counts,"distinctTeachingTaskReferenceCount",included.stream().map(BizWorkloadItem::getTaskId).filter(Objects::nonNull).distinct().count());
        put(counts,"distinctRoleAssignmentReferenceCount",included.stream().map(BizWorkloadItem::getAssignmentId).filter(Objects::nonNull).distinct().count());
        put(counts,"manualDeclarationItemCount",included.stream().filter(i->"SELF".equals(i.getSourceType())||"MANUAL".equals(i.getSourceType())).count());
        put(counts,"importRawRowCount",null);put(counts,"importRawMissingReason","未保存可精确归属本教师的原始导入行全集；不能使用整个导入批次行数");
        if(members.isEmpty())throw ReviewCommands.conflict("审核成员不得为空");
        var aq=new BizAllowanceItem();aq.setUserId(s.getUserId());aq.setSemester(s.getSemester());aq.setStatus(1);aq.getParams().put("currentRead",true);ArrayNode as=n.putArray("allowances");
        for(var a:allowances.selectBizAllowanceItemList(aq).stream().sorted(Comparator.comparing(BizAllowanceItem::getId)).toList()){ObjectNode v=as.addObject();put(v,"id",a.getId());put(v,"amount",a.getAmount());put(v,"calculationEvidenceJson",a.getCalculationEvidenceJson());}
        var pq=new BizPayRecord();pq.setUserId(s.getUserId());pq.setSemester(s.getSemester());pq.getParams().put("currentRead",true);var ps=pays.selectBizPayRecordList(pq);if(ps.size()!=1)throw ReviewCommands.conflict("酬金记录缺失或重复，请重新核算");var p=ps.get(0);ObjectNode pay=n.putObject("pay");put(pay,"id",p.getId());put(pay,"summaryId",p.getSummaryId());put(pay,"courseHourPay",p.getCourseHourPay());put(pay,"otherPayTotal",p.getOtherPayTotal());put(pay,"totalPay",p.getTotalPay());put(pay,"status",p.getStatus());put(pay,"settlementSnapshotId",p.getSettlementSnapshotId());
        if(s.getSettlementSnapshotId()==null){n.putNull("settlementSnapshotId");n.putNull("settlementSnapshotHash");n.put("settlementEvidenceStatus","HISTORICAL_MISSING");}
        else {var row=settlements.selectByIdCurrent(s.getSettlementSnapshotId());if(row==null)throw ReviewCommands.conflict("原结算证据缺失");SettlementEvidence.decode(row.getPayloadJson(),row.getHash());n.put("settlementSnapshotId",row.getId());n.put("settlementSnapshotHash",row.getHash());n.put("settlementEvidenceStatus","VERIFIED");}
        return n;
    }
    private ObjectNode itemEvidence(BizWorkloadSummary s,BizWorkloadItem i,String batchNo){
        boolean manual="SELF".equals(i.getSourceType())||"MANUAL".equals(i.getSourceType());boolean role="G11".equals(i.getItemType());
        if(role&&i.getAssignmentId()==null||!role&&!manual&&i.getTaskId()==null)throw ReviewCommands.conflict("明细缺少可解释来源，itemId="+i.getId());
        String type=Integer.valueOf(1).equals(i.getIsOverLimit())?"OVER_LIMIT":role?"G11_ROLE":manual?"MANUAL_DECLARATION":"TEACHING_TASK";
        ObjectNode n=ReviewSnapshotCodec.object();n.put("formatVersion",1);n.put("summaryId",s.getId());n.put("userId",s.getUserId());n.put("semester",s.getSemester());n.put("itemId",i.getId());n.put("reviewBatchNo",batchNo);n.put("reviewType",type);ObjectNode v=n.putObject("values");
        put(v,"itemType",i.getItemType());put(v,"sourceType",i.getSourceType());put(v,"taskId",i.getTaskId());put(v,"assignmentId",i.getAssignmentId());put(v,"roleType",i.getRoleType());put(v,"courseName",i.getCourseName());put(v,"educationLevel",i.getEducationLevel());put(v,"majorCategory",i.getMajorCategory());put(v,"description",i.getDescription());put(v,"calculatedWorkload",i.getCalculatedWorkload());put(v,"isOverLimit",i.getIsOverLimit());put(v,"status",i.getStatus());put(v,"calculationVersion",i.getCalculationVersion());put(v,"lastCalculatedAt",i.getLastCalculatedAt());put(v,"factorProvenanceJson",i.getFactorProvenanceJson());put(v,"createBy",i.getCreateBy());put(v,"createTime",i.getCreateTime());
        var calc=calculations.selectLatestByItemIdCurrent(i.getId());
        if(calc!=null){SnapshotIntegrity.verify(calc);put(v,"calculationSnapshotId",calc.getId());put(v,"calculationSnapshotHash",calc.getSnapshotHash());put(v,"calculationSnapshotJson",ReviewSnapshotCodec.tree(calc).toString());put(v,"evidenceStatus","VERIFIED");}
        else{put(v,"calculationSnapshotId",null);put(v,"calculationSnapshotHash",null);put(v,"calculationSnapshotJson",null);put(v,"evidenceStatus","HISTORICAL_MISSING");}return n;
    }
    public static ObjectNode summaryValues(BizWorkloadSummary s){
        ObjectNode v=ReviewSnapshotCodec.object();put(v,"academicYear",s.getAcademicYear());put(v,"g7",s.getG7());put(v,"g8",s.getG8());put(v,"g9",s.getG9());put(v,"g10",s.getG10());put(v,"g11",s.getG11());put(v,"totalWorkload",s.getTotalWorkload());put(v,"ratedWorkload",s.getRatedWorkload());put(v,"excessWorkload",s.getExcessWorkload());put(v,"g8Remark",s.getG8Remark());put(v,"g11Remark",s.getG11Remark());put(v,"title",s.getTitle());put(v,"payRate",s.getPayRate());put(v,"performancePay",s.getPerformancePay());put(v,"isCapped",s.getIsCapped());put(v,"basicTeachingStandard",s.getBasicTeachingStandard());put(v,"basicTeachingMet",s.getBasicTeachingMet());put(v,"teacherSign",s.getTeacherSign());put(v,"teacherSignTime",s.getTeacherSignTime());put(v,"deptLeaderSign",s.getDeptLeaderSign());put(v,"deptLeaderSignTime",s.getDeptLeaderSignTime());put(v,"academicAssistantSign",s.getAcademicAssistantSign());put(v,"academicAssistantSignTime",s.getAcademicAssistantSignTime());put(v,"needsRecalculation",s.getNeedsRecalculation());put(v,"lastCalculatedAt",s.getLastCalculatedAt());return v;
    }
    static void put(ObjectNode n,String key,Object value){if(value==null)n.putNull(key);else n.put(key,value instanceof BigDecimal d?d.stripTrailingZeros().toPlainString():value instanceof Date d?String.valueOf(d.getTime()):value.toString());}
    static String nullable(JsonNode n){return n==null||n.isNull()?null:n.textValue();}
}
