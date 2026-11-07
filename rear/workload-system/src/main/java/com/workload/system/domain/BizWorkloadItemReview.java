package com.workload.system.domain;

import java.util.Date;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.workload.common.core.domain.BaseEntity;

/** Server-owned review evidence row; controllers bind only explicit command contracts. */
public class BizWorkloadItemReview extends BaseEntity {
    private Long id;
    public Long getId(){return id;}
    public void setId(Long value){id=value;}
    private Long summaryId;
    public Long getSummaryId(){return summaryId;}
    public void setSummaryId(Long value){summaryId=value;}
    private Long itemId;
    public Long getItemId(){return itemId;}
    public void setItemId(Long value){itemId=value;}
    private String reviewBatchNo;
    public String getReviewBatchNo(){return reviewBatchNo;}
    public void setReviewBatchNo(String value){reviewBatchNo=value;}
    private String reviewType;
    public String getReviewType(){return reviewType;}
    public void setReviewType(String value){reviewType=value;}
    private Integer formatVersion;
    public Integer getFormatVersion(){return formatVersion;}
    public void setFormatVersion(Integer value){formatVersion=value;}
    private String snapshotJson;
    public String getSnapshotJson(){return snapshotJson;}
    public void setSnapshotJson(String value){snapshotJson=value;}
    private String dataHash;
    public String getDataHash(){return dataHash;}
    public void setDataHash(String value){dataHash=value;}
    private String result;
    public String getResult(){return result;}
    public void setResult(String value){result=value;}
    private String reason;
    public String getReason(){return reason;}
    public void setReason(String value){reason=value;}
    private Long reviewerId;
    public Long getReviewerId(){return reviewerId;}
    public void setReviewerId(Long value){reviewerId=value;}
    private String reviewerName;
    public String getReviewerName(){return reviewerName;}
    public void setReviewerName(String value){reviewerName=value;}
    private Date reviewedAt;
    public Date getReviewedAt(){return reviewedAt;}
    public void setReviewedAt(Date value){reviewedAt=value;}
    private Long version;
    public Long getVersion(){return version;}
    public void setVersion(Long value){version=value;}
}
