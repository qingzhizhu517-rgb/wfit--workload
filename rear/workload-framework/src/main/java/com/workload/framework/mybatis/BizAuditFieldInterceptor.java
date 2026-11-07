package com.workload.framework.mybatis;

import com.workload.common.core.domain.AuditableEntity;
import com.workload.common.core.domain.audit.AuditFields;
import com.workload.system.domain.*;
import com.workload.system.mapper.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.*;
import java.util.function.Supplier;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

/** Explicit business statement allowlist. No thread-local state and no object graph reflection. */
@Intercepts(@Signature(type=Executor.class, method="update", args={MappedStatement.class,Object.class}))
public final class BizAuditFieldInterceptor implements Interceptor {
    private record Contract(Class<?> entity, Set<String> keys) { }
    private static final Map<String, Contract> CONTRACTS = contracts();
    private final Supplier<String> username;
    private final Clock clock;
    public BizAuditFieldInterceptor() { this(AuditFields::currentUsername, Clock.systemUTC()); }
    public BizAuditFieldInterceptor(Supplier<String> username, Clock clock) {
        this.username=Objects.requireNonNull(username); this.clock=Objects.requireNonNull(clock);
    }
    public static Set<String> statementIds() { return CONTRACTS.keySet(); }
    private static Map<String,Contract> contracts() {
        Map<String,Contract> result=new LinkedHashMap<>();
        register(result, BizAllowanceItem.class, BizAllowanceItemMapper.class, "insertBizAllowanceItem", "updateBizAllowanceItem", "updateIfSummaryDraft", "updateCalculationEvidence");
        register(result, BizAuditLog.class, BizAuditLogMapper.class, "insertBizAuditLog");
        register(result, BizCoefficientAdjustment.class, BizCoefficientAdjustmentMapper.class, "insertCoefficientAdjustment", "markApprovedIfPending", "markRejectedIfPending");
        register(result, BizImportBatch.class, BizImportBatchMapper.class, "insertBizImportBatch", "updateBizImportBatch", "markConfirmed", "markStatus");
        register(result, BizImportRow.class, BizImportRowMapper.class, "insertBizImportRow", "updateBizImportRow");
        register(result, BizPayRate.class, BizPayRateMapper.class, "insertBizPayRate", "updateBizPayRate");
        register(result, BizPayRecord.class, BizPayRecordMapper.class, "insertBizPayRecord", "updateBizPayRecord", "updateIfSummaryDraft", "updateSettlementPointer");
        register(result, BizRoleAssignment.class, BizRoleAssignmentMapper.class, "insertBizRoleAssignment", "updateBizRoleAssignment");
        register(result, BizTeacherProfile.class, BizTeacherProfileMapper.class, "insertBizTeacherProfile", "updateBizTeacherProfile");
        register(result, BizTeachingTask.class, BizTeachingTaskMapper.class, "insertBizTeachingTask", "updateBizTeachingTask");
        register(result, BizWlConcentratedInternship.class, BizWlConcentratedInternshipMapper.class, "insertBizWlConcentratedInternship", "updateBizWlConcentratedInternship");
        register(result, BizWlCourseDesign.class, BizWlCourseDesignMapper.class, "insertBizWlCourseDesign", "updateBizWlCourseDesign");
        register(result, BizWlInternshipTraining.class, BizWlInternshipTrainingMapper.class, "insertBizWlInternshipTraining", "updateBizWlInternshipTraining");
        register(result, BizWlManagement.class, BizWlManagementMapper.class, "insertBizWlManagement", "updateBizWlManagement", "updateProrationIfEditable");
        register(result, BizWlPractice.class, BizWlPracticeMapper.class, "insertBizWlPractice", "updateBizWlPractice", "updateKIfVersion", "updateC2IfVersion", "updateQ1IfVersion", "updateQ2IfVersion");
        register(result, BizWlTheory.class, BizWlTheoryMapper.class, "insertBizWlTheory", "updateBizWlTheory", "updateC1IfVersion", "updateK1IfVersion", "updateQ1IfVersion", "updateQ2IfVersion", "updateNIfVersion");
        register(result, BizWlThesis.class, BizWlThesisMapper.class, "insertBizWlThesis", "updateBizWlThesis");
        register(result, BizWorkloadCategoryDict.class, BizWorkloadCategoryDictMapper.class, "insertBizWorkloadCategoryDict", "updateBizWorkloadCategoryDict");
        register(result, BizWorkloadItem.class, BizWorkloadItemMapper.class, "insertBizWorkloadItem", "updateBizWorkloadItem", "updateCalculationIfEditable", "confirmIfUnchanged", "invalidateCalculation", "updateFactorProvenance");
        register(result, BizWorkloadItemReview.class, BizWorkloadItemReviewMapper.class, "insert", "reviewIfPending");
        register(result, BizWorkloadRule.class, BizWorkloadRuleMapper.class, "insertBizWorkloadRule", "updateBizWorkloadRule");
        register(result, BizWorkloadSummary.class, BizWorkloadSummaryMapper.class, "insertBizWorkloadSummary", "updateBizWorkloadSummary", "updateStatusIf", "approveSummary", "rejectSummary", "unlockById", "teacherConfirmById", "updateCalculatedFieldsIfStatus", "markNeedsRecalculation", "updateSettlementPointer", "advanceDataVersion", "setReviewBatch");
        return Collections.unmodifiableMap(result);
    }
    private static void register(Map<String,Contract> result, Class<?> entity, Class<?> mapper, String... ids) {
        for (String id: ids) {
            Method method=Arrays.stream(mapper.getMethods()).filter(m->m.getName().equals(id)).findFirst().orElseThrow();
            Set<String> keys=new HashSet<>();
            for(int i=0;i<method.getParameterCount();i++) {
                Param named=method.getParameters()[i].getAnnotation(Param.class);
                if(named!=null) keys.add(named.value());
                keys.add("param"+(i+1));
            }
            // MyBatis wraps direct collections in these documented keys.
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == entity
                    && method.getParameters()[0].getAnnotation(Param.class) == null) {
                keys.addAll(Set.of("collection", "list", "array"));
            }
            result.put(mapper.getName()+"."+id,new Contract(entity,Set.copyOf(keys)));
        }
    }
    @Override public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement=(MappedStatement)invocation.getArgs()[0];
        Contract contract=CONTRACTS.get(statement.getId());
        if(contract==null) return invocation.proceed();
        if(statement.getSqlCommandType()!=SqlCommandType.INSERT && statement.getSqlCommandType()!=SqlCommandType.UPDATE)
            throw new IllegalArgumentException("Unexpected audited command");
        Object parameter=invocation.getArgs()[1];
        Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        List<AuditableEntity> entities=new ArrayList<>();
        validate(parameter, contract, seen, entities, true);
        AuditFields stamp=AuditFields.of(username.get(),clock);
        for(AuditableEntity entity:entities) {
            if(statement.getSqlCommandType()==SqlCommandType.INSERT) {
                entity.setCreateBy(stamp.getBy()); entity.setCreateTime(stamp.getTime());
            }
            entity.setUpdateBy(stamp.getBy()); entity.setUpdateTime(stamp.getTime());
        }
        if(parameter instanceof Map<?,?>) {
            @SuppressWarnings("unchecked") Map<String,Object> map=new HashMap<>((Map<String,Object>)parameter);
            map.put(AuditFields.PARAMETER,stamp);
            invocation.getArgs()[1]=map;
            try { return invocation.proceed(); }
            finally { map.remove(AuditFields.PARAMETER); invocation.getArgs()[1]=parameter; }
        }
        return invocation.proceed();
    }
    private static void validate(Object value, Contract contract, Set<Object> seen,
            List<AuditableEntity> entities, boolean root) {
        if(value==null) throw new IllegalArgumentException("Missing audited parameter");
        if(!seen.add(value)) return;
        if(value instanceof AuditableEntity entity) {
            if(value.getClass()!=contract.entity()) throw new IllegalArgumentException("Wrong audited entity");
            entities.add(entity); return;
        }
        if(value instanceof Map<?,?> map) {
            for(Object key:map.keySet()) {
                if(!(key instanceof String name) || !contract.keys().contains(name))
                    throw new IllegalArgumentException("Uncontracted audit parameter: "+key);
                Object nested=map.get(key);
                if(nested!=null) validate(nested,contract,seen,entities,false);
            }
            return;
        }
        if(value instanceof Collection<?> collection) {
            for(Object nested:collection) validate(nested,contract,seen,entities,false);
            return;
        }
        if(value.getClass().isArray()) {
            for(int i=0;i<Array.getLength(value);i++) validate(Array.get(value,i),contract,seen,entities,false);
            return;
        }
        if(root || !(value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Date))
            throw new IllegalArgumentException("Unsupported audited parameter shape");
    }
}
