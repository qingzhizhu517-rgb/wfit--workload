package com.workload.system.calc;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
/** Formal activation requires a separately approved semester. QA never implies approval. */
@Component @ConfigurationProperties(prefix="wl.settlement")
public class SettlementScope {
 private String formalStartSemester="";
 private List<String> verificationSemesters=new ArrayList<>();
 public String mode(String semester) {
  if(verificationSemesters.contains(semester)) return "ISOLATED_VERIFICATION_ONLY";
  if(formalStartSemester!=null && !formalStartSemester.isBlank() && semester!=null && semester.compareTo(formalStartSemester)>=0) return "FORMAL";
  return "LEGACY_NOT_ENABLED";
 }
 public boolean required(String semester){return !"LEGACY_NOT_ENABLED".equals(mode(semester));}
 public String getFormalStartSemester(){return formalStartSemester;}
 public void setFormalStartSemester(String s){if(s!=null&&!s.isBlank())new SemesterCalendar().endOf(s);formalStartSemester=s;}
 public List<String> getVerificationSemesters(){return verificationSemesters;}
 public void setVerificationSemesters(List<String> s){s.forEach(v->new SemesterCalendar().endOf(v));verificationSemesters=new ArrayList<>(s);}
}
