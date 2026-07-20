package com.workload.system.mapper;

import java.util.List;
import com.workload.system.domain.BizPayRate;

/**
 * 职称单位酬金费率Mapper接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface BizPayRateMapper 
{
    /**
     * 查询职称单位酬金费率
     * 
     * @param id 职称单位酬金费率主键
     * @return 职称单位酬金费率
     */
    public BizPayRate selectBizPayRateById(Long id);

    /**
     * 查询职称单位酬金费率列表
     * 
     * @param bizPayRate 职称单位酬金费率
     * @return 职称单位酬金费率集合
     */
    public List<BizPayRate> selectBizPayRateList(BizPayRate bizPayRate);

    /**
     * 新增职称单位酬金费率
     * 
     * @param bizPayRate 职称单位酬金费率
     * @return 结果
     */
    public int insertBizPayRate(BizPayRate bizPayRate);

    /**
     * 修改职称单位酬金费率
     * 
     * @param bizPayRate 职称单位酬金费率
     * @return 结果
     */
    public int updateBizPayRate(BizPayRate bizPayRate);

    /**
     * 删除职称单位酬金费率
     * 
     * @param id 职称单位酬金费率主键
     * @return 结果
     */
    public int deleteBizPayRateById(Long id);

    /**
     * 批量删除职称单位酬金费率
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizPayRateByIds(Long[] ids);
}
