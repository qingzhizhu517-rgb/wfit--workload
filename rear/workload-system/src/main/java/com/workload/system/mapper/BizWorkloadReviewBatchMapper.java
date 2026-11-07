package com.workload.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.workload.system.domain.BizWorkloadReviewBatch;

public interface BizWorkloadReviewBatchMapper {
    int insert(BizWorkloadReviewBatch row);
    BizWorkloadReviewBatch selectById(@Param("id") Long id);
    BizWorkloadReviewBatch selectBatch(@Param("summaryId") Long summaryId,@Param("batchNo") String batchNo,@Param("current") boolean current);
    long countHistoryCurrent(@Param("summaryId") Long summaryId);
}
