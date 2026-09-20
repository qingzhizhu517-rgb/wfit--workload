package com.workload.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.workload.system.domain.BizImportBatch;

/**
 * 导入批次记录Mapper接口
 *
 * @author wflg
 * @date 2026-07-20
 */
public interface BizImportBatchMapper
{
    /**
     * 查询导入批次记录
     * 
     * @param id 导入批次记录主键
     * @return 导入批次记录
     */
    public BizImportBatch selectBizImportBatchById(Long id);

    /**
     * 查询导入批次记录列表
     * 
     * @param bizImportBatch 导入批次记录
     * @return 导入批次记录集合
     */
    public List<BizImportBatch> selectBizImportBatchList(BizImportBatch bizImportBatch);

    /**
     * 新增导入批次记录
     * 
     * @param bizImportBatch 导入批次记录
     * @return 结果
     */
    public int insertBizImportBatch(BizImportBatch bizImportBatch);

    /**
     * 修改导入批次记录
     * 
     * @param bizImportBatch 导入批次记录
     * @return 结果
     */
    public int updateBizImportBatch(BizImportBatch bizImportBatch);

    /**
     * 删除导入批次记录
     * 
     * @param id 导入批次记录主键
     * @return 结果
     */
    public int deleteBizImportBatchById(Long id);

    /**
     * 批量删除导入批次记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizImportBatchByIds(Long[] ids);

    /**
     * 条件更新：待确认(expectedStatus) → 已导入(targetStatus)，同时落确认时间。
     * 影响行数≠1 视为并发冲突或批次不处于期望状态。
     *
     * @param id             批次主键
     * @param expectedStatus 期望的当前状态（乐观锁条件）
     * @param targetStatus   目标状态（应为 2 已导入）
     * @param operator       操作者
     * @return 受影响行数
     */
    public int markConfirmed(@Param("id") Long id, @Param("expectedStatus") int expectedStatus,
            @Param("targetStatus") int targetStatus, @Param("operator") String operator);

    /**
     * 条件更新批次状态（不落确认时间），用于 0→1 预检完成、1→4 确认失败等转移。
     * 影响行数≠1 视为并发冲突或批次不处于期望状态。
     *
     * @param id             批次主键
     * @param expectedStatus 期望的当前状态（乐观锁条件）
     * @param targetStatus   目标状态
     * @param operator       操作者
     * @return 受影响行数
     */
    public int markStatus(@Param("id") Long id, @Param("expectedStatus") int expectedStatus,
            @Param("targetStatus") int targetStatus, @Param("operator") String operator);

    /**
     * 统计同一类型 + 源文件哈希、且已进入终态「已导入(2)」的批次数。
     * 用于预检时的文件去重：同一文件已成功导入过则拒绝再次导入；
     * 而阻断/放弃(status 0/1/4)的历史批次不算数，允许修数据后重传（不再靠唯一索引在预检期锁死文件）。
     *
     * @param importType 导入类型
     * @param sourceHash 源文件 SHA-256
     * @return 已导入批次数
     */
    public int countConfirmedBySource(@Param("importType") String importType,
            @Param("sourceHash") String sourceHash);
}
