package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizWlTheory;

/**
 * G1理论课明细Service接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizWlTheoryService 
{
    /**
     * 查询G1理论课明细
     * 
     * @param itemId G1理论课明细主键
     * @return G1理论课明细
     */
    public BizWlTheory selectBizWlTheoryByItemId(Long itemId);

    /**
     * 查询G1理论课明细列表
     * 
     * @param bizWlTheory G1理论课明细
     * @return G1理论课明细集合
     */
    public List<BizWlTheory> selectBizWlTheoryList(BizWlTheory bizWlTheory);

    /**
     * 新增G1理论课明细
     * 
     * @param bizWlTheory G1理论课明细
     * @return 结果
     */
    public int insertBizWlTheory(BizWlTheory bizWlTheory);

    /**
     * 修改G1理论课明细
     * 
     * @param bizWlTheory G1理论课明细
     * @return 结果
     */
    public int updateBizWlTheory(BizWlTheory bizWlTheory);

    /**
     * 批量删除G1理论课明细
     * 
     * @param itemIds 需要删除的G1理论课明细主键集合
     * @return 结果
     */
    public int deleteBizWlTheoryByItemIds(Long[] itemIds);

    /**
     * 删除G1理论课明细信息
     * 
     * @param itemId G1理论课明细主键
     * @return 结果
     */
    public int deleteBizWlTheoryByItemId(Long itemId);
}
