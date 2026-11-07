package com.workload.system.review;

import com.workload.common.core.page.TableDataInfo;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DataScopeUtil;
import com.workload.system.domain.*;
import com.workload.system.mapper.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class ReviewQueryService {
    @Autowired private BizWorkloadSummaryMapper summaries;
    @Autowired private BizWorkloadReviewBatchMapper batches;
    @Autowired private BizWorkloadItemReviewMapper reviews;
    @Autowired private BizOfflineAdjustmentNoteMapper notes;
    @Autowired private BizAuditLogMapper logs;
    @Autowired private ReviewBatchService evidence;
    public BizWorkloadSummary requireSummary(Long id){var s=summaries.selectBizWorkloadSummaryById(id);if(s==null)throw new ServiceException("汇总不存在",404);DataScopeUtil.assertOwnOrAdmin(s.getUserId());return s;}
    public Map<String,Object> detail(Long id){
        var s=requireSummary(id);Map<String,Object> result=new LinkedHashMap<>();result.put("summary",s);var batch=s.getReviewBatchNo()==null?null:batches.selectBatch(id,s.getReviewBatchNo(),false);
        boolean draft=Integer.valueOf(0).equals(s.getStatus());boolean matches=batch!=null&&Objects.equals(batch.getSummaryDataVersion(),s.getDataVersion());
        boolean fresh=!Integer.valueOf(1).equals(s.getNeedsRecalculation());
        String status=batch==null?(draft&&s.getReviewBatchNo()==null?"NOT_SUBMITTED":"HISTORICAL_MISSING"):draft?"HISTORY_ONLY":matches&&fresh?"VERIFIED":"STALE";
        result.put("reviewEvidenceStatus",status);result.put("reviewEvidenceMissingReason","HISTORICAL_MISSING".equals(status)?"历史记录未保存审核批次，请勿按当前来源补造":null);
        if(batch==null){result.put("currentBatch",null);result.put("summarySnapshot",null);result.put("paySnapshot",null);result.put("sourceCounts",null);result.put("reviewCounts",Map.of());}
        else {ObjectNode n=evidence.verifyBatchRow(batch);Map<String,Object> meta=new LinkedHashMap<>();meta.put("current",!draft&&matches&&fresh);meta.put("versionMatches",matches);meta.put("id",batch.getId());meta.put("reviewBatchNo",batch.getReviewBatchNo());meta.put("summaryDataVersion",batch.getSummaryDataVersion());meta.put("formatVersion",batch.getFormatVersion());meta.put("dataHash",batch.getDataHash());meta.put("createdBy",batch.getCreatedBy());meta.put("createdAt",batch.getCreatedAt());meta.put("settlementSnapshotId",batch.getSettlementSnapshotId());meta.put("settlementSnapshotHash",batch.getSettlementSnapshotHash());result.put("currentBatch",meta);result.put("summarySnapshot",ReviewSnapshotCodec.plain(n.get("summary")));result.put("paySnapshot",ReviewSnapshotCodec.plain(n.get("pay")));result.put("sourceCounts",ReviewSnapshotCodec.plain(n.get("sourceCounts")));
            Map<String,Long> counts=new LinkedHashMap<>();for(String state:List.of("PENDING","PASS","REJECT","WAIVE"))counts.put(state,reviews.countPage(id,batch.getReviewBatchNo(),null,null,state));result.put("reviewCounts",counts);}
        return result;
    }
    public TableDataInfo reviews(Long summaryId,String batchNo,ReviewCommands.Page page){var s=requireSummary(summaryId);String no=batchNo==null?s.getReviewBatchNo():batchNo;if(no==null)return table(List.of(),0);validateBatchNo(no);return reviewPage(summaryId,no,null,null,null,page);}
    public TableDataInfo center(Long summaryId,String batchNo,Long userId,String type,String result,ReviewCommands.Page page){if(summaryId!=null)requireSummary(summaryId);if(batchNo!=null)validateBatchNo(batchNo);if(type!=null&&!Set.of("TEACHING_TASK","MANUAL_DECLARATION","G11_ROLE","OVER_LIMIT").contains(type))throw ReviewCommands.bad("审核类型无效");if(result!=null&&!Set.of("PENDING","PASS","REJECT","WAIVE").contains(result))throw ReviewCommands.bad("审核结果无效");return reviewPage(summaryId,batchNo,DataScopeUtil.resolveUserId(userId),type,result,page);}
    private TableDataInfo reviewPage(Long summaryId,String no,Long userId,String type,String result,ReviewCommands.Page page){var rows=reviews.selectPage(summaryId,no,userId,type,result,page.offset(),page.size());rows.forEach(evidence::verifyReviewRow);return table(rows,reviews.countPage(summaryId,no,userId,type,result));}
    public BizWorkloadItemReview review(Long id){var r=reviews.selectById(id);if(r==null)throw new ServiceException("审核记录不存在",404);requireSummary(r.getSummaryId());evidence.verifyReviewRow(r);return r;}
    public TableDataInfo logs(Long summaryId,ReviewCommands.Page page){requireSummary(summaryId);return table(logs.selectPage(summaryId,page.offset(),page.size()),logs.countPage(summaryId));}
    public TableDataInfo notes(Long summaryId,ReviewCommands.Page page){requireSummary(summaryId);return table(notes.selectPage(summaryId,page.offset(),page.size()),notes.countPage(summaryId));}
    private void validateBatchNo(String no){if(!no.matches("[A-Za-z0-9-]{1,50}"))throw ReviewCommands.bad("批次号无效");}
    private static TableDataInfo table(List<?> rows,long total){TableDataInfo t=new TableDataInfo(rows,total);t.setCode(200);t.setMsg("查询成功");return t;}
}
