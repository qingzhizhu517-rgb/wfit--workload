package com.workload.system.calc.rule;
import java.math.BigDecimal;
import com.workload.system.domain.dto.TeachingTaskImportDTO;
import com.workload.system.domain.dto.TeachingTaskImportV1DTO;
import com.workload.common.exception.ServiceException;
import org.springframework.util.StringUtils;

/** Frozen V1 transformation: input template conversion and defaults, no database/current-policy access. */
public final class TeachingTaskInputNormalizer {
    public static final String VERSION="TEACHING_TASK_INPUT_V1";
    public static final String CONTENT="V1 baseValue -> G1/G2 plannedHours, G3 days, G4 credits, G6 weeks; G5 ignores baseValue; V2 quantity fields unchanged; both templates blank courseNature -> 必修, blank majorCategory -> 理工类; trim these two fields; all other fields unchanged";
    private static final java.util.Set<String> NUMERIC_FIELDS=java.util.Set.of("plannedHours","days","weeks","credits","studentCount","courseCoefficient","repeatOrder");
    private TeachingTaskInputNormalizer() {}
    public static boolean sameField(String field,Object left,Object right) {
        if(NUMERIC_FIELDS.contains(field)) return (left==null || left instanceof Number) && (right==null || right instanceof Number)
                && FactorProvenance.same(FactorProvenance.plain(left),FactorProvenance.plain(right));
        return java.util.Objects.equals(left,right);
    }
    public static boolean samePlainField(String field,Object normalized,String input) {
        if(NUMERIC_FIELDS.contains(field)) return FactorProvenance.same(FactorProvenance.plain(normalized),input);
        return java.util.Objects.equals(normalized,input);
    }
    public static TeachingTaskImportDTO convertV1(TeachingTaskImportV1DTO v1)
    {
        TeachingTaskImportDTO dto = new TeachingTaskImportDTO();
        dto.setSemester(v1.getSemester());
        dto.setUserCode(v1.getUserCode());
        dto.setUserName(v1.getUserName());
        dto.setCourseName(v1.getCourseName());
        dto.setCourseCode(v1.getCourseCode());
        dto.setWorkloadType(v1.getWorkloadType());
        dto.setEducationLevel(v1.getEducationLevel());
        dto.setMajorCategory(v1.getMajorCategory());
        dto.setCourseNature(v1.getCourseNature());
        dto.setCourseLevel(v1.getCourseLevel());
        dto.setCourseRole(v1.getCourseRole());
        dto.setTeachingEval(v1.getTeachingEval());
        dto.setStudentCount(v1.getStudentCount());
        dto.setCourseCoefficient(v1.getCourseCoefficient());
        dto.setClassName(v1.getClassName());
        dto.setRepeatOrder(v1.getRepeatOrder());

        String type = v1.getWorkloadType() == null ? "" : v1.getWorkloadType().trim().toUpperCase();
        BigDecimal base = v1.getBaseValue();
        switch (type)
        {
            case "G1":
            case "G2":
                dto.setPlannedHours(base);
                break;
            case "G3":
                dto.setDays(base);
                break;
            case "G6":
                dto.setWeeks(base);
                break;
            case "G4":
                dto.setCredits(base);
                break;
            default:
                // G5 只用选课人数，基数列无语义；其余非法类别留待 validateRow 拒绝
                break;
        }
        return dto;
    }
    public static void normalize(TeachingTaskImportDTO dto)
    {
        if (dto == null)
        {
            throw new ServiceException("导入行不能为空");
        }
        dto.setCourseNature(defaultStr(dto.getCourseNature(), "必修").trim());
        dto.setMajorCategory(defaultStr(dto.getMajorCategory(), "理工类").trim());
    }
    private static String defaultStr(String value,String fallback){return StringUtils.hasText(value)?value:fallback;}
    public static void validateTree(com.fasterxml.jackson.databind.JsonNode tree,String template) {
        if(tree==null || !tree.isObject()) throw new IllegalArgumentException("输入行必须是对象");
        Class<?> type=switch(template){case "V1"->TeachingTaskImportV1DTO.class;case "V2"->TeachingTaskImportDTO.class;default->throw new IllegalArgumentException("未知输入模板版本");};
        java.util.Map<String,Class<?>> fields=new java.util.HashMap<>();
        for(var field:type.getDeclaredFields()) if(field.isAnnotationPresent(com.alibaba.excel.annotation.ExcelProperty.class)) fields.put(field.getName(),field.getType());
        var names=tree.fieldNames();
        while(names.hasNext()) {
            String name=names.next();Class<?> fieldType=fields.get(name);var value=tree.get(name);
            if(fieldType==null) throw new IllegalArgumentException("未知输入字段："+name);
            if(value.isNull()) continue;
            boolean valid=fieldType==String.class ? value.isTextual() : fieldType==Integer.class ? value.isIntegralNumber() && value.canConvertToInt() : fieldType==BigDecimal.class && value.isNumber();
            if(!valid) throw new IllegalArgumentException("输入字段类型不符："+name);
        }
    }
    public static TeachingTaskImportDTO replay(String raw,String template,String version) {
        if(!VERSION.equals(version)) throw new IllegalArgumentException("未知输入转换版本");
        try {
            var json=new com.fasterxml.jackson.databind.ObjectMapper()
                .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            validateTree(json.readTree(raw),template);
            TeachingTaskImportDTO dto=switch(template) {
                case "V1" -> convertV1(json.readValue(raw,TeachingTaskImportV1DTO.class));
                case "V2" -> json.readValue(raw,TeachingTaskImportDTO.class);
                default -> throw new IllegalArgumentException("未知输入模板版本");
            };
            normalize(dto);return dto;
        }catch(Exception e){throw new IllegalArgumentException("原始输入无法回放转换",e);}
    }
}
