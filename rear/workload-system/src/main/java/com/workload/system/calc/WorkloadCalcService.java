package com.workload.system.calc;

import java.math.BigDecimal;
import java.util.List;
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

    /**
     * 批量一键核算：对每位教师依次执行「明细 → 汇总 → 酬金」。
     * <p>
     * 每位教师独立事务，单个教师失败（缺档案、策略未配等）只记入失败明细，
     * 不回滚也不中断其余教师 —— 一个人的脏数据不该让整批核算停摆。
     *
     * @param userIds  教师ID列表；为 null 或空表示该学期全部有明细的教师
     * @param semester 学年学期
     * @return 汇总结果：total / successCount / failCount / failures（每项含 userId、userName、reason）
     */
    public Map<String, Object> recalcAllBatch(List<Long> userIds, String semester);
}
