package com.workload.system.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.workload.system.domain.BizWorkloadCalcSnapshot;
import com.workload.system.service.SnapshotContextCodec.Context;

/** SHA-256 内容一致性校验（不是签名或不可变政策来源证明）。 */
public final class SnapshotIntegrity
{
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final List<List<String>> LEGACY_ORDERS = List.of(
            List.of("J1", "C1", "K1", "Q1", "Q2", "N", "Q3"),
            List.of("J1", "C1", "K1", "Q1", "Q2", "N"),
            List.of("J2", "K", "C2", "Q1", "Q2", "Q3"),
            List.of("J2", "K", "C2", "Q1", "Q2"),
            List.of("T", "D", "K", "Q1", "Q2", "Q3"),
            List.of("T", "D", "K", "Q1", "Q2"),
            List.of("J4", "R4", "CONST"), List.of("R5", "K5"),
            List.of("W", "R6", "R6_EFFECTIVE", "CONST"), List.of("岗位减免"));

    public static final int ERROR_CODE = 50017;
    private SnapshotIntegrity() {}
    public static com.workload.common.exception.ServiceException failure(String message)
    {
        return new com.workload.common.exception.ServiceException("计算快照完整性校验失败：" + message, ERROR_CODE);
    }
    public static boolean isFailure(RuntimeException error)
    {
        return error instanceof com.workload.common.exception.ServiceException service
                && Integer.valueOf(ERROR_CODE).equals(service.getCode());
    }
    static ObjectMapper json() { return JSON; }
    public record Verified(Map<String, String> values, Map<String, String> sources, Context context) {}

    public static Verified verify(BizWorkloadCalcSnapshot snapshot)
    {
        try
        {
            Map<String, String> values = readMap(snapshot.getFactorJson());
            Map<String, String> sources = readMap(snapshot.getSourceJson());
            if (snapshot.getFormulaExpression() == null || snapshot.getFormulaExpression().isBlank()
                    || snapshot.getResult() == null || values.isEmpty() || !values.keySet().equals(sources.keySet()))
                throw new IllegalArgumentException("公式、结果、因子或来源不完整");
            // 非 null 的上下文只能显式识别格式，禁止损坏或未知版本降级为旧五段算法。
            Context context = snapshot.getContextJson() == null ? null : SnapshotContextCodec.decode(snapshot.getContextJson());
            List<String> order;
            if (context != null) order = context.factors().stream().map(SnapshotContextCodec.FactorContext::key).toList();
            else order = LEGACY_ORDERS.stream().filter(keys -> new HashSet<>(keys).equals(values.keySet()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("旧快照缺少已知因子顺序的历史证据"));
            if (order.size() != values.size() || !new HashSet<>(order).equals(values.keySet()))
                throw new IllegalArgumentException("快照上下文遗漏或增加因子");
            Map<String, String> orderedValues = ordered(values, order), orderedSources = ordered(sources, order);
            String expected = snapshot.getSnapshotHash();
            if (expected == null || !expected.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("内容哈希缺失或格式损坏");
            boolean matches = context != null && context.formatVersion() == 2
                    ? expected.equals(hashV2(snapshot))
                    : legacyMatches(snapshot, orderedValues, orderedSources);
            if (!matches) throw new IllegalArgumentException("内容哈希不符，历史证据已变更或缺失");
            return new Verified(orderedValues, orderedSources, context);
        }
        catch (com.workload.common.exception.ServiceException e) { throw e; }
        catch (Exception e) { throw SnapshotIntegrity.failure(e.getMessage()); }
    }

    public static String hashV2(BizWorkloadCalcSnapshot snapshot)
    {
        try
        {
            Context context = SnapshotContextCodec.decode(snapshot.getContextJson());
            if (context.formatVersion() != 2) throw new IllegalArgumentException("新哈希仅接受格式2");
            ObjectNode payload = JSON.createObjectNode();
            payload.put("formatVersion", 2);
            // 写入和读取均取持久化快照字段；类型只来自 context.formulaType。
            payload.set("itemId", JSON.valueToTree(snapshot.getItemId()));
            payload.set("calculationVersion", JSON.valueToTree(snapshot.getCalculationVersion()));
            payload.set("context", JSON.readTree(snapshot.getContextJson()));
            payload.set("factors", JSON.valueToTree(readMap(snapshot.getFactorJson())));
            payload.set("sources", JSON.valueToTree(readMap(snapshot.getSourceJson())));
            payload.set("formula", JSON.valueToTree(snapshot.getFormulaExpression()));
            payload.set("ruleVersion", JSON.valueToTree(snapshot.getRuleVersion()));
            payload.set("result", JSON.valueToTree(snapshot.getResult() == null ? null
                    : snapshot.getResult().stripTrailingZeros().toPlainString()));
            return digest(JSON.writeValueAsString(canonical(payload)));
        }
        catch (com.workload.common.exception.ServiceException e) { throw e; }
        catch (Exception e) { throw SnapshotIntegrity.failure(e.getMessage()); }
    }

    private static boolean legacyMatches(BizWorkloadCalcSnapshot snapshot, Map<String, String> values,
            Map<String, String> sources) throws Exception
    {
        String prefix = JSON.writeValueAsString(values) + '\n' + JSON.writeValueAsString(sources) + '\n'
                + nullable(snapshot.getFormulaExpression()) + '\n' + nullable(snapshot.getRuleVersion()) + '\n';
        String suffix = snapshot.getContextJson() == null ? "" : "\n" + snapshot.getContextJson();
        // result 原字段为 DECIMAL(10,2)：仅尝试等值的 scale 0..2，不猜被舍入的原值或其他精度。
        for (int scale = 0; scale <= 2; scale++)
        {
            try
            {
                String result = snapshot.getResult().setScale(scale, RoundingMode.UNNECESSARY).toPlainString();
                if (snapshot.getSnapshotHash().equals(digest(prefix + result + suffix))) return true;
            }
            catch (ArithmeticException ignored) { /* 该 scale 无法精确表示，不是历史等值候选。 */ }
        }
        return false;
    }

    private static String nullable(String value) { return value == null ? "" : value; }
    private static Map<String, String> readMap(String text) throws Exception
    {
        JsonNode node = JSON.readTree(text);
        if (node == null || !node.isObject()) throw new IllegalArgumentException("因子/来源 JSON 必须是对象");
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext())
        {
            var entry = fields.next();
            if (entry.getKey().isBlank() || (!entry.getValue().isNull() && !entry.getValue().isTextual()))
                throw new IllegalArgumentException("因子/来源必须保留原始字符串取值");
        }
        return JSON.convertValue(node, new TypeReference<LinkedHashMap<String, String>>() {});
    }
    private static Map<String, String> ordered(Map<String, String> map, List<String> keys)
    {
        Map<String, String> result = new LinkedHashMap<>();
        keys.forEach(key -> result.put(key, map.get(key)));
        return result;
    }
    private static JsonNode canonical(JsonNode node)
    {
        if (node.isObject())
        {
            ObjectNode sorted = JSON.createObjectNode();
            List<String> keys = new ArrayList<>(); node.fieldNames().forEachRemaining(keys::add);
            Collections.sort(keys); keys.forEach(key -> sorted.set(key, canonical(node.get(key))));
            return sorted;
        }
        if (node.isArray())
        {
            ArrayNode array = JSON.createArrayNode(); node.forEach(value -> array.add(canonical(value))); return array;
        }
        return node;
    }
    private static String digest(String text) throws Exception
    {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
