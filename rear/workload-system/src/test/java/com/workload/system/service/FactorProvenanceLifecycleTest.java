package com.workload.system.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workload.system.domain.*;
class FactorProvenanceLifecycleTest {
 @Test void allowanceUsesExplicitPolicyContext() {
  assertTrue(java.util.Arrays.stream(com.workload.system.calc.allowance.AllowanceCalcStrategy.class.getMethods())
    .anyMatch(m -> m.getName().equals("calculate") && m.getParameterCount()==2), "actual amount calculation needs an explicit policy context");
 }
 @Test void provenanceColumnsAreServerOwned() throws Exception {
  var field = java.util.Arrays.stream(BizWorkloadItem.class.getDeclaredFields()).filter(f -> f.getName().equals("factorProvenanceJson")).findFirst();
  assertTrue(field.isPresent(), "missing server-owned persistent factor provenance");
  var item = new ObjectMapper().readValue("{\"factorProvenanceJson\":\"forged\"}", BizWorkloadItem.class);
  field.get().setAccessible(true); assertNull(field.get().get(item));
  var allowance = new ObjectMapper().readValue("{\"calculationEvidenceJson\":\"forged\"}", BizAllowanceItem.class);
  var evidence = BizAllowanceItem.class.getDeclaredField("calculationEvidenceJson"); evidence.setAccessible(true); assertNull(evidence.get(allowance));
 }
}
