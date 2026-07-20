package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizWlManagement;

/**
 * G11管理服务明细Service接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizWlManagementService 
{
    /**
     * 查询G11管理服务明细
     * 
     * @param itemId G11管理服务明细主键
     * @return G11管理服务明细
     */
    public BizWlManagement selectBizWlManagementByItemId(Long itemId);

    /**
     * 查询G11管理服务明细列表
     * 
     * @param bizWlManagement G11管理服务明细
     * @return G11管理服务明细集合
     */
    public List<BizWlManagement> selectBizWlManagementList(BizWlManagement bizWlManagement);

    /**
     * 新增G11管理服务明细
     * 
     * @param bizWlManagement G11管理服务明细
     * @return 结果
     */
    public int insertBizWlManagement(BizWlManagement bizWlManagement);

    /**
     * 修改G11管理服务明细
     * 
     * @param bizWlManagement G11管理服务明细
     * @return 结果
     */
    public int updateBizWlManagement(BizWlManagement bizWlManagement);

    /**
     * 批量删除G11管理服务明细
     * 
     * @param itemIds 需要删除的G11管理服务明细主键集合
     * @return 结果
     */
    public int deleteBizWlManagementByItemIds(Long[] itemIds);

    /**
     * 删除G11管理服务明细信息
     * 
     * @param itemId G11管理服务明细主键
     * @return 结果
     */
    public int deleteBizWlManagementByItemId(Long itemId);
}
