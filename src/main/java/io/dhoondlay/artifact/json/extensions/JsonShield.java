package io.dhoondlay.artifact.json.extensions;

import io.dhoondlay.artifact.json.model.*;

import java.util.Optional;

/**
 * <h2>JsonShield — Safe Access with Default Values</h2>
 *
 * <p>Wraps a {@link JsonNode} root and provides null-safe, default-value-backed
 * access using dot-path navigation. Never throws for missing paths — always
 * returns the specified default.</p>
 *
 * <h3>Use cases:</h3>
 * <ul>
 *   <li>Reading configuration objects where keys may be absent</li>
 *   <li>Safely extracting API response fields without nested null checks</li>
 *   <li>PII redaction via {@link #redact(String...)}</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * JsonShield shield = new JsonShield(configNode);
 *
 * String host    = shield.getString("server.host", "localhost");
 * int    port    = shield.getInt("server.port", 8080);
 * boolean debug  = shield.getBoolean("logging.debug", false);
 * double  timeout= shield.getDouble("timeout.ms", 30_000.0);
 *
 * // Redact sensitive fields from the tree
 * JsonNode safe = shield.redact("password", "ssn", "token");
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public class JsonShield {

    private final JsonNode root;

    /** Wraps the given root node. */
    public JsonShield(JsonNode root) {
        this.root = root;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Safe Getters
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Safely retrieves a String value at the given dot-path.
     *
     * @param path         dot-path (e.g., {@code "server.host"})
     * @param defaultValue value to return if path is missing or not a string
     */
    public String getString(String path, String defaultValue) {
        return root.find(path)
                   .filter(JsonNode::isValue)
                   .map(n -> n.asText(defaultValue))
                   .orElse(defaultValue);
    }

    /**
     * Safely retrieves an int value at the given dot-path.
     *
     * @param path         dot-path
     * @param defaultValue fallback value
     */
    public int getInt(String path, int defaultValue) {
        return root.find(path)
                   .filter(JsonNode::isNumber)
                   .map(n -> n.asInt(defaultValue))
                   .orElse(defaultValue);
    }

    /**
     * Safely retrieves a long value at the given dot-path.
     *
     * @param path         dot-path
     * @param defaultValue fallback value
     */
    public long getLong(String path, long defaultValue) {
        return root.find(path)
                   .filter(JsonNode::isNumber)
                   .map(n -> n.asLong(defaultValue))
                   .orElse(defaultValue);
    }

    /**
     * Safely retrieves a double value at the given dot-path.
     *
     * @param path         dot-path
     * @param defaultValue fallback value
     */
    public double getDouble(String path, double defaultValue) {
        return root.find(path)
                   .filter(JsonNode::isNumber)
                   .map(n -> n.asDouble(defaultValue))
                   .orElse(defaultValue);
    }

    /**
     * Safely retrieves a boolean value at the given dot-path.
     *
     * @param path         dot-path
     * @param defaultValue fallback value
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return root.find(path)
                   .filter(JsonNode::isBoolean)
                   .map(n -> n.asBoolean(defaultValue))
                   .orElse(defaultValue);
    }

    /**
     * Returns the raw {@link JsonNode} at the path, or empty if not found.
     *
     * @param path dot-path
     */
    public Optional<JsonNode> get(String path) {
        return root.find(path);
    }

    /**
     * Returns {@code true} if the given path exists in the tree.
     *
     * @param path dot-path
     */
    public boolean exists(String path) {
        return root.find(path).isPresent();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Redaction
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a new JSON tree with the specified keys replaced by {@code "[REDACTED]"}
     * at every level of nesting.
     *
     * @param keys field names to redact (case-insensitive)
     * @return new tree with sensitive fields masked
     */
    public JsonNode redact(String... keys) {
        java.util.Set<String> keySet = new java.util.HashSet<>();
        for (String k : keys) keySet.add(k.toLowerCase());
        return redactNode(root, keySet);
    }

    private JsonNode redactNode(JsonNode node, java.util.Set<String> keys) {
        return switch (node) {
            case JsonObject obj -> {
                JsonObject result = new JsonObject(obj.size());
                obj.fields().forEach((k, v) -> {
                    if (keys.contains(k.toLowerCase())) result.put(k, new JsonValue("[REDACTED]"));
                    else result.put(k, redactNode(v, keys));
                });
                yield result;
            }
            case JsonArray arr -> {
                JsonArray result = new JsonArray(arr.size());
                arr.elements().forEach(e -> result.add(redactNode(e, keys)));
                yield result;
            }
            default -> node;
        };
    }
}
