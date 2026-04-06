package io.github.dhoondlayai.artifact.json.model;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <h2>JsonObject â€” High-Performance JSON Object Node</h2>
 *
 * <p>
 * A mutable, ordered JSON object backed by a {@link LinkedHashMap} for O(1)
 * lookups
 * while preserving insertion order (important for deterministic serialization).
 * </p>
 *
 * <p>
 * All mutating methods return {@code this} for fluent chaining. All read
 * operations
 * are fully Stream-API compatible.
 * </p>
 *
 * <h3>Core Performance Advantages:</h3>
 * <ul>
 * <li>No schema pre-compilation required</li>
 * <li>Sealed type hierarchy enables JIT devirtualization</li>
 * <li>Parallel stream support for large objects</li>
 * <li>Zero reflection for read operations</li>
 * </ul>
 *
 * <h3>Examples:</h3>
 * 
 * <pre>{@code
 * JsonObject user = new JsonObject()
 *         .put("name", new JsonValue("Alice"))
 *         .put("age", new JsonValue(30))
 *         .put("active", new JsonValue(true));
 *
 * // Fluent access
 * String name = user.getString("name").orElse("unknown");
 * int age = user.getInt("age").orElse(0);
 *
 * // Stream API
 * user.stream()
 *         .filter(e -> e.getValue().isValue())
 *         .forEach(e -> System.out.println(e.getKey() + "=" + e.getValue().asText()));
 *
 * // Deep merge
 * JsonObject merged = user.merge(other);
 *
 * // Rename a key
 * user.rename("name", "fullName");
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public final class JsonObject implements JsonNode {

    private final Map<String, JsonNode> data;

    //
    // Constructors
    //

    /** Creates an empty JSON object. */
    public JsonObject() {
        this.data = new LinkedHashMap<>();
    }

    /** Creates a JSON object with pre-allocated capacity to avoid rehashing. */
    public JsonObject(int initialCapacity) {
        this.data = new LinkedHashMap<>(initialCapacity);
    }

    /** Wraps an existing map (defensive copy). */
    public JsonObject(Map<String, JsonNode> source) {
        this.data = new LinkedHashMap<>(source);
    }

    //
    // Write Operations (all return `this` for chaining)
    //

    /**
     * Puts a field into this object.
     *
     * @param key  the field name
     * @param node the value (any {@link JsonNode})
     * @return {@code this} for chaining
     */
    public JsonObject put(String key, JsonNode node) {
        data.put(Objects.requireNonNull(key, "key"), node);
        return this;
    }

    /** Convenience overload â€” wraps a raw {@link String}. */
    public JsonObject put(String key, String value) {
        return put(key, new JsonValue(value));
    }

    /** Convenience overload â€” wraps a raw {@link Number}. */
    public JsonObject put(String key, Number value) {
        return put(key, new JsonValue(value));
    }

    /** Convenience overload â€” wraps a raw {@link Boolean}. */
    public JsonObject put(String key, boolean value) {
        return put(key, new JsonValue(value));
    }

    /** Puts a null JSON value for the given key. */
    public JsonObject putNull(String key) {
        return put(key, new JsonValue(null));
    }

    /**
     * Puts the field only if the key is not already present.
     *
     * @return {@code this}
     */
    public JsonObject putIfAbsent(String key, JsonNode node) {
        data.putIfAbsent(key, node);
        return this;
    }

    /**
     * Computes and stores a value if the key is absent.
     *
     * @param key      the field name
     * @param supplier lambda that produces the value
     * @return the existing or newly computed {@link JsonNode}
     */
    public JsonNode computeIfAbsent(String key, Function<String, JsonNode> supplier) {
        return data.computeIfAbsent(key, supplier);
    }

    /**
     * Removes a field by key.
     *
     * @param key the field name to remove
     * @return the removed {@link JsonNode}, or {@code null} if not present
     */
    public JsonNode remove(String key) {
        return data.remove(key);
    }

    /**
     * Renames a key. No-op if the old key doesn't exist.
     *
     * @param oldKey existing key
     * @param newKey new key name
     * @return {@code this}
     */
    public JsonObject rename(String oldKey, String newKey) {
        JsonNode val = data.remove(oldKey);
        if (val != null)
            data.put(newKey, val);
        return this;
    }

    /**
     * Deep-merges another {@link JsonObject} into this one.
     * Fields in {@code other} override fields in {@code this}.
     * Nested objects are merged recursively. 
     * 
     * @param other the object to merge in
     * @return {@code this} for chaining
     */
    public JsonObject merge(JsonObject other) {
        if (other == null) return this;
        other.data.forEach((k, v) -> {
            JsonNode existing = data.get(k);
            if (v instanceof JsonObject otherObj && existing instanceof JsonObject thisObj) {
                thisObj.merge(otherObj);
            } else {
                data.put(k, v.deepCopy());
            }
        });
        return this;
    }

    /** Removes all fields. Returns {@code this}. */
    public JsonObject clear() {
        data.clear();
        return this;
    }

    //
    // Read Operations
    //

    /**
     * Returns the {@link JsonNode} for the given key, or {@code null}.
     *
     * @param key field name
     */
    public JsonNode field(String key) {
        return data.get(key);
    }

    /**
     * Returns the value for the given key, or {@code defaultValue} if absent.
     *
     * @param key          field name
     * @param defaultValue fallback
     */
    public JsonNode getOrDefault(String key, JsonNode defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }

    /** Returns {@code true} if this object contains the given key. */
    public boolean contains(String key) {
        return data.containsKey(key);
    }

    /** Alias for {@link #contains(String)} — familiar {@code Map}-style naming. */
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    /** Returns the number of fields in this object. */
    @Override
    public int size() {
        return data.size();
    }

    /** Returns {@code true} if this object has no fields. */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /** Returns an unmodifiable view of the key set. */
    public Set<String> keys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    /** Returns an unmodifiable view of the internal field map. */
    public Map<String, JsonNode> fields() {
        return Collections.unmodifiableMap(data);
    }

    //
    // Typed Getters (return Optional for safety)
    //

    /** Returns the value as {@link String} wrapped in an {@link Optional}. */
    public Optional<String> getString(String key) {
        JsonNode n = data.get(key);
        return (n instanceof JsonValue v && v.value() instanceof String s)
                ? Optional.of(s)
                : Optional.empty();
    }

    /** Returns the value as {@link Integer} wrapped in an {@link Optional}. */
    public Optional<Integer> getInt(String key) {
        JsonNode n = data.get(key);
        return (n instanceof JsonValue v && v.value() instanceof Number num)
                ? Optional.of(num.intValue())
                : Optional.empty();
    }

    /** Returns the value as {@link Long} wrapped in an {@link Optional}. */
    public Optional<Long> getLong(String key) {
        JsonNode n = data.get(key);
        return (n instanceof JsonValue v && v.value() instanceof Number num)
                ? Optional.of(num.longValue())
                : Optional.empty();
    }

    /** Returns the value as {@link Double} wrapped in an {@link Optional}. */
    public Optional<Double> getDouble(String key) {
        JsonNode n = data.get(key);
        return (n instanceof JsonValue v && v.value() instanceof Number num)
                ? Optional.of(num.doubleValue())
                : Optional.empty();
    }

    /** Returns the value as {@link Boolean} wrapped in an {@link Optional}. */
    public Optional<Boolean> getBoolean(String key) {
        JsonNode n = data.get(key);
        return (n instanceof JsonValue v && v.value() instanceof Boolean b)
                ? Optional.of(b)
                : Optional.empty();
    }

    /** Returns the child {@link JsonObject} for the key, or empty. */
    public Optional<JsonObject> getObject(String key) {
        JsonNode n = data.get(key);
        return (n instanceof JsonObject obj) ? Optional.of(obj) : Optional.empty();
    }

    /** Returns the child {@link JsonArray} for the key, or empty. */
    public Optional<JsonArray> getArray(String key) {
        JsonNode n = data.get(key);
        return (n instanceof JsonArray arr) ? Optional.of(arr) : Optional.empty();
    }

    //
    // Stream API
    //

    /** Returns a sequential {@link Stream} of entries (key-value pairs). */
    public Stream<Map.Entry<String, JsonNode>> stream() {
        return data.entrySet().stream();
    }

    /** Returns a parallel {@link Stream} of entries. Useful for large objects. */
    public Stream<Map.Entry<String, JsonNode>> parallelStream() {
        return data.entrySet().parallelStream();
    }

    /** Returns a stream of only the field keys. */
    public Stream<String> keyStream() {
        return data.keySet().stream();
    }

    /** Returns a stream of only the field values. */
    public Stream<JsonNode> valueStream() {
        return data.values().stream();
    }

    //
    // Transformation
    //

    /**
     * Returns a new {@link JsonObject} where all values have been transformed
     * by the given function.
     *
     * @param mapper the transformation function
     * @return new transformed {@link JsonObject}
     */
    public JsonObject mapValues(Function<JsonNode, JsonNode> mapper) {
        JsonObject result = new JsonObject(data.size());
        data.forEach((k, v) -> result.put(k, mapper.apply(v)));
        return result;
    }

    /**
     * Converts this object's fields to a {@link Map} of raw Java values.
     * Only works for leaf-level {@link JsonValue} fields.
     *
     * @return flat map of field names to their raw values
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>(data.size());
        data.forEach((k, v) -> {
            if (v instanceof JsonValue val)
                result.put(k, val.value());
        });
        return result;
    }

    /**
     * Performs an operation for each entry.
     *
     * @param action the action to apply
     */
    public void forEach(BiConsumer<String, JsonNode> action) {
        data.forEach(action);
    }

    //
    // Serialization
    //

    /**
     * Returns a compact JSON string. Uses a pre-sized {@link StringBuilder}
     * to minimize allocation.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(data.size() * 24 + 2);
        sb.append('{');
        boolean first = true;
        for (var entry : data.entrySet()) {
            if (!first)
                sb.append(',');
            sb.append('"').append(escapeString(entry.getKey())).append("\":");
            sb.append(entry.getValue());
            first = false;
        }
        return sb.append('}').toString();
    }

    /**
     * Returns a pretty-printed JSON string with the given indent.
     *
     * @param indent number of spaces per level
     */
    public String toPrettyString(int indent) {
        return toPrettyInternal(indent, 0);
    }

    String toPrettyInternal(int indent, int depth) {
        if (data.isEmpty())
            return "{}";
        String pad = " ".repeat(indent * (depth + 1));
        String closePad = " ".repeat(indent * depth);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        boolean first = true;
        for (var e : data.entrySet()) {
            if (!first)
                sb.append(",\n");
            sb.append(pad).append('"').append(escapeString(e.getKey())).append("\": ");
            if (e.getValue() instanceof JsonObject o)
                sb.append(o.toPrettyInternal(indent, depth + 1));
            else if (e.getValue() instanceof JsonArray a)
                sb.append(a.toPrettyInternal(indent, depth + 1));
            else
                sb.append(e.getValue());
            first = false;
        }
        sb.append('\n').append(closePad).append('}');
        return sb.toString();
    }

    /**
     * Returns a deep copy of this object.
     */
    @Override
    public JsonObject deepCopy() {
        JsonObject copy = new JsonObject(data.size());
        data.forEach((k, v) -> copy.put(k, v.deepCopy()));
        return copy;
    }

    /**
     * Removes all fields that do not match the given predicate.
     */
    public JsonObject filter(java.util.function.Predicate<Map.Entry<String, JsonNode>> predicate) {
        data.entrySet().removeIf(e -> !predicate.test(e));
        return this;
    }

    private static String escapeString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof JsonObject other))
            return false;
        return data.equals(other.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
