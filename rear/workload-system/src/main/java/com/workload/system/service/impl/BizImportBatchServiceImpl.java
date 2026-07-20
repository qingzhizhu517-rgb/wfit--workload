package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizImportBatchMapper;
import com.workload.system.domain.BizImportBatch;
import com.workload.system.service.IBizImportBatchService;

/**
 * 导入批次记录Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizImportBatchServiceImpl implements IBizImportBatchService 
{
    @Autowired
    private BizImportBatchMapper bizImportBatchMapper;

    /**
     * 查询导入批次记录
     * 
     * @param id 导入批次记录主键
     * @return 导入批次记录
     */
    @Override
    public BizImportBatch selectBizImportBatchById(Long id)
    {
        return bizImportBatchMapper.selectBizImportBatchById(id);
    }

    /**
     * 查询导入批次记录列表
     * 
     * @param bizImportBatch 导入批次记录
     * @return 导入批次记录
     */
    @Override
    public List<BizImportBatch> selectBizImportBatchList(BizImportBatch bizImportBatch)
    {
        return bizImportBatchMapper.selectBizImportBatchList(bizImportBatch);
    }

    /**
     * 新增导入批次记录
     * 
     * @param bizImportBatch 导入批次记录
     * @return 结果
     */
    @Override
    public int insertBizImportBatch(BizImportBatch bizImportBatch)
    {
        bizImportBatch.setCreateTime(DateUtils.getNowDate());
        return bizImportBatchMapper.insertBizImportBatch(bizImportBatch);
    }

    /**
     * 修改导入批次记录
     * 
     * @param bizImportBatch 导入批次记录
     * @return 结果
     */
    @Override
    public int updateBizImportBatch(BizImportBatch bizImportBatch)
    {
        bizImportBatch.setUpdateTime(DateUtils.getNowDate());
        return bizImportBatchMapper.updateBizImportBatch(bizImportBatch);
    }

    /**
     * 批量删除导入批次记录
     * 
     * @param ids 需要删除的导入批次记录主键
     * @return 结果
     */
    @Override
    public int deleteBizImportBatchByIds(Long[] ids)
    {
        return bizImportBatchMapper.deleteBizImportBatchByIds(ids);
    }

    /**
     * 删除导入批次记录信息
     * 
     * @param id 导入批次记录主键
     * @return 结果
     */
    @Override
    public int deleteBizImportBatchById(Long id)
    {
        return bizImportBatchMapper.deleteBizImportBatchById(id);
    }
}
