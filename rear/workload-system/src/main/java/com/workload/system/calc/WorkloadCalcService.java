package com.workload.system.calc;

import java.math.BigDecimal;

/**
 * 工作量明细计算服务（单条/批量重算 + 可编辑护栏）
 *
 * @author wflg
 * @date 2026-07-21
 */
public interface WorkloadCalcService
{
    /**
     * 重算单条明细工作量并回写 calculated_workload（已核对/汇总已锁定拒绝）
     *
     * @param itemId 明细主表 id
     * @return 计算值（G8/G9 等无策略类别返回现值）
     */
    public BigDecimal recalcItem(Long itemId);

    /**
     * 批量重算某教师某学期全部未冻结明细
     *
     * @param userId 教师
     * @param semester 学期
     * @return 重算条数
     */
    public int recalcItems(Long userId, String semester);

    /**
     * 可编辑护栏：明细已核对（status=1）或学期汇总已锁定（status=3）时抛 ServiceException
     *
     * @param itemId 明细主表 id
     */
    public void assertEditable(Long itemId);

    /**
     * 类别明细删除后的善后：主表 calculated_workload 清零（避免残留陈旧值）
     *
     * @param itemId 明细主表 id
     */
    public void onDetailDeleted(Long itemId);
}
