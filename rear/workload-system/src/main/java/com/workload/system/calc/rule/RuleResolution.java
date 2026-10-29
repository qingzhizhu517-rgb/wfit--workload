package com.workload.system.calc.rule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import com.workload.system.domain.BizWorkloadRule;

/** A resolution is evidence of what was actually selected, never an inference from equal values. */
public record RuleResolution(String sourceKind, String requestedCode, String asOfDate,
        String fallbackPurpose, String missingReason, RuleContent content, String contentHash) {
    public static RuleResolution database(BizWorkloadRule row, LocalDate date) {
        RuleContent content=RuleContent.copyOf(row);
        return new RuleResolution("DB_RULE",row.getRuleCode(),date.toString(),null,null,content,PolicyHash.hash(content));
    }
    public static RuleResolution fallback(String code, LocalDate date, BigDecimal value, String purpose, String reason) {
        RuleContent content=new RuleContent(null,code,value.toPlainString(),null,null,null,null);
        return new RuleResolution("CODE_FALLBACK",code,date.toString(),purpose,reason,content,PolicyHash.hash(content));
    }
    public BigDecimal value() { return new BigDecimal(content.value()); }
    public String resolutionHash() { return PolicyHash.hash(this); }
    public void verify() {
        if (content == null || requestedCode == null || requestedCode.isBlank()
                || !Objects.equals(requestedCode,content.code()) || !Objects.equals(contentHash,PolicyHash.hash(content)))
            throw new IllegalArgumentException("政策内容摘要或编码不一致");
        LocalDate date=LocalDate.parse(asOfDate); value();
        if ("DB_RULE".equals(sourceKind)) {
            if (content.id() == null || !Integer.valueOf(1).equals(content.status()) || fallbackPurpose != null || missingReason != null
                    || (content.effectiveFrom()!=null && LocalDate.parse(content.effectiveFrom()).isAfter(date))
                    || (content.effectiveTo()!=null && LocalDate.parse(content.effectiveTo()).isBefore(date)))
                throw new IllegalArgumentException("数据库政策来源无效");
        } else if ("CODE_FALLBACK".equals(sourceKind)) {
            if (content.id()!=null || content.status()!=null || content.effectiveFrom()!=null || content.effectiveTo()!=null
                    || fallbackPurpose==null || fallbackPurpose.isBlank() || missingReason==null || missingReason.isBlank())
                throw new IllegalArgumentException("默认值缺少用途或缺失原因");
        } else throw new IllegalArgumentException("未知政策来源");
    }
}
