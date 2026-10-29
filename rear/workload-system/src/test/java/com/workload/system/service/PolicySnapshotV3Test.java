package com.workload.system.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.workload.system.calc.rule.*;
import com.workload.system.domain.*;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.domain.vo.FactorFormulaVo.*;
import com.workload.system.mapper.BizWorkloadCalcSnapshotMapper;

class PolicySnapshotV3Test {
    private FactorFormulaVo formula() {
        RuleResolution rule=RuleResolution.fallback("CONST_COURSE_DESIGN",LocalDate.of(2026,1,31),new BigDecimal("0.4"),"常量","NO_EFFECTIVE_RULE");
        FactorFormulaVo vo=new FactorFormulaVo("G4","J4 × R4 × 0.4",new BigDecimal("8"),"说明",true,List.of(
                new FactorVo("J4",new BigDecimal("2"),"RECORDED_INPUT","学分","NORMAL"),
                new FactorVo("R4",new BigDecimal("10"),"RECORDED_INPUT","人数","NORMAL"),
                new FactorVo("CONST",new BigDecimal("0.4"),"CODE_FALLBACK","常量","NORMAL")));
        RuleResolution cap=RuleResolution.fallback("CAP_R4_MAX",LocalDate.of(2026,1,31),new BigDecimal("60"),"告警阈值","NO_EFFECTIVE_RULE");
        vo.getFactors().get(2).setRuleCode(rule.requestedCode()); vo.getFactors().get(2).setRuleVersion(rule.contentHash());
        vo.getFactors().get(2).setSourceRef(rule.resolutionHash());
        vo.setAsOfDate("2026-01-31");
        vo.getFactors().get(0).setSourceRef("biz_wl_course_design:9:J4");vo.getFactors().get(1).setSourceRef("biz_wl_course_design:9:R4");
        vo.setPolicySources(List.of(cap,rule));
        vo.setFactorEvidence(List.of(new FactorEvidence("J4","RECORDED_INPUT","2","biz_wl_course_design","J4",null,null,null,null),
                new FactorEvidence("R4","RECORDED_INPUT","10","biz_wl_course_design","R4",null,null,null,null),
                new FactorEvidence("CONST","POLICY_RULE","0.4",null,null,rule.resolutionHash(),null,null,null)));
        vo.setDecisionEvidence(List.of(new DecisionEvidence("G4_LIMIT", cap.resolutionHash(), "R4", "GT", "60", false, null))); vo.setEvidenceStatus("COMPLETE"); vo.setMissingEvidenceReasons(List.of()); return vo;
    }
    private BizWorkloadCalcSnapshot snapshot() {
        BizWorkloadItem item=new BizWorkloadItem(); item.setId(9L);
        FactorFormulaVo f=formula();
        return new WorkloadSnapshotService(mock(BizWorkloadCalcSnapshotMapper.class)).capture(item,f,PolicyHash.hash(f.getPolicySources()));
    }
    @Test void v3RestoresSelfContainedPolicyAndEvidence() {
        BizWorkloadCalcSnapshot s=snapshot();
        assertThat(s.getContextJson()).contains("\"formatVersion\":3");
        SnapshotIntegrity.verify(s);
        FactorFormulaVo restored=SnapshotContextCodec.restore(s.getContextJson(),formula());
        assertThat(restored.getPolicySources()).isEqualTo(formula().getPolicySources());
        assertThat(restored.getEvidenceStatus()).isEqualTo("COMPLETE");
    }
    @Test void sourceContentTamperingIsRejectedEvenAfterOuterHashIsRecomputed() {
        BizWorkloadCalcSnapshot s=snapshot(); s.setContextJson(s.getContextJson().replace("NO_EFFECTIVE_RULE","OTHER_REASON").replace("\"value\":\"0.4\"","\"value\":\"0.9\""));
        s.setSnapshotHash(SnapshotIntegrity.hashV3(s));
        assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).satisfies(e -> assertThat(SnapshotIntegrity.isFailure((RuntimeException)e)).isTrue());
    }
    @Test void fakeReferenceOrValueBindingIsRejectedEvenAfterOuterHashIsRecomputed() {
        for (String target:List.of("reference","value")) {
            BizWorkloadCalcSnapshot s=snapshot();
            if (target.equals("reference")) s.setContextJson(s.getContextJson().replace(formula().getPolicySources().get(0).resolutionHash(),"a".repeat(64)));
            else s.setFactorJson(s.getFactorJson().replace("0.4","0.9"));
            s.setSnapshotHash(SnapshotIntegrity.hashV3(s));
            assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).satisfies(e -> assertThat(SnapshotIntegrity.isFailure((RuntimeException)e)).isTrue());
        }
    }
    @Test void deletingCapturedDecisionCannotTurnACompleteG4IntoAValidSnapshot() throws Exception {
        BizWorkloadCalcSnapshot s=snapshot();
        var tree=(com.fasterxml.jackson.databind.node.ObjectNode)new com.fasterxml.jackson.databind.ObjectMapper().readTree(s.getContextJson());
        tree.putArray("decisionEvidence");s.setContextJson(tree.toString());s.setSnapshotHash(SnapshotIntegrity.hashV3(s));
        assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).satisfies(e -> assertThat(SnapshotIntegrity.isFailure((RuntimeException)e)).isTrue());
    }
    @Test void misleadingFactorMetadataAndSourceTextCannotOverrideInnerEvidence() throws Exception {
        for (String mutation:List.of("meta","source")) {
            BizWorkloadCalcSnapshot s=snapshot();
            if (mutation.equals("source")) s.setSourceJson(s.getSourceJson().replace("CODE_FALLBACK","DB_RULE"));
            else {
                var tree=(com.fasterxml.jackson.databind.node.ObjectNode)new com.fasterxml.jackson.databind.ObjectMapper().readTree(s.getContextJson());
                ((com.fasterxml.jackson.databind.node.ObjectNode)tree.get("factors").get(2)).put("sourceRef","fake");
                s.setContextJson(tree.toString());
            }
            s.setSnapshotHash(SnapshotIntegrity.hashV3(s));
            assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).satisfies(e -> assertThat(SnapshotIntegrity.isFailure((RuntimeException)e)).isTrue());
        }
    }

    @Test void constantCannotBorrowAValidWarningPolicyEvenWithConsistentRefsAndRecomputedHashes() throws Exception {
        BizWorkloadCalcSnapshot s=snapshot();var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var tree=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(s.getContextJson());
        RuleResolution cap=formula().getPolicySources().get(0);
        ((com.fasterxml.jackson.databind.node.ObjectNode)tree.path("factors").get(2)).put("sourceRef",cap.resolutionHash()).put("ruleCode",cap.requestedCode()).put("ruleVersion",cap.contentHash());
        ((com.fasterxml.jackson.databind.node.ObjectNode)tree.path("factorEvidence").get(2)).put("ruleRef",cap.resolutionHash()).put("boundValue","60");
        tree.putArray("policySources").add(mapper.valueToTree(cap));
        s.setContextJson(tree.toString());s.setFactorJson(s.getFactorJson().replace("0.4","60"));
        s.setRuleVersion(PolicyHash.hash(List.of(cap)));s.setSnapshotHash(SnapshotIntegrity.hashV3(s));
        assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).satisfies(e -> assertThat(SnapshotIntegrity.isFailure((RuntimeException)e)).isTrue());
    }

    @Test void legacyFormatsRejectV3EvidenceFieldsEvenWithRecomputedOuterHashes() {
        for (int version:List.of(1,2)) for (String field:List.of("policySources","factorEvidence","decisionEvidence","evidenceStatus","missingEvidenceReasons","asOfDate")) {
            FactorFormulaVo f=new FactorFormulaVo("G5","R5 × K5",new BigDecimal("2"),"old",List.of(
                    new FactorVo("R5","2","IMPORT_VALUE","count","NORMAL"),new FactorVo("K5","1","RULE_DEFAULT","rate","NORMAL")));
            BizWorkloadItem i=new BizWorkloadItem();i.setId(9L);
            var s=new WorkloadSnapshotService(mock(BizWorkloadCalcSnapshotMapper.class)).capture(i,f,"old");
            s.setContextJson(s.getContextJson().replace("\"formatVersion\":2","\"formatVersion\":"+version).replaceFirst("\\{","{\""+field+"\":null,"));
            if (version==1) {
                s.setSnapshotHash(SnapshotHashTestFixture.legacyHash(s));
                assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).satisfies(e -> assertThat(SnapshotIntegrity.isFailure((RuntimeException)e)).isTrue());
            } else assertThatThrownBy(() -> SnapshotIntegrity.hashV2(s)).satisfies(e -> assertThat(SnapshotIntegrity.isFailure((RuntimeException)e)).isTrue());
        }
    }

    @Test void recordedInputReferenceCannotClaimAnotherTableOrField() throws Exception {
        BizWorkloadCalcSnapshot s=snapshot();var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var tree=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(s.getContextJson());
        ((com.fasterxml.jackson.databind.node.ObjectNode)tree.path("factorEvidence").get(0)).put("table","other_table").put("field","K5");
        s.setContextJson(tree.toString());s.setSnapshotHash(SnapshotIntegrity.hashV3(s));
        assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).satisfies(e -> assertThat(SnapshotIntegrity.isFailure((RuntimeException)e)).isTrue());
    }

    @Test void constantCannotEscapePolicyBindingThroughAnotherEvidenceKind() throws Exception {
        for (String kind:List.of("RECORDED_INPUT","DISPLAY_ONLY","MISSING_HISTORICAL_SOURCE")) {
            BizWorkloadCalcSnapshot s=snapshot();var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(s.getContextJson());
            var meta=(com.fasterxml.jackson.databind.node.ObjectNode)tree.path("factors").get(2);
            meta.putNull("ruleCode").putNull("ruleVersion").putNull("sourceRef");
            var evidence=(com.fasterxml.jackson.databind.node.ObjectNode)tree.path("factorEvidence").get(2);
            evidence.put("kind",kind).putNull("ruleRef");
            if (kind.equals("RECORDED_INPUT")) { evidence.put("table","biz_wl_course_design").put("field","CONST");meta.put("sourceRef","biz_wl_course_design:9:CONST"); }
            if (kind.equals("MISSING_HISTORICAL_SOURCE")) {
                evidence.put("missingReason","MISSING_HISTORICAL_SOURCE:CONST");tree.put("evidenceStatus","PARTIAL");tree.putArray("missingEvidenceReasons").add("MISSING_HISTORICAL_SOURCE:CONST");
            }
            RuleResolution cap=formula().getPolicySources().get(0);tree.putArray("policySources").add(mapper.valueToTree(cap));
            s.setContextJson(tree.toString());s.setSourceJson(s.getSourceJson().replace("CODE_FALLBACK",kind));s.setRuleVersion(PolicyHash.hash(List.of(cap)));s.setSnapshotHash(SnapshotIntegrity.hashV3(s));
            assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).satisfies(e -> assertThat(SnapshotIntegrity.isFailure((RuntimeException)e)).isTrue());
        }
    }

    @Test void deletingTheWholeMandatoryPolicyFactorCannotLeaveACompleteSnapshot() throws Exception {
        BizWorkloadCalcSnapshot s=snapshot();var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var tree=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(s.getContextJson());
        ((com.fasterxml.jackson.databind.node.ArrayNode)tree.path("factors")).remove(2);
        ((com.fasterxml.jackson.databind.node.ArrayNode)tree.path("factorEvidence")).remove(2);
        RuleResolution cap=formula().getPolicySources().get(0);tree.putArray("policySources").add(mapper.valueToTree(cap));
        var factors=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(s.getFactorJson());factors.remove("CONST");
        var sources=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(s.getSourceJson());sources.remove("CONST");
        s.setFactorJson(factors.toString());s.setSourceJson(sources.toString());s.setContextJson(tree.toString());
        s.setRuleVersion(PolicyHash.hash(List.of(cap)));s.setSnapshotHash(SnapshotIntegrity.hashV3(s));
        assertThatThrownBy(() -> SnapshotIntegrity.verify(s)).satisfies(e -> assertThat(SnapshotIntegrity.isFailure((RuntimeException)e)).isTrue());
    }

}
