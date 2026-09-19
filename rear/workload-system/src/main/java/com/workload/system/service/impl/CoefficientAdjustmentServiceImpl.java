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
import com.workload.system.calc.WorkloadCalcService;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.domain.BizCoefficientAdjustment;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizCoefficientAdjustmentMapper;
import com.workload.system.mapper.BizWlPracticeMapper;
import com.workload.system.mapper.BizWlTheoryMapper;
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

    @Autowired
    private BizWlTheoryMapper theoryMapper;

    @Autowired
    private BizWlPracticeMapper practiceMapper;

    @Autowired
    private WorkloadCalcService workloadCalcService;

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
        // 顺序（同一事务，任一步失败整体回滚）：
        //   a. 读申请并确认仍为待审；
        //   b. 按白名单原子应用被申请因子到 G 子表（JOIN 主表校验 baseVersion/明细未核对/汇总非草稿）；
        //      —— 只更新因子，不动主表 calculation_version；
        //   c. recalcItem：用新因子重算，产出 APPROVED_OVERRIDE 新快照并把主表版本 +1（原子条件更新）；
        //   d. 条件置 APPROVED（仍待审且 base_calculation_version 未变才成功）。
        BizCoefficientAdjustment adjustment = requirePending(id);
        Long baseVersion = adjustment.getBaseCalculationVersion();
        // b. 原子应用因子；先更新因子（不改版本），再 recalcItem（版本 +1），保证版本序一致。
        applyRequestedFactor(adjustment, baseVersion);
        // c. 重算：recalcItem 内部读回被改写的 G 子表新值，落新快照并原子更新主表版本。
        workloadCalcService.recalcItem(adjustment.getItemId());
        // d. 条件置 APPROVED。
        int affected = adjustmentMapper.markApprovedIfPending(
                id, SecurityUtils.getUserId(), reviewReason, baseVersion);
        if (affected == 0)
        {
            throw new ServiceException("申请状态已变更或计算版本已更新，通过失败（并发冲突）");
        }
    }

    /**
     * 按 category+factorCode 走 Java switch 白名单，分发到明确的 G 子表条件更新方法。
     * <p>严禁用 {@code ${}} 拼列名；未命中白名单（即便库中记录被绕过 submit 写入）一律拒绝。
     * 条件更新影响行数不为 1（版本变化/明细已核对/汇总已进入审批）时抛并发冲突并回滚。</p>
     */
    private void applyRequestedFactor(BizCoefficientAdjustment adjustment, Long baseVersion)
    {
        Long itemId = adjustment.getItemId();
        BigDecimal value = adjustment.getRequestedValue();
        String category = adjustment.getCategory();
        String factor = adjustment.getFactorCode();
        String key = category + ":" + factor;
        int affected = switch (key)
        {
            case "G1:C1" -> theoryMapper.updateC1IfVersion(itemId, value, baseVersion);
            case "G1:K1" -> theoryMapper.updateK1IfVersion(itemId, value, baseVersion);
            case "G1:Q1" -> theoryMapper.updateQ1IfVersion(itemId, value, baseVersion);
            case "G1:Q2" -> theoryMapper.updateQ2IfVersion(itemId, value, baseVersion);
            case "G1:N" -> theoryMapper.updateNIfVersion(itemId, value, baseVersion);
            case "G2:K" -> practiceMapper.updateKIfVersion(itemId, value, baseVersion);
            case "G2:C2" -> practiceMapper.updateC2IfVersion(itemId, value, baseVersion);
            case "G2:Q1" -> practiceMapper.updateQ1IfVersion(itemId, value, baseVersion);
            case "G2:Q2" -> practiceMapper.updateQ2IfVersion(itemId, value, baseVersion);
            default -> throw new ServiceException(
                    "不允许调整的系数: category=" + category + ", factor=" + factor);
        };
        if (affected != 1)
        {
            throw new ServiceException("明细或汇总状态已变化（或计算版本已更新），系数应用失败（并发冲突）");
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
        // 非待审直接拒绝，避免在最终条件更新前产生任何副作用（因子应用/重算）
        if (adjustment.getStatus() == null || adjustment.getStatus() != BizCoefficientAdjustment.STATUS_PENDING)
        {
            throw new ServiceException("申请不是待审状态，无法处理, id=" + id);
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
