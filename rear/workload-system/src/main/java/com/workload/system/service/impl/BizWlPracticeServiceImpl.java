package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizWlPracticeMapper;
import com.workload.system.domain.BizWlPractice;
import com.workload.system.service.IBizWlPracticeService;

/**
 * G2课内实践明细Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWlPracticeServiceImpl implements IBizWlPracticeService 
{
    @Autowired
    private BizWlPracticeMapper bizWlPracticeMapper;

    /**
     * 查询G2课内实践明细
     * 
     * @param itemId G2课内实践明细主键
     * @return G2课内实践明细
     */
    @Override
    public BizWlPractice selectBizWlPracticeByItemId(Long itemId)
    {
        return bizWlPracticeMapper.selectBizWlPracticeByItemId(itemId);
    }

    /**
     * 查询G2课内实践明细列表
     * 
     * @param bizWlPractice G2课内实践明细
     * @return G2课内实践明细
     */
    @Override
    public List<BizWlPractice> selectBizWlPracticeList(BizWlPractice bizWlPractice)
    {
        return bizWlPracticeMapper.selectBizWlPracticeList(bizWlPractice);
    }

    /**
     * 新增G2课内实践明细
     * 
     * @param bizWlPractice G2课内实践明细
     * @return 结果
     */
    @Override
    public int insertBizWlPractice(BizWlPractice bizWlPractice)
    {
        bizWlPractice.setCreateTime(DateUtils.getNowDate());
        return bizWlPracticeMapper.insertBizWlPractice(bizWlPractice);
    }

    /**
     * 修改G2课内实践明细
     * 
     * @param bizWlPractice G2课内实践明细
     * @return 结果
     */
    @Override
    public int updateBizWlPractice(BizWlPractice bizWlPractice)
    {
        bizWlPractice.setUpdateTime(DateUtils.getNowDate());
        return bizWlPracticeMapper.updateBizWlPractice(bizWlPractice);
    }

    /**
     * 批量删除G2课内实践明细
     * 
     * @param itemIds 需要删除的G2课内实践明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlPracticeByItemIds(Long[] itemIds)
    {
        return bizWlPracticeMapper.deleteBizWlPracticeByItemIds(itemIds);
    }

    /**
     * 删除G2课内实践明细信息
     * 
     * @param itemId G2课内实践明细主键
     * @return 结果
     */
    @Override
    public int deleteBizWlPracticeByItemId(Long itemId)
    {
        return bizWlPracticeMapper.deleteBizWlPracticeByItemId(itemId);
    }
}
