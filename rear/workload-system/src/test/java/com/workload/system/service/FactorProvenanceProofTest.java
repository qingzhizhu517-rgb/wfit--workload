package com.workload.system.service;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import com.workload.system.calc.rule.*;
class FactorProvenanceProofTest {
 private Map<String,String> refs() {return Map.of("batch","IMP-A","itemId","9","actor","1:reviewer","recordedAt","2026-01-01T00:00:00Z","applicationId","11","factor","Q1","approvedValue","1","baseVersion","2");}
 @Test void recalculatingAllHashesCannotTurnDerivedCoefficientIntoPlainImportInput() {
  var p=FactorProvenance.create("G1","C1",BigDecimal.ONE,"IMPORT_INPUT","FACTOR","EXPLICIT_VALUE_V1",Map.of("value","1"),List.of(),refs());
  assertThatThrownBy(()->FactorProvenance.verify(p)).hasMessageContaining("来源");
 }
 @Test void recalculatingAllHashesCannotInventUnsupportedApprovalRole() {
  var p=FactorProvenance.create("G3","Q1",BigDecimal.ONE,"APPROVED_OVERRIDE","FACTOR","EXPLICIT_VALUE_V1",Map.of("value","1"),List.of(),refs());
  assertThatThrownBy(()->FactorProvenance.verify(p)).hasMessageContaining("来源");
 }
 @Test void normalizedInputCannotDisagreeWithActualBranchInputEvenWithAllHashesRecomputed() {
  var refs=new TreeMap<>(FactorProvenance.selection(new com.workload.system.calc.SemesterCalendar(),"2025-2026-1"));
  refs.put("itemId","9");refs.put("batch","IMP");refs.put("rawInputJson","{\"studentCount\":151}");refs.put("normalizedInputJson","{\"studentCount\":151}");refs.put("templateVersion","V2");refs.put("inputTransformVersion","TEACHING_TASK_INPUT_V1");refs.put("inputTransformContent",com.workload.system.calc.rule.TeachingTaskInputNormalizer.CONTENT);
  var p=FactorProvenance.create("G1","N",BigDecimal.ONE,"CODE_POLICY","FACTOR","G1_N_V1",Map.of("studentCount","119"),List.of(),refs);
  assertThatThrownBy(()->FactorProvenance.verify(p)).hasMessageContaining("输入");
 }
 @Test void approvalWithoutCapturedActualRowIsIncomplete() {
  var p=FactorProvenance.create("G1","Q1",BigDecimal.ONE,"APPROVED_OVERRIDE","FACTOR","EXPLICIT_VALUE_V1",Map.of("value","1"),List.of(),refs());
  assertThatThrownBy(()->FactorProvenance.verify(p)).hasMessageContaining("批准");
 }
 @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings={"different-count","not-json"})
 void rawInputMustReplayToTheCapturedNormalizedInputEvenWithAllHashesRecomputed(String change)throws Exception {
  var json=new com.fasterxml.jackson.databind.ObjectMapper();var dto=new com.workload.system.domain.dto.TeachingTaskImportDTO();dto.setSemester("2025-2026-1");dto.setStudentCount(119);dto.setCourseNature("必修");dto.setMajorCategory("理工类");
  Map<String,Object> values=json.convertValue(dto,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});var inputs=new TreeMap<String,String>();values.forEach((key,value)->inputs.put(key,FactorProvenance.plain(value)));
  String normalized=json.writeValueAsString(dto);dto.setStudentCount(151);String raw=change.equals("not-json")?"not-json":json.writeValueAsString(dto);
  var refs=new TreeMap<>(FactorProvenance.selection(new com.workload.system.calc.SemesterCalendar(),"2025-2026-1"));refs.put("itemId","9");refs.put("batch","IMP");refs.put("rawInputJson",raw);refs.put("normalizedInputJson",normalized);refs.put("templateVersion","V2");refs.put("inputTransformVersion","TEACHING_TASK_INPUT_V1");refs.put("inputTransformContent",com.workload.system.calc.rule.TeachingTaskInputNormalizer.CONTENT);
  var p=FactorProvenance.create("G1","N",BigDecimal.ONE,"CODE_POLICY","FACTOR","G1_N_V1",inputs,List.of(),refs);
  assertThatThrownBy(()->FactorProvenance.verify(p)).hasMessageContaining("输入");
 }

 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.CsvSource({"userCode,raw-to-normalized","courseCode,raw-to-normalized","courseName,raw-to-normalized","className,raw-to-normalized","userCode,normalized-to-inputs","courseCode,normalized-to-inputs","courseName,normalized-to-inputs","className,normalized-to-inputs"})
 void unchangedTextFieldsCannotBeCoercedIntoNumbersByResigning(String key,String layer)throws Exception {
  var json=new com.fasterxml.jackson.databind.ObjectMapper();var dto=new com.workload.system.domain.dto.TeachingTaskImportDTO();dto.setSemester("2025-2026-1");dto.setStudentCount(119);dto.setCourseNature("必修");dto.setMajorCategory("理工类");
  org.springframework.test.util.ReflectionTestUtils.setField(dto,key,"001003");String raw=json.writeValueAsString(dto);var normalized=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(raw);
  if(layer.equals("raw-to-normalized")) normalized.put(key,"1003");Map<String,Object> values=json.convertValue(normalized,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});var inputs=new TreeMap<String,String>();values.forEach((field,value)->inputs.put(field,FactorProvenance.plain(value)));if(layer.equals("normalized-to-inputs")) inputs.put(key,"1003");
  var refs=new TreeMap<>(FactorProvenance.selection(new com.workload.system.calc.SemesterCalendar(),"2025-2026-1"));refs.put("itemId","9");refs.put("batch","IMP");refs.put("rawInputJson",raw);refs.put("normalizedInputJson",normalized.toString());refs.put("templateVersion","V2");refs.put("inputTransformVersion",TeachingTaskInputNormalizer.VERSION);refs.put("inputTransformContent",TeachingTaskInputNormalizer.CONTENT);
  var proof=FactorProvenance.create("G1","N",BigDecimal.ONE,"CODE_POLICY","FACTOR","G1_N_V1",inputs,List.of(),refs);assertThatThrownBy(()->FactorProvenance.verify(proof)).hasMessageContaining("输入");
 }

 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.CsvSource({"V2,studentCount,text,raw","V2,studentCount,fraction,raw","V2,repeatOrder,text,raw","V2,repeatOrder,fraction,raw","V2,userCode,number,raw","V2,studentCount,decimal,normalized","V1,baseValue,text,raw","V1,userCode,number,raw","V1,studentCount,fraction,raw"})
 void jsonTypeCoercionCannotChangeDeclaredInputTypes(String template,String field,String mutation,String layer)throws Exception {
  var json=new com.fasterxml.jackson.databind.ObjectMapper();var v1=new com.workload.system.domain.dto.TeachingTaskImportV1DTO();v1.setSemester("2025-2026-1");v1.setUserCode("1003");v1.setStudentCount(119);v1.setRepeatOrder(1);v1.setWorkloadType("G1");v1.setBaseValue(new BigDecimal("10"));v1.setCourseNature("必修");v1.setMajorCategory("理工类");
  var dto=TeachingTaskInputNormalizer.convertV1(v1);var raw=(com.fasterxml.jackson.databind.node.ObjectNode)json.valueToTree(template.equals("V1")?v1:dto);var normalized=(com.fasterxml.jackson.databind.node.ObjectNode)json.valueToTree(dto);var changed=layer.equals("raw")?raw:normalized;
  switch(mutation){case "text"->changed.put(field,field.equals("baseValue")?"10":field.equals("repeatOrder")?"1":"119");case "fraction"->changed.put(field,field.equals("repeatOrder")?1.9:119.9);case "number"->changed.put(field,1003);case "decimal"->changed.put(field,119.0);}
  Map<String,Object> values=json.convertValue(dto,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});var inputs=new TreeMap<String,String>();values.forEach((key,value)->inputs.put(key,FactorProvenance.plain(value)));
  var refs=new TreeMap<>(FactorProvenance.selection(new com.workload.system.calc.SemesterCalendar(),"2025-2026-1"));refs.put("itemId","9");refs.put("batch","IMP");refs.put("rawInputJson",raw.toString());refs.put("normalizedInputJson",normalized.toString());refs.put("templateVersion",template);refs.put("inputTransformVersion",TeachingTaskInputNormalizer.VERSION);refs.put("inputTransformContent",TeachingTaskInputNormalizer.CONTENT);
  var proof=FactorProvenance.create("G1","N",BigDecimal.ONE,"CODE_POLICY","FACTOR","G1_N_V1",inputs,List.of(),refs);assertThatThrownBy(()->FactorProvenance.verify(proof)).hasMessageContaining("输入");
 }

}
