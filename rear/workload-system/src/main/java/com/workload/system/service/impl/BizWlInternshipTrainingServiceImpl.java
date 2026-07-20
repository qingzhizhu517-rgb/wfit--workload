package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizWlInternshipTrainingMapper;
import com.workload.system.domain.BizWlInternshipTraining;
import com.workload.system.service.IBizWlInternshipTrainingService;

/**
 * G3教学实习实训明细Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWlInternshipTrainingServiceImpl implements IBizWlInternshipTrainingService 
{
    @Autowired
    private BizWlInternshipTrainingMapper bizWlInternshipTrainingMapper;

    /**
     * 查询G3教学实习实训明细
     * 
     * @param itemId G3教学实习实训明细主键
     * @return G3教学实习实训明细
     */
    @Override
    public BizWlInternshipTraining selectBizWlInternshipTrainingByItemId(Long itemId)
    {
        return bizWlInternshipTrainingMapper.selectBizWlInternshipTrainingByItemId(itemId);
    }

    /**
     * 查询G3教学实习实训明细列表
     * 
     * @param bizWlInternshipTraining G3教学实习实训明细
     * @return G3教学实习实训明细
     */
    @Override
    public List<BizWlInternshipTraining> selectBizWlInternshipTrainingList(BizWlInternshipTraining bizWlInternshipTraining)
    {
        return bizWlInternshipTrainingMapper.selectBizWlInternshipTrainingList(bizWlInternshipTraining);
    }

    /**
     * 新增G3教学实习实训明细
     * 
     * @param bizWlInternshipTraining G3教学实习实训明细
     * @return 结果
     */
    @Override
    public int insertBizWlInternshipTraining(BizWlInternshipTraining bizWlInternshipTraining)
    {
        bizWlInternshipTraining.setCreateTime(DateUtils.getNowDate());
        return bizWlInternshipTrainingMapper.insertBizWlInternshipTraining(bizWlInternshipTraining);
    }

    /**
     * 修改G3教学实习实训明细
     * 
     * @param bizWlInternshipTraining G3教学实习实训明细
     * @return 结果
     */
    @Override
    public int updateBizWlInternshipTraining(BizWlInternshipTraining bizWlInternshipTraining)
    {
        bizWlInternshipTraining.setUpdateTime(DateUtils.getNowDate());
        return bizWlInternshipTrainingMapper.updateBizWlInternshipTraining(bizWlInternshipTraining);
    }

    /**
     * 批量删除G3教学实习实训明细
     * 
     * @param itemIds 需要删除的G3教学实习实训明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlInternshipTrainingByItemIds(Long[] itemIds)
    {
        return bizWlInternshipTrainingMapper.deleteBizWlInternshipTrainingByItemIds(itemIds);
    }

    /**
     * 删除G3教学实习实训明细信息
     * 
     * @param itemId G3教学实习实训明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlInternshipTrainingByItemId(Long itemId)
    {
        return bizWlInternshipTrainingMapper.deleteBizWlInternshipTrainingByItemId(itemId);
    }
}
