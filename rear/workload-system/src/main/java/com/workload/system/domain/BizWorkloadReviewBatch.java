package com.workload.system.domain;

import java.util.Date;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.workload.common.core.domain.BaseEntity;

/** Server-owned review evidence row; controllers bind only explicit command contracts. */
public class BizWorkloadReviewBatch {
    private Long id;
    public Long getId(){return id;}
    public void setId(Long value){id=value;}
    private Long summaryId;
    public Long getSummaryId(){return summaryId;}
    public void setSummaryId(Long value){summaryId=value;}
    private String reviewBatchNo;
    public String getReviewBatchNo(){return reviewBatchNo;}
    public void setReviewBatchNo(String value){reviewBatchNo=value;}
    private Long summaryDataVersion;
    public Long getSummaryDataVersion(){return summaryDataVersion;}
    public void setSummaryDataVersion(Long value){summaryDataVersion=value;}
    private Long userId;
    public Long getUserId(){return userId;}
    public void setUserId(Long value){userId=value;}
    private String semester;
    public String getSemester(){return semester;}
    public void setSemester(String value){semester=value;}
    private Long settlementSnapshotId;
    public Long getSettlementSnapshotId(){return settlementSnapshotId;}
    public void setSettlementSnapshotId(Long value){settlementSnapshotId=value;}
    private String settlementSnapshotHash;
    public String getSettlementSnapshotHash(){return settlementSnapshotHash;}
    public void setSettlementSnapshotHash(String value){settlementSnapshotHash=value;}
    private Integer formatVersion;
    public Integer getFormatVersion(){return formatVersion;}
    public void setFormatVersion(Integer value){formatVersion=value;}
    private String snapshotJson;
    public String getSnapshotJson(){return snapshotJson;}
    public void setSnapshotJson(String value){snapshotJson=value;}
    private String dataHash;
    public String getDataHash(){return dataHash;}
    public void setDataHash(String value){dataHash=value;}
    private String createdBy;
    public String getCreatedBy(){return createdBy;}
    public void setCreatedBy(String value){createdBy=value;}
    private Date createdAt;
    public Date getCreatedAt(){return createdAt;}
    public void setCreatedAt(Date value){createdAt=value;}
}
