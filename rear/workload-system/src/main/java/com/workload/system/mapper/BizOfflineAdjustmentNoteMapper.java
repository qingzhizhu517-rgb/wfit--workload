package com.workload.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.workload.system.domain.BizOfflineAdjustmentNote;

public interface BizOfflineAdjustmentNoteMapper {
    int insert(BizOfflineAdjustmentNote row);
    BizOfflineAdjustmentNote selectById(@Param("id") Long id);
    BizOfflineAdjustmentNote selectByRequest(@Param("summaryId") Long summaryId,@Param("key") String key);
    List<BizOfflineAdjustmentNote> selectPage(@Param("summaryId") Long summaryId,@Param("offset") long offset,@Param("size") int size);
    long countPage(@Param("summaryId") Long summaryId);
}
