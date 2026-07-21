package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.system.calc.WorkloadCalcService;
import com.workload.system.mapper.BizWlCourseDesignMapper;
import com.workload.system.domain.BizWlCourseDesign;
import com.workload.system.service.IBizWlCourseDesignService;

/**
 * G4课程设计明细Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWlCourseDesignServiceImpl implements IBizWlCourseDesignService 
{
    @Autowired
    private BizWlCourseDesignMapper bizWlCourseDesignMapper;

    @Autowired
    private WorkloadCalcService workloadCalcService;

    /**
     * 查询G4课程设计明细
     * 
     * @param itemId G4课程设计明细主键
     * @return G4课程设计明细
     */
    @Override
    public BizWlCourseDesign selectBizWlCourseDesignByItemId(Long itemId)
    {
        return bizWlCourseDesignMapper.selectBizWlCourseDesignByItemId(itemId);
    }

    /**
     * 查询G4课程设计明细列表
     * 
     * @param bizWlCourseDesign G4课程设计明细
     * @return G4课程设计明细
     */
    @Override
    public List<BizWlCourseDesign> selectBizWlCourseDesignList(BizWlCourseDesign bizWlCourseDesign)
    {
        return bizWlCourseDesignMapper.selectBizWlCourseDesignList(bizWlCourseDesign);
    }

    /**
     * 新增G4课程设计明细
     * 
     * @param bizWlCourseDesign G4课程设计明细
     * @return 结果
     */
    @Override
    @Transactional
    public int insertBizWlCourseDesign(BizWlCourseDesign bizWlCourseDesign)
    {
        workloadCalcService.assertEditable(bizWlCourseDesign.getItemId());
        bizWlCourseDesign.setCreateTime(DateUtils.getNowDate());
        int rows = bizWlCourseDesignMapper.insertBizWlCourseDesign(bizWlCourseDesign);
        workloadCalcService.recalcItem(bizWlCourseDesign.getItemId());
        return rows;
    }

    /**
     * 修改G4课程设计明细
     * 
     * @param bizWlCourseDesign G4课程设计明细
     * @return 结果
     */
    @Override
    @Transactional
    public int updateBizWlCourseDesign(BizWlCourseDesign bizWlCourseDesign)
    {
        workloadCalcService.assertEditable(bizWlCourseDesign.getItemId());
        bizWlCourseDesign.setUpdateTime(DateUtils.getNowDate());
        int rows = bizWlCourseDesignMapper.updateBizWlCourseDesign(bizWlCourseDesign);
        workloadCalcService.recalcItem(bizWlCourseDesign.getItemId());
        return rows;
    }

    /**
     * 批量删除G4课程设计明细
     * 
     * @param itemIds 需要删除的G4课程设计明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizWlCourseDesignByItemIds(Long[] itemIds)
    {
        for (Long itemId : itemIds)
        {
            workloadCalcService.assertEditable(itemId);
        }
        int rows = bizWlCourseDesignMapper.deleteBizWlCourseDesignByItemIds(itemIds);
        for (Long itemId : itemIds)
        {
            workloadCalcService.onDetailDeleted(itemId);
        }
        return rows;
    }

    /**
     * 删除G4课程设计明细信息
     * 
     * @param itemId G4课程设计明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizWlCourseDesignByItemId(Long itemId)
    {
        workloadCalcService.assertEditable(itemId);
        int rows = bizWlCourseDesignMapper.deleteBizWlCourseDesignByItemId(itemId);
        workloadCalcService.onDetailDeleted(itemId);
        return rows;
    }
}
