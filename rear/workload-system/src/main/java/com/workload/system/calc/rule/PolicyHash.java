package com.workload.system.calc.rule;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

/** Stable structured hashes; object order is irrelevant, array order is retained. */
public final class PolicyHash {
    private static final ObjectMapper JSON = new ObjectMapper();
    private PolicyHash() {}
    public static String hash(Object value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(JSON.writeValueAsString(canonical(JSON.valueToTree(value))).getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalArgumentException("政策内容无法摘要", e); }
    }
    private static JsonNode canonical(JsonNode node) {
        if (node.isObject()) {
            ObjectNode out=JSON.createObjectNode(); List<String> keys=new ArrayList<>();
            node.fieldNames().forEachRemaining(keys::add); Collections.sort(keys);
            keys.forEach(k -> out.set(k,canonical(node.get(k)))); return out;
        }
        if (node.isArray()) { ArrayNode out=JSON.createArrayNode(); node.forEach(n -> out.add(canonical(n))); return out; }
        return node;
    }
}
