package com.workload.system.service;

import java.math.BigDecimal;
import java.util.*;
import com.workload.system.calc.rule.*;
import com.workload.system.domain.BizWorkloadCalcSnapshot;
import com.workload.system.domain.vo.FactorFormulaVo.*;
import com.workload.system.service.SnapshotContextCodec.Context;

/** v3 inner evidence checks are independent of the re-computable outer content hash. */
final class PolicyEvidenceIntegrity {
    private PolicyEvidenceIntegrity() {}
    static void verify(BizWorkloadCalcSnapshot snapshot, Context c, Map<String,String> values, Map<String,String> sourceLabels) {
        if (c.policySources()==null || c.factorEvidence()==null || c.decisionEvidence()==null
                || c.missingEvidenceReasons()==null || !Set.of("COMPLETE","PARTIAL").contains(c.evidenceStatus()))
            throw SnapshotIntegrity.failure("缺少政策来源完整性状态");
        java.time.LocalDate.parse(c.asOfDate());
        Map<String,RuleResolution> sources=new HashMap<>(); String previous=null;
        for (RuleResolution r:c.policySources()) {
            r.verify();
            if (!c.asOfDate().equals(r.asOfDate())) throw SnapshotIntegrity.failure("政策选择日与计算上下文不一致");
            if (previous!=null && previous.compareTo(r.requestedCode())>=0) throw SnapshotIntegrity.failure("政策集合编码重复或顺序不规范");
            previous=r.requestedCode(); sources.put(r.resolutionHash(),r);
        }
        if (!PolicyHash.hash(c.policySources()).equals(snapshot.getRuleVersion())) throw SnapshotIntegrity.failure("政策集合摘要不符");
        Map<String,SnapshotContextCodec.FactorContext> metadata=new HashMap<>();
        c.factors().forEach(f -> metadata.put(f.key(),f));
        Set<String> expectedKeys=switch(c.formulaType()) {
            case "G1" -> Set.of("J1","C1","K1","Q1","Q2","N","Q3");
            case "G2" -> Set.of("J2","K","C2","Q1","Q2","Q3");
            case "G3" -> Set.of("T","D","K","Q1","Q2","Q3");
            case "G4" -> Set.of("J4","R4","CONST"); case "G5" -> Set.of("R5","K5");
            case "G6" -> Set.of("W","R6","R6_EFFECTIVE","CONST"); case "G11" -> Set.of("岗位减免");
            default -> throw SnapshotIntegrity.failure("不支持的来源公式类别");
        };
        if (!expectedKeys.equals(values.keySet())) throw SnapshotIntegrity.failure("类别公式因子集合不完整");
        Set<String> referenced=new HashSet<>(), seen=new HashSet<>(), missingReasons=new HashSet<>(); boolean missing=false;
        for (FactorEvidence f:c.factorEvidence()) {
            if (f==null || !seen.add(f.key()) || !values.containsKey(f.key()) || !same(values.get(f.key()),f.boundValue()))
                throw SnapshotIntegrity.failure("因子证据取值绑定不符");
            String expectedKind=switch(c.formulaType()) {
                case "G1", "G2", "G3" -> "Q3".equals(f.key()) ? "DISPLAY_ONLY" : "MISSING_HISTORICAL_SOURCE";
                case "G4" -> "CONST".equals(f.key()) ? "POLICY_RULE" : inputKind(f.boundValue());
                case "G5" -> "K5".equals(f.key()) ? "MISSING_HISTORICAL_SOURCE" : inputKind(f.boundValue());
                case "G6" -> switch(f.key()) { case "CONST" -> "POLICY_RULE"; case "R6_EFFECTIVE" -> "DERIVED"; default -> inputKind(f.boundValue()); };
                case "G11" -> "MISSING_HISTORICAL_SOURCE"; // no provenance column exists yet; never invent a field named 岗位减免.
                default -> throw SnapshotIntegrity.failure("未知来源类别");
            };
            if (!expectedKind.equals(f.kind())) throw SnapshotIntegrity.failure("来源种类与类别因子语义不一致");
            SnapshotContextCodec.FactorContext meta=metadata.get(f.key());
            if (f.ruleRef()!=null) referenced.add(f.ruleRef());
            switch (f.kind()) {
                case "RECORDED_INPUT" -> {
                    String expectedTable=switch(c.formulaType()) {
                        case "G4" -> "biz_wl_course_design"; case "G5" -> "biz_wl_thesis";
                        case "G6" -> "biz_wl_concentrated_internship"; default -> null;
                    };
                    if (expectedTable==null || !expectedTable.equals(f.table()) || !f.key().equals(f.field())
                            || !Objects.equals(meta.sourceRef(),f.table()+":"+snapshot.getItemId()+":"+f.field()) || f.ruleRef()!=null
                            || !"RECORDED_INPUT".equals(sourceLabels.get(f.key())))
                        throw SnapshotIntegrity.failure("输入证据缺少表字段");
                }
                case "POLICY_RULE" -> {
                    RuleResolution r=source(sources,f.ruleRef());
                    if ((!("G4".equals(c.formulaType()) || "G6".equals(c.formulaType())) || !"CONST".equals(f.key())
                            || !"CONST_COURSE_DESIGN".equals(r.requestedCode())) || !same(r.content().value(),f.boundValue()) || !r.sourceKind().equals(sourceLabels.get(f.key()))
                            || !Objects.equals(meta.sourceRef(),f.ruleRef()) || !Objects.equals(meta.ruleCode(),r.requestedCode())
                            || !Objects.equals(meta.ruleVersion(),r.contentHash())) throw SnapshotIntegrity.failure("政策因子值或元数据绑定不符");
                }
                case "DERIVED" -> {
                    RuleResolution r=source(sources,f.ruleRef());
                    if (!"G6".equals(c.formulaType()) || !"R6_EFFECTIVE".equals(f.key()) || !"CAP_R6_MAX".equals(r.requestedCode())
                            || !"R6".equals(f.inputKey()) || !"DERIVED".equals(sourceLabels.get(f.key())) || !Objects.equals(meta.sourceRef(),f.ruleRef())
                            || !"MIN".equals(f.operation()) || !values.containsKey(f.inputKey())
                            || !same(numberOrZero(values.get(f.inputKey())).min(r.value()).toPlainString(),f.boundValue()))
                        throw SnapshotIntegrity.failure("派生因子证据不符");
                }
                case "MISSING_HISTORICAL_SOURCE" -> {
                    missing=true; missingReasons.add(f.missingReason());
                    if (f.missingReason()==null || f.missingReason().isBlank() || f.ruleRef()!=null
                            || !"MISSING_HISTORICAL_SOURCE".equals(sourceLabels.get(f.key()))
                            || meta.sourceRef()!=null || meta.ruleCode()!=null || meta.ruleVersion()!=null)
                        throw SnapshotIntegrity.failure("历史来源缺失原因不完整");
                }
                case "DISPLAY_ONLY" -> {
                    if (f.ruleRef()!=null || meta.sourceRef()!=null || meta.ruleCode()!=null || meta.ruleVersion()!=null
                            || !"DISPLAY_ONLY".equals(sourceLabels.get(f.key()))) throw SnapshotIntegrity.failure("展示因子冒用政策引用");
                }
                default -> throw SnapshotIntegrity.failure("未知因子来源种类");
            }
        }
        if (!seen.equals(values.keySet())) throw SnapshotIntegrity.failure("因子证据遗漏");
        Set<String> decisionKeys=new HashSet<>();
        for (DecisionEvidence d:c.decisionEvidence()) {
            if (d==null || !decisionKeys.add(d.key())) throw SnapshotIntegrity.failure("决策证据重复或缺失");
            RuleResolution r=source(sources,d.ruleRef()); referenced.add(d.ruleRef());
            if (!(values.get(d.inputKey()) == null ? "GT_NULL_AS_ZERO" : "GT").equals(d.operation())
                    || !same(r.content().value(),d.threshold()) || !values.containsKey(d.inputKey())
                    || (numberOrZero(values.get(d.inputKey())).compareTo(r.value())>0)!=d.result())
                throw SnapshotIntegrity.failure("决策阈值或输入绑定不符");
            String expectedCode=switch (d.key()) {
                case "G4_LIMIT" -> "CAP_R4_MAX";
                case "G6_LIMIT" -> "CAP_R6_MAX";
                case "G5_APPROVAL" -> d.educationLevel()!=null && d.educationLevel().contains("专") ? "APPROVAL_R5_JUNIOR" : "APPROVAL_R5_BACHELOR";
                default -> throw SnapshotIntegrity.failure("未知决策证据");
            };
            String expectedInput=d.key().equals("G4_LIMIT")?"R4":d.key().equals("G6_LIMIT")?"R6":"R5";
            if (!expectedCode.equals(r.requestedCode()) || !expectedInput.equals(d.inputKey()))
                throw SnapshotIntegrity.failure("决策政策与类别或教育层次不符");
        }
        String requiredDecision=switch(c.formulaType()) { case "G4" -> "G4_LIMIT"; case "G5" -> "G5_APPROVAL"; case "G6" -> "G6_LIMIT"; default -> null; };
        if (requiredDecision != null && !decisionKeys.equals(Set.of(requiredDecision))
                || requiredDecision == null && !decisionKeys.isEmpty() || !referenced.equals(sources.keySet()))
            throw SnapshotIntegrity.failure("遗漏决策或政策来源引用");
        if (c.policySources().isEmpty()) { missing=true; missingReasons.add("NO_CAPTURED_POLICY_SOURCE"); }
        if (!new HashSet<>(c.missingEvidenceReasons()).equals(missingReasons)) throw SnapshotIntegrity.failure("来源缺失原因与因子证据不一致");
        if ("COMPLETE".equals(c.evidenceStatus()) && (missing || !c.missingEvidenceReasons().isEmpty())
                || "PARTIAL".equals(c.evidenceStatus()) && c.missingEvidenceReasons().isEmpty())
            throw SnapshotIntegrity.failure("来源完整性状态与证据不一致");
    }
    private static RuleResolution source(Map<String,RuleResolution> sources,String ref) {
        RuleResolution r=sources.get(ref); if (r==null) throw SnapshotIntegrity.failure("政策来源引用不存在"); return r;
    }
    private static String inputKind(String value) { return value==null ? "MISSING_HISTORICAL_SOURCE" : "RECORDED_INPUT"; }
    private static BigDecimal numberOrZero(String value) { return value == null ? BigDecimal.ZERO : new BigDecimal(value); }
    private static boolean same(String a,String b) {
        if (Objects.equals(a,b)) return true;
        if (a==null || b==null) return false;
        try { return new BigDecimal(a).compareTo(new BigDecimal(b))==0; } catch (NumberFormatException e) { return false; }
    }
}
