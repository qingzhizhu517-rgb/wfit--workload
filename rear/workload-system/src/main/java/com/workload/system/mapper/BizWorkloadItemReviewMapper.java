package com.workload.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.workload.system.domain.BizWorkloadItemReview;

public interface BizWorkloadItemReviewMapper {
    int insert(BizWorkloadItemReview row);
    BizWorkloadItemReview selectById(@Param("id") Long id);
    List<BizWorkloadItemReview> selectForBatch(@Param("summaryId") Long summaryId,@Param("batchNo") String batchNo,@Param("current") boolean current);
    List<BizWorkloadItemReview> selectPage(@Param("summaryId") Long summaryId,@Param("batchNo") String batchNo,@Param("userId") Long userId,@Param("reviewType") String reviewType,@Param("result") String result,@Param("offset") long offset,@Param("size") int size);
    long countPage(@Param("summaryId") Long summaryId,@Param("batchNo") String batchNo,@Param("userId") Long userId,@Param("reviewType") String reviewType,@Param("result") String result);
    int reviewIfPending(@Param("review") BizWorkloadItemReview review,@Param("expectedVersion") long expectedVersion,@Param("dataVersion") long dataVersion);
}
