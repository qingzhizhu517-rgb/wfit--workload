package com.workload.system.review;

import com.workload.common.exception.ServiceException;
import com.workload.common.utils.*;
import com.workload.system.domain.*;
import com.workload.system.mapper.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ItemReviewService {
    @Autowired private ReviewBatchService batches;
    @Autowired private BizWorkloadItemReviewMapper reviews;
    @Autowired private BizAuditLogMapper logs;
    @Transactional(propagation=Propagation.REQUIRES_NEW,rollbackFor=Exception.class)
    public void review(Long id,String action,ReviewCommands.Review command){
        authorize(action);if(command==null||command.version()<0)throw ReviewCommands.bad("审核版本必填");
        String reason=command.reason();if(reason!=null&&reason.length()>500||Set.of("REJECT","WAIVE").contains(action)&&(reason==null||reason.trim().isEmpty()))throw ReviewCommands.bad("审核原因必填且最多500字");
        var initial=reviews.selectById(id);if(initial==null)throw new ServiceException("审核记录不存在",404);
        var s=batches.lock(initial.getSummaryId());DataScopeUtil.assertOwnOrAdmin(s.getUserId());
        var current=batches.assertCurrent(s,false);var row=current.stream().filter(r->Objects.equals(r.getId(),id)).findFirst().orElseThrow(()->ReviewCommands.conflict("审核批次已过期"));
        if(!"PENDING".equals(row.getResult())||!Objects.equals(row.getVersion(),command.version()))throw ReviewCommands.conflict("审核已处理或版本过期，请刷新");
        row.setResult(action);row.setReason(reason);row.setReviewerId(SecurityUtils.getUserId());row.setReviewerName(SecurityUtils.getUsername());row.setReviewedAt(new Date());
        if(reviews.reviewIfPending(row,command.version(),s.getDataVersion())!=1)throw ReviewCommands.conflict("审核版本或批次已过期");
        var log=new BizAuditLog();log.setSummaryId(s.getId());log.setAction("review_"+action.toLowerCase(java.util.Locale.ROOT));log.setFromStatus(1);log.setToStatus(1);log.setOperatorId(SecurityUtils.getUserId());log.setOperatorName(SecurityUtils.getUsername());log.setReason("reviewId="+id+";batch="+row.getReviewBatchNo()+";result="+action);var stamp=com.workload.common.core.domain.audit.AuditFields.current();log.setCreateBy(stamp.getBy());log.setCreateTime(stamp.getTime());log.setUpdateBy(stamp.getBy());log.setUpdateTime(stamp.getTime());if(logs.insertBizAuditLog(log)!=1)throw ReviewCommands.conflict("审核审计保存失败");
    }
    public static void authorize(String action){
        if(!Set.of("PASS","REJECT","WAIVE").contains(action))throw ReviewCommands.bad("不支持的审核动作");
        if(DataScopeUtil.isTeacherOnly()||!SecurityUtils.hasPermi("WAIVE".equals(action)?"system:auditRecord:waive":"system:auditRecord:review"))throw new ServiceException("无权执行审核动作",403);
        if(SecurityUtils.getUserId()==null||SecurityUtils.getUsername()==null)throw new ServiceException("缺少可信登录身份",403);
    }
}
