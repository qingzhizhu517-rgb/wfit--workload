package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import com.workload.system.calc.SemesterCalendar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizWlCourseDesign;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlCourseDesignMapper;

/**
 * G4 课程设计计算策略：G4 = J4 × R4 × 0.4
 * <p>
 * 依据《办法》第十四条4，条文只说「R4≤60」，未写「超出部分不计算」
 * （对比第十四条6注4的明写截断），故按实际人数计算、不截断；
 * R4 超过 CAP_R4_MAX（告警阈值）时置 is_over_limit=1 提示人工核查。
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

    @Autowired
    private SemesterCalendar semesterCalendar = new SemesterCalendar();

    @Override
    public String getTypeCode()
    {
        return "G4";
    }

    @Override
    public BigDecimal calculate(BizWorkloadItem item)
    {
        BizWlCourseDesign detail = loadDetail(item);
        BigDecimal constant = ruleParamService.get("CONST_COURSE_DESIGN", new BigDecimal("0.4"), semesterCalendar.endOf(item.getSemester()));
        // 第十四条4 未写「超出部分不计算」，R4 按实际人数计（2026-09-10 按办法原文修正，
        // 原 min(R4, CAP_R4_MAX) 截断会擅自减少教师工作量）
        return scale(mul(num(detail.getJ4()), num(detail.getR4()), constant));
    }

    @Override
    public void afterCalculated(BizWorkloadItem item, BigDecimal value)
    {
        BizWlCourseDesign detail = loadDetail(item);
        // CAP_R4_MAX 由截断上限改为告警阈值：超 60 人仅置超标标记，不减量
        BigDecimal warnThreshold = ruleParamService.get("CAP_R4_MAX", new BigDecimal("60"), semesterCalendar.endOf(item.getSemester()));
        item.setIsOverLimit(num(detail.getR4()).compareTo(warnThreshold) > 0 ? 1 : 0);
    }

    private BizWlCourseDesign loadDetail(BizWorkloadItem item)
    {
        BizWlCourseDesign detail = Boolean.TRUE.equals(item.getParams().get("currentRead"))
                ? bizWlCourseDesignMapper.selectByItemIdForUpdate(item.getId())
                : bizWlCourseDesignMapper.selectBizWlCourseDesignByItemId(item.getId());
        if (detail == null)
        {
            throw new ServiceException("G4课程设计明细缺失, itemId=" + item.getId());
        }
        return detail;
    }
}
