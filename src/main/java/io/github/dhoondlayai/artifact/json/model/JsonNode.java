package io.github.dhoondlayai.artifact.json.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <h2>JsonNode â€” The Core Type of artifact-json</h2>
 *
 * <p>
 * A sealed interface that forms the base of the entire JSON tree model.
 * All JSON elements â€” objects, arrays, and scalar values â€” implement this
 * interface.
 * </p>
 *
 * Uses Java 17+ sealed hierarchy + pattern matching for safe, zero-cast access.
 * </p>
 *
 * <h3>Type Hierarchy:</h3>
 * 
 * <pre>
 *   JsonNode
 *   â”œâ”€â”€ JsonObject  (key-value pairs)
 *   â”œâ”€â”€ JsonArray   (ordered list)
 *   â””â”€â”€ JsonValue   (String, Number, Boolean, null)
 * </pre>
 *
 * <h3>Example:</h3>
 * 
 * <pre>{@code
 * JsonNode node = FastJsonEngine.parse("{\"name\":\"Alice\",\"age\":30}");
 * String name = node.get("name").asText(); // "Alice"
 * int age = node.get("age").asInt(); // 30
 * boolean exists = node.get("name").isValue(); // true
 * Optional<JsonNode> safe = node.find("address.city"); // Optional.empty()
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public sealed interface JsonNode permits JsonObject, JsonArray, JsonValue {

    //
    // Type Checks
    //

    /** Returns {@code true} if this node is a {@link JsonObject}. */
    default boolean isObject() {
        return this instanceof JsonObject;
    }

    /** Returns {@code true} if this node is a {@link JsonArray}. */
    default boolean isArray() {
        return this instanceof JsonArray;
    }

    /** Returns {@code true} if this node is a {@link JsonValue}. */
    default boolean isValue() {
        return this instanceof JsonValue;
    }

    /**
     * Returns {@code true} if this node represents a JSON {@code null}.
     * Only {@link JsonValue} with {@code null} payload qualifies.
     */
    default boolean isNull() {
        return this instanceof JsonValue v && v.value() == null;
    }

    /** Returns {@code true} if this node holds a {@link String}. */
    default boolean isString() {
        return this instanceof JsonValue v && v.value() instanceof String;
    }

    /** Returns {@code true} if this node holds a {@link Number}. */
    default boolean isNumber() {
        return this instanceof JsonValue v && v.value() instanceof Number;
    }

    /** Returns {@code true} if this node holds a {@link Boolean}. */
    default boolean isBoolean() {
        return this instanceof JsonValue v && v.value() instanceof Boolean;
    }

    //
    // Child Access â€” by key (objects) and index (arrays)
    //

    /**
     * Accesses a child by string key. Returns {@code null} if not a
     * {@link JsonObject}
     * or if the key is absent.
     *
     * @param key the field name
     * @return child {@link JsonNode} or {@code null}
     */
    default JsonNode get(String key) {
        return (this instanceof JsonObject obj) ? obj.field(key) : null;
    }

    /**
     * Accesses an element by index. Returns {@code null} if not a {@link JsonArray}
     * or if the index is out of bounds.
     *
     * @param index zero-based position
     * @return child {@link JsonNode} or {@code null}
     */
    default JsonNode get(int index) {
        if (this instanceof JsonArray arr) {
            try {
                return arr.element(index);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
        return null;
    }

    //
    // Value Extraction â€” with safe defaults
    //

    /**
     * Returns the string representation of this node's value.
     * For {@link JsonObject}/{@link JsonArray} returns the JSON string.
     * For {@code null} values returns {@code "null"}.
     */
    default String asText() {
        if (this instanceof JsonValue v) {
            return v.value() == null ? "null" : String.valueOf(v.value());
        }
        return toString();
    }

    /**
     * Returns string value, or {@code defaultValue} if null or not a string.
     *
     * @param defaultValue fallback
     */
    default String asText(String defaultValue) {
        if (this instanceof JsonValue v && v.value() instanceof String s)
            return s;
        return defaultValue;
    }

    /**
     * Returns this node's integer value.
     * Returns {@code 0} for non-numeric nodes.
     */
    default int asInt() {
        return (this instanceof JsonValue v && v.value() instanceof Number n) ? n.intValue() : 0;
    }

    /** Returns this node's integer value, or {@code defaultValue}. */
    default int asInt(int defaultValue) {
        return (this instanceof JsonValue v && v.value() instanceof Number n) ? n.intValue() : defaultValue;
    }

    /**
     * Returns this node's long value.
     * Returns {@code 0L} for non-numeric nodes.
     */
    default long asLong() {
        return (this instanceof JsonValue v && v.value() instanceof Number n) ? n.longValue() : 0L;
    }

    /** Returns this node's long value, or {@code defaultValue}. */
    default long asLong(long defaultValue) {
        return (this instanceof JsonValue v && v.value() instanceof Number n) ? n.longValue() : defaultValue;
    }

    /**
     * Returns this node's double value.
     * Returns {@code 0.0} for non-numeric nodes.
     */
    default double asDouble() {
        return (this instanceof JsonValue v && v.value() instanceof Number n) ? n.doubleValue() : 0.0;
    }

    /** Returns this node's double value, or {@code defaultValue}. */
    default double asDouble(double defaultValue) {
        return (this instanceof JsonValue v && v.value() instanceof Number n) ? n.doubleValue() : defaultValue;
    }

    /**
     * Returns this node's {@link BigDecimal} value for precision-sensitive use
     * cases.
     * Returns {@link BigDecimal#ZERO} for non-numeric nodes.
     */
    default BigDecimal asBigDecimal() {
        if (this instanceof JsonValue v && v.value() instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        return BigDecimal.ZERO;
    }

    /**
     * Returns this node's boolean value.
     * Returns {@code false} for non-boolean nodes.
     */
    default boolean asBoolean() {
        return (this instanceof JsonValue v && v.value() instanceof Boolean b) && b;
    }

    /** Returns this node's boolean value, or {@code defaultValue}. */
    default boolean asBoolean(boolean defaultValue) {
        return (this instanceof JsonValue v && v.value() instanceof Boolean b) ? b : defaultValue;
    }

    //
    // Path Navigation â€” dot-path and array index expressions
    //

    /**
     * Navigates a dot-separated path with optional array index notation.
     * Returns an {@link Optional} so callers can safely handle missing paths.
     *
     * <h4>Example paths:</h4>
     * <ul>
     * <li>{@code "store.name"}</li>
     * <li>{@code "orders[0].id"}</li>
     * <li>{@code "data.items[2].price"}</li>
     * </ul>
     *
     * @param path dot-separated path expression
     * @return {@link Optional} containing the node, or empty if not found
     */
    default Optional<JsonNode> find(String path) {
        if (path == null || path.isBlank())
            return Optional.of(this);
        String[] segments = path.split("\\.");
        JsonNode current = this;
        for (String segment : segments) {
            if (current == null)
                return Optional.empty();
            if (segment.contains("[") && segment.contains("]")) {
                String key = segment.substring(0, segment.indexOf('['));
                String indexStr = segment.substring(segment.indexOf('[') + 1, segment.indexOf(']'));
                if (!key.isEmpty()) {
                    current = current.get(key);
                    if (current == null)
                        return Optional.empty();
                }
                try {
                    current = current.get(Integer.parseInt(indexStr));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            } else {
                current = current.get(segment);
            }
        }
        return Optional.ofNullable(current);
    }

    /**
     * Finds all descendants matching the given key at any depth (recursive search).
     *
     * @param key the key to find everywhere in the tree
     * @return a list of all matching {@link JsonNode} values
     */
    default List<JsonNode> findAll(String key) {
        List<JsonNode> results = new ArrayList<>();
        collectByKey(this, key, results);
        return results;
    }

    private static void collectByKey(JsonNode node, String key, List<JsonNode> results) {
        if (node instanceof JsonObject obj) {
            obj.fields().forEach((k, v) -> {
                if (k.equals(key))
                    results.add(v);
                collectByKey(v, key, results);
            });
        } else if (node instanceof JsonArray arr) {
            arr.elements().forEach(e -> collectByKey(e, key, results));
        }
    }

    //
    // Deep Equality
    //

    /**
     * Performs a deep structural equality check between two {@link JsonNode} trees.
     * This is not the same as reference equality ({@code ==}).
     *
     * @param other the node to compare with
     * @return {@code true} if both trees are structurally identical
     */
    default boolean deepEquals(JsonNode other) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        return this.toString().equals(other.toString());
    }

    /**
     * Returns the size of this node:
     * <ul>
     * <li>{@link JsonObject} â†’ number of fields</li>
     * <li>{@link JsonArray} â†’ number of elements</li>
     * <li>{@link JsonValue} â†’ always 1</li>
     * </ul>
     */
    default int size() {
        if (this instanceof JsonObject obj) return obj.size();
        if (this instanceof JsonArray arr) return arr.size();
        return 1;
    }

    /**
     * Returns a deep copy of this node and all its children.
     */
    JsonNode deepCopy();

    /**
     * Returns the canonical JSON string representation of this node.
     * Objects: {@code {"key":"value",...}}, Arrays: {@code [...]}, Values: quoted
     * or raw.
     */
    String toString();
}
