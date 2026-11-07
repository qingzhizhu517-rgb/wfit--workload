package com.workload.system.review;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.workload.system.domain.BizWorkloadCalcSnapshot;
import com.workload.system.service.SnapshotIntegrity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Review format 1 has its own canonical contract. It never reconstructs historical sources. */
public final class ReviewSnapshotCodec {
    private static final ObjectMapper JSON=new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    public static final Set<String> ITEM_KEYS=Set.of("itemType","sourceType","taskId","assignmentId","roleType","courseName","educationLevel","majorCategory","description","calculatedWorkload","isOverLimit","status","calculationVersion","lastCalculatedAt","factorProvenanceJson","calculationSnapshotId","calculationSnapshotHash","calculationSnapshotJson","evidenceStatus","createBy","createTime");
    public static final Set<String> SUMMARY_KEYS=Set.of("academicYear","g7","g8","g9","g10","g11","totalWorkload","ratedWorkload","excessWorkload","g8Remark","g11Remark","title","payRate","performancePay","isCapped","basicTeachingStandard","basicTeachingMet","teacherSign","teacherSignTime","deptLeaderSign","deptLeaderSignTime","academicAssistantSign","academicAssistantSignTime","needsRecalculation","lastCalculatedAt");
    public static final Set<String> PAY_KEYS=Set.of("id","summaryId","courseHourPay","otherPayTotal","totalPay","status","settlementSnapshotId");
    public record Seal(String json,String hash) {}
    private ReviewSnapshotCodec(){}
    public static ObjectNode object(){return JSON.createObjectNode();}
    public static Object plain(JsonNode value){return JSON.convertValue(value,Object.class);}
    public static JsonNode tree(Object value){return JSON.valueToTree(value);}
    public static Seal sealItem(ObjectNode node){validateItem(node);return seal(node);}
    public static Seal sealBatch(ObjectNode node){validateBatch(node);return seal(node);}
    public static ObjectNode readItem(String json,String hash){ObjectNode node=read(json,hash);validateItem(node);return node;}
    public static ObjectNode readBatch(String json,String hash){ObjectNode node=read(json,hash);validateBatch(node);return node;}
    private static ObjectNode read(String text,String expected){
        try{JsonNode node=JSON.readTree(text);if(!(node instanceof ObjectNode n))throw new IllegalArgumentException("对象缺失");if(expected==null||!expected.matches("[0-9a-f]{64}")||!expected.equals(seal(n).hash()))throw new IllegalArgumentException("hash不符");return n;}
        catch(Exception e){throw ReviewCommands.conflict("审核证据损坏："+e.getMessage());}
    }
    public static boolean equivalent(JsonNode a,JsonNode b){return seal(a).hash().equals(seal(b).hash());}
    private static Seal seal(JsonNode node){try{String json=JSON.writeValueAsString(canonical(node));return new Seal(json,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8))));}catch(Exception e){throw ReviewCommands.conflict("审核证据编码失败");}}
    private static JsonNode canonical(JsonNode n){if(n.isObject()){ObjectNode o=object();TreeMap<String,JsonNode> fields=new TreeMap<>();n.fields().forEachRemaining(f->fields.put(f.getKey(),f.getValue()));fields.forEach((k,v)->o.set(k,canonical(v)));return o;}if(n.isArray()){ArrayNode a=JSON.createArrayNode();n.forEach(v->a.add(canonical(v)));return a;}return n;}
    private static void exact(JsonNode n,Set<String> keys){if(n==null||!n.isObject())throw ReviewCommands.conflict("审核证据字段对象缺失");Set<String> actual=new HashSet<>();n.fieldNames().forEachRemaining(actual::add);if(!actual.equals(keys))throw ReviewCommands.conflict("审核证据字段不完整或未知");}
    private static void identity(JsonNode n){number(n.get("formatVersion"),1);if(n.get("formatVersion").longValue()!=1)throw ReviewCommands.conflict("不支持审核证据格式");number(n.get("summaryId"),1);number(n.get("userId"),1);text(n.get("semester"),false);if(!n.get("semester").textValue().matches("[0-9]{4}-[0-9]{4}-[12]"))throw ReviewCommands.conflict("审核学期无效");}
    private static void number(JsonNode n,long min){if(n==null||!n.isIntegralNumber()||!n.canConvertToLong()||n.longValue()<min)throw ReviewCommands.conflict("审核证据必须为明确整数");}
    private static void text(JsonNode n,boolean nullable){if(n==null||!(n.isTextual()||nullable&&n.isNull()))throw ReviewCommands.conflict("审核证据必须为文本或显式NULL");}
    private static void strings(JsonNode n,Set<String> keys){exact(n,keys);n.forEach(v->text(v,true));}
    private static void decimal(JsonNode values,String key,boolean nullable){JsonNode n=values.get(key);if(nullable&&n.isNull())return;try{if(!n.isTextual()||!n.textValue().matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?"))throw new IllegalArgumentException();new java.math.BigDecimal(n.textValue());}catch(Exception e){throw ReviewCommands.conflict("审核证据数值无效："+key);}}
    private static void integerText(JsonNode v,String key,boolean nullable){
        if(nullable&&v.get(key).isNull())return;
        try{if(!v.get(key).isTextual()||!v.get(key).textValue().matches("0|[1-9][0-9]*"))throw new IllegalArgumentException();Long.parseLong(v.get(key).textValue());}
        catch(IllegalArgumentException e){throw ReviewCommands.conflict("审核证据整数无效："+key);}
    }
    private static void enumText(JsonNode values,String key,boolean nullable,Set<String> allowed){
        JsonNode value=values.get(key);if(nullable&&value.isNull())return;
        if(!value.isTextual()||!allowed.contains(value.textValue()))throw ReviewCommands.conflict("审核证据枚举无效："+key);
    }
    /** Date fields are captured as canonical epoch-millisecond long strings, never arbitrary date text. */
    private static void epochMillis(JsonNode values,String key,boolean nullable){
        JsonNode value=values.get(key);if(nullable&&value.isNull())return;
        try{if(!value.isTextual()||!value.textValue().matches("0|-?[1-9][0-9]*"))throw new IllegalArgumentException();Long.parseLong(value.textValue());}
        catch(IllegalArgumentException e){throw ReviewCommands.conflict("审核时间必须为epoch毫秒整数："+key);}
    }
    private static void positiveIntegerText(JsonNode values,String key,boolean nullable){
        integerText(values,key,nullable);if(!values.get(key).isNull()&&Long.parseLong(values.get(key).textValue())<=0)throw ReviewCommands.conflict("审核引用必须为正整数："+key);
    }
    private static void pair(JsonNode n,String id,String hash){JsonNode i=n.get(id),h=n.get(hash);if(i.isNull()!=h.isNull())throw ReviewCommands.conflict("审核证据引用不成对");if(!i.isNull()){if(i.isTextual()){integerText(n,id,false);if("0".equals(i.textValue()))throw ReviewCommands.conflict("审核证据引用无效");}else number(i,1);if(!h.isTextual()||!h.textValue().matches("[0-9a-f]{64}"))throw ReviewCommands.conflict("审核证据引用hash无效");}}
    private static void validateItem(ObjectNode n){
        exact(n,Set.of("formatVersion","summaryId","userId","semester","itemId","reviewBatchNo","reviewType","values"));identity(n);number(n.get("itemId"),1);text(n.get("reviewBatchNo"),false);text(n.get("reviewType"),false);
        if(!Set.of("TEACHING_TASK","MANUAL_DECLARATION","G11_ROLE","OVER_LIMIT").contains(n.get("reviewType").textValue()))throw ReviewCommands.conflict("审核类型无效");
        JsonNode v=n.get("values");strings(v,ITEM_KEYS);if(v.get("itemType").isNull()||!Set.of("G1","G2","G3","G4","G5","G6","G8","G9","G11").contains(v.get("itemType").textValue()))throw ReviewCommands.conflict("明细项目类型无效");
        if(v.get("sourceType").isNull()||!Set.of("SELF","MANUAL","AUTO","IMPORT").contains(v.get("sourceType").textValue()))throw ReviewCommands.conflict("明细来源类型无效");
        decimal(v,"calculatedWorkload",false);for(String k:List.of("taskId","assignmentId","calculationVersion","isOverLimit","status"))integerText(v,k,Set.of("taskId","assignmentId","isOverLimit").contains(k));
        for(String k:List.of("lastCalculatedAt","createTime"))epochMillis(v,k,true);
        boolean manual=Set.of("SELF","MANUAL").contains(v.get("sourceType").textValue()),role="G11".equals(v.get("itemType").textValue());
        if(role&&(v.get("assignmentId").isNull()||"0".equals(v.get("assignmentId").textValue()))||!role&&!manual&&(v.get("taskId").isNull()||"0".equals(v.get("taskId").textValue())))throw ReviewCommands.conflict("审核来源引用缺失");
        String expectedType="1".equals(v.get("isOverLimit").asText())?"OVER_LIMIT":role?"G11_ROLE":manual?"MANUAL_DECLARATION":"TEACHING_TASK";
        if(!expectedType.equals(n.get("reviewType").textValue()))throw ReviewCommands.conflict("审核分类与来源优先级不符");
        if(!v.get("factorProvenanceJson").isNull())com.workload.system.calc.rule.FactorProvenance.read(v.get("factorProvenanceJson").textValue(),n.get("itemId").longValue(),v.get("itemType").textValue());
        if(!"1".equals(v.get("status").textValue()))throw ReviewCommands.conflict("审核成员必须已核对");if(!v.get("isOverLimit").isNull()&&!Set.of("0","1").contains(v.get("isOverLimit").textValue()))throw ReviewCommands.conflict("超限枚举无效");
        if(!Set.of("VERIFIED","HISTORICAL_MISSING").contains(v.get("evidenceStatus").asText()))throw ReviewCommands.conflict("证据状态无效");
        pair(v,"calculationSnapshotId","calculationSnapshotHash");
        if(v.get("calculationSnapshotId").isNull()!=v.get("evidenceStatus").asText().equals("HISTORICAL_MISSING"))throw ReviewCommands.conflict("审核计算证据状态与引用不符");
        if(v.get("calculationSnapshotId").isNull()!=v.get("calculationSnapshotJson").isNull())throw ReviewCommands.conflict("审核计算引用缺失");
        if(!v.get("calculationSnapshotJson").isNull())try{var row=JSON.readValue(v.get("calculationSnapshotJson").textValue(),BizWorkloadCalcSnapshot.class);SnapshotIntegrity.verify(row);if(!Objects.equals(row.getId().toString(),v.get("calculationSnapshotId").textValue())||!Objects.equals(row.getItemId(),n.get("itemId").longValue())||!Objects.equals(row.getSnapshotHash(),v.get("calculationSnapshotHash").textValue())||!Objects.equals(String.valueOf(row.getCalculationVersion()),v.get("calculationVersion").textValue())||row.getResult().compareTo(new java.math.BigDecimal(v.get("calculatedWorkload").textValue()))!=0)throw new IllegalArgumentException("计算引用绑定不符");}catch(Exception e){throw ReviewCommands.conflict("审核计算证据引用不符："+e.getMessage());}
    }
    private static void validateBatch(ObjectNode n){
        exact(n,Set.of("formatVersion","summaryId","userId","semester","reviewBatchNo","dataVersion","summary","pay","allowances","items","sourceCounts","settlementSnapshotId","settlementSnapshotHash","settlementEvidenceStatus"));identity(n);number(n.get("dataVersion"),1);text(n.get("reviewBatchNo"),false);strings(n.get("summary"),SUMMARY_KEYS);
        for(String k:List.of("g7","g8","g9","g10","g11","totalWorkload","ratedWorkload","excessWorkload","payRate","performancePay","basicTeachingStandard"))decimal(n.get("summary"),k,true);
        enumText(n.get("summary"),"needsRecalculation",false,Set.of("0"));
        for(String key:List.of("isCapped","basicTeachingMet"))enumText(n.get("summary"),key,true,Set.of("0","1"));
        epochMillis(n.get("summary"),"lastCalculatedAt",false);
        for(String key:List.of("teacherSignTime","deptLeaderSignTime","academicAssistantSignTime"))epochMillis(n.get("summary"),key,true);
        JsonNode pay=n.get("pay");strings(pay,PAY_KEYS);
        for(String key:List.of("id","summaryId"))positiveIntegerText(pay,key,false);
        positiveIntegerText(pay,"settlementSnapshotId",true);integerText(pay,"totalPay",false);
        // The existing pay screen defines 0=unpaid, 1=paid; historical NULL stays unknown.
        enumText(pay,"status",true,Set.of("0","1"));
        for(String key:List.of("courseHourPay","otherPayTotal"))decimal(pay,key,false);
        if(!pay.get("summaryId").textValue().equals(n.get("summaryId").asText()))throw ReviewCommands.conflict("酬金快照归属不符");
        if(!n.get("allowances").isArray())throw ReviewCommands.conflict("酬金证据数组缺失");long aid=0;for(JsonNode a:n.get("allowances")){strings(a,Set.of("id","amount","calculationEvidenceJson"));integerText(a,"id",false);decimal(a,"amount",false);long id=Long.parseLong(a.get("id").textValue());if(id<=aid)throw ReviewCommands.conflict("酬金引用必须唯一有序");aid=id;}
        pair(n,"settlementSnapshotId","settlementSnapshotHash");
        if(!pay.get("settlementSnapshotId").asText().equals(n.get("settlementSnapshotId").asText()))throw ReviewCommands.conflict("酬金与批次结算引用不符");
        if(!Set.of("VERIFIED","HISTORICAL_MISSING").contains(n.get("settlementEvidenceStatus").asText())||n.get("settlementSnapshotId").isNull()!=n.get("settlementEvidenceStatus").asText().equals("HISTORICAL_MISSING"))throw ReviewCommands.conflict("结算证据状态不符");
        strings(n.get("sourceCounts"),Set.of("includedReviewItemCount","excludedRejectedItemCount","distinctTeachingTaskReferenceCount","distinctRoleAssignmentReferenceCount","manualDeclarationItemCount","importRawRowCount","importRawMissingReason"));
        for(String key:List.of("includedReviewItemCount","excludedRejectedItemCount","distinctTeachingTaskReferenceCount","distinctRoleAssignmentReferenceCount","manualDeclarationItemCount"))integerText(n.get("sourceCounts"),key,false);
        if(!n.get("sourceCounts").get("importRawRowCount").isNull()||n.get("sourceCounts").get("importRawMissingReason").isNull())throw ReviewCommands.conflict("原始导入行缺史标记不符");
        if(!n.get("items").isArray()||n.get("items").isEmpty())throw ReviewCommands.conflict("审核成员不得为空");long previous=0;
        for(JsonNode member:n.get("items")){exact(member,Set.of("json","hash"));text(member.get("json"),false);text(member.get("hash"),false);ObjectNode i=readItem(member.get("json").textValue(),member.get("hash").textValue());long id=i.get("itemId").longValue();if(id<=previous)throw ReviewCommands.conflict("审核成员必须唯一有序");previous=id;for(String key:List.of("summaryId","userId","semester","reviewBatchNo"))if(!i.get(key).asText().equals(n.get(key).asText()))throw ReviewCommands.conflict("审核成员归属不符");}
    }
}
