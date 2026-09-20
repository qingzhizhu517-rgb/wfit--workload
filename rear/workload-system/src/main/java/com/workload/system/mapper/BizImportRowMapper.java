package com.workload.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.workload.system.domain.BizImportRow;

/**
 * 导入逐行暂存Mapper接口
 *
 * @author wflg
 * @date 2026-09-12
 */
public interface BizImportRowMapper
{
    /**
     * 查询暂存行
     *
     * @param id 主键
     * @return 暂存行
     */
    public BizImportRow selectBizImportRowById(Long id);

    /**
     * 按批次查询暂存行（按 row_no 升序，供确认阶段顺序回放）
     *
     * @param batchId 批次ID
     * @return 暂存行集合
     */
    public List<BizImportRow> selectBizImportRowByBatchId(Long batchId);

    /**
     * 按批次和状态查询暂存行
     *
     * @param batchId 批次ID
     * @param status  行状态
     * @return 暂存行集合
     */
    public List<BizImportRow> selectByBatchIdAndStatus(@Param("batchId") Long batchId,
            @Param("status") String status);

    /**
     * 统计批次中指定状态的行数
     *
     * @param batchId 批次ID
     * @param status  行状态
     * @return 行数
     */
    public int countByBatchIdAndStatus(@Param("batchId") Long batchId, @Param("status") String status);

    /**
     * 新增暂存行
     *
     * @param bizImportRow 暂存行
     * @return 结果
     */
    public int insertBizImportRow(BizImportRow bizImportRow);

    /**
     * 修改暂存行
     *
     * @param bizImportRow 暂存行
     * @return 结果
     */
    public int updateBizImportRow(BizImportRow bizImportRow);

    /**
     * 按批次删除暂存行
     *
     * @param batchId 批次ID
     * @return 结果
     */
    public int deleteBizImportRowByBatchId(Long batchId);
}
