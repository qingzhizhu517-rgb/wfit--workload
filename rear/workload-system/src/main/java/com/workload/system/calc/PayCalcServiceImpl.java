package com.workload.system.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.common.constant.HttpStatus;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DateUtils;
import com.workload.system.domain.BizAllowanceItem;
import com.workload.system.domain.BizPayRecord;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.mapper.BizAllowanceItemMapper;
import com.workload.system.mapper.BizPayRecordMapper;
import com.workload.system.mapper.BizTeacherProfileMapper;
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
    @Autowired
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    @Autowired
    private BizAllowanceItemMapper bizAllowanceItemMapper;

    @Autowired
    private BizPayRecordMapper bizPayRecordMapper;

    @Autowired
    private BizTeacherProfileMapper bizTeacherProfileMapper;

    @Autowired
    private WorkloadWriteGuard writeGuard;

    @Autowired private SettlementScope settlementScope = new SettlementScope();
    @Autowired private SettlementSnapshotService settlementSnapshots;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizPayRecord recalcPay(Long userId, String semester)
    {
        return calculate(userId,semester,null,true);
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public BizPayRecord recalcPayWithEvidence(Long userId,String semester,SettlementEvidence.SummaryEvidence evidence) {
        if(!settlementScope.required(semester)||evidence==null) throw new ServiceException("结算来源上下文缺失");
        return calculate(userId,semester,evidence,false);
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public BizPayRecord recalcPayFromSummary(Long userId,String semester) {
        return calculate(userId,semester,null,false);
    }

    private BizPayRecord calculate(Long userId,String semester,SettlementEvidence.SummaryEvidence evidence,boolean independent) {
        writeGuard.lockDraftOrAbsent(userId, semester);
        // 写前锁定读：FOR UPDATE 行锁读取汇总并复核冻结状态，关闭"初查草稿 → 审批置待审
        // → 酬金仍按旧状态写入"的并发窗口。锁定顺序固定为 summary → allowance → pay_record，
        // 审批只更新 summary，避免逆序死锁。
        BizWorkloadSummary summary = bizWorkloadSummaryMapper.selectForPayUpdate(userId, semester);
        if (summary == null)
        {
            throw new ServiceException("学期汇总不存在，请先重算汇总");
        }
        if(settlementSnapshots!=null) settlementSnapshots.assertRecalculationBoundary(summary);
        // Only genuine legacy periods may consult the current profile here; saved settlements never downgrade.
        if (!settlementScope.required(semester) && bizTeacherProfileMapper.selectBizTeacherProfileByUserId(userId) == null)
        {
            throw new ServiceException("教师档案不存在，无法重算");
        }
        if (WorkloadSummaryStatus.isWriteFrozen(summary.getStatus()))
        {
            throw new ServiceException("学期汇总已进入审批流程，酬金已冻结", HttpStatus.CONFLICT);
        }

        if (Integer.valueOf(1).equals(summary.getNeedsRecalculation()))
        {
            throw new ServiceException("源数据已变更，请先重新核算汇总和酬金");
        }

        // Independent pay cannot turn a historical amount into a genuine summary calculation.
        // Summary orchestration supplies a persisted calculation time before its first pay/version write.
        if (summary.getLastCalculatedAt() == null || independent
                && (summary.getDataVersion() == null || summary.getDataVersion() <= 0))
            throw new ServiceException("历史汇总尚未完成新版计算，请先重新核算汇总",409);

        if(settlementScope.required(semester)) {
            if(evidence==null) evidence=settlementSnapshots.savedSummary(summary);
            SettlementEvidence.verifySummary(evidence);
            if(!userId.equals(evidence.userId())||!semester.equals(evidence.semester())) throw com.workload.system.service.SnapshotIntegrity.failure("结算上下文归属不符");
        }
        BigDecimal courseHourPay = summary.getPerformancePay() == null ? BigDecimal.ZERO
                : summary.getPerformancePay();

        BizAllowanceItem allowanceQuery = new BizAllowanceItem();
        allowanceQuery.setUserId(userId);
        allowanceQuery.setSemester(semester);
        allowanceQuery.setStatus(1);
        allowanceQuery.getParams().put("currentRead", true);
        List<BizAllowanceItem> allowances = bizAllowanceItemMapper.selectBizAllowanceItemList(allowanceQuery);
        BigDecimal otherTotal = allowances.stream()
                .map(a -> a.getAmount() == null ? BigDecimal.ZERO : a.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BizPayRecord record = findPayRecord(userId, semester);
        if(independent && settlementScope.required(semester)) settlementSnapshots.assertPreviousPay(summary,record);
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
                record.setStatus(existed.getStatus());
                updateDraftRecord(record);
            }
        }
        else
        {
            updateDraftRecord(record);
        }
        if(settlementScope.required(semester)) {settlementSnapshots.append(summary,record,evidence,allowances);settlementSnapshots.decorate(record);}
        if(bizWorkloadSummaryMapper.advanceDataVersion(summary.getId())!=1)
            throw new ServiceException("汇总版本推进失败，请重新核算",409);
        return record;
    }

    @Override @Transactional(readOnly=true) public BizPayRecord currentPay(Long userId,String semester){var record=findPayRecord(userId,semester,false);if(settlementSnapshots!=null)settlementSnapshots.decorate(record);return record;}

    private void updateDraftRecord(BizPayRecord record)
    {
        record.setUpdateTime(DateUtils.getNowDate());
        if (bizPayRecordMapper.updateIfSummaryDraft(record) != 1)
        {
            throw new ServiceException("学期汇总或酬金状态已变化，酬金未保存，请刷新后重试");
        }
    }

    @Override
    public void assertAllowanceEditable(Long userId, String semester)
    {
        BizWorkloadSummary summary = findSummary(userId, semester);
        if (summary != null && WorkloadSummaryStatus.isWriteFrozen(summary.getStatus()))
        {
            throw new ServiceException("学期汇总已进入审批流程，其他酬金禁止修改", HttpStatus.CONFLICT);
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

    private BizPayRecord findPayRecord(Long userId, String semester) {return findPayRecord(userId,semester,true);}

    private BizPayRecord findPayRecord(Long userId, String semester,boolean currentRead)
    {
        BizPayRecord query = new BizPayRecord();
        query.setUserId(userId);
        query.setSemester(semester);
        query.getParams().put("currentRead", currentRead);
        List<BizPayRecord> list = bizPayRecordMapper.selectBizPayRecordList(query);
        return list.isEmpty() ? null : list.get(0);
    }
}
