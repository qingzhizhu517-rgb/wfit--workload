package com.workload.system.calc.rule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** One calculation owns one explicit context. No shared state or ThreadLocal. */
public final class CalculationPolicyContext {
    @FunctionalInterface public interface Resolver {
        RuleResolution resolve(String code, LocalDate date, BigDecimal fallback, String purpose);
    }
    private final LocalDate asOfDate;
    private final Resolver resolver;
    private final Map<String, RuleResolution> resolved=new TreeMap<>();
    private final Map<String, String> defaults=new HashMap<>();
    public CalculationPolicyContext(LocalDate date, RuleParamService service) { this(date, (code, asOf, fallback, purpose) -> service.resolve(code, asOf, fallback, purpose)); }
    public CalculationPolicyContext(LocalDate date, Resolver resolver) {
        this.asOfDate=Objects.requireNonNull(date,"asOfDate"); this.resolver=Objects.requireNonNull(resolver,"resolver");
    }
    public LocalDate asOfDate() { return asOfDate; }
    public RuleResolution resolve(String code, BigDecimal fallback) { return resolve(code,fallback,"兼容计算默认值："+code); }
    public RuleResolution resolve(String code, BigDecimal fallback, String purpose) {
        String request=(fallback==null ? "REQUIRED" : fallback.stripTrailingZeros().toPlainString())+"|"+purpose;
        if (defaults.containsKey(code) && !defaults.get(code).equals(request))
            throw new IllegalArgumentException("同次计算规则默认值冲突："+code);
        if (!resolved.containsKey(code)) {
            RuleResolution resolution=Objects.requireNonNull(resolver.resolve(code,asOfDate,fallback,purpose),"rule resolution");
            resolution.verify();
            if (!code.equals(resolution.requestedCode()) || !asOfDate.toString().equals(resolution.asOfDate()))
                throw new IllegalArgumentException("规则解析上下文不一致");
            resolved.put(code,resolution); defaults.put(code,request);
        }
        return resolved.get(code);
    }
    public BigDecimal value(String code, BigDecimal fallback) { return resolve(code,fallback).value(); }
    public List<RuleResolution> sources() { return List.copyOf(resolved.values()); }
    public String policySetHash() { return PolicyHash.hash(sources()); }
}
