package io.dhoondlay.artifact.json.extensions;

import io.dhoondlay.artifact.json.model.*;

import java.util.*;

/**
 * <h2>JsonExtensions — Advanced Search and Transformation Utilities</h2>
 *
 * <p>Provides wildcard path search, deep key/value search, schema inference,
 * FIX protocol parsing, and self-healing parser capabilities.</p>
 *
 * <h3>Examples:</h3>
 * <pre>{@code
 * JsonExtensions ext = new JsonExtensions();
 *
 * // Wildcard: all book prices
 * List<JsonNode> prices = ext.wildcardFind(root, "store.books[*].price");
 *
 * // Deep key search
 * List<JsonNode> allAuthors = ext.searchByKey(root, "author");
 *
 * // Deep value search
 * List<JsonNode> found = ext.searchByValue(root, "George Orwell");
 *
 * // Infer JSON Schema from a POJO
 * JsonNode schema = ext.inferSchema(MyDto.class);
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public class JsonExtensions {

    // ─────────────────────────────────────────────────────────────────────────
    // Wildcard Path Search
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves a wildcard path expression against a JSON tree.
     *
     * <h4>Supported syntax:</h4>
     * <ul>
     *   <li>{@code store.books[*].title} — all titles in books array</li>
     *   <li>{@code store.*.price}        — all price values in any child of store</li>
     *   <li>{@code *}                    — all direct children of root</li>
     * </ul>
     *
     * @param root the JSON tree to search
     * @param path wildcard path expression
     * @return list of matching {@link JsonNode} values
     */
    public List<JsonNode> wildcardFind(JsonNode root, String path) {
        List<JsonNode> current = List.of(root);
        String[] segments = path.split("\\.");
        for (String segment : segments) {
            List<JsonNode> next = new ArrayList<>();
            for (JsonNode n : current) {
                if (segment.equals("*")) {
                    if (n instanceof JsonObject obj) next.addAll(obj.fields().values());
                    else if (n instanceof JsonArray arr) next.addAll(arr.elements());
                } else if (segment.endsWith("[*]")) {
                    String key = segment.substring(0, segment.indexOf('['));
                    JsonNode node = key.isEmpty() ? n : n.get(key);
                    if (node instanceof JsonArray arr) next.addAll(arr.elements());
                } else {
                    JsonNode node = n.get(segment);
                    if (node != null) next.add(node);
                }
            }
            current = next;
        }
        return current;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Deep Key/Value Search
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Searches for all nodes with the given key anywhere in the tree (DFS).
     *
     * @param root the tree to search
     * @param key  the field name to find
     * @return list of matching value nodes
     */
    public List<JsonNode> searchByKey(JsonNode root, String key) {
        List<JsonNode> results = new ArrayList<>();
        JsonTraversal.traverse(root, node -> {
            if (node instanceof JsonObject obj && obj.contains(key)) {
                results.add(obj.field(key));
            }
        });
        return results;
    }

    /**
     * Searches for all {@link JsonValue} nodes whose value equals the given object.
     *
     * @param root  the tree to search
     * @param value the value to match (compared via {@link Objects#equals})
     * @return list of matching {@link JsonValue} nodes
     */
    public List<JsonNode> searchByValue(JsonNode root, Object value) {
        List<JsonNode> results = new ArrayList<>();
        JsonTraversal.traverse(root, node -> {
            if (node instanceof JsonValue val && Objects.equals(val.value(), value)) {
                results.add(val);
            }
        });
        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case Conversion
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts a {@code snake_case} string to {@code camelCase}.
     *
     * @param s input string
     * @return camelCase version
     */
    public String snakeToCamel(String s) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : s.toCharArray()) {
            if (c == '_')        { nextUpper = true; }
            else if (nextUpper)  { sb.append(Character.toUpperCase(c)); nextUpper = false; }
            else                 { sb.append(c); }
        }
        return sb.toString();
    }

    /**
     * Converts a {@code camelCase} string to {@code snake_case}.
     *
     * @param s input string
     * @return snake_case version
     */
    public String camelToSnake(String s) {
        return s.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Schema Inference
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Infers a draft JSON Schema from a Java class via reflection.
     * Returns a {@link JsonObject} with {@code type} and {@code properties}.
     *
     * @param clazz the Java class to inspect
     * @return JSON Schema as a {@link JsonNode}
     */
    public JsonNode inferSchema(Class<?> clazz) {
        JsonObject schema = new JsonObject();
        schema.put("type", "object");
        schema.put("title", clazz.getSimpleName());
        JsonObject properties = new JsonObject();
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            String typeName = field.getType().getSimpleName();
            String jsonType = switch (typeName) {
                case "String"            -> "string";
                case "int", "Integer", "long", "Long", "double", "Double", "float", "Float" -> "number";
                case "boolean", "Boolean"-> "boolean";
                default                  -> "object";
            };
            JsonObject fieldSchema = new JsonObject().put("type", jsonType);
            properties.put(field.getName(), fieldSchema);
        }
        schema.put("properties", properties);
        return schema;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Format Adapters
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses a FIX Protocol (tag=value|tag=value) message into a {@link JsonObject}.
     * FIX is a standard used in financial markets.
     *
     * @param fixMessage FIX format string (pipe-delimited key=value pairs)
     * @return parsed {@link JsonObject}
     */
    public JsonNode fixToJson(String fixMessage) {
        JsonObject obj = new JsonObject();
        for (String pair : fixMessage.split("\\|")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) obj.put("tag_" + kv[0].trim(), new JsonValue(kv[1].trim()));
        }
        return obj;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Self-Healing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempts to parse potentially malformed JSON by stripping trailing commas,
     * fixing unquoted keys, and other common mistakes before delegating to the
     * full parser.
     *
     * @param rawJson potentially malformed JSON string
     * @return best-effort {@link JsonNode}
     */
    public JsonNode parseSelfHealing(String rawJson) {
        if (rawJson == null) return new JsonValue(null);
        String healed = rawJson
                .replaceAll(",\\s*([}\\]])", "$1")     // trailing commas
                .replaceAll("([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)\\s*:", "$1\"$2\":")  // unquoted keys
                .replaceAll("'([^']*)'", "\"$1\"");     // single-quoted strings
        try {
            return io.dhoondlay.artifact.json.streaming.FastJsonEngine.parse(healed);
        } catch (Exception e) {
            return new JsonValue("PARSE_FAILED: " + e.getMessage());
        }
    }
}
