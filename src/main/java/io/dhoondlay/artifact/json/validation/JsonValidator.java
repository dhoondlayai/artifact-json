package io.dhoondlay.artifact.json.validation;

import io.dhoondlay.artifact.json.model.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * ⚡ Fluent JSON Schema Validator.
 * No need for heavy external JSON schemas. Build verification trees
 * using high-performance rules.
 */
public class JsonValidator {
    private final Map<String, Predicate<JsonNode>> rules = new LinkedHashMap<>();

    public JsonValidator require(String path, Predicate<JsonNode> rule) {
        rules.put(path, rule);
        return this;
    }

    public JsonValidator isString(String path) {
        return require(path, node -> node instanceof JsonValue v && v.value() instanceof String);
    }

    public JsonValidator isNumber(String path) {
        return require(path, node -> node instanceof JsonValue v && v.value() instanceof Number);
    }

    /**
     * Executes the verification and returns a list of failed paths.
     */
    public List<String> verify(JsonNode root) {
        List<String> violations = new ArrayList<>();
        rules.forEach((path, rule) -> {
            Optional<JsonNode> node = root.find(path);
            if (node.isEmpty() || !rule.test(node.get())) {
                violations.add(path);
            }
        });
        return violations;
    }
}
