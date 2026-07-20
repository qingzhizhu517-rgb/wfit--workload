package com.workload.system.mapper;

import java.util.List;
import com.workload.system.domain.BizWorkloadSummary;

/**
 * 学期工作量汇总Mapper接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface BizWorkloadSummaryMapper 
{
    /**
     * 查询学期工作量汇总
     * 
     * @param id 学期工作量汇总主键
     * @return 学期工作量汇总
     */
    public BizWorkloadSummary selectBizWorkloadSummaryById(Long id);

    /**
     * 查询学期工作量汇总列表
     * 
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 学期工作量汇总集合
     */
    public List<BizWorkloadSummary> selectBizWorkloadSummaryList(BizWorkloadSummary bizWorkloadSummary);

    /**
     * 新增学期工作量汇总
     * 
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 结果
     */
    public int insertBizWorkloadSummary(BizWorkloadSummary bizWorkloadSummary);

    /**
     * 修改学期工作量汇总
     * 
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 结果
     */
    public int updateBizWorkloadSummary(BizWorkloadSummary bizWorkloadSummary);

    /**
     * 删除学期工作量汇总
     * 
     * @param id 学期工作量汇总主键
     * @return 结果
     */
    public int deleteBizWorkloadSummaryById(Long id);

    /**
     * 批量删除学期工作量汇总
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizWorkloadSummaryByIds(Long[] ids);
}
