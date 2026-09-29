package com.workload.system.calc;

import java.util.stream.Stream;

/**
 * 绩效酬金验收夹具：固化 {@link SummaryCalcServiceImpl} 的绩效计算边界。
 *
 * <p>口径（勿凭直觉改期望；与实现/规则不符须先报告，不擅改）：
 * <ul>
 *   <li>绩效 = (min(总工作量, CAP_200PCT=540) − 额定 180) × 职称单位酬金；</li>
 *   <li>仅当 总工作量 &gt; 180 且 人员性质 ∈ {专任, null} 且 费率非空 时计发，否则 0.00；</li>
 *   <li>isCapped = 总工作量 &gt; 540（严格大于，540 恰好不触顶）；</li>
 *   <li>无费率（待办#9 当前行为）：不报错、payRate=null、绩效 0.00。</li>
 * </ul>
 *
 * @author wflg
 * @date 2026-09-12
 */
public final class PayAcceptanceFixture
{
    private PayAcceptanceFixture()
    {
    }

    /**
     * 绩效验收场景。
     *
     * @param name        场景名（同时用作参数化用例显示名）
     * @param nature      人员性质（专任/外聘/…；null 视同专任）
     * @param title       职称
     * @param total       学期总工作量（= 明细总和）
     * @param rate        职称单位酬金；null 表示无费率
     * @param expectedPay 期望绩效酬金（2 位小数）
     * @param capped      期望 isCapped（0/1）
     */
    public record SummaryCase(String name, String nature, String title, String total,
            String rate, String expectedPay, int capped)
    {
        @Override
        public String toString()
        {
            return name;
        }
    }

    public static Stream<SummaryCase> performanceCases()
    {
        return Stream.of(
                new SummaryCase("专任正常·教授282.72@70", "专任", "教授", "282.72", "70", "7190.40", 0),
                new SummaryCase("低于额定·讲师150@50", "专任", "讲师", "150", "50", "0.00", 0),
                new SummaryCase("外聘·讲师982.33@50", "外聘", "讲师", "982.33", "50", "0.00", 1),
                new SummaryCase("超过540·教授600@70", "专任", "教授", "600", "70", "25200.00", 1),
                new SummaryCase("恰好540·教授540@70", "专任", "教授", "540", "70", "25200.00", 0),
                new SummaryCase("无费率·未定职称300", "专任", "未定职称", "300", null, "0.00", 0));
    }
}
