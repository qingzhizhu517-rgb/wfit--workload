package com.workload.system.calc;

import com.workload.system.domain.BizWorkloadSummary;

/**
 * 学期工作量汇总计算服务（G7/G10/总额定/绩效/达标）
 *
 * @author wflg
 * @date 2026-07-21
 */
public interface SummaryCalcService
{
    /**
     * 重算某教师某学期汇总
     *
     * @param userId 教师
     * @param semester 学期
     * @param persist true=落库（upsert）；false=仅预览不保存
     * @return 汇总对象（含未核对条数提示，见 remark 由控制层补充）
     */
    public BizWorkloadSummary recalcSummary(Long userId, String semester, boolean persist);

    /**
     * 统计该教师该学期未核对明细条数（status 0草稿/2有异议）
     *
     * @param userId 教师
     * @param semester 学期
     * @return 未核对条数
     */
    public int countUnconfirmed(Long userId, String semester);
}
