package io.github.dhoondlayai.artifact.json.extensions;

import io.github.dhoondlayai.artifact.json.model.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Enterprise Data Redaction & PII Masking Engine.
 * Automatically obfuscates sensitive data (e.g., passwords, SSNs, credit cards)
 * directly within the JSON Tree before logging or serialization.
 */
public class JsonMasker {

    private final Set<String> sensitiveKeys = new HashSet<>();
    private final String maskString;

    public JsonMasker(String maskString) {
        this.maskString = maskString;
    }

    public JsonMasker addSensitiveKey(String key) {
        this.sensitiveKeys.add(key.toLowerCase());
        return this;
    }

    /**
     * Traverses the JSON tree and returns a new JsonNode with sensitive fields
     * masked.
     * Uses an immutable transformation logic to prevent modifying the original
     * tree.
     */
    public JsonNode mask(JsonNode root) {
        if (root instanceof JsonObject obj) {
            JsonObject maskedObj = new JsonObject();
            obj.fields().forEach((k, v) -> {
                if (sensitiveKeys.contains(k.toLowerCase())) {
                    maskedObj.put(k, new JsonValue(maskString));
                } else {
                    maskedObj.put(k, mask(v));
                }
            });
            return maskedObj;
        } else if (root instanceof JsonArray arr) {
            JsonArray maskedArr = new JsonArray();
            for (JsonNode element : arr.elements()) {
                maskedArr.add(mask(element));
            }
            return maskedArr;
        }
        return root; // Leaf values that are not under sensitive keys remain unchanged
    }
}
