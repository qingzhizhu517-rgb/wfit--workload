package com.workload.common.core.domain.audit;

import java.time.Clock;
import java.util.Date;
import java.util.Map;
import com.workload.common.utils.SecurityUtils;

/** Immutable server stamp shared by the interceptor and standalone mapper configurations. */
public final class AuditFields {
    public static final String PARAMETER = "_auditFields";
    private final String by;
    private final long millis;
    private AuditFields(String by, long millis) { this.by = by; this.millis = millis; }
    public static AuditFields of(String by, Clock clock) {
        return new AuditFields(by == null || by.isBlank() ? "SYSTEM" : by, clock.millis());
    }
    public static String currentUsername() {
        try {
            var authentication = SecurityUtils.getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof com.workload.common.core.domain.model.LoginUser)) return "SYSTEM";
            return SecurityUtils.getUsername();
        }
        catch (RuntimeException absentLogin) { return "SYSTEM"; }
    }
    public static AuditFields current() { return of(currentUsername(), Clock.systemUTC()); }
    /** XML fallback is server-side too: arbitrary JSON maps are never accepted as stamps. */
    public static AuditFields resolve(Object parameter) {
        if (parameter instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                if (key instanceof String name && name.startsWith("_audit")) {
                    if (!PARAMETER.equals(name) || !(map.get(key) instanceof AuditFields))
                        throw new IllegalArgumentException("Reserved audit parameter");
                }
            }
            if (map.containsKey(PARAMETER)) return (AuditFields) map.get(PARAMETER);
        }
        return current();
    }
    public String getBy() { return by; }
    public Date getTime() { return new Date(millis); }
}
