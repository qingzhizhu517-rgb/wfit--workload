package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.service.IBizWorkloadSummaryService;

/**
 * 学期工作量汇总Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWorkloadSummaryServiceImpl implements IBizWorkloadSummaryService 
{
    @Autowired
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    /**
     * 查询学期工作量汇总
     * 
     * @param id 学期工作量汇总主键
     * @return 学期工作量汇总
     */
    @Override
    public BizWorkloadSummary selectBizWorkloadSummaryById(Long id)
    {
        return bizWorkloadSummaryMapper.selectBizWorkloadSummaryById(id);
    }

    /**
     * 查询学期工作量汇总列表
     * 
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 学期工作量汇总
     */
    @Override
    public List<BizWorkloadSummary> selectBizWorkloadSummaryList(BizWorkloadSummary bizWorkloadSummary)
    {
        return bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(bizWorkloadSummary);
    }

    /**
     * 新增学期工作量汇总
     * 
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 结果
     */
    @Override
    public int insertBizWorkloadSummary(BizWorkloadSummary bizWorkloadSummary)
    {
        bizWorkloadSummary.setCreateTime(DateUtils.getNowDate());
        return bizWorkloadSummaryMapper.insertBizWorkloadSummary(bizWorkloadSummary);
    }

    /**
     * 修改学期工作量汇总
     * 
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 结果
     */
    @Override
    public int updateBizWorkloadSummary(BizWorkloadSummary bizWorkloadSummary)
    {
        bizWorkloadSummary.setUpdateTime(DateUtils.getNowDate());
        return bizWorkloadSummaryMapper.updateBizWorkloadSummary(bizWorkloadSummary);
    }

    /**
     * 批量删除学期工作量汇总
     * 
     * @param ids 需要删除的学期工作量汇总主键
     * @return 结果
     */
    @Override
    public int deleteBizWorkloadSummaryByIds(Long[] ids)
    {
        return bizWorkloadSummaryMapper.deleteBizWorkloadSummaryByIds(ids);
    }

    /**
     * 删除学期工作量汇总信息
     *
     * @param id 学期工作量汇总主键
     * @return 结果
     */
    @Override
    public int deleteBizWorkloadSummaryById(Long id)
    {
        return bizWorkloadSummaryMapper.deleteBizWorkloadSummaryById(id);
    }

    /**
     * 按教师+学期查询汇总
     */
    @Override
    public BizWorkloadSummary selectBizWorkloadSummaryByUserAndSemester(Long userId, String semester)
    {
        BizWorkloadSummary query = new BizWorkloadSummary();
        query.setUserId(userId);
        query.setSemester(semester);
        List<BizWorkloadSummary> list = bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(query);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
}
