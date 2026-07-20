package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizWorkloadCategoryDict;

/**
 * 工作量类别字典Service接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizWorkloadCategoryDictService 
{
    /**
     * 查询工作量类别字典
     * 
     * @param typeCode 工作量类别字典主键
     * @return 工作量类别字典
     */
    public BizWorkloadCategoryDict selectBizWorkloadCategoryDictByTypeCode(String typeCode);

    /**
     * 查询工作量类别字典列表
     * 
     * @param bizWorkloadCategoryDict 工作量类别字典
     * @return 工作量类别字典集合
     */
    public List<BizWorkloadCategoryDict> selectBizWorkloadCategoryDictList(BizWorkloadCategoryDict bizWorkloadCategoryDict);

    /**
     * 新增工作量类别字典
     * 
     * @param bizWorkloadCategoryDict 工作量类别字典
     * @return 结果
     */
    public int insertBizWorkloadCategoryDict(BizWorkloadCategoryDict bizWorkloadCategoryDict);

    /**
     * 修改工作量类别字典
     * 
     * @param bizWorkloadCategoryDict 工作量类别字典
     * @return 结果
     */
    public int updateBizWorkloadCategoryDict(BizWorkloadCategoryDict bizWorkloadCategoryDict);

    /**
     * 批量删除工作量类别字典
     * 
     * @param typeCodes 需要删除的工作量类别字典主键集合
     * @return 结果
     */
    public int deleteBizWorkloadCategoryDictByTypeCodes(String[] typeCodes);

    /**
     * 删除工作量类别字典信息
     * 
     * @param typeCode 工作量类别字典主键
     * @return 结果
     */
    public int deleteBizWorkloadCategoryDictByTypeCode(String typeCode);
}
