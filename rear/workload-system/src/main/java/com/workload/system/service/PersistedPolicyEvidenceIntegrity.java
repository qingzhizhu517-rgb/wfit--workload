package com.workload.system.service;
import java.util.*;
import com.workload.system.calc.rule.*;
import com.workload.system.domain.BizWorkloadCalcSnapshot;
import com.workload.system.domain.vo.FactorFormulaVo.*;
import com.workload.system.service.SnapshotContextCodec.*;

/** Format 4 adds persisted value-selection proofs. Format 3 is verified without changing its semantics. */
final class PersistedPolicyEvidenceIntegrity {
 private PersistedPolicyEvidenceIntegrity() {}
 static String policyHash(List<RuleResolution> policies,List<FactorProvenance.Proof> proofs) {
  return PolicyHash.hash(Arrays.asList("PERSISTED_FACTOR_POLICY_V1",policies,proofs));
 }
 static void verify(BizWorkloadCalcSnapshot snapshot,Context c,Map<String,String> values,Map<String,String> labels) {
  if(c.persistedFactorSources()==null || c.policySources()==null || c.factorEvidence()==null || c.decisionEvidence()==null || c.missingEvidenceReasons()==null)
   throw SnapshotIntegrity.failure("format4来源字段缺失");
  if(!Objects.equals(snapshot.getRuleVersion(),policyHash(c.policySources(),c.persistedFactorSources()))) throw SnapshotIntegrity.failure("持久化政策集合摘要不符");
  Map<String,FactorProvenance.Proof> proofs=new HashMap<>(); String prev=null;
  for(var proof:c.persistedFactorSources()) {
   FactorProvenance.verify(proof);
   if(!Objects.equals(snapshot.getItemId().toString(),proof.references().get("itemId")) || !c.formulaType().equals(proof.category()) || !values.containsKey(proof.key()) || !FactorProvenance.same(values.get(proof.key()),proof.value())
     || (prev!=null && prev.compareTo(proof.key())>=0) || proofs.put(proof.key(),proof)!=null) throw SnapshotIntegrity.failure("持久化因子角色、值或顺序不符");
   prev=proof.key();
  }
  Map<String,FactorContext> metas=new HashMap<>();c.factors().forEach(m -> metas.put(m.key(),m));
  List<FactorEvidence> projected=new ArrayList<>();List<FactorContext> projectedMetas=new ArrayList<>(c.factors());
  Map<String,String> projectedLabels=new LinkedHashMap<>(labels);Set<String> used=new HashSet<>();Set<String> missing=new HashSet<>();Set<String> projectedMissing=new HashSet<>();
  for(var f:c.factorEvidence()) {
   if("PERSISTED_SOURCE".equals(f.kind())) {
    var p=proofs.get(f.key());var m=metas.get(f.key());
    if(p==null || !used.add(p.key()) || !Objects.equals(f.ruleRef(),p.hash()) || !FactorProvenance.same(f.boundValue(),p.value())
       || !Objects.equals(labels.get(p.key()),p.kind()) || !Objects.equals(m.sourceRef(),p.hash()) || !Objects.equals(m.ruleVersion(),p.hash()) || m.ruleCode()!=null
       || f.table()!=null || f.field()!=null || f.inputKey()!=null || f.operation()!=null || f.missingReason()!=null) throw SnapshotIntegrity.failure("持久化来源引用或值绑定不符");
    // A's matrix still checks every factor and mandatory decision. Only the new persisted inputs are projected.
    String kind="Q3".equals(p.key())?"DISPLAY_ONLY":recorded(c.formulaType(),p.key())?"RECORDED_INPUT":"MISSING_HISTORICAL_SOURCE";
    if("RECORDED_INPUT".equals(kind)) {
     String table=table(c.formulaType()); projected.add(new FactorEvidence(f.key(),kind,f.boundValue(),table,f.key(),null,null,null,null));
     projectedLabels.put(f.key(),kind); replace(projectedMetas,new FactorContext(f.key(),m.description(),m.status(),null,null,table+":"+snapshot.getItemId()+":"+f.key()));
    } else {
     String reason="MISSING_HISTORICAL_SOURCE".equals(kind)?"MISSING_HISTORICAL_SOURCE:"+f.key():null;
     projected.add(new FactorEvidence(f.key(),kind,f.boundValue(),null,null,null,null,null,reason));
     projectedLabels.put(f.key(),kind); replace(projectedMetas,new FactorContext(f.key(),m.description(),m.status(),null,null,null));
     if(reason!=null) projectedMissing.add(reason);
    }
   } else {
    if(proofs.containsKey(f.key())) throw SnapshotIntegrity.failure("持久化来源未被使用");
    projected.add(f); if("MISSING_HISTORICAL_SOURCE".equals(f.kind())) {missing.add(f.missingReason()); projectedMissing.add(f.missingReason());}
   }
  }
  if(!used.equals(proofs.keySet())) throw SnapshotIntegrity.failure("持久化来源遗漏");
  if(c.policySources().isEmpty()) {projectedMissing.add("NO_CAPTURED_POLICY_SOURCE");if(proofs.isEmpty())missing.add("NO_CAPTURED_POLICY_SOURCE");}
  if(!missing.equals(new HashSet<>(c.missingEvidenceReasons())) || !(missing.isEmpty()?"COMPLETE":"PARTIAL").equals(c.evidenceStatus())) throw SnapshotIntegrity.failure("来源完整性状态不符");
  Context legacy=new Context(3,c.formulaType(),c.description(),c.reproducible(),c.sourceTask(),projectedMetas,c.policySources(),projected,c.decisionEvidence(),projectedMissing.isEmpty()?"COMPLETE":"PARTIAL",new ArrayList<>(projectedMissing),c.asOfDate(),null);
  BizWorkloadCalcSnapshot a=new BizWorkloadCalcSnapshot();a.setItemId(snapshot.getItemId());a.setRuleVersion(PolicyHash.hash(c.policySources()));
  PolicyEvidenceIntegrity.verify(a,legacy,values,projectedLabels);
 }
 private static void replace(List<FactorContext> list,FactorContext replacement) { for(int n=0;n<list.size();n++) if(list.get(n).key().equals(replacement.key())) {list.set(n,replacement);return;} }
 private static boolean recorded(String c,String k) {return "G4".equals(c)&&Set.of("J4","R4").contains(k)||"G5".equals(c)&&"R5".equals(k)||"G6".equals(c)&&Set.of("W","R6").contains(k);}
 private static String table(String c) {return switch(c){case "G4"->"biz_wl_course_design";case "G5"->"biz_wl_thesis";case "G6"->"biz_wl_concentrated_internship";default->throw SnapshotIntegrity.failure("未知输入表");};}
}
