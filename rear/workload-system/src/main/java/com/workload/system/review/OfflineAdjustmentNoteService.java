package com.workload.system.review;

import com.workload.common.core.domain.audit.AuditFields;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.*;
import com.workload.system.calc.SettlementEvidence;
import com.workload.system.domain.*;
import com.workload.system.mapper.*;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfflineAdjustmentNoteService {
    @Autowired private ReviewBatchService batches;
    @Autowired private BizOfflineAdjustmentNoteMapper notes;
    @Autowired private BizWorkloadSettlementSnapshotMapper settlements;
    @Autowired private BizAuditLogMapper logs;
    @Transactional(rollbackFor=Exception.class)
    public BizOfflineAdjustmentNote append(Long summaryId,ReviewCommands.Note supplied){
        if(DataScopeUtil.isTeacherOnly()||!SecurityUtils.hasPermi("system:offlineAdjustmentNote:add")||SecurityUtils.getUserId()==null||SecurityUtils.getUsername()==null)throw new ServiceException("无权追加线下说明",403);
        var command=ReviewCommands.validateNote(supplied);var s=batches.lock(summaryId);DataScopeUtil.assertOwnOrAdmin(s.getUserId());
        if(!Integer.valueOf(2).equals(s.getStatus()))throw ReviewCommands.conflict("仅已完结汇总可追加线下补差说明");
        var old=notes.selectByRequest(summaryId,command.key());if(old!=null){if(!same(old,command)||!Objects.equals(old.getHandledById(),SecurityUtils.getUserId()))throw ReviewCommands.conflict("幂等请求标识已用于不同内容或经办人");return old;}
        var row=new BizOfflineAdjustmentNote();row.setSummaryId(summaryId);row.setUserId(s.getUserId());row.setSemester(s.getSemester());row.setRequestKey(command.key());row.setAdjustmentAmount(command.amount());row.setReason(command.reason());row.setHandledAt(command.handledAt());row.setHandledById(SecurityUtils.getUserId());row.setHandledBy(SecurityUtils.getUsername());row.setReferenceNo(command.referenceNo());row.setNoteText(command.noteText());row.setAttachmentRef(command.attachmentRef());
        row.setSettlementSnapshotId(s.getSettlementSnapshotId());if(s.getSettlementSnapshotId()!=null){var original=settlements.selectByIdCurrent(s.getSettlementSnapshotId());if(original==null||!Objects.equals(original.getSummaryId(),summaryId)||!Objects.equals(original.getUserId(),s.getUserId())||!Objects.equals(original.getSemester(),s.getSemester()))throw ReviewCommands.conflict("原结算证据引用损坏");SettlementEvidence.decode(original.getPayloadJson(),original.getHash());row.setSettlementSnapshotHash(original.getHash());}
        var stamp=AuditFields.current();row.setCreatedBy(stamp.getBy());row.setCreatedAt(stamp.getTime());if(notes.insert(row)!=1)throw ReviewCommands.conflict("线下说明保存失败");
        var log=new BizAuditLog();log.setSummaryId(summaryId);log.setAction("offlineNote");log.setFromStatus(2);log.setToStatus(2);log.setOperatorId(SecurityUtils.getUserId());log.setOperatorName(SecurityUtils.getUsername());log.setReason("noteId="+row.getId()+";requestKey="+command.key());log.setCreateBy(stamp.getBy());log.setCreateTime(stamp.getTime());log.setUpdateBy(stamp.getBy());log.setUpdateTime(stamp.getTime());if(logs.insertBizAuditLog(log)!=1)throw ReviewCommands.conflict("线下说明审计保存失败");return row;
    }
    private boolean same(BizOfflineAdjustmentNote row,ReviewCommands.Note c){return row.getAdjustmentAmount().compareTo(c.amount())==0&&Objects.equals(row.getReason(),c.reason())&&Objects.equals(row.getHandledAt(),c.handledAt())&&Objects.equals(row.getReferenceNo(),c.referenceNo())&&Objects.equals(row.getNoteText(),c.noteText())&&Objects.equals(row.getAttachmentRef(),c.attachmentRef());}
}
