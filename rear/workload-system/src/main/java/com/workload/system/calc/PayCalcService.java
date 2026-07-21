package com.workload.system.calc;

import com.workload.system.domain.BizPayRecord;

/**
 * 酬金汇总计算服务（课时酬金 + 其他酬金 A~G -> pay_record）
 *
 * @author wflg
 * @date 2026-07-21
 */
public interface PayCalcService
{
    /**
     * 重算某教师某学期酬金并落库（需先重算学期汇总；汇总已锁定则拒绝）
     *
     * @param userId 教师
     * @param semester 学期
     * @return 酬金记录
     */
    public BizPayRecord recalcPay(Long userId, String semester);

    /**
     * 其他酬金可编辑护栏：学期汇总已锁定（酬金定稿）时抛 ServiceException
     *
     * @param userId 教师
     * @param semester 学期
     */
    public void assertAllowanceEditable(Long userId, String semester);
}
