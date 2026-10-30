package com.workload.system.calc;
import static org.assertj.core.api.Assertions.*;
import java.util.*;import java.util.stream.*;
import org.junit.jupiter.api.Test;import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.*;
import com.workload.system.calc.rule.PolicyHash;
import static com.workload.system.calc.SettlementEvidence.*;
/** Resealing every hash must not turn omitted historical fields into current-code defaults. */
class SettlementTreeCompletenessTest {
 static Stream<String> missingFields(){var e=new SettlementTestData().evidence();return Stream.of(Map.entry("person",e.person()),Map.entry("rate",e.rate()),Map.entry("selection",e.selection()),Map.entry("results",e.results())).flatMap(entry->entry.getValue().keySet().stream().map(key->entry.getKey()+":"+key));}
 @ParameterizedTest @MethodSource("missingFields")
 void eachHistoricalMapKeyIsMandatoryEvenWhenItsValueMayBeNull(String field)throws Exception {var e=new SettlementTestData().evidence();var maps=maps(e);var parts=field.split(":");maps.get(parts[0]).remove(parts[1]);assertInvalid(rebuild(e,maps));}
 @ParameterizedTest @ValueSource(strings={"person","rate","selection","results"})
 void unknownMapFieldsAreNotAFormatExtension(String section)throws Exception {var e=new SettlementTestData().evidence();var maps=maps(e);maps.get(section).put("unknownField","mixed version");assertInvalid(rebuild(e,maps));}
 @ParameterizedTest @ValueSource(strings={"autumnStart","autumnEnd","springStart","springEnd"})
 void everyCalendarFieldMustExistWithoutDefaultFallback(String key)throws Exception {var e=new SettlementTestData().evidence();var maps=maps(e);var json=new com.fasterxml.jackson.databind.ObjectMapper();var calendar=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(e.selection().get("calendarJson"));calendar.remove(key);maps.get("selection").put("calendarJson",calendar.toString());assertInvalid(rebuild(e,maps));}
 @ParameterizedTest @ValueSource(strings={"autumnStart","autumnEnd","springStart","springEnd"})
 void everyCalendarFieldMustHaveAValidMonthDayString(String key)throws Exception {var e=new SettlementTestData().evidence();var maps=maps(e);var json=new com.fasterxml.jackson.databind.ObjectMapper();var calendar=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(e.selection().get("calendarJson"));calendar.put(key,false);maps.get("selection").put("calendarJson",calendar.toString());assertInvalid(rebuild(e,maps));}
 @Test void skinnyPersonAndRateCannotBeResealedAsComplete()throws Exception {var e=new SettlementTestData().evidence();var maps=maps(e);maps.get("person").keySet().retainAll(Set.of("title","nature"));maps.get("rate").keySet().retainAll(Set.of("id","title","rate","status"));assertInvalid(rebuild(e,maps));}
 private Map<String,Map<String,String>> maps(SummaryEvidence e){Map<String,Map<String,String>> m=new HashMap<>();m.put("person",new TreeMap<>(e.person()));m.put("rate",new TreeMap<>(e.rate()));m.put("selection",new TreeMap<>(e.selection()));m.put("results",new TreeMap<>(e.results()));return m;}
 private SummaryEvidence rebuild(SummaryEvidence e,Map<String,Map<String,String>> m){return seal(new SummaryEvidence(e.summaryId(),e.userId(),e.semester(),e.mode(),m.get("selection"),m.get("person"),e.eligibilityReason(),e.profileBoundary(),e.items(),e.rules(),m.get("rate"),PolicyHash.hash(m.get("rate")),m.get("results"),e.threeTheoryCourses(),e.warnings(),null));}
 private void assertInvalid(SummaryEvidence e){var pay=seal(new PayEvidence(2L,e.hash(),List.of(),"0.00","0.00",0L,"course/other:scale2;total:scale0;HALF_UP",null));var payload=new Payload(1,1L,ALGORITHM,e,pay);assertThatThrownBy(()->decode(encode(payload),hash(payload))).isInstanceOf(com.workload.common.exception.ServiceException.class).extracting("code").isEqualTo(50017);}
}
