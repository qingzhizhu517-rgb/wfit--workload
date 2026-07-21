package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizWlThesis;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlThesisMapper;

/**
 * G5 毕业论文(设计)计算策略：G5 = R5 × K5
 * 人数不截断；本科 R5&gt;8 或专科 R5&gt;15 置 is_over_limit=1 触发院长审批
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component("thesisCalcStrategy")
public class ThesisCalcStrategy extends AbstractWorkloadCalcStrategy
{
    @Autowired
    private BizWlThesisMapper bizWlThesisMapper;

    @Autowired
    private RuleParamService ruleParamService;

    @Override
    public String getTypeCode()
    {
        return "G5";
    }

    @Override
    public BigDecimal calculate(BizWorkloadItem item)
    {
        BizWlThesis detail = loadDetail(item.getId());
        return scale(mul(num(detail.getR5()), coef(detail.getK5())));
    }

    @Override
    public void afterCalculated(BizWorkloadItem item, BigDecimal value)
    {
        BizWlThesis detail = loadDetail(item.getId());
        String level = StringUtils.hasText(detail.getEducationLevel()) ? detail.getEducationLevel()
                : item.getEducationLevel();
        BigDecimal r5 = num(detail.getR5());
        boolean overLimit;
        if (level != null && level.contains("专"))
        {
            overLimit = r5.compareTo(ruleParamService.get("CAP_R5_JUNIOR", new BigDecimal("15"))) > 0;
        }
        else
        {
            overLimit = r5.compareTo(ruleParamService.get("APPROVAL_R5_BACHELOR", new BigDecimal("8"))) > 0;
        }
        item.setIsOverLimit(overLimit ? 1 : 0);
    }

    private BizWlThesis loadDetail(Long itemId)
    {
        BizWlThesis detail = bizWlThesisMapper.selectBizWlThesisByItemId(itemId);
        if (detail == null)
        {
            throw new ServiceException("G5毕业论文明细缺失, itemId=" + itemId);
        }
        return detail;
    }
}
