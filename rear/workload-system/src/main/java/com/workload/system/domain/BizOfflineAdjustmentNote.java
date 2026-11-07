package com.workload.system.domain;

import java.util.Date;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.workload.common.core.domain.BaseEntity;

/** Server-owned review evidence row; controllers bind only explicit command contracts. */
public class BizOfflineAdjustmentNote {
    private Long id;
    public Long getId(){return id;}
    public void setId(Long value){id=value;}
    private Long summaryId;
    public Long getSummaryId(){return summaryId;}
    public void setSummaryId(Long value){summaryId=value;}
    private Long userId;
    public Long getUserId(){return userId;}
    public void setUserId(Long value){userId=value;}
    private String semester;
    public String getSemester(){return semester;}
    public void setSemester(String value){semester=value;}
    private String requestKey;
    public String getRequestKey(){return requestKey;}
    public void setRequestKey(String value){requestKey=value;}
    private BigDecimal adjustmentAmount;
    public BigDecimal getAdjustmentAmount(){return adjustmentAmount;}
    public void setAdjustmentAmount(BigDecimal value){adjustmentAmount=value;}
    private String reason;
    public String getReason(){return reason;}
    public void setReason(String value){reason=value;}
    private LocalDate handledAt;
    public LocalDate getHandledAt(){return handledAt;}
    public void setHandledAt(LocalDate value){handledAt=value;}
    private Long handledById;
    public Long getHandledById(){return handledById;}
    public void setHandledById(Long value){handledById=value;}
    private String handledBy;
    public String getHandledBy(){return handledBy;}
    public void setHandledBy(String value){handledBy=value;}
    private String referenceNo;
    public String getReferenceNo(){return referenceNo;}
    public void setReferenceNo(String value){referenceNo=value;}
    private String noteText;
    public String getNoteText(){return noteText;}
    public void setNoteText(String value){noteText=value;}
    private String attachmentRef;
    public String getAttachmentRef(){return attachmentRef;}
    public void setAttachmentRef(String value){attachmentRef=value;}
    private Long settlementSnapshotId;
    public Long getSettlementSnapshotId(){return settlementSnapshotId;}
    public void setSettlementSnapshotId(Long value){settlementSnapshotId=value;}
    private String settlementSnapshotHash;
    public String getSettlementSnapshotHash(){return settlementSnapshotHash;}
    public void setSettlementSnapshotHash(String value){settlementSnapshotHash=value;}
    private String createdBy;
    public String getCreatedBy(){return createdBy;}
    public void setCreatedBy(String value){createdBy=value;}
    private Date createdAt;
    public Date getCreatedAt(){return createdAt;}
    public void setCreatedAt(Date value){createdAt=value;}
}
