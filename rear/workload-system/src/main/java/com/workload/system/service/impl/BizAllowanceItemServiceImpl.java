package com.workload.system.service.impl;

import java.util.List;
import java.util.Arrays;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.PayCalcService;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.calc.allowance.AllowanceCalcStrategy;
import com.workload.system.calc.allowance.AllowanceStrategyFactory;
import com.workload.system.mapper.BizAllowanceItemMapper;
import com.workload.system.domain.BizAllowanceItem;
import com.workload.system.service.IBizAllowanceItemService;

/**
 * 其他酬金明细Service业务层处理
 *
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizAllowanceItemServiceImpl implements IBizAllowanceItemService
{
    private static final String SELF_STUDY_ZERO_WARNING =
            "人数≥20 已达单独开班标准（第十五条1(2)），本项不计酬金，工作量按理论课路线核算";
    @Autowired
    private BizAllowanceItemMapper bizAllowanceItemMapper;

    @Autowired
    private AllowanceStrategyFactory allowanceStrategyFactory;

    @Autowired
    private PayCalcService payCalcService;

    @Autowired
    private WorkloadWriteGuard writeGuard;

    /**
     * 查询其他酬金明细
     *
     * @param id 其他酬金明细主键
     * @return 其他酬金明细
     */
    @Override
    public BizAllowanceItem selectBizAllowanceItemById(Long id)
    {
        return bizAllowanceItemMapper.selectBizAllowanceItemById(id);
    }

    /**
     * 查询其他酬金明细列表
     *
     * @param bizAllowanceItem 其他酬金明细
     * @return 其他酬金明细
     */
    @Override
    public List<BizAllowanceItem> selectBizAllowanceItemList(BizAllowanceItem bizAllowanceItem)
    {
        return bizAllowanceItemMapper.selectBizAllowanceItemList(bizAllowanceItem);
    }

    /**
     * 新增其他酬金明细（自动按 fee_type 策略计算金额）
     *
     * @param bizAllowanceItem 其他酬金明细
     * @return 结果
     */
    @Override
    @Transactional
    public int insertBizAllowanceItem(BizAllowanceItem bizAllowanceItem)
    {
        writeGuard.lockDraftOrAbsent(bizAllowanceItem.getUserId(), bizAllowanceItem.getSemester());
        payCalcService.assertAllowanceEditable(bizAllowanceItem.getUserId(), bizAllowanceItem.getSemester());
        recalcAmount(bizAllowanceItem);
        bizAllowanceItem.setCreateTime(DateUtils.getNowDate());
        return bizAllowanceItemMapper.insertBizAllowanceItem(bizAllowanceItem);
    }

    /**
     * 修改其他酬金明细（自动按 fee_type 策略重算金额）
     *
     * @param bizAllowanceItem 其他酬金明细
     * @return 结果
     */
    @Override
    @Transactional
    public int updateBizAllowanceItem(BizAllowanceItem bizAllowanceItem)
    {
        BizAllowanceItem old = bizAllowanceItemMapper.selectBizAllowanceItemById(bizAllowanceItem.getId());
        if (old == null)
        {
            throw new ServiceException("其他酬金明细不存在, id=" + bizAllowanceItem.getId());
        }
        writeGuard.lockDraftOrAbsent(old.getUserId(), old.getSemester());
        payCalcService.assertAllowanceEditable(old.getUserId(), old.getSemester());
        bizAllowanceItem.setUserId(old.getUserId());
        bizAllowanceItem.setSemester(old.getSemester());
        recalcAmount(bizAllowanceItem);
        bizAllowanceItem.setUpdateTime(DateUtils.getNowDate());
        return requireWritten(bizAllowanceItemMapper.updateIfSummaryDraft(bizAllowanceItem));
    }

    /**
     * 批量删除其他酬金明细
     *
     * @param ids 需要删除的其他酬金明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizAllowanceItemByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择要删除的其他酬金明细");
        }
        int rows = 0;
        for (Long id : Arrays.stream(ids).distinct().toList())
        {
            rows += deleteBizAllowanceItemById(id);
        }
        return rows;
    }

    /**
     * 删除其他酬金明细信息
     *
     * @param id 其他酬金明细主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBizAllowanceItemById(Long id)
    {
        BizAllowanceItem old = bizAllowanceItemMapper.selectBizAllowanceItemById(id);
        if (old == null)
        {
            throw new ServiceException("其他酬金明细不存在, id=" + id);
        }
        writeGuard.lockDraftOrAbsent(old.getUserId(), old.getSemester());
        payCalcService.assertAllowanceEditable(old.getUserId(), old.getSemester());
        return requireWritten(bizAllowanceItemMapper.deleteByIdIfSummaryDraft(id));
    }

    private int requireWritten(int rows)
    {
        if (rows != 1)
        {
            throw new ServiceException("其他酬金明细或汇总状态已变化，请刷新后重试");
        }
        return rows;
    }

    /**
     * 按 fee_type 策略计算金额（A~G 已全量注册，D 代阅卷 2026-09-10 起按第十五条5 启用）
     */
    private void recalcAmount(BizAllowanceItem item)
    {
        AllowanceCalcStrategy strategy = allowanceStrategyFactory.get(item.getFeeType());
        if (strategy == null)
        {
            throw new ServiceException("酬金类型未启用: " + item.getFeeType());
        }
        item.setAmount(strategy.calculate(item));
        // 第十五条1(2)：≥20 人应单独开班走 G1（理论课）路线，本项归零。
        // 静默归零教师无从判断是漏报还是规则如此，故落 remark 告警
        // （2026-09-10 统一原则：截断与降级必须告警）
        if ("A".equals(item.getFeeType()) && item.getFeeSubtype() != null
                && item.getFeeSubtype().contains("自学")
                && item.getStudentCount() != null && item.getStudentCount() >= 20)
        {
            item.setRemark(SELF_STUDY_ZERO_WARNING);
        }
        else if (SELF_STUDY_ZERO_WARNING.equals(item.getRemark()))
        {
            item.setRemark(null);
        }
    }
}
