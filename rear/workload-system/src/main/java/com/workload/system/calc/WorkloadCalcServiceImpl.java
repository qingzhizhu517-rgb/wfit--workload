package com.workload.system.calc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DateUtils;
import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.calc.strategy.WorkloadCalcStrategy;
import com.workload.system.domain.BizPayRecord;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.domain.dto.CalculationRunRequest;
import com.workload.system.domain.vo.CalculationRunResult;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.mapper.BizCoefficientAdjustmentMapper;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import com.workload.system.service.ISysUserService;
import com.workload.system.service.IWorkloadFactorFormulaService;
import com.workload.system.service.WorkloadSnapshotService;
import com.workload.common.core.domain.entity.SysUser;

/**
 * 工作量明细计算服务实现
 *
 * @author wflg
 * @date 2026-07-21
 */
@Service
public class WorkloadCalcServiceImpl implements WorkloadCalcService
{
    /** 明细状态：已核对（冻结） */
    private static final int ITEM_STATUS_CONFIRMED = 1;

    @Autowired
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    @Autowired
    private BizTeacherProfileMapper bizTeacherProfileMapper;

    @Autowired
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    @Autowired
    private CalcStrategyFactory calcStrategyFactory;

    @Autowired
    private SummaryCalcService summaryCalcService;

    @Autowired
    private PayCalcService payCalcService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private IWorkloadFactorFormulaService factorFormulaService;

    @Autowired
    private WorkloadSnapshotService snapshotService;

    @Autowired
    private WorkloadWriteGuard writeGuard;

    @Autowired
    private BizCoefficientAdjustmentMapper coefficientAdjustmentMapper;

    /**
     * G11 生成器与本服务互为依赖（生成器 recalcItem，本服务 run 时同步 G11），
     * 用 {@code @Lazy} 打破构造期循环（Spring 默认已禁用循环引用自动解析）。
     */
    @Lazy
    @Autowired
    private ManagementItemGenerator managementItemGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal recalcItem(Long itemId)
    {
        BizWorkloadItem item = bizWorkloadItemMapper.selectBizWorkloadItemById(itemId);
        if (item == null)
        {
            throw new ServiceException("工作量明细不存在, id=" + itemId);
        }
        assertEditable(item);
        WorkloadCalcStrategy strategy = calcStrategyFactory.get(item.getItemType());
        if (strategy == null)
        {
            // G8/G9 等无策略类别：金额直录，不重算，也不落快照
            return item.getCalculatedWorkload();
        }
        BigDecimal value = strategy.calculate(item);
        item.setCalculatedWorkload(value);
        strategy.afterCalculated(item, value);

        // 计算 → 构建新鲜公式因子 → 固化不可变快照 → 原子条件更新主表（同事务）。
        // 必须用 buildFresh（按当前子表构造，不做快照 overlay），否则会把上一版快照因子
        // 冻进新快照，污染可追溯性。条件更新影响行数不为 1 时抛错回滚，快照 INSERT 随之回滚。
        FactorFormulaVo formula = factorFormulaService.buildFresh(item);
        if (formula == null)
        {
            throw new ServiceException("无法构建计算公式，明细子表可能缺失, id=" + itemId);
        }
        Long version = snapshotService.capture(item, formula, ruleVersionOf(item)).getCalculationVersion();
        item.setCalculationVersion(version);
        item.setLastCalculatedAt(DateUtils.getNowDate());
        item.setUpdateTime(item.getLastCalculatedAt());
        int affected = bizWorkloadItemMapper.updateCalculationIfEditable(item,
                ITEM_STATUS_CONFIRMED, WorkloadSummaryStatus.DRAFT);
        if (affected != 1)
        {
            throw new ServiceException("明细或汇总状态已变化，请刷新后重试");
        }
        return value;
    }

    /** 规则版本标识：按学期固定，便于历史学期按当时规则复现。 */
    private String ruleVersionOf(BizWorkloadItem item)
    {
        String semester = item.getSemester();
        return "RULE-" + (semester == null || semester.isBlank() ? "UNKNOWN" : semester);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recalcItems(Long userId, String semester)
    {
        return recalcItemsInternal(userId, semester, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recalcNonG11Items(Long userId, String semester)
    {
        // run() 编排专用：G11 已由 GENERATE_G11 阶段（generate→recalcItem）单独处理，
        // 此处只算 G1~G6/G8/G9，避免同一 G11 明细在一次 run 内被重算两次（重复快照 + 版本虚增）。
        return recalcItemsInternal(userId, semester, false);
    }

    /** @param includeG11 false 时跳过 G11 明细（由 run 的 GENERATE_G11 阶段负责）。 */
    private int recalcItemsInternal(Long userId, String semester, boolean includeG11)
    {
        BizWorkloadItem query = new BizWorkloadItem();
        query.setUserId(userId);
        query.setSemester(semester);
        List<BizWorkloadItem> items = bizWorkloadItemMapper.selectBizWorkloadItemList(query);
        int count = 0;
        for (BizWorkloadItem item : items)
        {
            if (item.getStatus() != null && item.getStatus() == ITEM_STATUS_CONFIRMED)
            {
                continue;
            }
            if (!includeG11 && "G11".equals(item.getItemType()))
            {
                continue;
            }
            recalcItem(item.getId());
            count++;
        }
        return count;
    }

    @Override
    public void onDetailDeleted(Long itemId)
    {
        BizWorkloadItem item = bizWorkloadItemMapper.selectBizWorkloadItemById(itemId);
        if (item == null)
        {
            return;
        }
        item.setCalculatedWorkload(BigDecimal.ZERO);
        item.setIsOverLimit(0);
        item.setUpdateTime(DateUtils.getNowDate());
        bizWorkloadItemMapper.updateBizWorkloadItem(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recalcAll(Long userId, String semester)
    {
        // userId 合法性校验：教师档案不存在则快速失败，避免任意 userId 生成零值脏数据
        if (bizTeacherProfileMapper.selectBizTeacherProfileByUserId(userId) == null)
        {
            throw new ServiceException("教师档案不存在，无法重算");
        }
        // 明细 → 汇总 → 酬金 三步同事务：recalcItems 为自调用但已处本事务内；
        // recalcSummary/recalcPay 为跨 Bean 调用，其 @Transactional(REQUIRED) 加入本事务
        int itemCount = recalcItems(userId, semester);
        BizWorkloadSummary summary = summaryCalcService.recalcSummary(userId, semester, true);
        BizPayRecord payRecord = payCalcService.recalcPay(userId, semester);
        Map<String, Object> data = new HashMap<>();
        data.put("recalcItemCount", itemCount);
        data.put("summary", summary);
        data.put("payRecord", payRecord);
        data.put("unconfirmedCount", summaryCalcService.countUnconfirmed(userId, semester));
        return data;
    }

    @Override
    public Map<String, Object> recalcAllBatch(List<Long> userIds, String semester)
    {
        // 刻意不加 @Transactional：本方法只做编排，事务边界必须落在每位教师身上，
        // 否则任一教师失败会把已成功的教师一起回滚。
        List<Long> targets = (userIds == null || userIds.isEmpty())
                ? bizWorkloadItemMapper.selectUserIdsBySemester(semester)
                : userIds.stream().filter(java.util.Objects::nonNull).distinct().toList();

        // 通过代理自调用，让每位教师的 recalcAll 各起一个新事务（exposeProxy=true，见 ApplicationConfig）
        WorkloadCalcService proxy = (WorkloadCalcService) AopContext.currentProxy();

        List<Map<String, Object>> failures = new ArrayList<>();
        int successCount = 0;
        for (Long uid : targets)
        {
            try
            {
                proxy.recalcAll(uid, semester);
                successCount++;
            }
            catch (Exception e)
            {
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("userId", uid);
                fail.put("userName", resolveUserLabel(uid));
                fail.put("reason", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                failures.add(fail);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("semester", semester);
        data.put("total", targets.size());
        data.put("successCount", successCount);
        data.put("failCount", failures.size());
        data.put("failures", failures);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CalculationRunResult run(CalculationRunRequest request)
    {
        if (request == null || request.getUserId() == null || !StringUtils.hasText(request.getSemester()))
        {
            throw new ServiceException("缺少教师或学期，无法核算");
        }
        Long userId = request.getUserId();
        String semester = request.getSemester();
        boolean includeG11 = request.isIncludeG11();

        CalculationRunResult result = new CalculationRunResult(userId, semester, includeG11);

        // 阶段 1：Guard（冻结锁）+ 一致性校验。任一失败抛错，整体事务回滚，零写入。
        // 失败阶段标识写入 ServiceException.detailMessage，供 runBatch 归因（ServiceException 为 final，不能子类化）。
        try
        {
            writeGuard.lockDraftOrAbsent(userId, semester);
            validateConsistency(userId, semester);
            result.addStage(CalculationRunResult.STAGE_VALIDATE, true, 0, "校验通过");
        }
        catch (ServiceException e)
        {
            throw stageFailure(CalculationRunResult.STAGE_VALIDATE, e);
        }

        // 阶段 2：同步教务岗位减免到 G11（includeG11=false 时占位跳过，绝不调用生成器）
        if (includeG11)
        {
            int g11 = managementItemGenerator.generate(userId, semester);
            result.setGeneratedG11Count(g11);
            result.addStage(CalculationRunResult.STAGE_GENERATE_G11, true, g11, "已同步岗位减免");
        }
        else
        {
            result.addStage(CalculationRunResult.STAGE_GENERATE_G11, true, 0, "未勾选同步 G11，已跳过");
        }

        // 阶段 3：重算明细。includeG11=true 时 G11 已在阶段 2 由生成器重算过，此处跳过
        // 避免同一 G11 被重算两次（重复快照 + 版本虚增）；includeG11=false 时未同步 G11，
        // 仍需算上存量 G11 以保持与 recalcAll 一致。
        int itemCount = includeG11
                ? recalcNonG11Items(userId, semester)
                : recalcItems(userId, semester);
        result.setRecalcItemCount(itemCount);
        result.addStage(CalculationRunResult.STAGE_RECALC_ITEMS, true, itemCount, "已重算明细");

        // 阶段 4：重算汇总（落库）
        BizWorkloadSummary summary = summaryCalcService.recalcSummary(userId, semester, true);
        result.setSummary(summary);
        result.addStage(CalculationRunResult.STAGE_RECALC_SUMMARY, true, 1, "已重算汇总");

        // 阶段 5：重算酬金（落库）
        BizPayRecord payRecord = payCalcService.recalcPay(userId, semester);
        result.setPayRecord(payRecord);
        result.addStage(CalculationRunResult.STAGE_RECALC_PAY, true, 1, "已重算酬金");

        result.setUnconfirmedCount(summaryCalcService.countUnconfirmed(userId, semester));
        return result;
    }

    @Override
    public Map<String, Object> runBatch(List<Long> userIds, String semester, boolean includeG11)
    {
        // 刻意不加 @Transactional：编排层，事务边界落在每位教师身上，避免一人失败连累全批。
        // 先过滤出有效 userId；无有效 id（null/空/全为 null）统一落全学期兜底，
        // 避免传入 [null] 时过滤成空列表却静默空跑、还报成功。
        List<Long> explicit = userIds == null ? java.util.List.of()
                : userIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        List<Long> targets = explicit.isEmpty()
                ? bizWorkloadItemMapper.selectUserIdsBySemester(semester)
                : explicit;

        WorkloadCalcService proxy = (WorkloadCalcService) AopContext.currentProxy();

        List<Map<String, Object>> failures = new ArrayList<>();
        int successCount = 0;
        int generatedG11Count = 0;
        int recalcItemCount = 0;
        for (Long uid : targets)
        {
            try
            {
                CalculationRunResult one = proxy.run(new CalculationRunRequest(uid, semester, includeG11));
                successCount++;
                generatedG11Count += one.getGeneratedG11Count();
                recalcItemCount += one.getRecalcItemCount();
            }
            catch (Exception e)
            {
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("userId", uid);
                fail.put("userName", resolveUserLabel(uid));
                fail.put("stage", e instanceof ServiceException se && se.getDetailMessage() != null
                        ? se.getDetailMessage() : "UNKNOWN");
                fail.put("reason", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                failures.add(fail);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("semester", semester);
        data.put("includeG11", includeG11);
        data.put("total", targets.size());
        data.put("successCount", successCount);
        data.put("failCount", failures.size());
        data.put("generatedG11Count", generatedG11Count);
        data.put("recalcItemCount", recalcItemCount);
        data.put("failures", failures);
        return data;
    }

    /**
     * 一致性校验：教师档案存在、子表齐全（可构建计算公式）、无待审系数调整申请。
     * 已核对(status=1)明细由 recalcItems 天然跳过，不会被覆盖。
     */
    private void validateConsistency(Long userId, String semester)
    {
        if (bizTeacherProfileMapper.selectBizTeacherProfileByUserId(userId) == null)
        {
            throw new ServiceException("教师档案不存在，无法核算");
        }
        if (coefficientAdjustmentMapper.countPendingByUserSemester(userId, semester) > 0)
        {
            throw new ServiceException("存在待审的系数调整申请，请先处理后再核算");
        }
        // 子表齐全性：对有策略的明细逐条尝试构建公式，构建不出即子表缺失，fail-loud
        BizWorkloadItem query = new BizWorkloadItem();
        query.setUserId(userId);
        query.setSemester(semester);
        for (BizWorkloadItem item : bizWorkloadItemMapper.selectBizWorkloadItemList(query))
        {
            if (item.getStatus() != null && (item.getStatus() == 3 || item.getStatus() == ITEM_STATUS_CONFIRMED))
            {
                continue; // 已驳回不参与；已核对已冻结、不会被重算覆盖
            }
            if (calcStrategyFactory.get(item.getItemType()) == null)
            {
                continue; // G8/G9 等无策略类别：金额直录，无子表
            }
            // 用 buildFresh 当子表探针：只按当前子表构造，不触发快照查询/解析，
            // 避免某条损坏快照 JSON 拖垮整教师核算（探针只关心子表是否齐全）。
            if (factorFormulaService.buildFresh(item) == null)
            {
                throw new ServiceException("明细子表缺失，无法核算, itemId=" + item.getId());
            }
        }
    }

    /**
     * 把阶段标识挂到 ServiceException.detailMessage，保留原始业务提示为 message。
     * ServiceException 为 final 不能子类化，故复用其 detailMessage 承载阶段。
     */
    private ServiceException stageFailure(String stage, ServiceException cause)
    {
        return new ServiceException(cause.getMessage(), cause.getCode()).setDetailMessage(stage);
    }

    /**
     * 失败明细里的教师标识：优先「姓名(工号)」，查不到用户则退回裸 userId，
     * 让教务能直接定位到人，而不是拿着一串数字回头翻库。
     */
    private String resolveUserLabel(Long userId)
    {
        try
        {
            SysUser user = sysUserService.selectUserById(userId);
            if (user == null)
            {
                return "userId=" + userId;
            }
            return user.getNickName() + "(" + user.getUserName() + ")";
        }
        catch (Exception e)
        {
            return "userId=" + userId;
        }
    }

    @Override
    public void assertEditable(Long itemId)
    {
        BizWorkloadItem item = bizWorkloadItemMapper.selectBizWorkloadItemById(itemId);
        if (item == null)
        {
            throw new ServiceException("工作量明细不存在, id=" + itemId);
        }
        assertEditable(item);
    }

    /**
     * 明细已核对或所在学期汇总已进入审批流程 -> 拒绝修改
     */
    private void assertEditable(BizWorkloadItem item)
    {
        if (item.getStatus() != null && item.getStatus() == ITEM_STATUS_CONFIRMED)
        {
            throw new ServiceException("明细已核对，系数已冻结，请先取消核对");
        }
        BizWorkloadSummary query = new BizWorkloadSummary();
        query.setUserId(item.getUserId());
        query.setSemester(item.getSemester());
        List<BizWorkloadSummary> summaries = bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(query);
        boolean locked = summaries.stream()
                .anyMatch(s -> WorkloadSummaryStatus.isWriteFrozen(s.getStatus()));
        if (locked)
        {
            throw new ServiceException("学期汇总已锁定，禁止修改明细");
        }
    }
}
