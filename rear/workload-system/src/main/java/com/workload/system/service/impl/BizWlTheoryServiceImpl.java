package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.system.calc.WorkloadCalcService;
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

    @Autowired
    private WorkloadCalcService workloadCalcService;

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
    @Transactional
    public int insertBizWlTheory(BizWlTheory bizWlTheory)
    {
        workloadCalcService.assertEditable(bizWlTheory.getItemId());
        bizWlTheory.setCreateTime(DateUtils.getNowDate());
        int rows = bizWlTheoryMapper.insertBizWlTheory(bizWlTheory);
        workloadCalcService.recalcItem(bizWlTheory.getItemId());
        return rows;
    }

    /**
     * 修改G1理论课明细
     * 
     * @param bizWlTheory G1理论课明细
     * @return 结果
     */
    @Override
    @Transactional
    public int updateBizWlTheory(BizWlTheory bizWlTheory)
    {
        workloadCalcService.assertEditable(bizWlTheory.getItemId());
        bizWlTheory.setUpdateTime(DateUtils.getNowDate());
        int rows = bizWlTheoryMapper.updateBizWlTheory(bizWlTheory);
        workloadCalcService.recalcItem(bizWlTheory.getItemId());
        return rows;
    }

    /**
     * 批量删除G1理论课明细
     * 
     * @param itemIds 需要删除的G1理论课明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizWlTheoryByItemIds(Long[] itemIds)
    {
        for (Long itemId : itemIds)
        {
            workloadCalcService.assertEditable(itemId);
        }
        int rows = bizWlTheoryMapper.deleteBizWlTheoryByItemIds(itemIds);
        for (Long itemId : itemIds)
        {
            workloadCalcService.onDetailDeleted(itemId);
        }
        return rows;
    }

    /**
     * 删除G1理论课明细信息
     * 
     * @param itemId G1理论课明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizWlTheoryByItemId(Long itemId)
    {
        workloadCalcService.assertEditable(itemId);
        int rows = bizWlTheoryMapper.deleteBizWlTheoryByItemId(itemId);
        workloadCalcService.onDetailDeleted(itemId);
        return rows;
    }
}
