package com.workload.system.service.impl;

import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.service.IBizWorkloadSummaryService;

/**
 * 学期工作量汇总Service业务层处理
 *
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWorkloadSummaryServiceImpl implements IBizWorkloadSummaryService
{
    @Autowired private com.workload.system.review.ReviewBatchService reviewBatches;

    @Autowired private com.workload.system.calc.SettlementSnapshotService settlementSnapshots;

    @Autowired
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    @Autowired
    private WorkloadWriteGuard writeGuard;

    /**
     * 查询学期工作量汇总
     *
     * @param id 学期工作量汇总主键
     * @return 学期工作量汇总
     */
    @Override
    @Transactional(readOnly = true)
    public BizWorkloadSummary selectBizWorkloadSummaryById(Long id)
    {
        var result=bizWorkloadSummaryMapper.selectBizWorkloadSummaryById(id);
        if(settlementSnapshots!=null) settlementSnapshots.decorate(result);
        return result;
    }

    /**
     * 查询学期工作量汇总列表
     *
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 学期工作量汇总
     */
    @Override
    @Transactional(readOnly = true)
    public List<BizWorkloadSummary> selectBizWorkloadSummaryList(BizWorkloadSummary bizWorkloadSummary)
    {
        var list=bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(bizWorkloadSummary);
        if(settlementSnapshots!=null) list.forEach(settlementSnapshots::decorate);
        return list;
    }

    /**
     * 新增学期工作量汇总
     *
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 结果
     */
    @Override
    public int insertBizWorkloadSummary(BizWorkloadSummary bizWorkloadSummary)
    {
        throw new ServiceException("学期汇总必须通过核算生成，不允许手工新增计算结果");
    }

    /**
     * 修改学期工作量汇总
     *
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBizWorkloadSummary(BizWorkloadSummary bizWorkloadSummary)
    {
        BizWorkloadSummary existing = requireEditable(bizWorkloadSummary.getId());
        if ((bizWorkloadSummary.getUserId() != null && !Objects.equals(existing.getUserId(), bizWorkloadSummary.getUserId()))
                || (bizWorkloadSummary.getSemester() != null && !Objects.equals(existing.getSemester(), bizWorkloadSummary.getSemester())))
        {
            throw new ServiceException("汇总归属不可修改，请对目标教师与学期重新核算");
        }
        // 所有角色只能通过通用编辑维护备注；计算结果与审批信息分别由核算、审批流程写入。
        BizWorkloadSummary update = new BizWorkloadSummary();
        update.setId(existing.getId());
        update.setRemark(bizWorkloadSummary.getRemark());
        update.setUpdateTime(DateUtils.getNowDate());
        return bizWorkloadSummaryMapper.updateBizWorkloadSummary(update);
    }

    /**
     * 批量删除学期工作量汇总
     *
     * @param ids 需要删除的学期工作量汇总主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizWorkloadSummaryByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) return 0;
        for (Long id : ids){
            var existing=requireEditable(id);
            if(settlementSnapshots!=null)settlementSnapshots.assertSummaryDeletionAllowed(existing);
            reviewBatches.assertDeletionAllowed(existing);
        }
        return bizWorkloadSummaryMapper.deleteBizWorkloadSummaryByIds(ids);
    }

    /**
     * 删除学期工作量汇总信息
     *
     * @param id 学期工作量汇总主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizWorkloadSummaryById(Long id)
    {
        var existing=requireEditable(id);
        if(settlementSnapshots!=null)settlementSnapshots.assertSummaryDeletionAllowed(existing);
            reviewBatches.assertDeletionAllowed(existing);
        return bizWorkloadSummaryMapper.deleteBizWorkloadSummaryById(id);
    }

    /**
     * 按教师+学期查询汇总
     */
    @Override
    @Transactional(readOnly = true)
    public BizWorkloadSummary selectBizWorkloadSummaryByUserAndSemester(Long userId, String semester)
    {
        BizWorkloadSummary query = new BizWorkloadSummary();
        query.setUserId(userId);
        query.setSemester(semester);
        List<BizWorkloadSummary> list = bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(query);
        var result=(list != null && !list.isEmpty()) ? list.get(0) : null;
        if(settlementSnapshots!=null) settlementSnapshots.decorate(result);
        return result;
    }
    private BizWorkloadSummary requireEditable(Long id)
    {
        BizWorkloadSummary existing = id == null ? null : bizWorkloadSummaryMapper.selectBizWorkloadSummaryById(id);
        if (existing == null) throw new ServiceException("学期汇总不存在");
        // 事务内锁定真实教师与学期对应的行，状态由锁后读取判断，防止与审批并发改数。
        writeGuard.lockDraftOrAbsent(existing.getUserId(), existing.getSemester());
        return existing;
    }
}
