package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.system.calc.WorkloadCalcService;
import com.workload.system.mapper.BizWlThesisMapper;
import com.workload.system.domain.BizWlThesis;
import com.workload.system.service.IBizWlThesisService;

/**
 * G5毕业论文明细Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWlThesisServiceImpl implements IBizWlThesisService 
{
    @Autowired
    private BizWlThesisMapper bizWlThesisMapper;

    @Autowired
    private WorkloadCalcService workloadCalcService;

    /**
     * 查询G5毕业论文明细
     * 
     * @param itemId G5毕业论文明细主键
     * @return G5毕业论文明细
     */
    @Override
    public BizWlThesis selectBizWlThesisByItemId(Long itemId)
    {
        return bizWlThesisMapper.selectBizWlThesisByItemId(itemId);
    }

    /**
     * 查询G5毕业论文明细列表
     * 
     * @param bizWlThesis G5毕业论文明细
     * @return G5毕业论文明细
     */
    @Override
    public List<BizWlThesis> selectBizWlThesisList(BizWlThesis bizWlThesis)
    {
        return bizWlThesisMapper.selectBizWlThesisList(bizWlThesis);
    }

    /**
     * 新增G5毕业论文明细
     * 
     * @param bizWlThesis G5毕业论文明细
     * @return 结果
     */
    @Override
    @Transactional
    public int insertBizWlThesis(BizWlThesis bizWlThesis)
    {
        workloadCalcService.assertEditable(bizWlThesis.getItemId());
        bizWlThesis.setCreateTime(DateUtils.getNowDate());
        int rows = bizWlThesisMapper.insertBizWlThesis(bizWlThesis);
        workloadCalcService.recalcItem(bizWlThesis.getItemId());
        return rows;
    }

    /**
     * 修改G5毕业论文明细
     * 
     * @param bizWlThesis G5毕业论文明细
     * @return 结果
     */
    @Override
    @Transactional
    public int updateBizWlThesis(BizWlThesis bizWlThesis)
    {
        workloadCalcService.assertEditable(bizWlThesis.getItemId());
        bizWlThesis.setUpdateTime(DateUtils.getNowDate());
        int rows = bizWlThesisMapper.updateBizWlThesis(bizWlThesis);
        workloadCalcService.recalcItem(bizWlThesis.getItemId());
        return rows;
    }

    /**
     * 批量删除G5毕业论文明细
     * 
     * @param itemIds 需要删除的G5毕业论文明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizWlThesisByItemIds(Long[] itemIds)
    {
        for (Long itemId : itemIds)
        {
            workloadCalcService.assertEditable(itemId);
        }
        int rows = bizWlThesisMapper.deleteBizWlThesisByItemIds(itemIds);
        for (Long itemId : itemIds)
        {
            workloadCalcService.onDetailDeleted(itemId);
        }
        return rows;
    }

    /**
     * 删除G5毕业论文明细信息
     * 
     * @param itemId G5毕业论文明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizWlThesisByItemId(Long itemId)
    {
        workloadCalcService.assertEditable(itemId);
        int rows = bizWlThesisMapper.deleteBizWlThesisByItemId(itemId);
        workloadCalcService.onDetailDeleted(itemId);
        return rows;
    }
}
