package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizWlManagementMapper;
import com.workload.system.domain.BizWlManagement;
import com.workload.system.service.IBizWlManagementService;

/**
 * G11管理服务明细Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWlManagementServiceImpl implements IBizWlManagementService 
{
    @Autowired
    private BizWlManagementMapper bizWlManagementMapper;

    /**
     * 查询G11管理服务明细
     * 
     * @param itemId G11管理服务明细主键
     * @return G11管理服务明细
     */
    @Override
    public BizWlManagement selectBizWlManagementByItemId(Long itemId)
    {
        return bizWlManagementMapper.selectBizWlManagementByItemId(itemId);
    }

    /**
     * 查询G11管理服务明细列表
     * 
     * @param bizWlManagement G11管理服务明细
     * @return G11管理服务明细
     */
    @Override
    public List<BizWlManagement> selectBizWlManagementList(BizWlManagement bizWlManagement)
    {
        return bizWlManagementMapper.selectBizWlManagementList(bizWlManagement);
    }

    /**
     * 新增G11管理服务明细
     * 
     * @param bizWlManagement G11管理服务明细
     * @return 结果
     */
    @Override
    public int insertBizWlManagement(BizWlManagement bizWlManagement)
    {
        bizWlManagement.setCreateTime(DateUtils.getNowDate());
        return bizWlManagementMapper.insertBizWlManagement(bizWlManagement);
    }

    /**
     * 修改G11管理服务明细
     * 
     * @param bizWlManagement G11管理服务明细
     * @return 结果
     */
    @Override
    public int updateBizWlManagement(BizWlManagement bizWlManagement)
    {
        bizWlManagement.setUpdateTime(DateUtils.getNowDate());
        return bizWlManagementMapper.updateBizWlManagement(bizWlManagement);
    }

    /**
     * 批量删除G11管理服务明细
     * 
     * @param itemIds 需要删除的G11管理服务明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlManagementByItemIds(Long[] itemIds)
    {
        return bizWlManagementMapper.deleteBizWlManagementByItemIds(itemIds);
    }

    /**
     * 删除G11管理服务明细信息
     * 
     * @param itemId G11管理服务明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlManagementByItemId(Long itemId)
    {
        return bizWlManagementMapper.deleteBizWlManagementByItemId(itemId);
    }
}
