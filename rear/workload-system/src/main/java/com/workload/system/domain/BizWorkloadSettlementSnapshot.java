package com.workload.system.domain;
import java.util.Date;
/** Append-only settlement evidence; no mutable CRUD endpoint. */
public class BizWorkloadSettlementSnapshot {
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
 private Long settlementVersion;
 public Long getSettlementVersion(){return settlementVersion;}
 public void setSettlementVersion(Long value){settlementVersion=value;}
 private Integer formatVersion;
 public Integer getFormatVersion(){return formatVersion;}
 public void setFormatVersion(Integer value){formatVersion=value;}
 private String payloadJson;
 public String getPayloadJson(){return payloadJson;}
 public void setPayloadJson(String value){payloadJson=value;}
 private String hash;
 public String getHash(){return hash;}
 public void setHash(String value){hash=value;}
 private String createdBy;
 public String getCreatedBy(){return createdBy;}
 public void setCreatedBy(String value){createdBy=value;}
 private Date createdAt;
 public Date getCreatedAt(){return createdAt;}
 public void setCreatedAt(Date value){createdAt=value;}
}
