package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.service.IBizWorkloadItemService;

/**
 * 工作量明细主表Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWorkloadItemServiceImpl implements IBizWorkloadItemService 
{
    @Autowired
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    /**
     * 查询工作量明细主表
     * 
     * @param id 工作量明细主表主键
     * @return 工作量明细主表
     */
    @Override
    public BizWorkloadItem selectBizWorkloadItemById(Long id)
    {
        return bizWorkloadItemMapper.selectBizWorkloadItemById(id);
    }

    /**
     * 查询工作量明细主表列表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 工作量明细主表
     */
    @Override
    public List<BizWorkloadItem> selectBizWorkloadItemList(BizWorkloadItem bizWorkloadItem)
    {
        return bizWorkloadItemMapper.selectBizWorkloadItemList(bizWorkloadItem);
    }

    /**
     * 新增工作量明细主表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 结果
     */
    @Override
    public int insertBizWorkloadItem(BizWorkloadItem bizWorkloadItem)
    {
        bizWorkloadItem.setCreateTime(DateUtils.getNowDate());
        return bizWorkloadItemMapper.insertBizWorkloadItem(bizWorkloadItem);
    }

    /**
     * 修改工作量明细主表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 结果
     */
    @Override
    public int updateBizWorkloadItem(BizWorkloadItem bizWorkloadItem)
    {
        bizWorkloadItem.setUpdateTime(DateUtils.getNowDate());
        return bizWorkloadItemMapper.updateBizWorkloadItem(bizWorkloadItem);
    }

    /**
     * 批量删除工作量明细主表
     * 
     * @param ids 需要删除的工作量明细主表主键
     * @return 结果
     */
    @Override
    public int deleteBizWorkloadItemByIds(Long[] ids)
    {
        return bizWorkloadItemMapper.deleteBizWorkloadItemByIds(ids);
    }

    /**
     * 删除工作量明细主表信息
     * 
     * @param id 工作量明细主表主键
     * @return 结果
     */
    @Override
    public int deleteBizWorkloadItemById(Long id)
    {
        return bizWorkloadItemMapper.deleteBizWorkloadItemById(id);
    }
}
