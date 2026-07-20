package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizPayRateMapper;
import com.workload.system.domain.BizPayRate;
import com.workload.system.service.IBizPayRateService;

/**
 * 职称单位酬金费率Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizPayRateServiceImpl implements IBizPayRateService 
{
    @Autowired
    private BizPayRateMapper bizPayRateMapper;

    /**
     * 查询职称单位酬金费率
     * 
     * @param id 职称单位酬金费率主键
     * @return 职称单位酬金费率
     */
    @Override
    public BizPayRate selectBizPayRateById(Long id)
    {
        return bizPayRateMapper.selectBizPayRateById(id);
    }

    /**
     * 查询职称单位酬金费率列表
     * 
     * @param bizPayRate 职称单位酬金费率
     * @return 职称单位酬金费率
     */
    @Override
    public List<BizPayRate> selectBizPayRateList(BizPayRate bizPayRate)
    {
        return bizPayRateMapper.selectBizPayRateList(bizPayRate);
    }

    /**
     * 新增职称单位酬金费率
     * 
     * @param bizPayRate 职称单位酬金费率
     * @return 结果
     */
    @Override
    public int insertBizPayRate(BizPayRate bizPayRate)
    {
        bizPayRate.setCreateTime(DateUtils.getNowDate());
        return bizPayRateMapper.insertBizPayRate(bizPayRate);
    }

    /**
     * 修改职称单位酬金费率
     * 
     * @param bizPayRate 职称单位酬金费率
     * @return 结果
     */
    @Override
    public int updateBizPayRate(BizPayRate bizPayRate)
    {
        bizPayRate.setUpdateTime(DateUtils.getNowDate());
        return bizPayRateMapper.updateBizPayRate(bizPayRate);
    }

    /**
     * 批量删除职称单位酬金费率
     * 
     * @param ids 需要删除的职称单位酬金费率主键
     * @return 结果
     */
    @Override
    public int deleteBizPayRateByIds(Long[] ids)
    {
        return bizPayRateMapper.deleteBizPayRateByIds(ids);
    }

    /**
     * 删除职称单位酬金费率信息
     * 
     * @param id 职称单位酬金费率主键
     * @return 结果
     */
    @Override
    public int deleteBizPayRateById(Long id)
    {
        return bizPayRateMapper.deleteBizPayRateById(id);
    }
}
