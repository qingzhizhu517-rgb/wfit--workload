package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    public int insertBizWlCourseDesign(BizWlCourseDesign bizWlCourseDesign)
    {
        bizWlCourseDesign.setCreateTime(DateUtils.getNowDate());
        return bizWlCourseDesignMapper.insertBizWlCourseDesign(bizWlCourseDesign);
    }

    /**
     * 修改G4课程设计明细
     * 
     * @param bizWlCourseDesign G4课程设计明细
     * @return 结果
     */
    @Override
    public int updateBizWlCourseDesign(BizWlCourseDesign bizWlCourseDesign)
    {
        bizWlCourseDesign.setUpdateTime(DateUtils.getNowDate());
        return bizWlCourseDesignMapper.updateBizWlCourseDesign(bizWlCourseDesign);
    }

    /**
     * 批量删除G4课程设计明细
     * 
     * @param itemIds 需要删除的G4课程设计明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlCourseDesignByItemIds(Long[] itemIds)
    {
        return bizWlCourseDesignMapper.deleteBizWlCourseDesignByItemIds(itemIds);
    }

    /**
     * 删除G4课程设计明细信息
     * 
     * @param itemId G4课程设计明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlCourseDesignByItemId(Long itemId)
    {
        return bizWlCourseDesignMapper.deleteBizWlCourseDesignByItemId(itemId);
    }
}
