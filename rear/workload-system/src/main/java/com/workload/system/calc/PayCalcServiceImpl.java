package com.workload.system.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DateUtils;
import com.workload.system.domain.BizAllowanceItem;
import com.workload.system.domain.BizPayRecord;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.mapper.BizAllowanceItemMapper;
import com.workload.system.mapper.BizPayRecordMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

/**
 * 酬金汇总计算服务实现
 *
 * course_hour_pay = 汇总绩效；other_pay_total = Σ 有效其他酬金；total_pay 四舍五入取整
 *
 * @author wflg
 * @date 2026-07-21
 */
@Service
public class PayCalcServiceImpl implements PayCalcService
{
    /** 汇总状态：已锁定（酬金随之定稿） */
    private static final int SUMMARY_STATUS_LOCKED = 3;

    @Autowired
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    @Autowired
    private BizAllowanceItemMapper bizAllowanceItemMapper;

    @Autowired
    private BizPayRecordMapper bizPayRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizPayRecord recalcPay(Long userId, String semester)
    {
        BizWorkloadSummary summary = findSummary(userId, semester);
        if (summary == null)
        {
            throw new ServiceException("学期汇总不存在，请先重算汇总");
        }
        if (summary.getStatus() != null && summary.getStatus() == SUMMARY_STATUS_LOCKED)
        {
            throw new ServiceException("学期汇总已锁定，酬金已定稿");
        }

        BigDecimal courseHourPay = summary.getPerformancePay() == null ? BigDecimal.ZERO
                : summary.getPerformancePay();

        BizAllowanceItem allowanceQuery = new BizAllowanceItem();
        allowanceQuery.setUserId(userId);
        allowanceQuery.setSemester(semester);
        allowanceQuery.setStatus(1);
        List<BizAllowanceItem> allowances = bizAllowanceItemMapper.selectBizAllowanceItemList(allowanceQuery);
        BigDecimal otherTotal = allowances.stream()
                .map(a -> a.getAmount() == null ? BigDecimal.ZERO : a.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BizPayRecord record = findPayRecord(userId, semester);
        boolean isNew = record == null;
        if (isNew)
        {
            record = new BizPayRecord();
            record.setUserId(userId);
            record.setSemester(semester);
            record.setStatus(0);
        }
        record.setSummaryId(summary.getId());
        record.setCourseHourPay(courseHourPay.setScale(2, RoundingMode.HALF_UP));
        record.setOtherPayTotal(otherTotal.setScale(2, RoundingMode.HALF_UP));
        record.setTotalPay(courseHourPay.add(otherTotal).setScale(0, RoundingMode.HALF_UP).longValue());

        if (isNew)
        {
            record.setCreateTime(DateUtils.getNowDate());
            try
            {
                bizPayRecordMapper.insertBizPayRecord(record);
            }
            catch (DuplicateKeyException e)
            {
                // 并发撞 uk_user_sem 唯一键时降级为更新，消除 check-then-act 竞态
                BizPayRecord existed = findPayRecord(userId, semester);
                if (existed == null)
                {
                    throw new ServiceException("酬金记录保存失败，请重试");
                }
                record.setId(existed.getId());
                record.setCreateTime(existed.getCreateTime());
                record.setUpdateTime(DateUtils.getNowDate());
                bizPayRecordMapper.updateBizPayRecord(record);
            }
        }
        else
        {
            record.setUpdateTime(DateUtils.getNowDate());
            bizPayRecordMapper.updateBizPayRecord(record);
        }
        return record;
    }

    @Override
    public void assertAllowanceEditable(Long userId, String semester)
    {
        BizWorkloadSummary summary = findSummary(userId, semester);
        if (summary != null && summary.getStatus() != null && summary.getStatus() == SUMMARY_STATUS_LOCKED)
        {
            throw new ServiceException("学期汇总已锁定，其他酬金禁止修改");
        }
    }

    private BizWorkloadSummary findSummary(Long userId, String semester)
    {
        BizWorkloadSummary query = new BizWorkloadSummary();
        query.setUserId(userId);
        query.setSemester(semester);
        List<BizWorkloadSummary> list = bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(query);
        return list.isEmpty() ? null : list.get(0);
    }

    private BizPayRecord findPayRecord(Long userId, String semester)
    {
        BizPayRecord query = new BizPayRecord();
        query.setUserId(userId);
        query.setSemester(semester);
        List<BizPayRecord> list = bizPayRecordMapper.selectBizPayRecordList(query);
        return list.isEmpty() ? null : list.get(0);
    }
}
