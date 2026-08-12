package com.workload.system.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
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
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizPayRate;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
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
    /** 汇总状态：已锁定 */
    private static final int SUMMARY_STATUS_LOCKED = 3;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizWorkloadSummary recalcSummary(Long userId, String semester, boolean persist)
    {
        BizWorkloadSummary summary = findSummary(userId, semester);
        if (summary != null && summary.getStatus() != null && summary.getStatus() == SUMMARY_STATUS_LOCKED)
        {
            throw new ServiceException("学期汇总已锁定，禁止重算");
        }
        boolean isNew = summary == null;
        if (isNew)
        {
            summary = new BizWorkloadSummary();
            summary.setUserId(userId);
            summary.setSemester(semester);
            summary.setStatus(0);
        }

        // 1. 聚合明细（排除已驳回）
        Map<String, BigDecimal> typeSum = aggregateItems(userId, semester);
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

        // 3. 达标（第五条，展示用）
        applyBasicTeaching(summary, profile, g10, semester);

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
     * 达标标准与结果：年标准/2 -> 产假×0.5 / 在职读博 128/2 / 访学视同完成
     */
    private void applyBasicTeaching(BizWorkloadSummary summary, BizTeacherProfile profile,
            BigDecimal g10, String semester)
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
        summary.setBasicTeachingMet(deemedMet || g10.compareTo(standard) >= 0 ? 1 : 0);
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
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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

    private Map<String, BigDecimal> aggregateItems(Long userId, String semester)
    {
        BizWorkloadItem query = new BizWorkloadItem();
        query.setUserId(userId);
        query.setSemester(semester);
        List<BizWorkloadItem> items = bizWorkloadItemMapper.selectBizWorkloadItemList(query);
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
