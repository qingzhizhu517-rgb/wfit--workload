package com.workload.system.calc.rule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.workload.common.core.redis.RedisCache;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWorkloadRule;
import com.workload.system.mapper.BizWorkloadRuleMapper;

@Service
public class RuleParamServiceImpl implements RuleParamService
{
    public static final String CACHE_PREFIX = "wl_rule:";
    @Autowired private BizWorkloadRuleMapper bizWorkloadRuleMapper;
    @Autowired private RedisCache redisCache;

    @Override public BigDecimal get(String code) { return get(code, LocalDate.now()); }
    @Override public BigDecimal get(String code, BigDecimal fallback) { return get(code, fallback, LocalDate.now()); }

    @Override public BigDecimal get(String code, LocalDate asOfDate)
    {
        BigDecimal value = doGet(code, asOfDate);
        if (value == null) throw new ServiceException("规则参数缺失: " + code + "，日期 " + asOfDate);
        return value;
    }
    @Override public BigDecimal get(String code, BigDecimal fallback, LocalDate asOfDate)
    {
        BigDecimal value = doGet(code, asOfDate);
        return value == null ? fallback : value;
    }
    @Override public void evict(String code)
    {
        if (code == null || code.isEmpty()) return;
        // 世代键使提交前已在途的旧值回填不可达；旧世代按 TTL 回收，覆盖全部日期。
        AfterCommit.run(() -> {
            redisCache.setCacheObject(CACHE_PREFIX + code + ":generation", UUID.randomUUID().toString());
            redisCache.deleteObject(CACHE_PREFIX + code); // 迁移前兼容键
        });
    }
    private BigDecimal doGet(String code, LocalDate asOfDate)
    {
        Objects.requireNonNull(asOfDate, "asOfDate");
        boolean transactional = TransactionSynchronizationManager.isActualTransactionActive();
        String cacheKey = null;
        if (!transactional)
        {
            String generation = redisCache.getCacheObject(CACHE_PREFIX + code + ":generation");
            cacheKey = CACHE_PREFIX + code + ":" + (generation == null ? "0" : generation) + ":" + asOfDate;
            BigDecimal cached = redisCache.getCacheObject(cacheKey);
            if (cached != null) return cached;
        }
        BizWorkloadRule query = new BizWorkloadRule(); query.setRuleCode(code); query.setStatus(1);
        BizWorkloadRule row = EffectiveDateResolver.select(bizWorkloadRuleMapper.selectBizWorkloadRuleList(query),
                asOfDate, BizWorkloadRule::getEffectiveFrom, BizWorkloadRule::getEffectiveTo, code);
        BigDecimal value = row == null ? null : row.getRuleValue();
        // 事务快照中的旧值/未提交值永不进入共享缓存。
        if (!transactional && value != null) redisCache.setCacheObject(cacheKey, value, 24, TimeUnit.HOURS);
        return value;
    }
}
