package com.workload.system.calc;

import java.math.BigDecimal;
import java.util.Map;

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

    /**
     * 一把梭编排：重算全部未冻结明细 → 重算汇总（落库）→ 重算酬金（落库）。
     * 三步在同一事务内（facade），中途失败整体回滚，避免明细/汇总/酬金半成品状态。
     *
     * @param userId 教师
     * @param semester 学期
     * @return 结果 Map：recalcItemCount / summary / payRecord / unconfirmedCount
     */
    public Map<String, Object> recalcAll(Long userId, String semester);
}
