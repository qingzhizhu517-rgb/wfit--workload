package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizAllowanceItem;

/**
 * 其他酬金明细Service接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizAllowanceItemService 
{
    /**
     * 查询其他酬金明细
     * 
     * @param id 其他酬金明细主键
     * @return 其他酬金明细
     */
    public BizAllowanceItem selectBizAllowanceItemById(Long id);

    /**
     * 查询其他酬金明细列表
     * 
     * @param bizAllowanceItem 其他酬金明细
     * @return 其他酬金明细集合
     */
    public List<BizAllowanceItem> selectBizAllowanceItemList(BizAllowanceItem bizAllowanceItem);

    /**
     * 新增其他酬金明细
     * 
     * @param bizAllowanceItem 其他酬金明细
     * @return 结果
     */
    public int insertBizAllowanceItem(BizAllowanceItem bizAllowanceItem);

    /**
     * 修改其他酬金明细
     * 
     * @param bizAllowanceItem 其他酬金明细
     * @return 结果
     */
    public int updateBizAllowanceItem(BizAllowanceItem bizAllowanceItem);

    /**
     * 批量删除其他酬金明细
     * 
     * @param ids 需要删除的其他酬金明细主键集合
     * @return 结果
     */
    public int deleteBizAllowanceItemByIds(Long[] ids);

    /**
     * 删除其他酬金明细信息
     * 
     * @param id 其他酬金明细主键
     * @return 结果
     */
    public int deleteBizAllowanceItemById(Long id);
}
