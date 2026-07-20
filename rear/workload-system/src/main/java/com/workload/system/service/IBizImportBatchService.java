package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizImportBatch;

/**
 * 导入批次记录Service接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizImportBatchService 
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
     * 批量删除导入批次记录
     * 
     * @param ids 需要删除的导入批次记录主键集合
     * @return 结果
     */
    public int deleteBizImportBatchByIds(Long[] ids);

    /**
     * 删除导入批次记录信息
     * 
     * @param id 导入批次记录主键
     * @return 结果
     */
    public int deleteBizImportBatchById(Long id);
}
