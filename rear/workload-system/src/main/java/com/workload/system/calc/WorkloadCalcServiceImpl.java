package com.workload.system.calc;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DateUtils;
import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.calc.strategy.WorkloadCalcStrategy;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

/**
 * 工作量明细计算服务实现
 *
 * @author wflg
 * @date 2026-07-21
 */
@Service
public class WorkloadCalcServiceImpl implements WorkloadCalcService
{
    /** 明细状态：已核对（冻结） */
    private static final int ITEM_STATUS_CONFIRMED = 1;

    /** 汇总状态：已锁定 */
    private static final int SUMMARY_STATUS_LOCKED = 3;

    @Autowired
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    @Autowired
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    @Autowired
    private CalcStrategyFactory calcStrategyFactory;

    @Override
    public BigDecimal recalcItem(Long itemId)
    {
        BizWorkloadItem item = bizWorkloadItemMapper.selectBizWorkloadItemById(itemId);
        if (item == null)
        {
            throw new ServiceException("工作量明细不存在, id=" + itemId);
        }
        assertEditable(item);
        WorkloadCalcStrategy strategy = calcStrategyFactory.get(item.getItemType());
        if (strategy == null)
        {
            // G8/G9 等无策略类别：金额直录，不重算
            return item.getCalculatedWorkload();
        }
        BigDecimal value = strategy.calculate(item);
        item.setCalculatedWorkload(value);
        strategy.afterCalculated(item, value);
        item.setUpdateTime(DateUtils.getNowDate());
        bizWorkloadItemMapper.updateBizWorkloadItem(item);
        return value;
    }

    @Override
    public int recalcItems(Long userId, String semester)
    {
        BizWorkloadItem query = new BizWorkloadItem();
        query.setUserId(userId);
        query.setSemester(semester);
        List<BizWorkloadItem> items = bizWorkloadItemMapper.selectBizWorkloadItemList(query);
        int count = 0;
        for (BizWorkloadItem item : items)
        {
            if (item.getStatus() != null && item.getStatus() == ITEM_STATUS_CONFIRMED)
            {
                continue;
            }
            recalcItem(item.getId());
            count++;
        }
        return count;
    }

    @Override
    public void onDetailDeleted(Long itemId)
    {
        BizWorkloadItem item = bizWorkloadItemMapper.selectBizWorkloadItemById(itemId);
        if (item == null)
        {
            return;
        }
        item.setCalculatedWorkload(BigDecimal.ZERO);
        item.setIsOverLimit(0);
        item.setUpdateTime(DateUtils.getNowDate());
        bizWorkloadItemMapper.updateBizWorkloadItem(item);
    }

    @Override
    public void assertEditable(Long itemId)
    {
        BizWorkloadItem item = bizWorkloadItemMapper.selectBizWorkloadItemById(itemId);
        if (item == null)
        {
            throw new ServiceException("工作量明细不存在, id=" + itemId);
        }
        assertEditable(item);
    }

    /**
     * 明细已核对或所在学期汇总已锁定 -> 拒绝修改
     */
    private void assertEditable(BizWorkloadItem item)
    {
        if (item.getStatus() != null && item.getStatus() == ITEM_STATUS_CONFIRMED)
        {
            throw new ServiceException("明细已核对，系数已冻结，请先取消核对");
        }
        BizWorkloadSummary query = new BizWorkloadSummary();
        query.setUserId(item.getUserId());
        query.setSemester(item.getSemester());
        List<BizWorkloadSummary> summaries = bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(query);
        boolean locked = summaries.stream()
                .anyMatch(s -> s.getStatus() != null && s.getStatus() == SUMMARY_STATUS_LOCKED);
        if (locked)
        {
            throw new ServiceException("学期汇总已锁定，禁止修改明细");
        }
    }
}
