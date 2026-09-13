package com.workload.system.calc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DateUtils;
import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.calc.strategy.WorkloadCalcStrategy;
import com.workload.system.domain.BizPayRecord;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import com.workload.system.service.ISysUserService;
import com.workload.common.core.domain.entity.SysUser;

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

    @Autowired
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    @Autowired
    private BizTeacherProfileMapper bizTeacherProfileMapper;

    @Autowired
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    @Autowired
    private CalcStrategyFactory calcStrategyFactory;

    @Autowired
    private SummaryCalcService summaryCalcService;

    @Autowired
    private PayCalcService payCalcService;

    @Autowired
    private ISysUserService sysUserService;

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
        int affected = bizWorkloadItemMapper.updateCalculationIfEditable(item,
                ITEM_STATUS_CONFIRMED, WorkloadSummaryStatus.DRAFT);
        if (affected != 1)
        {
            throw new ServiceException("明细或汇总状态已变化，请刷新后重试");
        }
        return value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recalcAll(Long userId, String semester)
    {
        // userId 合法性校验：教师档案不存在则快速失败，避免任意 userId 生成零值脏数据
        if (bizTeacherProfileMapper.selectBizTeacherProfileByUserId(userId) == null)
        {
            throw new ServiceException("教师档案不存在，无法重算");
        }
        // 明细 → 汇总 → 酬金 三步同事务：recalcItems 为自调用但已处本事务内；
        // recalcSummary/recalcPay 为跨 Bean 调用，其 @Transactional(REQUIRED) 加入本事务
        int itemCount = recalcItems(userId, semester);
        BizWorkloadSummary summary = summaryCalcService.recalcSummary(userId, semester, true);
        BizPayRecord payRecord = payCalcService.recalcPay(userId, semester);
        Map<String, Object> data = new HashMap<>();
        data.put("recalcItemCount", itemCount);
        data.put("summary", summary);
        data.put("payRecord", payRecord);
        data.put("unconfirmedCount", summaryCalcService.countUnconfirmed(userId, semester));
        return data;
    }

    @Override
    public Map<String, Object> recalcAllBatch(List<Long> userIds, String semester)
    {
        // 刻意不加 @Transactional：本方法只做编排，事务边界必须落在每位教师身上，
        // 否则任一教师失败会把已成功的教师一起回滚。
        List<Long> targets = (userIds == null || userIds.isEmpty())
                ? bizWorkloadItemMapper.selectUserIdsBySemester(semester)
                : userIds.stream().filter(java.util.Objects::nonNull).distinct().toList();

        // 通过代理自调用，让每位教师的 recalcAll 各起一个新事务（exposeProxy=true，见 ApplicationConfig）
        WorkloadCalcService proxy = (WorkloadCalcService) AopContext.currentProxy();

        List<Map<String, Object>> failures = new ArrayList<>();
        int successCount = 0;
        for (Long uid : targets)
        {
            try
            {
                proxy.recalcAll(uid, semester);
                successCount++;
            }
            catch (Exception e)
            {
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("userId", uid);
                fail.put("userName", resolveUserLabel(uid));
                fail.put("reason", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                failures.add(fail);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("semester", semester);
        data.put("total", targets.size());
        data.put("successCount", successCount);
        data.put("failCount", failures.size());
        data.put("failures", failures);
        return data;
    }

    /**
     * 失败明细里的教师标识：优先「姓名(工号)」，查不到用户则退回裸 userId，
     * 让教务能直接定位到人，而不是拿着一串数字回头翻库。
     */
    private String resolveUserLabel(Long userId)
    {
        try
        {
            SysUser user = sysUserService.selectUserById(userId);
            if (user == null)
            {
                return "userId=" + userId;
            }
            return user.getNickName() + "(" + user.getUserName() + ")";
        }
        catch (Exception e)
        {
            return "userId=" + userId;
        }
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
     * 明细已核对或所在学期汇总已进入审批流程 -> 拒绝修改
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
                .anyMatch(s -> WorkloadSummaryStatus.isWriteFrozen(s.getStatus()));
        if (locked)
        {
            throw new ServiceException("学期汇总已锁定，禁止修改明细");
        }
    }
}
