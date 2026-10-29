package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import com.workload.system.calc.rule.CalculationPolicyContext;
import com.workload.system.calc.SemesterCalendar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizWlConcentratedInternship;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlConcentratedInternshipMapper;

/**
 * G6 集中实习(现场跟班)计算策略：G6 = W × min(R6, CAP_R6_MAX) × CONST_COURSE_DESIGN
 * R6&gt;20 超出部分不计并置 is_over_limit=1 触发院长审批
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component("concentratedInternshipCalcStrategy")
public class ConcentratedInternshipCalcStrategy extends AbstractWorkloadCalcStrategy
{
    private BigDecimal rule(String code, BigDecimal fallback, BizWorkloadItem item, CalculationPolicyContext context) {
        return context == null ? ruleParamService.get(code, fallback, semesterCalendar.endOf(item.getSemester())) : context.value(code, fallback);
    }

    @Autowired
    private BizWlConcentratedInternshipMapper bizWlConcentratedInternshipMapper;

    @Autowired
    private RuleParamService ruleParamService;

    @Autowired
    private SemesterCalendar semesterCalendar = new SemesterCalendar();

    @Override
    public String getTypeCode()
    {
        return "G6";
    }

    @Override
    public BigDecimal calculate(BizWorkloadItem item) { return calculate(item, null); }

    @Override
    public BigDecimal calculate(BizWorkloadItem item, CalculationPolicyContext context)
    {
        BizWlConcentratedInternship detail = loadDetail(item);
        BigDecimal cap = rule("CAP_R6_MAX", new BigDecimal("20"), item, context);
        BigDecimal r6 = num(detail.getR6()).min(cap);
        BigDecimal constant = rule("CONST_COURSE_DESIGN", new BigDecimal("0.4"), item, context);
        return scale(mul(num(detail.getW()), r6, constant));
    }

    @Override
    public void afterCalculated(BizWorkloadItem item, BigDecimal value) { afterCalculated(item, value, null); }

    @Override
    public void afterCalculated(BizWorkloadItem item, BigDecimal value, CalculationPolicyContext context)
    {
        BizWlConcentratedInternship detail = loadDetail(item);
        BigDecimal cap = rule("CAP_R6_MAX", new BigDecimal("20"), item, context);
        item.setIsOverLimit(num(detail.getR6()).compareTo(cap) > 0 ? 1 : 0);
    }

    private BizWlConcentratedInternship loadDetail(BizWorkloadItem item)
    {
        BizWlConcentratedInternship detail = Boolean.TRUE.equals(item.getParams().get("currentRead"))
                ? bizWlConcentratedInternshipMapper.selectByItemIdForUpdate(item.getId())
                : bizWlConcentratedInternshipMapper.selectBizWlConcentratedInternshipByItemId(item.getId());
        if (detail == null)
        {
            throw new ServiceException("G6集中实习明细缺失, itemId=" + item.getId());
        }
        return detail;
    }
}
