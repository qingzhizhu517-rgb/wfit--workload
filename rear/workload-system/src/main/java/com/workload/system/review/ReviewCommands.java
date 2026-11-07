package com.workload.system.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.workload.common.exception.ServiceException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Explicit transport contracts; no entity binding or coercion of optimistic versions. */
public final class ReviewCommands {
    private ReviewCommands() {}
    public record Review(long version,String reason) {}
    public record Entry(long id,Review command) {}
    public record Page(int number,int size) { public long offset(){return (long)(number-1)*size;} }
    public record Note(String key,BigDecimal amount,String reason,LocalDate handledAt,String referenceNo,String attachmentRef,String noteText) {}
    public static JsonNode parse(String body){
        try{return new com.fasterxml.jackson.databind.ObjectMapper().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true)).readTree(body);}
        catch(Exception e){throw bad("请求JSON格式无效或字段重复");}
    }
    public static Long positiveId(String value){if(value==null)return null;try{if(!value.matches("[1-9][0-9]*"))throw bad("ID须为正整数");return Long.valueOf(value);}catch(NumberFormatException e){throw bad("ID超出范围");}}
    public static ServiceException bad(String message){return new ServiceException(message,400);}
    public static ServiceException conflict(String message){return new ServiceException(message,409);}
    public static Review review(JsonNode body,String action) {
        object(body,Set.of("version","reason"));
        if(!Set.of("PASS","REJECT","WAIVE").contains(action))throw bad("不支持的审核动作");
        long version=integer(body.get("version"),"version",0);
        return new Review(version,rawText(body,"reason",500,!"PASS".equals(action)));
    }
    public static List<Entry> batch(JsonNode body,String action) {
        object(body,Set.of("items"));JsonNode items=body.get("items");
        if(items==null||!items.isArray()||items.isEmpty()||items.size()>100)throw bad("批量必须为1至100项");
        Set<Long> seen=new HashSet<>();List<Entry> result=new ArrayList<>();
        for(JsonNode item:items){object(item,Set.of("id","version","reason"));long id=integer(item.get("id"),"id",1);if(!seen.add(id))throw bad("批量不允许重复ID");
            var copy=item.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)copy).remove("id");result.add(new Entry(id,review(copy,action)));}
        return List.copyOf(result);
    }
    public static Page page(Map<String,String> query){
        if(!Set.of("pageNum","pageSize").containsAll(query.keySet()))throw bad("不支持的查询字段");
        int number=pageNumber(query.getOrDefault("pageNum","1"),Integer.MAX_VALUE,"pageNum");
        int size=pageNumber(query.getOrDefault("pageSize","10"),100,"pageSize");return new Page(number,size);
    }
    private static int pageNumber(String value,int max,String name){
        try{if(value==null||!value.matches("[1-9][0-9]*"))throw bad(name+"必须为正整数");long n=Long.parseLong(value);if(n>max)throw bad(name+"超出上限"+max);return (int)n;}
        catch(NumberFormatException e){throw bad(name+"超出上限");}
    }
    public static Note note(JsonNode body,String key){
        object(body,Set.of("adjustmentAmount","reason","handledAt","referenceNo","attachmentRef","noteText"));
        if(key==null||!key.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}"))throw bad("缺少或非法Idempotency-Key");
        JsonNode raw=body.get("adjustmentAmount");if(raw==null||!raw.isNumber())throw bad("补差金额必须为数值");
        BigDecimal amount=raw.decimalValue();if(amount.scale()>2||amount.abs().compareTo(new BigDecimal("9999999999.99"))>0)throw bad("补差金额须为DECIMAL(12,2)，禁止舍入");
        String date=text(body,"handledAt",10,true);LocalDate handled;
        try{if(!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw bad("处理日期须为ISO日期");handled=LocalDate.parse(date);}catch(java.time.DateTimeException e){throw bad("处理日期无效");}
        String attachment=text(body,"attachmentRef",500,false);
        if(attachment!=null&&(!attachment.matches("/profile/upload/[\\p{L}\\p{M}\\p{N}_./()（） -]+")||attachment.contains("..")||attachment.contains("//")||attachment.contains("/./")||attachment.endsWith("/.")||attachment.endsWith("/")))throw bad("附件必须为受控上传引用");
        return new Note(key,amount.setScale(2),rawText(body,"reason",500,true),handled,text(body,"referenceNo",100,false),attachment,rawText(body,"noteText",10000,false));
    }
    public static Note validateNote(Note c){
        if(c==null)throw bad("说明必填");var n=ReviewSnapshotCodec.object();
        if(c.amount()==null)n.putNull("adjustmentAmount");else n.put("adjustmentAmount",c.amount());
        n.put("reason",c.reason());n.put("handledAt",c.handledAt()==null?null:c.handledAt().toString());n.put("referenceNo",c.referenceNo());n.put("attachmentRef",c.attachmentRef());n.put("noteText",c.noteText());return note(n,c.key());
    }
    public static void object(JsonNode body,Set<String> allowed){
        if(body==null||!body.isObject())throw bad("请求必须是对象");body.fieldNames().forEachRemaining(k->{if(!allowed.contains(k))throw bad("不允许的字段："+k);});
    }
    private static long integer(JsonNode value,String name,long min){if(value==null||!value.isIntegralNumber()||!value.canConvertToLong()||value.longValue()<min)throw bad(name+"必须为明确整数");return value.longValue();}
    private static String rawText(JsonNode body,String name,int max,boolean required){
        JsonNode node=body.get(name);if(node==null||node.isNull()){if(required)throw bad(name+"原因或必填字段不能为空");return null;}
        if(!node.isTextual())throw bad(name+"必须为文本");String value=node.textValue();
        if(value.length()>max||required&&value.trim().isEmpty())throw bad(name+"原因或字段长度无效");return value;
    }
    private static String text(JsonNode body,String name,int max,boolean required){
        JsonNode node=body.get(name);if(node==null||node.isNull()){if(required)throw bad(name+"原因或必填字段不能为空");return null;}
        if(!node.isTextual())throw bad(name+"必须为文本");String value=node.textValue().trim();
        if(value.length()>max||required&&value.isEmpty())throw bad(name+"原因或字段长度无效");return value.isEmpty()?null:value;
    }
}
