package com.workload.system.calc.rule;

import com.workload.system.domain.BizWorkloadRule;

/** Immutable copy of the complete policy row; dates/numbers retain portable textual evidence. */
public record RuleContent(Long id, String code, String value, String effectiveFrom,
        String effectiveTo, String description, Integer status) {
    public static RuleContent copyOf(BizWorkloadRule row) {
        return new RuleContent(row.getId(), row.getRuleCode(),
                row.getRuleValue() == null ? null : row.getRuleValue().toPlainString(),
                row.getEffectiveFrom() == null ? null : row.getEffectiveFrom().toString(),
                row.getEffectiveTo() == null ? null : row.getEffectiveTo().toString(), row.getRuleDesc(), row.getStatus());
    }
}
