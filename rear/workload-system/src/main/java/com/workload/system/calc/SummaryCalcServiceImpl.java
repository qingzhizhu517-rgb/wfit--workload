package com.workload.system.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DateUtils;
import com.workload.system.calc.rule.ComplianceChecker;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizPayRate;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.WorkloadSummaryStatus;
import com.workload.system.mapper.BizPayRateMapper;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;

/**
 * 学期工作量汇总计算服务实现
 *
 * 口径：聚合 status!=3(已驳回) 的全部明细；G7=G1+..+G6，G10=G7+G8+G9，
 * G11 学期累计封顶 CAP_G11_SEMESTER，绩效=专任 (min(total,CAP_200PCT)−rated)×rate 下限 0，
 * 达标按职称年标准/2 再按特殊状态折算
 *
 * @author wflg
 * @date 2026-07-21
 */
@Service
public class SummaryCalcServiceImpl implements SummaryCalcService
{
    @Autowired
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    @Autowired
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    @Autowired
    private BizTeacherProfileMapper bizTeacherProfileMapper;

    @Autowired
    private BizPayRateMapper bizPayRateMapper;

    @Autowired
    private RuleParamService ruleParamService;

    @Autowired
    private SemesterCalendar semesterCalendar;

    @Autowired
    private ComplianceChecker complianceChecker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizWorkloadSummary recalcSummary(Long userId, String semester, boolean persist)
    {
        // userId 合法性校验：教师档案不存在则快速失败，避免任意 userId 生成零值脏数据
        if (bizTeacherProfileMapper.selectBizTeacherProfileByUserId(userId) == null)
        {
            throw new ServiceException("教师档案不存在，无法重算");
        }
        BizWorkloadSummary summary = findSummary(userId, semester);
        if (persist && summary != null && summary.getStatus() != null
                && (summary.getStatus() == WorkloadSummaryStatus.PENDING_AUDIT
                        || summary.getStatus() == WorkloadSummaryStatus.FINISHED))
        {
            throw new ServiceException("学期汇总已进入审批流程，禁止重算");
        }
        boolean isNew = summary == null;
        if (isNew)
        {
            summary = new BizWorkloadSummary();
            summary.setUserId(userId);
            summary.setSemester(semester);
            summary.setStatus(WorkloadSummaryStatus.DRAFT);
        }

        // 1. 聚合明细（排除已驳回）；明细列表同时供制度性校验复用
        List<BizWorkloadItem> items = loadItems(userId, semester);
        Map<String, BigDecimal> typeSum = aggregateItems(items);
        BigDecimal g7 = sumOf(typeSum, "G1", "G2", "G3", "G4", "G5", "G6");
        BigDecimal g8 = sumOf(typeSum, "G8");
        BigDecimal g9 = sumOf(typeSum, "G9");
        BigDecimal g10 = g7.add(g8).add(g9);
        BigDecimal g11Cap = ruleParamService.get("CAP_G11_SEMESTER", new BigDecimal("180"));
        BigDecimal g11 = sumOf(typeSum, "G11").min(g11Cap);
        BigDecimal total = g10.add(g11);

        BigDecimal rated = ruleParamService.get("RATED_WORKLOAD", new BigDecimal("180"));
        BigDecimal cap200 = ruleParamService.get("CAP_200PCT", new BigDecimal("540"));

        summary.setAcademicYear(deriveAcademicYear(semester));
        summary.setG7(scale(g7));
        summary.setG8(scale(g8));
        summary.setG9(scale(g9));
        summary.setG10(scale(g10));
        summary.setG11(scale(g11));
        summary.setTotalWorkload(scale(total));
        summary.setRatedWorkload(scale(rated));
        summary.setExcessWorkload(scale(total.subtract(rated).max(BigDecimal.ZERO)));

        // 2. 职称/费率快照 + 绩效（仅专任）
        BizTeacherProfile profile = bizTeacherProfileMapper.selectBizTeacherProfileByUserId(userId);
        String title = profile == null ? null : profile.getTitle();
        String nature = profile == null ? "专任" : profile.getTeacherNature();
        BigDecimal rate = findCurrentRate(title);
        summary.setTitle(title);
        summary.setPayRate(rate);

        boolean capped = total.compareTo(cap200) > 0;
        BigDecimal performance = BigDecimal.ZERO;
        if ((nature == null || "专任".equals(nature)) && rate != null && total.compareTo(rated) > 0)
        {
            performance = total.min(cap200).subtract(rated).multiply(rate);
        }
        summary.setPerformancePay(scale(performance));
        summary.setIsCapped(capped ? 1 : 0);

        // 3. 达标（第五条，展示用）；第六条「三门理论课视同完成」参与判定
        ComplianceChecker.Result compliance = complianceChecker.check(userId, semester, items);
        applyBasicTeaching(summary, profile, g10, semester, compliance.isThreeTheoryCourses());

        // 4. 截断与降级统一告警（2026-09-10 原则：静默截断 = 教师无声少拿钱）。
        //    制度性校验（第六/八/九/十条）+ G11 学期封顶 + 540 绩效封顶，
        //    只提示不改变计算结果；每次重算覆盖重写保持与当前数据一致，
        //    驳回原因在教师重新提交后由本机制覆盖属预期行为。
        List<String> warnings = new ArrayList<>(compliance.getWarnings());
        BigDecimal g11Raw = sumOf(typeSum, "G11");
        if (g11Raw.compareTo(g11Cap) > 0)
        {
            warnings.add(String.format("G11 管理服务累计 %s 已按 %s/学期封顶（第十六条注）",
                    scale(g11Raw).stripTrailingZeros().toPlainString(),
                    g11Cap.stripTrailingZeros().toPlainString()));
        }
        if (capped)
        {
            warnings.add(String.format("总工作量 %s 超 CAP_200PCT=%s，绩效酬金已按封顶值核算（第二十条）",
                    scale(total).stripTrailingZeros().toPlainString(),
                    cap200.stripTrailingZeros().toPlainString()));
        }
        summary.setRemark(String.join("；", warnings));

        // 4. 落库（并发撞 uk_user_sem 唯一键时降级为更新，消除 check-then-act 竞态）
        if (persist)
        {
            if (isNew)
            {
                summary.setCreateTime(DateUtils.getNowDate());
                try
                {
                    bizWorkloadSummaryMapper.insertBizWorkloadSummary(summary);
                }
                catch (DuplicateKeyException e)
                {
                    BizWorkloadSummary existed = findSummary(userId, semester);
                    if (existed == null)
                    {
                        throw new ServiceException("学期汇总保存失败，请重试");
                    }
                    summary.setId(existed.getId());
                    summary.setCreateTime(existed.getCreateTime());
                    summary.setUpdateTime(DateUtils.getNowDate());
                    bizWorkloadSummaryMapper.updateBizWorkloadSummary(summary);
                }
            }
            else
            {
                summary.setUpdateTime(DateUtils.getNowDate());
                bizWorkloadSummaryMapper.updateBizWorkloadSummary(summary);
            }
        }
        return summary;
    }

    @Override
    public int countUnconfirmed(Long userId, String semester)
    {
        BizWorkloadItem query = new BizWorkloadItem();
        query.setUserId(userId);
        query.setSemester(semester);
        List<BizWorkloadItem> items = bizWorkloadItemMapper.selectBizWorkloadItemList(query);
        return (int) items.stream()
                .filter(i -> i.getStatus() == null || i.getStatus() == 0 || i.getStatus() == 2)
                .count();
    }

    /**
     * 达标标准与结果：年标准/2 -> 产假×0.5 / 在职读博 128/2 / 访学视同完成；
     * 第六条「每学期独立完成三门理论课视同为完成基本教学工作量（重复课等同于一门
     * 课程，三门课不包含实践类课程）」——独立完成无法从数据核验，按 G1 去重课程名≥3 判定
     */
    private void applyBasicTeaching(BizWorkloadSummary summary, BizTeacherProfile profile,
            BigDecimal g10, String semester, boolean threeTheoryCourses)
    {
        BigDecimal annual = annualStandard(summary.getTitle());
        BigDecimal standard = annual.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        boolean deemedMet = false;
        if (profile != null && StringUtils.hasText(profile.getSpecialStatus())
                && !"正常".equals(profile.getSpecialStatus())
                && specialStatusActive(profile, semester))
        {
            switch (profile.getSpecialStatus())
            {
                case "产假":
                    standard = standard.multiply(ruleParamService.get("FACTOR_MATERNITY", new BigDecimal("0.5")));
                    break;
                case "在职读博":
                    standard = ruleParamService.get("BASIC_TEACH_PHD", new BigDecimal("128"))
                            .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                    break;
                case "访学":
                    deemedMet = true;
                    break;
                default:
                    break;
            }
        }
        summary.setBasicTeachingStandard(scale(standard));
        summary.setBasicTeachingMet(deemedMet || threeTheoryCourses || g10.compareTo(standard) >= 0 ? 1 : 0);
    }

    /**
     * 特殊状态区间与学期区间是否有交集（起止均空视为全期有效）
     */
    private boolean specialStatusActive(BizTeacherProfile profile, String semester)
    {
        if (profile.getSpecialStatusStart() == null && profile.getSpecialStatusEnd() == null)
        {
            return true;
        }
        LocalDate[] range = semesterCalendar.rangeOf(semester);
        LocalDate start = toLocalDate(profile.getSpecialStatusStart());
        LocalDate end = toLocalDate(profile.getSpecialStatusEnd());
        boolean startsBeforeSemEnd = start == null || !start.isAfter(range[1]);
        boolean endsAfterSemStart = end == null || !end.isBefore(range[0]);
        return startsBeforeSemEnd && endsAfterSemStart;
    }

    private LocalDate toLocalDate(Date date)
    {
        // MyBatis 把 DATE/DATETIME 列映射为 java.sql.Date/Timestamp，
        // 而 java.sql.Date.toInstant() 会抛 UnsupportedOperationException，须分流处理
        if (date == null)
        {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate)
        {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 职称 -> 年度基本教学量规则值
     */
    private BigDecimal annualStandard(String title)
    {
        String ruleCode;
        if (title != null && title.contains("教授") && !title.contains("副"))
        {
            ruleCode = "BASIC_TEACH_PROF";
        }
        else if (title != null && title.contains("副教授"))
        {
            ruleCode = "BASIC_TEACH_APROF";
        }
        else if (title != null && title.contains("讲师"))
        {
            ruleCode = "BASIC_TEACH_LECT";
        }
        else
        {
            ruleCode = "BASIC_TEACH_ASSIST";
        }
        return ruleParamService.get(ruleCode, new BigDecimal("192"));
    }

    /**
     * 取职称当期生效单位酬金；无职称或无费率返回 null
     */
    private BigDecimal findCurrentRate(String title)
    {
        if (!StringUtils.hasText(title))
        {
            return null;
        }
        BizPayRate query = new BizPayRate();
        query.setTitle(title);
        query.setStatus(1);
        List<BizPayRate> rates = bizPayRateMapper.selectBizPayRateList(query);
        Date now = new Date();
        return rates.stream()
                .filter(r -> r.getEffectiveFrom() == null || !r.getEffectiveFrom().after(now))
                .filter(r -> r.getEffectiveTo() == null || !r.getEffectiveTo().before(now))
                .max(Comparator.comparing(BizPayRate::getEffectiveFrom,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(BizPayRate::getRate)
                .orElse(null);
    }

    private BizWorkloadSummary findSummary(Long userId, String semester)
    {
        BizWorkloadSummary query = new BizWorkloadSummary();
        query.setUserId(userId);
        query.setSemester(semester);
        List<BizWorkloadSummary> list = bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(query);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<BizWorkloadItem> loadItems(Long userId, String semester)
    {
        BizWorkloadItem query = new BizWorkloadItem();
        query.setUserId(userId);
        query.setSemester(semester);
        return bizWorkloadItemMapper.selectBizWorkloadItemList(query);
    }

    private Map<String, BigDecimal> aggregateItems(List<BizWorkloadItem> items)
    {
        Map<String, BigDecimal> typeSum = new HashMap<>();
        for (BizWorkloadItem item : items)
        {
            if (item.getStatus() != null && item.getStatus() == 3)
            {
                continue;
            }
            typeSum.merge(item.getItemType(),
                    item.getCalculatedWorkload() == null ? BigDecimal.ZERO : item.getCalculatedWorkload(),
                    BigDecimal::add);
        }
        return typeSum;
    }

    private BigDecimal sumOf(Map<String, BigDecimal> typeSum, String... types)
    {
        BigDecimal sum = BigDecimal.ZERO;
        for (String type : types)
        {
            sum = sum.add(typeSum.getOrDefault(type, BigDecimal.ZERO));
        }
        return sum;
    }

    private String deriveAcademicYear(String semester)
    {
        String[] parts = semester.split("-");
        return parts.length >= 2 ? parts[0] + "-" + parts[1] : semester;
    }

    private BigDecimal scale(BigDecimal value)
    {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
