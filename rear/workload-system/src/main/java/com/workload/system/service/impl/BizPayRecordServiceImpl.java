package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizPayRecordMapper;
import com.workload.system.domain.BizPayRecord;
import com.workload.system.service.IBizPayRecordService;

/**
 * 酬金汇总Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizPayRecordServiceImpl implements IBizPayRecordService 
{
    @Autowired
    private BizPayRecordMapper bizPayRecordMapper;

    /**
     * 查询酬金汇总
     * 
     * @param id 酬金汇总主键
     * @return 酬金汇总
     */
    @Override
    public BizPayRecord selectBizPayRecordById(Long id)
    {
        return bizPayRecordMapper.selectBizPayRecordById(id);
    }

    /**
     * 查询酬金汇总列表
     * 
     * @param bizPayRecord 酬金汇总
     * @return 酬金汇总
     */
    @Override
    public List<BizPayRecord> selectBizPayRecordList(BizPayRecord bizPayRecord)
    {
        return bizPayRecordMapper.selectBizPayRecordList(bizPayRecord);
    }

    /**
     * 新增酬金汇总
     * 
     * @param bizPayRecord 酬金汇总
     * @return 结果
     */
    @Override
    public int insertBizPayRecord(BizPayRecord bizPayRecord)
    {
        bizPayRecord.setCreateTime(DateUtils.getNowDate());
        return bizPayRecordMapper.insertBizPayRecord(bizPayRecord);
    }

    /**
     * 修改酬金汇总
     * 
     * @param bizPayRecord 酬金汇总
     * @return 结果
     */
    @Override
    public int updateBizPayRecord(BizPayRecord bizPayRecord)
    {
        bizPayRecord.setUpdateTime(DateUtils.getNowDate());
        return bizPayRecordMapper.updateBizPayRecord(bizPayRecord);
    }

    /**
     * 批量删除酬金汇总
     * 
     * @param ids 需要删除的酬金汇总主键
     * @return 结果
     */
    @Override
    public int deleteBizPayRecordByIds(Long[] ids)
    {
        return bizPayRecordMapper.deleteBizPayRecordByIds(ids);
    }

    /**
     * 删除酬金汇总信息
     * 
     * @param id 酬金汇总主键
     * @return 结果
     */
    @Override
    public int deleteBizPayRecordById(Long id)
    {
        return bizPayRecordMapper.deleteBizPayRecordById(id);
    }
}
