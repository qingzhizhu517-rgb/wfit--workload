package com.workload.system.service.impl;

import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.WorkloadWriteGuard;
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
    @Autowired private com.workload.system.calc.SettlementSnapshotService settlementSnapshots;

    @Autowired
    private BizPayRecordMapper bizPayRecordMapper;

    @Autowired
    private WorkloadWriteGuard writeGuard;

    /**
     * 查询酬金汇总
     *
     * @param id 酬金汇总主键
     * @return 酬金汇总
     */
    @Override
    @Transactional(readOnly = true)
    public BizPayRecord selectBizPayRecordById(Long id)
    {
        var result=bizPayRecordMapper.selectBizPayRecordById(id);
        if(settlementSnapshots!=null) settlementSnapshots.decorate(result);
        return result;
    }

    /**
     * 查询酬金汇总列表
     *
     * @param bizPayRecord 酬金汇总
     * @return 酬金汇总
     */
    @Override
    @Transactional(readOnly = true)
    public List<BizPayRecord> selectBizPayRecordList(BizPayRecord bizPayRecord)
    {
        var list=bizPayRecordMapper.selectBizPayRecordList(bizPayRecord);
        if(settlementSnapshots!=null) list.forEach(settlementSnapshots::decorate);
        return list;
    }

    /**
     * 新增酬金汇总
     *
     * @param bizPayRecord 酬金汇总
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBizPayRecord(BizPayRecord bizPayRecord)
    {
        writeGuard.lockDraftOrAbsent(bizPayRecord.getUserId(), bizPayRecord.getSemester());
        throw new ServiceException("酬金记录必须通过核算生成，不允许手工新增结果");
    }

    /**
     * 修改酬金汇总
     *
     * @param bizPayRecord 酬金汇总
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBizPayRecord(BizPayRecord bizPayRecord)
    {
        BizPayRecord existing = requireEditable(bizPayRecord.getId());
        if ((bizPayRecord.getUserId() != null && !Objects.equals(existing.getUserId(), bizPayRecord.getUserId()))
                || (bizPayRecord.getSemester() != null && !Objects.equals(existing.getSemester(), bizPayRecord.getSemester()))
                || (bizPayRecord.getSummaryId() != null && !Objects.equals(existing.getSummaryId(), bizPayRecord.getSummaryId())))
        {
            throw new ServiceException("酬金记录归属不可修改，请通过核算生成对应学期记录");
        }
        BizPayRecord update = new BizPayRecord();
        update.setId(existing.getId());
        update.setRemark(bizPayRecord.getRemark());
        update.setUpdateTime(DateUtils.getNowDate());
        return bizPayRecordMapper.updateBizPayRecord(update);
    }

    /**
     * 批量删除酬金汇总
     *
     * @param ids 需要删除的酬金汇总主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizPayRecordByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        // 先完整预检，之后才标记真正legacy汇总脏并删除；新结算链禁止通用删除。
        List<BizPayRecord> checked=new java.util.ArrayList<>();
        for (Long id : ids)
        {
            BizPayRecord existing = requireEditable(id);
            if(settlementSnapshots!=null)settlementSnapshots.assertPayDeletionAllowed(existing);
            checked.add(existing);
        }
        for(BizPayRecord existing:checked)writeGuard.markChanged(existing.getUserId(),existing.getSemester());
        return bizPayRecordMapper.deleteBizPayRecordByIds(ids);
    }

    /**
     * 删除酬金汇总信息
     *
     * @param id 酬金汇总主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizPayRecordById(Long id)
    {
        BizPayRecord existing = requireEditable(id);
        if(settlementSnapshots!=null)settlementSnapshots.assertPayDeletionAllowed(existing);
        writeGuard.markChanged(existing.getUserId(), existing.getSemester());
        return bizPayRecordMapper.deleteBizPayRecordById(id);
    }
    /** 冻结保护必须使用数据库归属，不能相信部分更新请求中的教师或学期。 */
    private BizPayRecord requireEditable(Long id)
    {
        BizPayRecord existing = id == null ? null : bizPayRecordMapper.selectBizPayRecordById(id);
        if (existing == null)
        {
            throw new ServiceException("酬金记录不存在");
        }
        writeGuard.lockDraftOrAbsent(existing.getUserId(), existing.getSemester());
        return existing;
    }
}
