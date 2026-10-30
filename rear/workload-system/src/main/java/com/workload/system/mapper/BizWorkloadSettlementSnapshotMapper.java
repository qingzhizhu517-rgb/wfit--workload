package com.workload.system.mapper;
import com.workload.system.domain.BizWorkloadSettlementSnapshot;
public interface BizWorkloadSettlementSnapshotMapper {
 int insert(BizWorkloadSettlementSnapshot snapshot);
 BizWorkloadSettlementSnapshot selectById(Long id);
 BizWorkloadSettlementSnapshot selectByIdCurrent(Long id);
 Long selectMaxVersion(Long summaryId);
 Long selectMaxVersionCurrent(Long summaryId);
}
