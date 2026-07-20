package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizWlTheoryMapper;
import com.workload.system.domain.BizWlTheory;
import com.workload.system.service.IBizWlTheoryService;

/**
 * G1理论课明细Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWlTheoryServiceImpl implements IBizWlTheoryService 
{
    @Autowired
    private BizWlTheoryMapper bizWlTheoryMapper;

    /**
     * 查询G1理论课明细
     * 
     * @param itemId G1理论课明细主键
     * @return G1理论课明细
     */
    @Override
    public BizWlTheory selectBizWlTheoryByItemId(Long itemId)
    {
        return bizWlTheoryMapper.selectBizWlTheoryByItemId(itemId);
    }

    /**
     * 查询G1理论课明细列表
     * 
     * @param bizWlTheory G1理论课明细
     * @return G1理论课明细
     */
    @Override
    public List<BizWlTheory> selectBizWlTheoryList(BizWlTheory bizWlTheory)
    {
        return bizWlTheoryMapper.selectBizWlTheoryList(bizWlTheory);
    }

    /**
     * 新增G1理论课明细
     * 
     * @param bizWlTheory G1理论课明细
     * @return 结果
     */
    @Override
    public int insertBizWlTheory(BizWlTheory bizWlTheory)
    {
        bizWlTheory.setCreateTime(DateUtils.getNowDate());
        return bizWlTheoryMapper.insertBizWlTheory(bizWlTheory);
    }

    /**
     * 修改G1理论课明细
     * 
     * @param bizWlTheory G1理论课明细
     * @return 结果
     */
    @Override
    public int updateBizWlTheory(BizWlTheory bizWlTheory)
    {
        bizWlTheory.setUpdateTime(DateUtils.getNowDate());
        return bizWlTheoryMapper.updateBizWlTheory(bizWlTheory);
    }

    /**
     * 批量删除G1理论课明细
     * 
     * @param itemIds 需要删除的G1理论课明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlTheoryByItemIds(Long[] itemIds)
    {
        return bizWlTheoryMapper.deleteBizWlTheoryByItemIds(itemIds);
    }

    /**
     * 删除G1理论课明细信息
     * 
     * @param itemId G1理论课明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlTheoryByItemId(Long itemId)
    {
        return bizWlTheoryMapper.deleteBizWlTheoryByItemId(itemId);
    }
}
