package com.workload.system.mapper;

import java.util.List;
import com.workload.system.domain.BizWlPractice;

/**
 * G2课内实践明细Mapper接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface BizWlPracticeMapper 
{
    /**
     * 查询G2课内实践明细
     * 
     * @param itemId G2课内实践明细主键
     * @return G2课内实践明细
     */
    public BizWlPractice selectBizWlPracticeByItemId(Long itemId);

    /**
     * 查询G2课内实践明细列表
     * 
     * @param bizWlPractice G2课内实践明细
     * @return G2课内实践明细集合
     */
    public List<BizWlPractice> selectBizWlPracticeList(BizWlPractice bizWlPractice);

    /**
     * 新增G2课内实践明细
     * 
     * @param bizWlPractice G2课内实践明细
     * @return 结果
     */
    public int insertBizWlPractice(BizWlPractice bizWlPractice);

    /**
     * 修改G2课内实践明细
     * 
     * @param bizWlPractice G2课内实践明细
     * @return 结果
     */
    public int updateBizWlPractice(BizWlPractice bizWlPractice);

    /**
     * 删除G2课内实践明细
     * 
     * @param itemId G2课内实践明细主键
     * @return 结果
     */
    public int deleteBizWlPracticeByItemId(Long itemId);

    /**
     * 批量删除G2课内实践明细
     * 
     * @param itemIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizWlPracticeByItemIds(Long[] itemIds);
}
