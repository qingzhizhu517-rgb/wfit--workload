package com.workload.system.calc.allowance;

import java.math.BigDecimal;
import com.workload.system.calc.SemesterCalendar;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizAllowanceItem;

/**
 * D 代阅卷酬金（协助外聘教师阅卷），按人数五档直接发放：
 * &lt;20 人 → 0 元；[20,60) → 30 元；[60,120) → 80 元；
 * [120,200) → 100 元；≥200 → 150 元。
 * <p>
 * 依据《办法》第十五条5。原「待正式文件、首期不启用」的理由不成立
 * ——办法已给完整阶梯，2026-09-10 起启用。
 *
 * @author wflg
 * @date 2026-09-10
 */
@Component
public class AllowanceDStrategy implements AllowanceCalcStrategy
{
    @Autowired
    private RuleParamService ruleParamService;

    @Autowired
    private SemesterCalendar semesterCalendar = new SemesterCalendar();

    @Override
    public String getFeeType()
    {
        return "D";
    }

    @Override
    public BigDecimal calculate(BizAllowanceItem item)
    {
        long count = item.getStudentCount() == null ? 0 : item.getStudentCount();
        // 第十五条5：协助外聘教师阅卷按人数五档直接发放
        BigDecimal amount;
        if (count < 20)
        {
            amount = ruleParamService.get("PAY_D_MARKING_LT20", new BigDecimal("0"), semesterCalendar.endOf(item.getSemester()));
        }
        else if (count < 60)
        {
            amount = ruleParamService.get("PAY_D_MARKING_20_60", new BigDecimal("30"), semesterCalendar.endOf(item.getSemester()));
        }
        else if (count < 120)
        {
            amount = ruleParamService.get("PAY_D_MARKING_60_120", new BigDecimal("80"), semesterCalendar.endOf(item.getSemester()));
        }
        else if (count < 200)
        {
            amount = ruleParamService.get("PAY_D_MARKING_120_200", new BigDecimal("100"), semesterCalendar.endOf(item.getSemester()));
        }
        else
        {
            amount = ruleParamService.get("PAY_D_MARKING_GE200", new BigDecimal("150"), semesterCalendar.endOf(item.getSemester()));
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
