package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    public int insertBizWlConcentratedInternship(BizWlConcentratedInternship bizWlConcentratedInternship)
    {
        bizWlConcentratedInternship.setCreateTime(DateUtils.getNowDate());
        return bizWlConcentratedInternshipMapper.insertBizWlConcentratedInternship(bizWlConcentratedInternship);
    }

    /**
     * 修改G6集中实习明细
     * 
     * @param bizWlConcentratedInternship G6集中实习明细
     * @return 结果
     */
    @Override
    public int updateBizWlConcentratedInternship(BizWlConcentratedInternship bizWlConcentratedInternship)
    {
        bizWlConcentratedInternship.setUpdateTime(DateUtils.getNowDate());
        return bizWlConcentratedInternshipMapper.updateBizWlConcentratedInternship(bizWlConcentratedInternship);
    }

    /**
     * 批量删除G6集中实习明细
     * 
     * @param itemIds 需要删除的G6集中实习明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlConcentratedInternshipByItemIds(Long[] itemIds)
    {
        return bizWlConcentratedInternshipMapper.deleteBizWlConcentratedInternshipByItemIds(itemIds);
    }

    /**
     * 删除G6集中实习明细信息
     * 
     * @param itemId G6集中实习明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlConcentratedInternshipByItemId(Long itemId)
    {
        return bizWlConcentratedInternshipMapper.deleteBizWlConcentratedInternshipByItemId(itemId);
    }
}
