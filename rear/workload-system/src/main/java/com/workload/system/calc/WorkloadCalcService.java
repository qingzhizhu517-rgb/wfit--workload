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
     * 批量重算某教师某学期未冻结明细，但跳过 G11（供 run 编排使用：
     * G11 由 GENERATE_G11 阶段单独同步重算，避免一次 run 内重算两次）。
     *
     * @param userId 教师
     * @param semester 学期
     * @return 重算条数（不含 G11）
     */
    public int recalcNonG11Items(Long userId, String semester);

    /**
     * 可编辑护栏：明细已核对（status=1）或学期汇总已锁定（status=2）时抛 ServiceException
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

    /**
     * 阶段化一键核算（单教师单事务）：先 Guard + 一致性校验（教师档案、子表齐全、
     * 无待审系数调整申请），再按 {@code includeG11} 显式执行
     * 「同步 G11 → 重算明细 → 重算汇总 → 重算酬金」四步，逐阶段返回结果与计数。
     * <p>
     * 校验失败该阶段 ok=false 并抛异常，整体零写入；成功阶段依次为
     * VALIDATE / GENERATE_G11 / RECALC_ITEMS / RECALC_SUMMARY / RECALC_PAY。
     *
     * @param request 含 userId / semester / includeG11
     * @return 阶段化结果（含 generatedG11Count / recalcItemCount / summary / payRecord / unconfirmedCount）
     */
    public com.workload.system.domain.vo.CalculationRunResult run(
            com.workload.system.domain.dto.CalculationRunRequest request);

    /**
     * 批量阶段化一键核算：对每位教师独立事务执行 {@link #run}，
     * 单人失败只记入 failures（含 stage、reason），不影响其余教师。
     *
     * @param userIds    教师ID列表；为 null 或空表示该学期全部有明细的教师
     * @param semester   学年学期
     * @param includeG11 是否同步教务岗位减免到 G11
     * @return 汇总结果：total / successCount / failCount / generatedG11Count / recalcItemCount / failures
     */
    public Map<String, Object> runBatch(List<Long> userIds, String semester, boolean includeG11);
}
