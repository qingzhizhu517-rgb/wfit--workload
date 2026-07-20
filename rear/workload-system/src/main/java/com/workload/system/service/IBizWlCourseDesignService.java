package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizWlCourseDesign;

/**
 * G4课程设计明细Service接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizWlCourseDesignService 
{
    /**
     * 查询G4课程设计明细
     * 
     * @param itemId G4课程设计明细主键
     * @return G4课程设计明细
     */
    public BizWlCourseDesign selectBizWlCourseDesignByItemId(Long itemId);

    /**
     * 查询G4课程设计明细列表
     * 
     * @param bizWlCourseDesign G4课程设计明细
     * @return G4课程设计明细集合
     */
    public List<BizWlCourseDesign> selectBizWlCourseDesignList(BizWlCourseDesign bizWlCourseDesign);

    /**
     * 新增G4课程设计明细
     * 
     * @param bizWlCourseDesign G4课程设计明细
     * @return 结果
     */
    public int insertBizWlCourseDesign(BizWlCourseDesign bizWlCourseDesign);

    /**
     * 修改G4课程设计明细
     * 
     * @param bizWlCourseDesign G4课程设计明细
     * @return 结果
     */
    public int updateBizWlCourseDesign(BizWlCourseDesign bizWlCourseDesign);

    /**
     * 批量删除G4课程设计明细
     * 
     * @param itemIds 需要删除的G4课程设计明细主键集合
     * @return 结果
     */
    public int deleteBizWlCourseDesignByItemIds(Long[] itemIds);

    /**
     * 删除G4课程设计明细信息
     * 
     * @param itemId G4课程设计明细主键
     * @return 结果
     */
    public int deleteBizWlCourseDesignByItemId(Long itemId);
}
