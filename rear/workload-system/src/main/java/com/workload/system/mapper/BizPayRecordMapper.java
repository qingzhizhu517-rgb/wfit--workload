package com.workload.system.mapper;

import java.util.List;
import com.workload.system.domain.BizPayRecord;

/**
 * 酬金汇总Mapper接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface BizPayRecordMapper 
{
    /**
     * 查询酬金汇总
     * 
     * @param id 酬金汇总主键
     * @return 酬金汇总
     */
    public BizPayRecord selectBizPayRecordById(Long id);

    /**
     * 查询酬金汇总列表
     * 
     * @param bizPayRecord 酬金汇总
     * @return 酬金汇总集合
     */
    public List<BizPayRecord> selectBizPayRecordList(BizPayRecord bizPayRecord);

    /**
     * 新增酬金汇总
     * 
     * @param bizPayRecord 酬金汇总
     * @return 结果
     */
    public int insertBizPayRecord(BizPayRecord bizPayRecord);

    /**
     * 修改酬金汇总
     * 
     * @param bizPayRecord 酬金汇总
     * @return 结果
     */
    public int updateBizPayRecord(BizPayRecord bizPayRecord);

    /**
     * 删除酬金汇总
     * 
     * @param id 酬金汇总主键
     * @return 结果
     */
    public int deleteBizPayRecordById(Long id);

    /**
     * 批量删除酬金汇总
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizPayRecordByIds(Long[] ids);
}
