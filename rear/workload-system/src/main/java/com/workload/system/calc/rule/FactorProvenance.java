package com.workload.system.calc.rule;

import java.math.BigDecimal;
import java.util.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.workload.system.service.SnapshotIntegrity;

/** Persisted at the moment a value is chosen. Never reconstructed by matching a current value. */
public final class FactorProvenance {
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private FactorProvenance() {}
    public record Proof(String category, String key, String value, String kind, String role,
            String branch, String branchContent, Map<String,String> inputs, List<RuleResolution> rules,
            Map<String,String> references, String hash) {}
    public record Document(int formatVersion, Long itemId, String category, Map<String,Proof> factors, String hash) {}
    public static Proof create(String category, String key, Object value, String kind, String role,
            String branch, Map<String,String> inputs, List<RuleResolution> rules, Map<String,String> refs) {
        Proof p = new Proof(category,key,plain(value),kind,role,branch,content(branch),freeze(inputs),List.copyOf(rules),freeze(refs),null);
        return new Proof(p.category(),p.key(),p.value(),p.kind(),p.role(),p.branch(),p.branchContent(),p.inputs(),p.rules(),p.references(),proofHash(p));
    }
    public static void requireDecimal2(BigDecimal value) {
        if(value==null) return;
        try { if(value.setScale(2,java.math.RoundingMode.UNNECESSARY).precision()>10) throw new ArithmeticException(); }
        catch(ArithmeticException e) {throw new com.workload.common.exception.ServiceException("因子不能无损存入DECIMAL(10,2)");}
    }
    public static String plain(Object value) { return value == null ? null : value instanceof BigDecimal b ? b.toPlainString() : value.toString(); }
    private static Map<String,String> freeze(Map<String,String> m) { return Collections.unmodifiableMap(new TreeMap<>(m)); }
    private static String proofHash(Proof p) { return PolicyHash.hash(Arrays.asList(p.category(),p.key(),p.value(),p.kind(),p.role(),p.branch(),p.branchContent(),p.inputs(),p.rules(),p.references())); }
    public static String encode(Long id,String category,Map<String,Proof> proofs) {
        try {
            Map<String,Proof> frozen=Collections.unmodifiableMap(new TreeMap<>(proofs));
            return JSON.writeValueAsString(new Document(1,id,category,frozen,PolicyHash.hash(Arrays.asList(1,id,category,frozen))));
        } catch(Exception e) { throw SnapshotIntegrity.failure("保存因子来源失败："+e.getMessage()); }
    }
    public static Map<String,Proof> read(String json,Long id,String category) {
        if(json==null) return Map.of();
        try {
            Document d=JSON.readValue(json,Document.class);
            if(d.formatVersion()!=1 || !Objects.equals(id,d.itemId()) || !Objects.equals(category,d.category()) || d.factors()==null
                    || !Objects.equals(d.hash(),PolicyHash.hash(Arrays.asList(1,d.itemId(),d.category(),d.factors()))))
                throw new IllegalArgumentException("来源文档绑定或摘要不符");
            d.factors().forEach((k,p)-> { verify(p); if(!Objects.equals(plain(id),p.references().get("itemId")) || !k.equals(p.key()) || !category.equals(p.category())) throw new IllegalArgumentException("来源角色绑定不符"); });
            return d.factors();
        } catch(Exception e) { throw SnapshotIntegrity.failure("读取因子来源失败："+e.getMessage()); }
    }
    public static Map<String,String> selection(com.workload.system.calc.SemesterCalendar calendar,String semester) {
        Map<String,String> refs=new TreeMap<>();refs.put("semester",semester);refs.put("selectionMode","SEMESTER_END");refs.put("asOfDate",calendar.endOf(semester).toString());
        try { refs.put("calendarJson",JSON.writeValueAsString(Map.of("autumnStart",calendar.getAutumnStart(),"autumnEnd",calendar.getAutumnEnd(),"springStart",calendar.getSpringStart(),"springEnd",calendar.getSpringEnd()))); }
        catch(Exception e) {throw SnapshotIntegrity.failure("固化校历失败");} return refs;
    }
    public static void verifySelection(Map<String,String> refs) {
        require(refs,"semester","selectionMode","asOfDate","calendarJson");
        if(!"SEMESTER_END".equals(refs.get("selectionMode"))) throw SnapshotIntegrity.failure("来源选择模式不符");
        try {
            var calendar=JSON.readValue(refs.get("calendarJson"),com.workload.system.calc.SemesterCalendar.class);
            if(!calendar.endOf(refs.get("semester")).toString().equals(refs.get("asOfDate"))) throw new IllegalArgumentException();
        } catch(Exception e) {throw SnapshotIntegrity.failure("固化校历与来源选择日不符");}
    }
    public static Map<String,String> approvalContent(com.workload.system.domain.BizCoefficientAdjustment a) {
        Map<String,String> m=new TreeMap<>();m.put("id",plain(a.getId()));m.put("userId",plain(a.getUserId()));m.put("semester",a.getSemester());m.put("taskId",plain(a.getTaskId()));m.put("itemId",plain(a.getItemId()));
        m.put("category",a.getCategory());m.put("factorCode",a.getFactorCode());m.put("oldValue",plain(a.getOldValue()));m.put("requestedValue",plain(a.getRequestedValue()));m.put("reason",a.getReason());m.put("attachmentUrl",a.getAttachmentUrl());
        m.put("status",plain(a.getStatus()));m.put("reviewerId",plain(a.getReviewerId()));m.put("reviewReason",a.getReviewReason());m.put("reviewedAt",instant(a.getReviewedAt()));m.put("baseCalculationVersion",plain(a.getBaseCalculationVersion()));
        m.put("createBy",a.getCreateBy());m.put("createTime",instant(a.getCreateTime()));m.put("updateBy",a.getUpdateBy());m.put("updateTime",instant(a.getUpdateTime()));m.put("remark",a.getRemark());return m;
    }
    private static String instant(java.util.Date date){return date==null?null:date.toInstant().toString();}
    public static String approvalJson(com.workload.system.domain.BizCoefficientAdjustment a) {try{return JSON.writeValueAsString(approvalContent(a));}catch(Exception e){throw SnapshotIntegrity.failure("批准内容保存失败");}}
    private static void verifyApproval(Proof p) {
        try {
            require(p.references(),"approvalJson","approvalContentHash");
            Map<String,String> a=JSON.readValue(p.references().get("approvalJson"),new com.fasterxml.jackson.core.type.TypeReference<Map<String,String>>(){});
            if(!p.references().get("approvalContentHash").equals(PolicyHash.hash(a)) || !"1".equals(a.get("status"))
                || !Objects.equals(p.references().get("applicationId"),a.get("id")) || !Objects.equals(p.references().get("itemId"),a.get("itemId"))
                || !p.category().equals(a.get("category")) || !p.key().equals(a.get("factorCode")) || !same(p.value(),a.get("requestedValue"))
                || !Objects.equals(p.references().get("actor"),a.get("reviewerId")) || !Objects.equals(p.references().get("recordedAt"),a.get("reviewedAt"))
                || !Objects.equals(p.references().get("baseVersion"),a.get("baseCalculationVersion"))) throw new IllegalArgumentException();
            require(a,"userId","semester","reason","reviewerId","reviewedAt","baseCalculationVersion");
            if(!a.keySet().equals(Set.of("id","userId","semester","taskId","itemId","category","factorCode","oldValue","requestedValue","reason","attachmentUrl","status","reviewerId","reviewReason","reviewedAt","baseCalculationVersion","createBy","createTime","updateBy","updateTime","remark"))) throw new IllegalArgumentException();
        } catch(Exception e) {throw SnapshotIntegrity.failure("批准完整内容或绑定不符");}
    }
    public static void verify(Proof p) {
        if(p==null || p.inputs()==null || p.rules()==null || p.references()==null || p.value()==null
                || !Objects.equals(p.hash(),proofHash(p)) || !Objects.equals(content(p.branch()),p.branchContent()))
            throw SnapshotIntegrity.failure("因子来源内容摘要不符");
        String role=p.category()+":"+p.key();
        Set<String> inputRoles=Set.of("G1:J1","G2:J2","G3:T","G4:J4","G4:R4","G5:R5","G6:W","G6:R6");
        Set<String> approvalRoles=Set.of("G1:C1","G1:K1","G1:Q1","G1:Q2","G1:N","G2:K","G2:C2","G2:Q1","G2:Q2");
        Set<String> derivedRoles=Set.of("G1:C1","G1:K1","G1:Q1","G1:Q2","G1:N","G1:Q3","G2:K","G2:C2","G2:Q1","G2:Q2","G2:Q3","G3:D","G3:K","G3:Q1","G3:Q2","G3:Q3","G5:K5");
        if("IMPORT_INPUT".equals(p.kind()) && !inputRoles.contains(role) || "APPROVED_OVERRIDE".equals(p.kind()) && !approvalRoles.contains(role)
                || Set.of("DB_RULE","CODE_FALLBACK","CODE_POLICY").contains(p.kind()) && !derivedRoles.contains(role)) throw SnapshotIntegrity.failure("因子来源种类与类别角色不符");
        String requiredRole="Q3".equals(p.key()) ? "DISPLAY_ONLY" : "FACTOR";
        if(!requiredRole.equals(p.role())) throw SnapshotIntegrity.failure("因子来源角色不符");
        p.rules().forEach(RuleResolution::verify);
        if(p.kind().startsWith("IMPORT") || Set.of("DB_RULE","CODE_FALLBACK","CODE_POLICY").contains(p.kind())) {
            verifySelection(p.references());require(p.references(),"rawInputJson","normalizedInputJson","batch");
            verifyImportedInputs(p);
            for(var r:p.rules()) if(!Objects.equals(r.asOfDate(),p.references().get("asOfDate"))) throw SnapshotIntegrity.failure("派生来源选择日不符");
        }
        String result;
        switch(p.kind()) {
            case "MANUAL_INPUT", "IMPORT_INPUT", "IMPORT_OVERRIDE", "APPROVED_OVERRIDE", "REGISTRY_INPUT" -> {
                if(!p.rules().isEmpty() || !"EXPLICIT_VALUE_V1".equals(p.branch()) || !same(p.value(),p.inputs().get("value")))
                    throw SnapshotIntegrity.failure("显式输入来源不符");
                result=p.inputs().get("value");
                if("IMPORT_OVERRIDE".equals(p.kind()) && (!Set.of("G2:K","G3:D","G5:K5").contains(p.category()+":"+p.key())
                        || !same(p.inputs().get("courseCoefficient"),p.value()))) throw SnapshotIntegrity.failure("导入覆盖字段不符");
                if("MANUAL_INPUT".equals(p.kind()) || "APPROVED_OVERRIDE".equals(p.kind()) || "REGISTRY_INPUT".equals(p.kind())) require(p.references(),"actor","recordedAt");
                if(p.kind().startsWith("IMPORT")) require(p.references(),"batch");
                if("APPROVED_OVERRIDE".equals(p.kind())) { require(p.references(),"applicationId","factor","approvedValue","baseVersion");
                    if(!p.key().equals(p.references().get("factor")) || !same(p.value(),p.references().get("approvedValue"))) throw SnapshotIntegrity.failure("批准因子绑定不符"); }
                if("APPROVED_OVERRIDE".equals(p.kind())) verifyApproval(p);
                if("REGISTRY_INPUT".equals(p.kind())) { if(!"G11:岗位减免".equals(p.category()+":"+p.key())) throw SnapshotIntegrity.failure("岗位来源角色不符"); require(p.references(),"assignmentId","semester");if(!p.references().containsKey("batch")) throw SnapshotIntegrity.failure("岗位来源缺少批次字段"); }
            }
            case "CODE_POLICY", "DB_RULE", "CODE_FALLBACK" -> {
                String expected=expectedCode(p.category(),p.key(),p.inputs());
                if(!Objects.equals(p.branch(),p.category()+"_"+p.key()+"_V1")) throw SnapshotIntegrity.failure("代码分支与类别因子不符");
                if(expected==null) { if(!p.rules().isEmpty() || !"CODE_POLICY".equals(p.kind())) throw SnapshotIntegrity.failure("代码常量冒用规则来源"); result=codeValue(p.category(),p.key(),p.inputs()); }
                else { if(p.rules().size()!=1 || !expected.equals(p.rules().get(0).requestedCode()) || !p.kind().equals(p.rules().get(0).sourceKind())) throw SnapshotIntegrity.failure("派生政策角色不符"); result=p.rules().get(0).content().value(); }
            }
            default -> throw SnapshotIntegrity.failure("未知持久化因子来源种类");
        }
        if(!same(p.value(),result)) throw SnapshotIntegrity.failure("因子来源计算值不符");
    }
    private static void verifyImportedInputs(Proof p) {
        try {
            require(p.references(),"templateVersion","inputTransformVersion","inputTransformContent");
            if(!TeachingTaskInputNormalizer.CONTENT.equals(p.references().get("inputTransformContent"))) throw new IllegalArgumentException("输入转换内容不符");
            var replayed=TeachingTaskInputNormalizer.replay(p.references().get("rawInputJson"),p.references().get("templateVersion"),p.references().get("inputTransformVersion"));
            Map<String,Object> replayedValues=JSON.convertValue(replayed,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
            TeachingTaskInputNormalizer.validateTree(JSON.readTree(p.references().get("normalizedInputJson")),"V2");
            Map<String,Object> normalized=JSON.readValue(p.references().get("normalizedInputJson"),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
            if(!replayedValues.keySet().equals(normalized.keySet())) throw new IllegalArgumentException("原始输入转换字段集合不符");
            for(var entry:replayedValues.entrySet()) if(!TeachingTaskInputNormalizer.sameField(entry.getKey(),entry.getValue(),normalized.get(entry.getKey()))) throw new IllegalArgumentException("原始输入转换结果不符");
            for(var entry:normalized.entrySet()) if(!TeachingTaskInputNormalizer.samePlainField(entry.getKey(),entry.getValue(),p.inputs().get(entry.getKey()))) throw new IllegalArgumentException("规范化输入不一致");
            for(String key:p.inputs().keySet()) if(!Set.of("value","resolvedRepeatOrder").contains(key) && !normalized.containsKey(key)) throw new IllegalArgumentException("规范化输入遗漏字段");
            String explicit=p.inputs().get("repeatOrder");
            if(explicit!=null && ("G1:C1".equals(p.category()+":"+p.key()) || "G3:K".equals(p.category()+":"+p.key())) && !same(explicit,p.inputs().get("resolvedRepeatOrder"))) throw new IllegalArgumentException("显式重复次序不符");
            if("IMPORT_INPUT".equals(p.kind())) {
                String source=switch(p.key()){case "J1","J2"->"plannedHours";case "T"->"days";case "J4"->"credits";case "W"->"weeks";default->"studentCount";};
                if(!same(p.value(),p.inputs().get(source))) throw new IllegalArgumentException("数量列绑定不符");
            }
        }catch(Exception e){throw SnapshotIntegrity.failure("导入实际输入绑定不符："+e.getMessage());}
    }
    private static void require(Map<String,String> refs,String...keys) { for(String k:keys) if(refs.get(k)==null || refs.get(k).isBlank()) throw SnapshotIntegrity.failure("来源缺少引用："+k); }
    public static boolean same(String a,String b) { if(Objects.equals(a,b)) return true; if(a==null||b==null) return false; try{return new BigDecimal(a).compareTo(new BigDecimal(b))==0;}catch(Exception e){return false;} }
    public static String expectedCode(String category,String key,Map<String,String> i) {
        String role=category+":"+key;
        return switch(role) {
            case "G1:C1" -> Long.parseLong(i.get("resolvedRepeatOrder"))<=1 ? "COEF_REPEAT_1ST" : Long.parseLong(i.get("resolvedRepeatOrder"))==2 ? "COEF_REPEAT_2ND" : "COEF_REPEAT_3RD_UP";
            case "G1:N" -> integer(i.get("studentCount"))>=151 ? "COEF_CLASS_151_UP" : integer(i.get("studentCount"))>=120 ? "COEF_CLASS_120_150" : null;
            case "G2:K" -> science(i) ? "COEF_PRACTICE_LG" : "COEF_PRACTICE_OTHER";
            case "G3:D" -> science(i) ? "COEF_TRAIN_D_LG" : contains(i.get("majorCategory"),"艺术") ? "COEF_TRAIN_D_ART" : "COEF_TRAIN_D_HUM";
            case "G3:K" -> Long.parseLong(i.get("resolvedRepeatOrder"))<=1 ? null : "COEF_REPEAT_2ND";
            case "G5:K5" -> "COEF_THESIS_K5_"+(science(i)?"LG":"HU")+"_"+("专科".equals(i.get("educationLevel"))?"C":"B");
            default -> null;
        };
    }
    public static String codeValue(String category,String key,Map<String,String> i) {
        return switch(key) {
            case "K1" -> "必修".equals(i.get("courseNature"))?"1.1":"1.0";
            case "Q1" -> "不合格".equals(i.get("teachingEval"))?"0.8":"1.0";
            case "Q2" -> "省级一流".equals(i.get("courseLevel")) ? ("主持人".equals(i.get("courseRole"))?"1.5":"团队前3".equals(i.get("courseRole"))?"1.2":"1.0") : "校级精品".equals(i.get("courseLevel")) ? ("主持人".equals(i.get("courseRole"))?"1.2":"团队前3".equals(i.get("courseRole"))?"1.1":"1.0") : "1.0";
            case "C2" -> "0.9";
            case "N", "K", "Q3" -> "1";
            default -> throw SnapshotIntegrity.failure("未识别代码分支");
        };
    }
    private static boolean science(Map<String,String> i) { return contains(i.get("majorCategory"),"理工"); }
    private static boolean contains(String s,String part){return s!=null&&s.contains(part);}
    private static long integer(String s){return s==null?0:Long.parseLong(s);}
    public static String content(String branch) {
        if("EXPLICIT_VALUE_V1".equals(branch)) return "result=input.value; no equality inference; server-selected source kind and references";
        String[] parts=branch==null?new String[0]:branch.split("_");
        if(parts.length!=3 || !"V1".equals(parts[2])) throw SnapshotIntegrity.failure("未知来源算法版本");
        String category=parts[0],key=parts[1];
        return switch(key) {
            case "C1" -> "G1 repeatOrder<=1:COEF_REPEAT_1ST(default 1.0); ==2:COEF_REPEAT_2ND(default 0.9); >=3:COEF_REPEAT_3RD_UP(default 0.8)";
            case "K1" -> "G1 courseNature==必修 ? 1.1 : 1.0";
            case "Q1" -> "teachingEval==不合格 ? 0.8 : 1.0";
            case "Q2" -> "courseLevel==省级一流: 主持人=1.5,团队前3=1.2,other=1.0; 校级精品:主持人=1.2,团队前3=1.1,other=1.0; other=1.0";
            case "N" -> "studentCount null->0; >=151:COEF_CLASS_151_UP(default 1.2); >=120:COEF_CLASS_120_150(default 1.1); <120:1.0";
            case "C2" -> "G2 practice repeat factor always 0.9";
            case "K" -> "G2".equals(category) ? "majorCategory contains 理工:COEF_PRACTICE_LG(default 1.0); other:COEF_PRACTICE_OTHER(default 0.9)" : "G3 repeatOrder<=1:1.0; >=2:COEF_REPEAT_2ND(default 0.9)";
            case "D" -> "majorCategory contains 理工:COEF_TRAIN_D_LG(default 4); contains 艺术:COEF_TRAIN_D_ART(default 3); other:COEF_TRAIN_D_HUM(default 2)";
            case "K5" -> "majorCategory contains 理工->LG,other->HU; educationLevel==专科->C,other->B; COEF_THESIS_K5_{discipline}_{level}; defaults LG_B=9,LG_C=5,HU_B=6,HU_C=4";
            case "Q3" -> "constant 1.0; display only; excluded from G1/G2/G3 multiplication";
            default -> throw SnapshotIntegrity.failure("未知来源分支");
        };
    }
}
