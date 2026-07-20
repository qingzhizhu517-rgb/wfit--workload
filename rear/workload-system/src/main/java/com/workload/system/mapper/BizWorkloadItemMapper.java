package com.workload.system.mapper;

import java.util.List;
import com.workload.system.domain.BizWorkloadItem;

/**
 * 工作量明细主表Mapper接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface BizWorkloadItemMapper 
{
    /**
     * 查询工作量明细主表
     * 
     * @param id 工作量明细主表主键
     * @return 工作量明细主表
     */
    public BizWorkloadItem selectBizWorkloadItemById(Long id);

    /**
     * 查询工作量明细主表列表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 工作量明细主表集合
     */
    public List<BizWorkloadItem> selectBizWorkloadItemList(BizWorkloadItem bizWorkloadItem);

    /**
     * 新增工作量明细主表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 结果
     */
    public int insertBizWorkloadItem(BizWorkloadItem bizWorkloadItem);

    /**
     * 修改工作量明细主表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 结果
     */
    public int updateBizWorkloadItem(BizWorkloadItem bizWorkloadItem);

    /**
     * 删除工作量明细主表
     * 
     * @param id 工作量明细主表主键
     * @return 结果
     */
    public int deleteBizWorkloadItemById(Long id);

    /**
     * 批量删除工作量明细主表
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizWorkloadItemByIds(Long[] ids);
}
