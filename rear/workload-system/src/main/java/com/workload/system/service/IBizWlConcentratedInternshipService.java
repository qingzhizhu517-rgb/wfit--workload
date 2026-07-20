package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizWlConcentratedInternship;

/**
 * G6集中实习明细Service接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizWlConcentratedInternshipService 
{
    /**
     * 查询G6集中实习明细
     * 
     * @param itemId G6集中实习明细主键
     * @return G6集中实习明细
     */
    public BizWlConcentratedInternship selectBizWlConcentratedInternshipByItemId(Long itemId);

    /**
     * 查询G6集中实习明细列表
     * 
     * @param bizWlConcentratedInternship G6集中实习明细
     * @return G6集中实习明细集合
     */
    public List<BizWlConcentratedInternship> selectBizWlConcentratedInternshipList(BizWlConcentratedInternship bizWlConcentratedInternship);

    /**
     * 新增G6集中实习明细
     * 
     * @param bizWlConcentratedInternship G6集中实习明细
     * @return 结果
     */
    public int insertBizWlConcentratedInternship(BizWlConcentratedInternship bizWlConcentratedInternship);

    /**
     * 修改G6集中实习明细
     * 
     * @param bizWlConcentratedInternship G6集中实习明细
     * @return 结果
     */
    public int updateBizWlConcentratedInternship(BizWlConcentratedInternship bizWlConcentratedInternship);

    /**
     * 批量删除G6集中实习明细
     * 
     * @param itemIds 需要删除的G6集中实习明细主键集合
     * @return 结果
     */
    public int deleteBizWlConcentratedInternshipByItemIds(Long[] itemIds);

    /**
     * 删除G6集中实习明细信息
     * 
     * @param itemId G6集中实习明细主键
     * @return 结果
     */
    public int deleteBizWlConcentratedInternshipByItemId(Long itemId);
}
