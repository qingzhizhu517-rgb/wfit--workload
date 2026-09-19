package com.workload.system.mapper;

import java.util.List;

import com.workload.system.domain.BizWorkloadCalcSnapshot;

/**
 * 不可变工作量计算快照 Mapper 接口。
 *
 * <p>快照 append-only：仅提供 INSERT 与只读查询，不暴露 UPDATE/DELETE。</p>
 */
public interface BizWorkloadCalcSnapshotMapper
{
    /**
     * 插入一份新的计算快照。
     *
     * @param snapshot 快照
     * @return 影响行数
     */
    int insertBizWorkloadCalcSnapshot(BizWorkloadCalcSnapshot snapshot);

    /**
     * 查询指定明细已有的最大计算版本；无快照返回 null。
     *
     * @param itemId 明细主键
     * @return 最大版本或 null
     */
    Long selectMaxVersionByItemId(Long itemId);

    /**
     * 查询指定明细的最新快照（版本最大的一条）；无快照返回 null。
     *
     * @param itemId 明细主键
     * @return 最新快照
     */
    BizWorkloadCalcSnapshot selectLatestByItemId(Long itemId);

    /**
     * 查询指定明细的全部快照，按版本升序。
     *
     * @param itemId 明细主键
     * @return 快照列表
     */
    List<BizWorkloadCalcSnapshot> selectByItemId(Long itemId);
}
