package com.workload.system.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.domain.BizWlConcentratedInternship;
import com.workload.system.domain.BizWlCourseDesign;
import com.workload.system.domain.BizWlInternshipTraining;
import com.workload.system.domain.BizWlManagement;
import com.workload.system.domain.BizWlPractice;
import com.workload.system.domain.BizWlTheory;
import com.workload.system.domain.BizWlThesis;
import com.workload.system.domain.BizWorkloadCalcSnapshot;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.domain.vo.FactorFormulaVo.FactorVo;
import com.workload.system.domain.vo.FactorFormulaVo.SourceTaskVo;
import com.workload.system.mapper.BizWorkloadCalcSnapshotMapper;
import com.workload.system.service.IBizTeachingTaskService;
import com.workload.system.service.IBizWlConcentratedInternshipService;
import com.workload.system.service.IBizWlCourseDesignService;
import com.workload.system.service.IBizWlInternshipTrainingService;
import com.workload.system.service.IBizWlManagementService;
import com.workload.system.service.IBizWlPracticeService;
import com.workload.system.service.IBizWlTheoryService;
import com.workload.system.service.IBizWlThesisService;
import com.workload.system.service.IWorkloadFactorFormulaService;

/**
 * 查询 G1-G6、G11 明细并构造可追溯的公式说明。
 *
 * <p>详情读取优先级：若该明细已存在计算快照（{@code selectLatestByItemId} 命中），
 * 因子的取值与来源一律从<strong>已固化的快照</strong>解释，来源标记直接用快照里的 source
 * （如 {@code APPROVED_OVERRIDE}），不用「当前规则」现场重算伪装历史来源；无快照才回落读子表并
 * 显式置 {@code legacy=true / reproducible=false}。快照 JSON 解析失败直接抛出，
 * 不吞异常后伪装可复现。</p>
 */
@Service
public class WorkloadFactorFormulaServiceImpl implements IWorkloadFactorFormulaService
{
    /** 因子来源枚举（归一后，替换旧的 NORMAL/UNKNOWN/CURRENT_RULE_DERIVED）。 */
    private static final String IMPORT_VALUE = "IMPORT_VALUE";
    private static final String RULE_DEFAULT = "RULE_DEFAULT";
    private static final String FORMULA_CONSTANT = "FORMULA_CONSTANT";
    private static final String DERIVED = "DERIVED";
    private static final String DISPLAY_ONLY = "DISPLAY_ONLY";

    @Autowired private IBizWlTheoryService theoryService;
    @Autowired private IBizWlPracticeService practiceService;
    @Autowired private IBizWlInternshipTrainingService internshipTrainingService;
    @Autowired private IBizWlCourseDesignService courseDesignService;
    @Autowired private IBizWlThesisService thesisService;
    @Autowired private IBizWlConcentratedInternshipService concentratedInternshipService;
    @Autowired private IBizWlManagementService managementService;
    @Autowired private RuleParamService ruleParamService;
    @Autowired private BizWorkloadCalcSnapshotMapper snapshotMapper;
    @Autowired private IBizTeachingTaskService teachingTaskService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public FactorFormulaVo build(BizWorkloadItem item)
    {
        FactorFormulaVo vo = buildFresh(item);
        if (vo == null) return null;
        // 详情读取：用最新已固化快照覆盖因子取值/来源（历史保真），无快照则 legacy。
        applySnapshotOrLegacy(vo, item);
        return vo;
    }

    @Override
    public FactorFormulaVo buildFresh(BizWorkloadItem item)
    {
        if (item == null || item.getItemType() == null) return null;
        FactorFormulaVo vo = switch (item.getItemType())
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
        if (vo == null) return null;
        // 源数据（教学任务）只读快照始终按当前任务表构造，与是否 overlay 无关。
        vo.setSourceTask(buildSourceTask(item));
        return vo;
    }

    /**
     * 详情来源解释：命中最新快照则从快照覆盖因子取值/来源并标记可复现；无快照回落 legacy。
     * 快照 JSON 解析失败直接抛出，绝不吞异常后伪装为可复现。
     */
    private void applySnapshotOrLegacy(FactorFormulaVo vo, BizWorkloadItem item)
    {
        BizWorkloadCalcSnapshot snapshot = item.getId() == null ? null
                : snapshotMapper.selectLatestByItemId(item.getId());
        if (snapshot == null)
        {
            vo.setLegacy(true);
            vo.setReproducible(false);
            return;
        }
        vo.setLegacy(false);
        vo.setReproducible(true);
        vo.setSnapshotVersion(snapshot.getCalculationVersion());
        vo.setSnapshotHash(snapshot.getSnapshotHash());
        vo.setCalculatedAt(snapshot.getCreatedAt());
        overlayFromSnapshot(vo, snapshot);
    }

    /** 用快照里已固化的 factor_json / source_json 覆盖每个因子的取值与来源。 */
    private void overlayFromSnapshot(FactorFormulaVo vo, BizWorkloadCalcSnapshot snapshot)
    {
        Map<String, String> values = parseMap(snapshot.getFactorJson());
        Map<String, String> sources = parseMap(snapshot.getSourceJson());
        if (vo.getFactors() == null) return;
        for (FactorVo factor : vo.getFactors())
        {
            if (factor == null || factor.getKey() == null) continue;
            if (sources.containsKey(factor.getKey()))
            {
                factor.setSource(sources.get(factor.getKey()));
            }
            if (values.containsKey(factor.getKey()))
            {
                factor.setValue(parseNumericOrRaw(values.get(factor.getKey())));
            }
        }
    }

    private Map<String, String> parseMap(String json)
    {
        if (json == null || json.isBlank())
        {
            return java.util.Collections.emptyMap();
        }
        try
        {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        }
        catch (Exception e)
        {
            // 快照存在但解析失败：显式失败，不得静默回落成「可复现」的假象
            throw new ServiceException("计算快照解析失败，无法可靠解释因子来源：" + e.getMessage());
        }
    }

    private Object parseNumericOrRaw(String raw)
    {
        if (raw == null) return null;
        try
        {
            return new BigDecimal(raw);
        }
        catch (NumberFormatException e)
        {
            return raw;
        }
    }

    /** 从教学任务（by taskId）关联出只读源数据快照；不把这些字段复制回主表。 */
    private SourceTaskVo buildSourceTask(BizWorkloadItem item)
    {
        if (item.getTaskId() == null) return null;
        BizTeachingTask task = teachingTaskService.selectBizTeachingTaskById(item.getTaskId());
        if (task == null) return null;
        SourceTaskVo vo = new SourceTaskVo();
        vo.setCourseLevel(task.getCourseLevel());
        vo.setCourseRole(task.getCourseRole());
        vo.setCourseNature(task.getCourseNature());
        vo.setClassName(task.getClassName());
        vo.setRepeatOrder(task.getRepeatOrder());
        // teachingEval 仅在导入时用于派生 Q1，未持久化到 biz_teaching_task；此处保持 null
        return vo;
    }

    private FactorFormulaVo buildG11(BizWorkloadItem item, BizWlManagement detail)
    {
        if (detail == null) return null;
        List<FactorVo> factors = new ArrayList<>();
        add(factors, "岗位减免", detail.getProratedAmount(), IMPORT_VALUE,
                "教务确认的本学期岗位减免原值，直接计入 G11", "NORMAL");
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
        add(f, "J1", num(d.getJ1()), IMPORT_VALUE, "理论学时", "NORMAL");
        add(f, "C1", coef(d.getC1()), RULE_DEFAULT, "重复系数", "NORMAL");
        add(f, "K1", coef(d.getK1()), RULE_DEFAULT, "课程类型系数", "NORMAL");
        add(f, "Q1", coef(d.getQ1()), RULE_DEFAULT, "教学质量系数", "NORMAL");
        add(f, "Q2", coef(d.getQ2()), RULE_DEFAULT, "课程质量系数", "NORMAL");
        add(f, "N", coef(d.getN()), RULE_DEFAULT, "合堂系数", "NORMAL");
        add(f, "Q3", d.getQ3(), DISPLAY_ONLY, "全外文系数（仅展示，不参与计算）", DISPLAY_ONLY);
        return formula(item, "G1", "J1 × C1 × K1 × Q1 × Q2 × N", "Q3 仅展示，不参与工作量计算", true, f);
    }

    private FactorFormulaVo buildG2(BizWorkloadItem item, BizWlPractice d)
    {
        if (d == null) return null;
        List<FactorVo> f = new ArrayList<>();
        add(f, "J2", num(d.getJ2()), IMPORT_VALUE, "实践学时", "NORMAL");
        add(f, "K", coef(d.getK()), RULE_DEFAULT, "专业类别系数", "NORMAL");
        add(f, "C2", coef(d.getC2()), RULE_DEFAULT, "实践重复系数", "NORMAL");
        add(f, "Q1", coef(d.getQ1()), RULE_DEFAULT, "教学质量系数", "NORMAL");
        add(f, "Q2", coef(d.getQ2()), RULE_DEFAULT, "课程质量系数", "NORMAL");
        add(f, "Q3", d.getQ3(), DISPLAY_ONLY, "全外文系数（仅展示，不参与计算）", DISPLAY_ONLY);
        return formula(item, "G2", "J2 × K × C2 × Q1 × Q2", "Q3 仅展示，不参与工作量计算", true, f);
    }

    private FactorFormulaVo buildG3(BizWorkloadItem item, BizWlInternshipTraining d)
    {
        if (d == null) return null;
        List<FactorVo> f = new ArrayList<>();
        add(f, "T", num(d.getT()), IMPORT_VALUE, "实际天数", "NORMAL");
        add(f, "D", coef(d.getD()), RULE_DEFAULT, "指导系数", "NORMAL");
        add(f, "K", coef(d.getK()), RULE_DEFAULT, "重复系数", "NORMAL");
        add(f, "Q1", coef(d.getQ1()), RULE_DEFAULT, "教学质量系数", "NORMAL");
        add(f, "Q2", coef(d.getQ2()), RULE_DEFAULT, "课程质量系数", "NORMAL");
        add(f, "Q3", d.getQ3(), DISPLAY_ONLY, "全外文系数（仅展示，不参与计算）", DISPLAY_ONLY);
        return formula(item, "G3", "T × D × K × Q1 × Q2", "Q3 仅展示，不参与工作量计算", true, f);
    }

    private FactorFormulaVo buildG4(BizWorkloadItem item, BizWlCourseDesign d)
    {
        if (d == null) return null;
        List<FactorVo> f = new ArrayList<>();
        add(f, "J4", num(d.getJ4()), IMPORT_VALUE, "课程设计学分", "NORMAL");
        String status = Integer.valueOf(1).equals(item.getIsOverLimit()) ? "WARNING" : "NORMAL";
        add(f, "R4", num(d.getR4()), IMPORT_VALUE, "实际指导人数；超过阈值仅告警，不截断", status);
        BigDecimal constant = ruleParamService.get("CONST_COURSE_DESIGN", new BigDecimal("0.4"));
        add(f, "CONST", constant, FORMULA_CONSTANT, "当前规则中的课程设计工作量常量", "NORMAL");
        return formula(item, "G4", "J4 × R4 × " + display(constant),
                "结果为已落库核算值；R4 按实际人数计算、不截断，超过阈值仅告警；常量展示当前规则，未保存历史规则快照",
                false, f);
    }

    private FactorFormulaVo buildG5(BizWorkloadItem item, BizWlThesis d)
    {
        if (d == null) return null;
        List<FactorVo> f = new ArrayList<>();
        String status = Integer.valueOf(1).equals(item.getIsOverLimit()) ? "APPROVAL_REQUIRED" : "NORMAL";
        add(f, "R5", num(d.getR5()), IMPORT_VALUE, "实际指导人数；超过申报阈值须报批但不封顶", status);
        add(f, "K5", coef(d.getK5()), RULE_DEFAULT, "毕业论文指导系数", "NORMAL");
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
        add(f, "W", num(d.getW()), IMPORT_VALUE, "实习周数", "NORMAL");
        add(f, "R6", actual, IMPORT_VALUE, "实际指导人数", capped ? "CAPPED" : DISPLAY_ONLY);
        add(f, "R6_EFFECTIVE", effective, DERIVED,
                "由实际人数和当前规则派生的计入人数：min(R6, " + display(cap) + ")", "NORMAL");
        BigDecimal constant = ruleParamService.get("CONST_COURSE_DESIGN", new BigDecimal("0.4"));
        add(f, "CONST", constant, FORMULA_CONSTANT, "当前规则中的集中实习工作量常量", "NORMAL");
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
    private void add(List<FactorVo> factors, String key, Object value, String source,
            String description, String status)
    {
        factors.add(new FactorVo(key, value, source, description, status));
    }
}
