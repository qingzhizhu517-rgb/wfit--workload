package com.workload.system.service.impl;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import com.workload.system.calc.strategy.StrategyCache;
import com.workload.system.calc.rule.AfterCommit;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizWorkloadCategoryDictMapper;
import com.workload.system.domain.BizWorkloadCategoryDict;
import com.workload.system.service.IBizWorkloadCategoryDictService;

/**
 * 工作量类别字典Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWorkloadCategoryDictServiceImpl implements IBizWorkloadCategoryDictService 
{
    @Autowired
    private BizWorkloadCategoryDictMapper bizWorkloadCategoryDictMapper;

    @Autowired
    private StrategyCache strategyCache;

    /**
     * 查询工作量类别字典
     * 
     * @param typeCode 工作量类别字典主键
     * @return 工作量类别字典
     */
    @Override
    public BizWorkloadCategoryDict selectBizWorkloadCategoryDictByTypeCode(String typeCode)
    {
        return bizWorkloadCategoryDictMapper.selectBizWorkloadCategoryDictByTypeCode(typeCode);
    }

    /**
     * 查询工作量类别字典列表
     * 
     * @param bizWorkloadCategoryDict 工作量类别字典
     * @return 工作量类别字典
     */
    @Override
    public List<BizWorkloadCategoryDict> selectBizWorkloadCategoryDictList(BizWorkloadCategoryDict bizWorkloadCategoryDict)
    {
        return bizWorkloadCategoryDictMapper.selectBizWorkloadCategoryDictList(bizWorkloadCategoryDict);
    }

    /**
     * 新增工作量类别字典
     * 
     * @param bizWorkloadCategoryDict 工作量类别字典
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBizWorkloadCategoryDict(BizWorkloadCategoryDict bizWorkloadCategoryDict)
    {
        bizWorkloadCategoryDict.setCreateTime(DateUtils.getNowDate());
        int rows = bizWorkloadCategoryDictMapper.insertBizWorkloadCategoryDict(bizWorkloadCategoryDict);
        if (rows > 0) AfterCommit.run(() -> strategyCache.evict(bizWorkloadCategoryDict.getTypeCode()));
        return rows;
    }

    /**
     * 修改工作量类别字典
     * 
     * @param bizWorkloadCategoryDict 工作量类别字典
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBizWorkloadCategoryDict(BizWorkloadCategoryDict bizWorkloadCategoryDict)
    {
        bizWorkloadCategoryDict.setUpdateTime(DateUtils.getNowDate());
        int rows = bizWorkloadCategoryDictMapper.updateBizWorkloadCategoryDict(bizWorkloadCategoryDict);
        if (rows > 0) AfterCommit.run(() -> strategyCache.evict(bizWorkloadCategoryDict.getTypeCode()));
        return rows;
    }

    /**
     * 批量删除工作量类别字典
     * 
     * @param typeCodes 需要删除的工作量类别字典主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizWorkloadCategoryDictByTypeCodes(String[] typeCodes)
    {
        int rows = bizWorkloadCategoryDictMapper.deleteBizWorkloadCategoryDictByTypeCodes(typeCodes);
        if (rows > 0) AfterCommit.run(() -> { for (String code : typeCodes) strategyCache.evict(code); });
        return rows;
    }

    /**
     * 删除工作量类别字典信息
     * 
     * @param typeCode 工作量类别字典主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizWorkloadCategoryDictByTypeCode(String typeCode)
    {
        int rows = bizWorkloadCategoryDictMapper.deleteBizWorkloadCategoryDictByTypeCode(typeCode);
        if (rows > 0) AfterCommit.run(() -> strategyCache.evict(typeCode));
        return rows;
    }
}
