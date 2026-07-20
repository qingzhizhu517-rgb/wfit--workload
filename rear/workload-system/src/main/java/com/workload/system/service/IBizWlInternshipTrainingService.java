package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizWlInternshipTraining;

/**
 * G3教学实习实训明细Service接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizWlInternshipTrainingService 
{
    /**
     * 查询G3教学实习实训明细
     * 
     * @param itemId G3教学实习实训明细主键
     * @return G3教学实习实训明细
     */
    public BizWlInternshipTraining selectBizWlInternshipTrainingByItemId(Long itemId);

    /**
     * 查询G3教学实习实训明细列表
     * 
     * @param bizWlInternshipTraining G3教学实习实训明细
     * @return G3教学实习实训明细集合
     */
    public List<BizWlInternshipTraining> selectBizWlInternshipTrainingList(BizWlInternshipTraining bizWlInternshipTraining);

    /**
     * 新增G3教学实习实训明细
     * 
     * @param bizWlInternshipTraining G3教学实习实训明细
     * @return 结果
     */
    public int insertBizWlInternshipTraining(BizWlInternshipTraining bizWlInternshipTraining);

    /**
     * 修改G3教学实习实训明细
     * 
     * @param bizWlInternshipTraining G3教学实习实训明细
     * @return 结果
     */
    public int updateBizWlInternshipTraining(BizWlInternshipTraining bizWlInternshipTraining);

    /**
     * 批量删除G3教学实习实训明细
     * 
     * @param itemIds 需要删除的G3教学实习实训明细主键集合
     * @return 结果
     */
    public int deleteBizWlInternshipTrainingByItemIds(Long[] itemIds);

    /**
     * 删除G3教学实习实训明细信息
     * 
     * @param itemId G3教学实习实训明细主键
     * @return 结果
     */
    public int deleteBizWlInternshipTrainingByItemId(Long itemId);
}
