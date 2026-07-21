package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizWlCourseDesign;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlCourseDesignMapper;

/**
 * G4 课程设计计算策略：G4 = J4 × min(R4, CAP_R4_MAX) × CONST_COURSE_DESIGN（超出人数不计）
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component("courseDesignCalcStrategy")
public class CourseDesignCalcStrategy extends AbstractWorkloadCalcStrategy
{
    @Autowired
    private BizWlCourseDesignMapper bizWlCourseDesignMapper;

    @Autowired
    private RuleParamService ruleParamService;

    @Override
    public String getTypeCode()
    {
        return "G4";
    }

    @Override
    public BigDecimal calculate(BizWorkloadItem item)
    {
        BizWlCourseDesign detail = bizWlCourseDesignMapper.selectBizWlCourseDesignByItemId(item.getId());
        if (detail == null)
        {
            throw new ServiceException("G4课程设计明细缺失, itemId=" + item.getId());
        }
        BigDecimal cap = ruleParamService.get("CAP_R4_MAX", new BigDecimal("20"));
        BigDecimal r4 = num(detail.getR4()).min(cap);
        BigDecimal constant = ruleParamService.get("CONST_COURSE_DESIGN", new BigDecimal("0.4"));
        return scale(mul(num(detail.getJ4()), r4, constant));
    }
}
