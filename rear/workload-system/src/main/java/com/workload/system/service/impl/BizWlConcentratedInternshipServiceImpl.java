package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.system.calc.WorkloadCalcService;
import com.workload.system.mapper.BizWlConcentratedInternshipMapper;
import com.workload.system.domain.BizWlConcentratedInternship;
import com.workload.system.service.IBizWlConcentratedInternshipService;

/**
 * G6集中实习明细Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWlConcentratedInternshipServiceImpl implements IBizWlConcentratedInternshipService 
{
    @Autowired
    private BizWlConcentratedInternshipMapper bizWlConcentratedInternshipMapper;

    @Autowired
    private WorkloadCalcService workloadCalcService;

    /**
     * 查询G6集中实习明细
     * 
     * @param itemId G6集中实习明细主键
     * @return G6集中实习明细
     */
    @Override
    public BizWlConcentratedInternship selectBizWlConcentratedInternshipByItemId(Long itemId)
    {
        return bizWlConcentratedInternshipMapper.selectBizWlConcentratedInternshipByItemId(itemId);
    }

    /**
     * 查询G6集中实习明细列表
     * 
     * @param bizWlConcentratedInternship G6集中实习明细
     * @return G6集中实习明细
     */
    @Override
    public List<BizWlConcentratedInternship> selectBizWlConcentratedInternshipList(BizWlConcentratedInternship bizWlConcentratedInternship)
    {
        return bizWlConcentratedInternshipMapper.selectBizWlConcentratedInternshipList(bizWlConcentratedInternship);
    }

    /**
     * 新增G6集中实习明细
     * 
     * @param bizWlConcentratedInternship G6集中实习明细
     * @return 结果
     */
    @Override
    @Transactional
    public int insertBizWlConcentratedInternship(BizWlConcentratedInternship bizWlConcentratedInternship)
    {
        workloadCalcService.assertEditable(bizWlConcentratedInternship.getItemId());
        bizWlConcentratedInternship.setCreateTime(DateUtils.getNowDate());
        int rows = bizWlConcentratedInternshipMapper.insertBizWlConcentratedInternship(bizWlConcentratedInternship);
        workloadCalcService.recalcItem(bizWlConcentratedInternship.getItemId());
        return rows;
    }

    /**
     * 修改G6集中实习明细
     * 
     * @param bizWlConcentratedInternship G6集中实习明细
     * @return 结果
     */
    @Override
    @Transactional
    public int updateBizWlConcentratedInternship(BizWlConcentratedInternship bizWlConcentratedInternship)
    {
        workloadCalcService.assertEditable(bizWlConcentratedInternship.getItemId());
        bizWlConcentratedInternship.setUpdateTime(DateUtils.getNowDate());
        int rows = bizWlConcentratedInternshipMapper.updateBizWlConcentratedInternship(bizWlConcentratedInternship);
        workloadCalcService.recalcItem(bizWlConcentratedInternship.getItemId());
        return rows;
    }

    /**
     * 批量删除G6集中实习明细
     * 
     * @param itemIds 需要删除的G6集中实习明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizWlConcentratedInternshipByItemIds(Long[] itemIds)
    {
        for (Long itemId : itemIds)
        {
            workloadCalcService.assertEditable(itemId);
        }
        int rows = bizWlConcentratedInternshipMapper.deleteBizWlConcentratedInternshipByItemIds(itemIds);
        for (Long itemId : itemIds)
        {
            workloadCalcService.onDetailDeleted(itemId);
        }
        return rows;
    }

    /**
     * 删除G6集中实习明细信息
     * 
     * @param itemId G6集中实习明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizWlConcentratedInternshipByItemId(Long itemId)
    {
        workloadCalcService.assertEditable(itemId);
        int rows = bizWlConcentratedInternshipMapper.deleteBizWlConcentratedInternshipByItemId(itemId);
        workloadCalcService.onDetailDeleted(itemId);
        return rows;
    }
}
