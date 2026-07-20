package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizAllowanceItemMapper;
import com.workload.system.domain.BizAllowanceItem;
import com.workload.system.service.IBizAllowanceItemService;

/**
 * 其他酬金明细Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizAllowanceItemServiceImpl implements IBizAllowanceItemService 
{
    @Autowired
    private BizAllowanceItemMapper bizAllowanceItemMapper;

    /**
     * 查询其他酬金明细
     * 
     * @param id 其他酬金明细主键
     * @return 其他酬金明细
     */
    @Override
    public BizAllowanceItem selectBizAllowanceItemById(Long id)
    {
        return bizAllowanceItemMapper.selectBizAllowanceItemById(id);
    }

    /**
     * 查询其他酬金明细列表
     * 
     * @param bizAllowanceItem 其他酬金明细
     * @return 其他酬金明细
     */
    @Override
    public List<BizAllowanceItem> selectBizAllowanceItemList(BizAllowanceItem bizAllowanceItem)
    {
        return bizAllowanceItemMapper.selectBizAllowanceItemList(bizAllowanceItem);
    }

    /**
     * 新增其他酬金明细
     * 
     * @param bizAllowanceItem 其他酬金明细
     * @return 结果
     */
    @Override
    public int insertBizAllowanceItem(BizAllowanceItem bizAllowanceItem)
    {
        bizAllowanceItem.setCreateTime(DateUtils.getNowDate());
        return bizAllowanceItemMapper.insertBizAllowanceItem(bizAllowanceItem);
    }

    /**
     * 修改其他酬金明细
     * 
     * @param bizAllowanceItem 其他酬金明细
     * @return 结果
     */
    @Override
    public int updateBizAllowanceItem(BizAllowanceItem bizAllowanceItem)
    {
        bizAllowanceItem.setUpdateTime(DateUtils.getNowDate());
        return bizAllowanceItemMapper.updateBizAllowanceItem(bizAllowanceItem);
    }

    /**
     * 批量删除其他酬金明细
     * 
     * @param ids 需要删除的其他酬金明细主键
     * @return 结果
     */
    @Override
    public int deleteBizAllowanceItemByIds(Long[] ids)
    {
        return bizAllowanceItemMapper.deleteBizAllowanceItemByIds(ids);
    }

    /**
     * 删除其他酬金明细信息
     * 
     * @param id 其他酬金明细主键
     * @return 结果
     */
    @Override
    public int deleteBizAllowanceItemById(Long id)
    {
        return bizAllowanceItemMapper.deleteBizAllowanceItemById(id);
    }
}
