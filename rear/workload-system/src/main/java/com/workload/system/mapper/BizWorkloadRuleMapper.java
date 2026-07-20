package com.workload.system.mapper;

import java.util.List;
import com.workload.system.domain.BizWorkloadRule;

/**
 * 全局核算规则参数Mapper接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface BizWorkloadRuleMapper 
{
    /**
     * 查询全局核算规则参数
     * 
     * @param id 全局核算规则参数主键
     * @return 全局核算规则参数
     */
    public BizWorkloadRule selectBizWorkloadRuleById(Long id);

    /**
     * 查询全局核算规则参数列表
     * 
     * @param bizWorkloadRule 全局核算规则参数
     * @return 全局核算规则参数集合
     */
    public List<BizWorkloadRule> selectBizWorkloadRuleList(BizWorkloadRule bizWorkloadRule);

    /**
     * 新增全局核算规则参数
     * 
     * @param bizWorkloadRule 全局核算规则参数
     * @return 结果
     */
    public int insertBizWorkloadRule(BizWorkloadRule bizWorkloadRule);

    /**
     * 修改全局核算规则参数
     * 
     * @param bizWorkloadRule 全局核算规则参数
     * @return 结果
     */
    public int updateBizWorkloadRule(BizWorkloadRule bizWorkloadRule);

    /**
     * 删除全局核算规则参数
     * 
     * @param id 全局核算规则参数主键
     * @return 结果
     */
    public int deleteBizWorkloadRuleById(Long id);

    /**
     * 批量删除全局核算规则参数
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizWorkloadRuleByIds(Long[] ids);
}
