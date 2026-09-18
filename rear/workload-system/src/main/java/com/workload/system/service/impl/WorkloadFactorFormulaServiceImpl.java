package com.workload.system.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizWlConcentratedInternship;
import com.workload.system.domain.BizWlCourseDesign;
import com.workload.system.domain.BizWlInternshipTraining;
import com.workload.system.domain.BizWlManagement;
import com.workload.system.domain.BizWlPractice;
import com.workload.system.domain.BizWlTheory;
import com.workload.system.domain.BizWlThesis;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.domain.vo.FactorFormulaVo.FactorVo;
import com.workload.system.service.IBizWlConcentratedInternshipService;
import com.workload.system.service.IBizWlCourseDesignService;
import com.workload.system.service.IBizWlInternshipTrainingService;
import com.workload.system.service.IBizWlManagementService;
import com.workload.system.service.IBizWlPracticeService;
import com.workload.system.service.IBizWlTheoryService;
import com.workload.system.service.IBizWlThesisService;
import com.workload.system.service.IWorkloadFactorFormulaService;

/** 查询 G1-G6、G11 明细并构造可追溯的公式说明。 */
@Service
public class WorkloadFactorFormulaServiceImpl implements IWorkloadFactorFormulaService
{
    private static final String NORMAL = "NORMAL";
    private static final String DISPLAY_ONLY = "DISPLAY_ONLY";
    private static final String FORMULA_CONSTANT = "FORMULA_CONSTANT";
    private static final String CURRENT_RULE_DERIVED = "CURRENT_RULE_DERIVED";
    private static final String UNKNOWN = "UNKNOWN";

    @Autowired private IBizWlTheoryService theoryService;
    @Autowired private IBizWlPracticeService practiceService;
    @Autowired private IBizWlInternshipTrainingService internshipTrainingService;
    @Autowired private IBizWlCourseDesignService courseDesignService;
    @Autowired private IBizWlThesisService thesisService;
    @Autowired private IBizWlConcentratedInternshipService concentratedInternshipService;
    @Autowired private IBizWlManagementService managementService;
    @Autowired private RuleParamService ruleParamService;

    @Override
    public FactorFormulaVo build(BizWorkloadItem item)
    {
        if (item == null || item.getItemType() == null) return null;
        return switch (item.getItemType())
        {
            case "G1" -> buildG1(item, theoryService.selectBizWlTheoryByItemId(item.getId()));
            case "G2" -> buildG2(item, practiceService.selectBizWlPracticeByItemId(item.getId()));
            case "G3" -> buildG3(item, internshipTrainingService.selectBizWlInternshipTrainingByItemId(item.getId()));
            case "G4" -> buildG4(item, courseDesignService.selectBizWlCourseDesignByItemId(item.getId()));
            case "G5" -> buildG5(item, thesisService.selectBizWlThesisByItemId(item.getId()));
            case "G6" -> buildG6(item, concentratedInternshipService.selectBizWlConcentratedInternshipByItemId(item.getId()));
            case "G11" -> buildG11(item, managementService.selectBizWlManagementByItemId(item.getId()));
            default -> null;
        };
    }

    private FactorFormulaVo buildG11(BizWorkloadItem item, BizWlManagement detail)
    {
        if (detail == null) return null;
        List<FactorVo> factors = new ArrayList<>();
        add(factors, "岗位减免", detail.getProratedAmount(), source(item),
                "教务确认的本学期岗位减免原值，直接计入 G11", NORMAL);
        String batch = detail.getSourceBatchId();
        String description = "岗位减免工作量（本学期）直接计入 G11；学期累计在汇总时按 180 封顶";
        if (batch != null && !batch.isBlank())
        {
            description += "；来源批次 " + batch;
        }
        return formula(item, "G11", "岗位减免工作量（本学期）", description, true, factors);
    }

    private FactorFormulaVo buildG1(BizWorkloadItem item, BizWlTheory d)
    {
        if (d == null) return null;
        List<FactorVo> f = new ArrayList<>();
        add(f, "J1", num(d.getJ1()), source(item), "理论学时", NORMAL);
        add(f, "C1", coef(d.getC1()), UNKNOWN, "重复系数", NORMAL);
        add(f, "K1", coef(d.getK1()), UNKNOWN, "课程类型系数", NORMAL);
        add(f, "Q1", coef(d.getQ1()), UNKNOWN, "教学质量系数", NORMAL);
        add(f, "Q2", coef(d.getQ2()), UNKNOWN, "课程质量系数", NORMAL);
        add(f, "N", coef(d.getN()), UNKNOWN, "合堂系数", NORMAL);
        add(f, "Q3", d.getQ3(), UNKNOWN, "全外文系数（仅展示，不参与计算）", DISPLAY_ONLY);
        return formula(item, "G1", "J1 × C1 × K1 × Q1 × Q2 × N", "Q3 仅展示，不参与工作量计算", true, f);
    }

    private FactorFormulaVo buildG2(BizWorkloadItem item, BizWlPractice d)
    {
        if (d == null) return null;
        List<FactorVo> f = new ArrayList<>();
        add(f, "J2", num(d.getJ2()), source(item), "实践学时", NORMAL);
        add(f, "K", coef(d.getK()), UNKNOWN, "专业类别系数", NORMAL);
        add(f, "C2", coef(d.getC2()), UNKNOWN, "实践重复系数", NORMAL);
        add(f, "Q1", coef(d.getQ1()), UNKNOWN, "教学质量系数", NORMAL);
        add(f, "Q2", coef(d.getQ2()), UNKNOWN, "课程质量系数", NORMAL);
        add(f, "Q3", d.getQ3(), UNKNOWN, "全外文系数（仅展示，不参与计算）", DISPLAY_ONLY);
        return formula(item, "G2", "J2 × K × C2 × Q1 × Q2", "Q3 仅展示，不参与工作量计算", true, f);
    }

    private FactorFormulaVo buildG3(BizWorkloadItem item, BizWlInternshipTraining d)
    {
        if (d == null) return null;
        List<FactorVo> f = new ArrayList<>();
        add(f, "T", num(d.getT()), source(item), "实际天数", NORMAL);
        add(f, "D", coef(d.getD()), UNKNOWN, "指导系数", NORMAL);
        add(f, "K", coef(d.getK()), UNKNOWN, "重复系数", NORMAL);
        add(f, "Q1", coef(d.getQ1()), UNKNOWN, "教学质量系数", NORMAL);
        add(f, "Q2", coef(d.getQ2()), UNKNOWN, "课程质量系数", NORMAL);
        add(f, "Q3", d.getQ3(), UNKNOWN, "全外文系数（仅展示，不参与计算）", DISPLAY_ONLY);
        return formula(item, "G3", "T × D × K × Q1 × Q2", "Q3 仅展示，不参与工作量计算", true, f);
    }

    private FactorFormulaVo buildG4(BizWorkloadItem item, BizWlCourseDesign d)
    {
        if (d == null) return null;
        List<FactorVo> f = new ArrayList<>();
        add(f, "J4", num(d.getJ4()), source(item), "课程设计学分", NORMAL);
        String status = Integer.valueOf(1).equals(item.getIsOverLimit()) ? "WARNING" : NORMAL;
        add(f, "R4", num(d.getR4()), source(item), "实际指导人数；超过阈值仅告警，不截断", status);
        BigDecimal constant = ruleParamService.get("CONST_COURSE_DESIGN", new BigDecimal("0.4"));
        add(f, "CONST", constant, FORMULA_CONSTANT, "当前规则中的课程设计工作量常量", NORMAL);
        return formula(item, "G4", "J4 × R4 × " + display(constant),
                "结果为已落库核算值；R4 按实际人数计算、不截断，超过阈值仅告警；常量展示当前规则，未保存历史规则快照",
                false, f);
    }

    private FactorFormulaVo buildG5(BizWorkloadItem item, BizWlThesis d)
    {
        if (d == null) return null;
        List<FactorVo> f = new ArrayList<>();
        String status = Integer.valueOf(1).equals(item.getIsOverLimit()) ? "APPROVAL_REQUIRED" : NORMAL;
        add(f, "R5", num(d.getR5()), source(item), "实际指导人数；超过申报阈值须报批但不封顶", status);
        add(f, "K5", coef(d.getK5()), UNKNOWN, "毕业论文指导系数", NORMAL);
        return formula(item, "G5", "R5 × K5", "按实际人数计算、未封顶；超过申报阈值仅须报批", true, f);
    }

    private FactorFormulaVo buildG6(BizWorkloadItem item, BizWlConcentratedInternship d)
    {
        if (d == null) return null;
        List<FactorVo> f = new ArrayList<>();
        BigDecimal cap = ruleParamService.get("CAP_R6_MAX", new BigDecimal("20"));
        BigDecimal actual = num(d.getR6());
        BigDecimal effective = actual.min(cap);
        boolean capped = actual.compareTo(cap) > 0;
        add(f, "W", num(d.getW()), source(item), "实习周数", NORMAL);
        add(f, "R6", actual, source(item), "实际指导人数", capped ? "CAPPED" : DISPLAY_ONLY);
        add(f, "R6_EFFECTIVE", effective, CURRENT_RULE_DERIVED,
                "由实际人数和当前规则派生的计入人数：min(R6, " + display(cap) + ")", NORMAL);
        BigDecimal constant = ruleParamService.get("CONST_COURSE_DESIGN", new BigDecimal("0.4"));
        add(f, "CONST", constant, FORMULA_CONSTANT, "当前规则中的集中实习工作量常量", NORMAL);
        return formula(item, "G6", "W × min(R6, " + display(cap) + ") × " + display(constant),
                "结果为已落库核算值；人数上限与常量展示当前规则，未保存历史规则快照", false, f);
    }

    private FactorFormulaVo formula(BizWorkloadItem item, String type, String expression,
            String description, boolean reproducible, List<FactorVo> factors)
    {
        boolean complete = factors.stream()
                .filter(factor -> !DISPLAY_ONLY.equals(factor.getStatus()))
                .allMatch(factor -> factor.getValue() != null);
        String resolvedDescription = complete ? description : description + "；存在缺失因子，无法可靠复算";
        return new FactorFormulaVo(type, expression, item.getCalculatedWorkload(),
                resolvedDescription, reproducible && complete, factors);
    }

    private BigDecimal coef(BigDecimal value) { return value; }
    private BigDecimal num(BigDecimal value) { return value; }
    private BigDecimal num(Long value) { return value == null ? null : new BigDecimal(value); }
    private String display(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
    private String source(BizWorkloadItem item)
    {
        String source = item.getSourceType();
        return "IMPORT".equals(source) || "AUTO".equals(source) || "MANUAL".equals(source)
                || "SELF".equals(source) ? source : UNKNOWN;
    }
    private void add(List<FactorVo> factors, String key, Object value, String source,
            String description, String status)
    {
        factors.add(new FactorVo(key, value, source, description, status));
    }
}
