package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizWlThesis;

/**
 * G5毕业论文明细Service接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizWlThesisService 
{
    /**
     * 查询G5毕业论文明细
     * 
     * @param itemId G5毕业论文明细主键
     * @return G5毕业论文明细
     */
    public BizWlThesis selectBizWlThesisByItemId(Long itemId);

    /**
     * 查询G5毕业论文明细列表
     * 
     * @param bizWlThesis G5毕业论文明细
     * @return G5毕业论文明细集合
     */
    public List<BizWlThesis> selectBizWlThesisList(BizWlThesis bizWlThesis);

    /**
     * 新增G5毕业论文明细
     * 
     * @param bizWlThesis G5毕业论文明细
     * @return 结果
     */
    public int insertBizWlThesis(BizWlThesis bizWlThesis);

    /**
     * 修改G5毕业论文明细
     * 
     * @param bizWlThesis G5毕业论文明细
     * @return 结果
     */
    public int updateBizWlThesis(BizWlThesis bizWlThesis);

    /**
     * 批量删除G5毕业论文明细
     * 
     * @param itemIds 需要删除的G5毕业论文明细主键集合
     * @return 结果
     */
    public int deleteBizWlThesisByItemIds(Long[] itemIds);

    /**
     * 删除G5毕业论文明细信息
     * 
     * @param itemId G5毕业论文明细主键
     * @return 结果
     */
    public int deleteBizWlThesisByItemId(Long itemId);
}
