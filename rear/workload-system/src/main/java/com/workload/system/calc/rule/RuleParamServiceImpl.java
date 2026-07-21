package com.workload.system.calc.rule;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.common.core.redis.RedisCache;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWorkloadRule;
import com.workload.system.mapper.BizWorkloadRuleMapper;

/**
 * 工作量规则参数读取服务实现（Redis 缓存 wl_rule:&lt;code&gt;，写操作失效）
 *
 * @author wflg
 * @date 2026-07-21
 */
@Service
public class RuleParamServiceImpl implements RuleParamService
{
    /** 缓存 key 前缀 */
    public static final String CACHE_PREFIX = "wl_rule:";

    @Autowired
    private BizWorkloadRuleMapper bizWorkloadRuleMapper;

    @Autowired
    private RedisCache redisCache;

    @Override
    public BigDecimal get(String ruleCode)
    {
        BigDecimal value = doGet(ruleCode);
        if (value == null)
        {
            throw new ServiceException("规则参数缺失: " + ruleCode);
        }
        return value;
    }

    @Override
    public BigDecimal get(String ruleCode, BigDecimal defaultValue)
    {
        BigDecimal value = doGet(ruleCode);
        return value == null ? defaultValue : value;
    }

    @Override
    public void evict(String ruleCode)
    {
        if (ruleCode != null && !ruleCode.isEmpty())
        {
            redisCache.deleteObject(CACHE_PREFIX + ruleCode);
        }
    }

    /**
     * 缓存优先取当期生效值；未命中回库按有效期筛选（多条取 effective_from 最新者）
     */
    private BigDecimal doGet(String ruleCode)
    {
        String cacheKey = CACHE_PREFIX + ruleCode;
        BigDecimal cached = redisCache.getCacheObject(cacheKey);
        if (cached != null)
        {
            return cached;
        }
        BizWorkloadRule query = new BizWorkloadRule();
        query.setRuleCode(ruleCode);
        query.setStatus(1);
        List<BizWorkloadRule> rules = bizWorkloadRuleMapper.selectBizWorkloadRuleList(query);
        Date now = new Date();
        BigDecimal value = rules.stream()
                .filter(r -> r.getEffectiveFrom() == null || !r.getEffectiveFrom().after(now))
                .filter(r -> r.getEffectiveTo() == null || !r.getEffectiveTo().before(now))
                .max(Comparator.comparing(BizWorkloadRule::getEffectiveFrom,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(BizWorkloadRule::getRuleValue)
                .orElse(null);
        if (value != null)
        {
            redisCache.setCacheObject(cacheKey, value);
        }
        return value;
    }
}
