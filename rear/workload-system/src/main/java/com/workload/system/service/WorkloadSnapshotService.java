package com.workload.system.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.SecurityUtils;
import com.workload.system.domain.BizWorkloadCalcSnapshot;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.domain.vo.FactorFormulaVo.FactorVo;
import com.workload.system.mapper.BizWorkloadCalcSnapshotMapper;

/**
 * 不可变工作量计算快照服务。
 *
 * <p>每次成功核算固化一份 append-only 快照：把公式、因子取值、因子来源、规则版本与结果规范化后
 * 序列化，并以 SHA-256 生成内容哈希。相同有序因子在相同规则版本下得到稳定哈希，便于追溯与去重。</p>
 *
 * <p>本服务只负责写快照。主表 {@code calculation_version / last_calculated_at} 的同步回写由
 * 后续任务在同一事务内完成，故 {@link #capture} 使用
 * {@link Propagation#MANDATORY}，必须由调用方开启事务。</p>
 */
@Component
public class WorkloadSnapshotService
{
    private final BizWorkloadCalcSnapshotMapper snapshotMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkloadSnapshotService(BizWorkloadCalcSnapshotMapper snapshotMapper)
    {
        this.snapshotMapper = snapshotMapper;
    }

    /**
     * 固化一份计算快照并落库（仅 INSERT）。
     *
     * @param item        目标明细（须含 id）
     * @param formula     本次核算使用的公式与因子
     * @param ruleVersion 规则版本标识
     * @return 已落库的快照（含生成的哈希与版本号）
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public BizWorkloadCalcSnapshot capture(BizWorkloadItem item, FactorFormulaVo formula, String ruleVersion)
    {
        if (item == null || item.getId() == null)
        {
            throw new ServiceException("缺少明细标识，无法固化计算快照");
        }
        if (formula == null)
        {
            throw new ServiceException("缺少公式因子，无法固化计算快照");
        }

        String factorJson = serialize(buildOrderedMap(formula.getFactors(), true));
        String sourceJson = serialize(buildOrderedMap(formula.getFactors(), false));
        String formulaExpression = normalizeExpression(formula.getExpression());
        BigDecimal result = formula.getResult();
        String snapshotHash = sha256(factorJson, sourceJson, formulaExpression, ruleVersion, result);

        BizWorkloadCalcSnapshot snapshot = new BizWorkloadCalcSnapshot();
        snapshot.setItemId(item.getId());
        snapshot.setCalculationVersion(nextVersion(item.getId()));
        snapshot.setFormulaExpression(formulaExpression);
        snapshot.setFactorJson(factorJson);
        snapshot.setSourceJson(sourceJson);
        snapshot.setRuleVersion(ruleVersion);
        snapshot.setResult(result);
        snapshot.setSnapshotHash(snapshotHash);
        snapshot.setCreatedBy(currentUser());
        snapshot.setCreatedAt(new Date());

        snapshotMapper.insertBizWorkloadCalcSnapshot(snapshot);
        return snapshot;
    }

    /** 版本单调递增：取快照表已有最大版本 + 1，无历史则从 1 开始。 */
    private Long nextVersion(Long itemId)
    {
        Long max = snapshotMapper.selectMaxVersionByItemId(itemId);
        return (max == null ? 0L : max) + 1L;
    }

    /** 按因子列表顺序构建有序键值映射；{@code useValue=true} 取取值，否则取来源。 */
    private Map<String, String> buildOrderedMap(List<FactorVo> factors, boolean useValue)
    {
        Map<String, String> ordered = new LinkedHashMap<>();
        if (factors == null)
        {
            return ordered;
        }
        for (FactorVo factor : factors)
        {
            if (factor == null || factor.getKey() == null)
            {
                continue;
            }
            ordered.put(factor.getKey(), useValue ? stringifyValue(factor.getValue()) : factor.getSource());
        }
        return ordered;
    }

    /** 数值一律以字符串序列化，避免浮点漂移；BigDecimal 用 plain string。 */
    private String stringifyValue(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof BigDecimal)
        {
            return ((BigDecimal) value).toPlainString();
        }
        return String.valueOf(value);
    }

    private String serialize(Map<String, String> ordered)
    {
        try
        {
            return objectMapper.writeValueAsString(ordered);
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("序列化计算快照失败：" + e.getMessage());
        }
    }

    /** 把公式中的乘号统一为全角「 × 」，规范空白。 */
    private String normalizeExpression(String expression)
    {
        if (expression == null || expression.isEmpty())
        {
            return expression;
        }
        return expression.replaceAll("\\s*[*×＊·]\\s*", " × ").trim();
    }

    /** 覆盖规范化后的 factorJson+sourceJson+formula+ruleVersion+result，生成稳定 SHA-256。 */
    private String sha256(String factorJson, String sourceJson, String formulaExpression,
            String ruleVersion, BigDecimal result)
    {
        StringBuilder payload = new StringBuilder();
        payload.append(factorJson == null ? "" : factorJson).append('\n');
        payload.append(sourceJson == null ? "" : sourceJson).append('\n');
        payload.append(formulaExpression == null ? "" : formulaExpression).append('\n');
        payload.append(ruleVersion == null ? "" : ruleVersion).append('\n');
        payload.append(result == null ? "" : result.toPlainString());

        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash)
            {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new ServiceException("生成计算快照哈希失败：" + e.getMessage());
        }
    }

    /** 安全获取当前用户名；无安全上下文（如系统任务/单测）时回落为 system。 */
    private String currentUser()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "system";
        }
    }
}
