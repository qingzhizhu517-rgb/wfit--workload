package com.workload.system.service;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.test.util.ReflectionTestUtils;
import com.workload.system.calc.*;
import com.workload.system.calc.rule.*;
import com.workload.system.calc.allowance.*;
import com.workload.system.domain.*;
class AllowanceEvidenceContentTest {
 @Test void cannotResignPersistedInputsThatDoNotComeFromActualAmountInputs()throws Exception {
  var calendar=new SemesterCalendar();var ctx=new CalculationPolicyContext(calendar.endOf("2025-2026-1"),(code,date,fallback,purpose)->RuleResolution.fallback(code,date,fallback,purpose,"NO_EFFECTIVE_RULE")).withSelectionEvidence(FactorProvenance.selection(calendar,"2025-2026-1"));
  BizAllowanceItem item=new BizAllowanceItem();item.setId(9L);item.setUserId(1003L);item.setFeeType("E");item.setSemester("2025-2026-1");item.setDurationHours(new BigDecimal("10"));item.setAmount(AllowanceEvidence.calculate(item,ctx));AllowanceEvidence.bindInserted(item,item);
  ObjectMapper json=new ObjectMapper();ObjectNode tree=(ObjectNode)json.readTree(item.getCalculationEvidenceJson());((ObjectNode)tree.get("persistedInputs")).put("durationHours","100");
  var changed=json.treeToValue(tree,AllowanceEvidence.Evidence.class);tree.put("hash",(String)ReflectionTestUtils.invokeMethod(AllowanceEvidence.class,"hash",changed));item.setCalculationEvidenceJson(tree.toString());item.setDurationHours(new BigDecimal("100"));
  assertThatThrownBy(()->AllowanceEvidence.verifyFor(item)).hasMessageContaining("输入");
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.CsvSource({"A,自学,5,120","A,自学,6,260","A,自学,19,260","A,自学,20,0","A,跟班,5,42.35","B,分散,10,100","B,集中不跟班,10,150","C,论文,2,240","D,代阅卷,19,0","D,代阅卷,20,30","D,代阅卷,60,80","D,代阅卷,120,100","D,代阅卷,200,150","E,讲座,1,90","F,运动会,1,450","G,值班,1,120"})
 void everyActualAmountBranchCapturesInputsRulesAndRounding(String type,String subtype,long count,String amount) {
  var calendar=new SemesterCalendar();var context=new CalculationPolicyContext(calendar.endOf("2025-2026-1"),(code,date,fallback,purpose)->RuleResolution.fallback(code,date,fallback,purpose,"NO_EFFECTIVE_RULE")).withSelectionEvidence(FactorProvenance.selection(calendar,"2025-2026-1"));
  BizAllowanceItem item=new BizAllowanceItem();item.setId(99L);item.setUserId(1003L);item.setSemester("2025-2026-1");item.setFeeType(type);item.setFeeSubtype(subtype);item.setStudentCount(count);item.setDurationHours(new BigDecimal("1.5"));item.setDays(new BigDecimal("2"));item.setClassCount(3L);item.setWorkloadUnits(new BigDecimal("4"));item.setAmount(new BigDecimal("42.345"));
  item.setAmount(AllowanceEvidence.calculate(item,context));AllowanceEvidence.bindInserted(item,item);var evidence=AllowanceEvidence.verify(item.getCalculationEvidenceJson());
  assertThat(item.getAmount()).isEqualByComparingTo(amount);assertThat(evidence.branchContent()).isNotBlank();assertThat(evidence.rounding()).isEqualTo("scale=2;HALF_UP");assertThat(evidence.rules()).allMatch(r->r.sourceKind().equals("CODE_FALLBACK"));assertThat(evidence.inputs()).containsEntry("studentCount",Long.toString(count));
 }

 @Test void persistedAmountInputMustEqualTheCapturedOutputAndActualRow()throws Exception {
  var calendar=new SemesterCalendar();var context=new CalculationPolicyContext(calendar.endOf("2025-2026-1"),(code,date,fallback,purpose)->RuleResolution.fallback(code,date,fallback,purpose,"NO_EFFECTIVE_RULE")).withSelectionEvidence(FactorProvenance.selection(calendar,"2025-2026-1"));
  var item=new BizAllowanceItem();item.setId(7L);item.setUserId(1003L);item.setSemester("2025-2026-1");item.setFeeType("E");item.setDurationHours(BigDecimal.ONE);item.setAmount(AllowanceEvidence.calculate(item,context));AllowanceEvidence.bindInserted(item,item);
  var json=new ObjectMapper();var tree=(ObjectNode)json.readTree(item.getCalculationEvidenceJson());((ObjectNode)tree.get("persistedInputs")).put("inputAmount","999999");var changed=json.treeToValue(tree,AllowanceEvidence.Evidence.class);tree.put("hash",(String)ReflectionTestUtils.invokeMethod(AllowanceEvidence.class,"hash",changed));item.setCalculationEvidenceJson(tree.toString());assertThatThrownBy(()->AllowanceEvidence.verifyFor(item)).hasMessageContaining("金额");
 }

}
