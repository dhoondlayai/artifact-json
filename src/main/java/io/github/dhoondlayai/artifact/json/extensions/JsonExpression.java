package io.github.dhoondlayai.artifact.json.extensions;

import io.github.dhoondlayai.artifact.json.model.*;
import java.util.*;
import java.util.regex.*;

/**
 * âš¡ Live Expression Engine for JSON Trees.
 * Allows calculating values dynamically within the JSON structure using expressions.
 * Example: {"total": "${price * quantity}"}
 */
public class JsonExpression {
    private static final Pattern EXPR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Evaluates all expressions within a JSON tree using the tree itself as context.
     * @param root The JsonNode to evaluate.
     * @return A new JsonNode with all expressions resolved to actual values.
     */
    public static JsonNode evaluate(JsonNode root) {
        return evaluateRecursive(root, root);
    }

    private static JsonNode evaluateRecursive(JsonNode current, JsonNode context) {
        if (current instanceof JsonObject obj) {
            JsonObject result = new JsonObject();
            obj.fields().forEach((k, v) -> result.put(k, evaluateRecursive(v, context)));
            return result;
        } else if (current instanceof JsonArray arr) {
            JsonArray result = new JsonArray();
            arr.elements().forEach(e -> result.add(evaluateRecursive(e, context)));
            return result;
        } else if (current instanceof JsonValue val && val.value() instanceof String str) {
            Matcher matcher = EXPR_PATTERN.matcher(str);
            if (matcher.find()) {
                String expression = matcher.group(1);
                return new JsonValue(calculate(expression, context));
            }
        }
        return current;
    }

    private static Object calculate(String expr, JsonNode context) {
        // Simple expression engine: handles path access and basic multiplication
        if (expr.contains("*")) {
            String[] parts = expr.split("\\*");
            double res = 1.0;
            for (String part : parts) {
                res *= getNumericValue(part.trim(), context);
            }
            return res;
        }
        // Basic path lookup
        return context.find(expr).map(node -> (node instanceof JsonValue v) ? v.value() : node).orElse(0.0);
    }

    private static double getNumericValue(String key, JsonNode context) {
        return context.find(key).map(node -> {
            if (node instanceof JsonValue v && v.value() instanceof Number num) {
                return num.doubleValue();
            }
            return 0.0;
        }).orElse(0.0);
    }
}
