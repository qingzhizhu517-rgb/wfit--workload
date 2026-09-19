package com.workload.system.service.impl;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DateUtils;
import com.workload.common.utils.SecurityUtils;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.domain.BizCoefficientAdjustment;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizCoefficientAdjustmentMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.service.ICoefficientAdjustmentService;

/**
 * G1/G2 系数调整申请Service业务层处理
 *
 * @author wflg
 * @date 2026-09-12
 */
@Service
public class CoefficientAdjustmentServiceImpl implements ICoefficientAdjustmentService
{
    /**
     * 可申请调整的系数白名单：G1 的 C1/K1/Q1/Q2/N 与 G2 的 K/C2/Q1/Q2。
     * 其它 category/factor 组合一律拒绝。
     */
    private static final Map<String, Set<String>> ALLOWED_FACTORS = Map.of(
            "G1", Set.of("C1", "K1", "Q1", "Q2", "N"),
            "G2", Set.of("K", "C2", "Q1", "Q2"));

    @Autowired
    private BizCoefficientAdjustmentMapper adjustmentMapper;

    @Autowired
    private BizWorkloadItemMapper itemMapper;

    @Autowired
    private WorkloadWriteGuard workloadWriteGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(CoefficientAdjustmentRequest request)
    {
        if (request == null)
        {
            throw new ServiceException("申请内容不能为空");
        }
        // 1) 白名单校验（先于任何库操作，避免非法组合触库）
        assertWhitelisted(request.getCategory(), request.getFactorCode());
        // 2) 申请值必须为正
        if (request.getRequestedValue() == null || request.getRequestedValue().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("申请系数值必须大于 0");
        }
        // 3) 理由非空
        if (!StringUtils.hasText(request.getReason()))
        {
            throw new ServiceException("申请理由不能为空");
        }
        // 4) 明细存在
        if (request.getItemId() == null)
        {
            throw new ServiceException("缺少工作量明细ID");
        }
        BizWorkloadItem item = itemMapper.selectBizWorkloadItemById(request.getItemId());
        if (item == null)
        {
            throw new ServiceException("工作量明细不存在, itemId=" + request.getItemId());
        }
        // 5) 明细类别须与申请类别一致
        if (!request.getCategory().equals(item.getItemType()))
        {
            throw new ServiceException("申请类别与明细类别不一致, itemId=" + request.getItemId());
        }
        // 6) 汇总冻结门（含「审批流程」提示）——事务内 SELECT ... FOR UPDATE
        workloadWriteGuard.lockDraftOrAbsent(item.getUserId(), item.getSemester());
        // 7) 同一 item+factor 只允许一条待审（唯一键为最终防线，此处先查给出友好错误）
        if (adjustmentMapper.countPendingByItemFactor(request.getItemId(), request.getFactorCode()) > 0)
        {
            throw new ServiceException("该系数已有待审申请，请勿重复提交");
        }
        // 8) old_value 由服务端读取当前值，忽略客户端传入
        BigDecimal currentValue = adjustmentMapper.selectCurrentFactorValue(
                request.getCategory(), request.getFactorCode(), request.getItemId());

        BizCoefficientAdjustment adjustment = new BizCoefficientAdjustment();
        adjustment.setUserId(item.getUserId());
        adjustment.setSemester(item.getSemester());
        adjustment.setTaskId(item.getTaskId());
        adjustment.setItemId(request.getItemId());
        adjustment.setCategory(request.getCategory());
        adjustment.setFactorCode(request.getFactorCode());
        adjustment.setOldValue(currentValue);
        adjustment.setRequestedValue(request.getRequestedValue());
        adjustment.setReason(request.getReason());
        adjustment.setAttachmentUrl(request.getAttachmentUrl());
        adjustment.setStatus(BizCoefficientAdjustment.STATUS_PENDING);
        // 从 item 读不到版本则记 0
        adjustment.setBaseCalculationVersion(
                item.getCalculationVersion() != null ? item.getCalculationVersion() : 0L);
        adjustment.setCreateTime(DateUtils.getNowDate());

        adjustmentMapper.insertCoefficientAdjustment(adjustment);
        return adjustment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, String reviewReason)
    {
        BizCoefficientAdjustment adjustment = requirePending(id);
        int affected = adjustmentMapper.markApprovedIfPending(
                id, SecurityUtils.getUserId(), reviewReason, adjustment.getBaseCalculationVersion());
        if (affected == 0)
        {
            throw new ServiceException("申请状态已变更或计算版本已更新，通过失败（并发冲突）");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reviewReason)
    {
        if (!StringUtils.hasText(reviewReason))
        {
            throw new ServiceException("驳回理由不能为空");
        }
        BizCoefficientAdjustment adjustment = requirePending(id);
        int affected = adjustmentMapper.markRejectedIfPending(
                id, SecurityUtils.getUserId(), reviewReason, adjustment.getBaseCalculationVersion());
        if (affected == 0)
        {
            throw new ServiceException("申请状态已变更或计算版本已更新，驳回失败（并发冲突）");
        }
    }

    private BizCoefficientAdjustment requirePending(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("缺少申请ID");
        }
        BizCoefficientAdjustment adjustment = adjustmentMapper.selectCoefficientAdjustmentById(id);
        if (adjustment == null)
        {
            throw new ServiceException("系数调整申请不存在, id=" + id);
        }
        return adjustment;
    }

    private void assertWhitelisted(String category, String factorCode)
    {
        Set<String> allowed = category == null ? null : ALLOWED_FACTORS.get(category);
        if (allowed == null || factorCode == null || !allowed.contains(factorCode))
        {
            throw new ServiceException("不允许调整的系数: category=" + category + ", factor=" + factorCode);
        }
    }
}
