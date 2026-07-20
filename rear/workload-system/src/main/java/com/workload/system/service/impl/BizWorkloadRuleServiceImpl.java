package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizWorkloadRuleMapper;
import com.workload.system.domain.BizWorkloadRule;
import com.workload.system.service.IBizWorkloadRuleService;

/**
 * 全局核算规则参数Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWorkloadRuleServiceImpl implements IBizWorkloadRuleService 
{
    @Autowired
    private BizWorkloadRuleMapper bizWorkloadRuleMapper;

    /**
     * 查询全局核算规则参数
     * 
     * @param id 全局核算规则参数主键
     * @return 全局核算规则参数
     */
    @Override
    public BizWorkloadRule selectBizWorkloadRuleById(Long id)
    {
        return bizWorkloadRuleMapper.selectBizWorkloadRuleById(id);
    }

    /**
     * 查询全局核算规则参数列表
     * 
     * @param bizWorkloadRule 全局核算规则参数
     * @return 全局核算规则参数
     */
    @Override
    public List<BizWorkloadRule> selectBizWorkloadRuleList(BizWorkloadRule bizWorkloadRule)
    {
        return bizWorkloadRuleMapper.selectBizWorkloadRuleList(bizWorkloadRule);
    }

    /**
     * 新增全局核算规则参数
     * 
     * @param bizWorkloadRule 全局核算规则参数
     * @return 结果
     */
    @Override
    public int insertBizWorkloadRule(BizWorkloadRule bizWorkloadRule)
    {
        bizWorkloadRule.setCreateTime(DateUtils.getNowDate());
        return bizWorkloadRuleMapper.insertBizWorkloadRule(bizWorkloadRule);
    }

    /**
     * 修改全局核算规则参数
     * 
     * @param bizWorkloadRule 全局核算规则参数
     * @return 结果
     */
    @Override
    public int updateBizWorkloadRule(BizWorkloadRule bizWorkloadRule)
    {
        bizWorkloadRule.setUpdateTime(DateUtils.getNowDate());
        return bizWorkloadRuleMapper.updateBizWorkloadRule(bizWorkloadRule);
    }

    /**
     * 批量删除全局核算规则参数
     * 
     * @param ids 需要删除的全局核算规则参数主键
     * @return 结果
     */
    @Override
    public int deleteBizWorkloadRuleByIds(Long[] ids)
    {
        return bizWorkloadRuleMapper.deleteBizWorkloadRuleByIds(ids);
    }

    /**
     * 删除全局核算规则参数信息
     * 
     * @param id 全局核算规则参数主键
     * @return 结果
     */
    @Override
    public int deleteBizWorkloadRuleById(Long id)
    {
        return bizWorkloadRuleMapper.deleteBizWorkloadRuleById(id);
    }
}
