package io.github.dhoondlayai.artifact.json.model;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <h2>JsonArray â€” High-Performance JSON Array Node</h2>
 *
 * <p>
 * An ordered, mutable JSON array backed by an {@link ArrayList}. Offers full
 * Java Stream API support including parallel processing for large datasets.
 * </p>
 *
 * <h3>Enterprise Performance Advantages:</h3>
 * <ul>
 * <li>Sealed type hierarchy â€” JVM can devirtualize method calls</li>
 * <li>No ObjectMapper or schema needed â€” direct tree access</li>
 * <li>{@code parallelStream()} for concurrent batch processing</li>
 * <li>In-place sort/reverse without copy</li>
 * <li>{@code groupBy()} returns a structured map</li>
 * </ul>
 *
 * <h3>Examples:</h3>
 * 
 * <pre>{@code
 * JsonArray prices = new JsonArray()
 *     .add(new JsonValue(10.5))
 *     .add(new JsonValue(25.0))
 *     .add(new JsonValue(7.99));
 *
 * // Stream API
 * double total = prices.stream()
 *     .mapToDouble(JsonNode::asDouble)
 *     .sum();
 *
 * // Sorting
 * prices.sort(Comparator.comparingDouble(JsonNode::asDouble));
 *
 * // Group objects by a field
 * JsonArray users = ...;
 * Map<String, JsonArray> byRole = users.groupBy("role");
 *
 * // Subarray (like SQL OFFSET + LIMIT)
 * JsonArray page = prices.subArray(0, 2);
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public final class JsonArray implements JsonNode, Iterable<JsonNode> {

    private final List<JsonNode> data;

    //
    // Constructors
    //

    /** Creates an empty JSON array. */
    public JsonArray() {
        this.data = new ArrayList<>();
    }

    /** Creates a JSON array with a pre-allocated internal capacity. */
    public JsonArray(int initialCapacity) {
        this.data = new ArrayList<>(initialCapacity);
    }

    /** Wraps an existing list as the internal backing â€” no copy. */
    public JsonArray(List<JsonNode> source) {
        this.data = new ArrayList<>(source);
    }

    //
    // Write Operations â€” all return `this` for chaining
    //

    /**
     * Appends a {@link JsonNode} to the end of this array.
     *
     * @param node the node to append
     * @return {@code this} for chaining
     */
    public JsonArray add(JsonNode node) {
        data.add(node);
        return this;
    }

    /** Convenience overload â€” wraps a {@link String}. */
    public JsonArray add(String value) {
        return add(new JsonValue(value));
    }

    /** Convenience overload â€” wraps a {@link Number}. */
    public JsonArray add(Number value) {
        return add(new JsonValue(value));
    }

    /** Convenience overload â€” wraps a {@code boolean}. */
    public JsonArray add(boolean value) {
        return add(new JsonValue(value));
    }

    /** Appends a JSON null. */
    public JsonArray addNull() {
        return add(new JsonValue(null));
    }

    /**
     * Inserts a node at the specified index, shifting existing elements right.
     *
     * @param index zero-based insertion position
     * @param node  the node to insert
     * @return {@code this}
     */
    public JsonArray addAt(int index, JsonNode node) {
        data.add(index, node);
        return this;
    }

    /**
     * Replaces the element at the given index.
     *
     * @param index zero-based position
     * @param node  replacement node
     * @return {@code this}
     */
    public JsonArray set(int index, JsonNode node) {
        data.set(index, node);
        return this;
    }

    /**
     * Removes the element at the given index.
     *
     * @param index zero-based position
     * @return the removed {@link JsonNode}
     */
    public JsonNode remove(int index) {
        return data.remove(index);
    }

    /**
     * Removes all elements equal to the given node.
     *
     * @param node the node to remove
     * @return {@code true} if any were removed
     */
    public boolean remove(JsonNode node) {
        return data.remove(node);
    }

    /** Removes all elements from this array. Returns {@code this}. */
    public JsonArray clear() {
        data.clear();
        return this;
    }

    /**
     * Appends all elements from another {@link JsonArray}.
     *
     * @param other the array to append from
     * @return {@code this}
     */
    public JsonArray addAll(JsonArray other) {
        data.addAll(other.data);
        return this;
    }

    //
    // Read Operations
    //

    /**
     * Returns the element at the given index.
     *
     * @param index zero-based position
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    public JsonNode element(int index) {
        return data.get(index);
    }

    /**
     * Returns the element at index, or {@code defaultValue} if out of range.
     *
     * @param index        position
     * @param defaultValue fallback
     */
    public JsonNode elementOrDefault(int index, JsonNode defaultValue) {
        if (index < 0 || index >= data.size())
            return defaultValue;
        return data.get(index);
    }

    /** Returns the number of elements. */
    @Override
    public int size() {
        return data.size();
    }

    /** Returns {@code true} if this array has no elements. */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Returns {@code true} if the given node is present (uses deep structural
     * equals).
     *
     * @param node the node to search for
     */
    public boolean contains(JsonNode node) {
        return data.contains(node);
    }

    /**
     * Returns the first index of the given node, or {@code -1} if not found.
     *
     * @param node the node to look up
     */
    public int indexOf(JsonNode node) {
        return data.indexOf(node);
    }

    /**
     * Returns the internal list as an unmodifiable view.
     */
    public List<JsonNode> elements() {
        return Collections.unmodifiableList(data);
    }

    /**
     * Returns a new {@link JsonArray} with elements from {@code fromIndex}
     * (inclusive)
     * to {@code toIndex} (exclusive).
     *
     * @param fromIndex start, inclusive
     * @param toIndex   end, exclusive
     */
    public JsonArray subArray(int fromIndex, int toIndex) {
        return new JsonArray(data.subList(fromIndex, toIndex));
    }

    /**
     * Returns a page of elements â€” useful for pagination.
     *
     * @param page     zero-based page number
     * @param pageSize elements per page
     */
    public JsonArray page(int page, int pageSize) {
        int from = page * pageSize;
        int to = Math.min(from + pageSize, data.size());
        if (from >= data.size())
            return new JsonArray();
        return new JsonArray(data.subList(from, to));
    }

    /**
     * Returns the first element of this array, or empty if the array is empty.
     *
     * @return {@link Optional} of the first element
     */
    public Optional<JsonNode> first() {
        return data.isEmpty() ? Optional.empty() : Optional.of(data.get(0));
    }

    /**
     * Returns the last element of this array, or empty if the array is empty.
     *
     * @return {@link Optional} of the last element
     */
    public Optional<JsonNode> last() {
        return data.isEmpty() ? Optional.empty() : Optional.of(data.get(data.size() - 1));
    }

    //
    // Stream API
    //

    /** Returns a sequential {@link Stream} of elements. */
    public Stream<JsonNode> stream() {
        return data.stream();
    }

    /**
     * Returns a parallel {@link Stream} of elements.
     * Use for large arrays where each element can be processed independently.
     */
    public Stream<JsonNode> parallelStream() {
        return data.parallelStream();
    }

    /** Iterates all elements. */
    @Override
    public Iterator<JsonNode> iterator() {
        return data.iterator();
    }

    //
    // Transformation
    //

    /**
     * Maps every element using the given function, producing a new
     * {@link JsonArray}.
     *
     * @param mapper transformation function
     */
    public JsonArray map(Function<JsonNode, JsonNode> mapper) {
        JsonArray result = new JsonArray(data.size());
        data.forEach(n -> result.add(mapper.apply(n)));
        return result;
    }

    /**
     * Filters elements matching the given predicate, returning a new
     * {@link JsonArray}.
     *
     * @param predicate filter condition
     */
    public JsonArray filter(Predicate<JsonNode> predicate) {
        JsonArray result = new JsonArray();
        data.stream().filter(predicate).forEach(result::add);
        return result;
    }

    /**
     * Returns a new {@link JsonArray} with duplicate elements removed.
     * Equality is based on {@link JsonNode#toString()}.
     */
    public JsonArray distinct() {
        Set<String> seen = new LinkedHashSet<>();
        JsonArray result = new JsonArray();
        for (JsonNode n : data) {
            if (seen.add(n.toString()))
                result.add(n);
        }
        return result;
    }

    /**
     * Sorts this array in-place using the given comparator.
     *
     * @param comparator sort order
     * @return {@code this}
     */
    public JsonArray sort(Comparator<JsonNode> comparator) {
        data.sort(comparator);
        return this;
    }

    /**
     * Reverses the order of elements in-place.
     *
     * @return {@code this}
     */
    public JsonArray reverse() {
        Collections.reverse(data);
        return this;
    }

    /**
     * Groups the elements of this array (expected to be {@link JsonObject} nodes)
     * by the string value of the given field key.
     *
     * <h4>Example:</h4>
     * 
     * <pre>{@code
     * // users = [{name:"A", role:"admin"}, {name:"B", role:"user"}, ...]
     * Map<String, JsonArray> grouped = users.groupBy("role");
     * // grouped.get("admin") â†’ JsonArray of admins
     * }</pre>
     *
     * @param fieldKey the field to group by
     * @return a {@link Map} from group key â†’ {@link JsonArray} of matching
     *         elements
     */
    public Map<String, JsonArray> groupBy(String fieldKey) {
        Map<String, JsonArray> groups = new LinkedHashMap<>();
        for (JsonNode node : data) {
            if (node instanceof JsonObject obj) {
                String groupVal = Optional.ofNullable(obj.field(fieldKey))
                        .map(JsonNode::asText).orElse("__null__");
                groups.computeIfAbsent(groupVal, k -> new JsonArray()).add(node);
            }
        }
        return groups;
    }

    /**
     * Converts this array to a {@link List} of raw Java values.
     * Leaf {@link JsonValue} elements are unwrapped; nested nodes are kept as-is.
     *
     * @return list of raw values
     */
    public List<Object> toList() {
        return data.stream()
                .map(n -> (n instanceof JsonValue v) ? v.value() : n)
                .collect(Collectors.toList());
    }

    /**
     * Converts this array to a typed {@link List} by applying a mapper to each element.
     *
     * <pre>{@code
     * List<String> names = users.toList(n -> ((JsonObject) n).getString("name").orElse(""));
     * // ["Alice", "Bob", "Charlie"]
     * }</pre>
     *
     * @param mapper function from {@link JsonNode} to target type
     * @param <T>    the target element type
     * @return typed list
     */
    public <T> List<T> toList(Function<JsonNode, T> mapper) {
        return data.stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * Converts this array to a {@link Set}, removing duplicates by JSON string equality.
     * Useful when you loaded a JSON array that may have duplicates.
     *
     * <pre>{@code
     * JsonArray tags = FastJsonEngine.parse("[\"java\",\"json\",\"java\"]").asArray();
     * Set<Object> unique = tags.toSet();
     * // {"java", "json"}
     * }</pre>
     *
     * @return {@link LinkedHashSet} preserving original order, duplicates removed
     */
    public Set<Object> toSet() {
        return data.stream()
                .map(n -> (n instanceof JsonValue v) ? v.value() : n)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Converts this array to a typed {@link Set} using a mapper function.
     *
     * <pre>{@code
     * Set<String> roles = users.toSet(n -> ((JsonObject) n).getString("role").orElse(""));
     * // {"admin", "user", "moderator"}
     * }</pre>
     *
     * @param mapper function from {@link JsonNode} to target type
     * @param <T>    target element type
     * @return {@link LinkedHashSet} of mapped values
     */
    public <T> Set<T> toSet(Function<JsonNode, T> mapper) {
        return data.stream().map(mapper).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Extracts the string value of a given field from every {@link JsonObject} element
     * and returns them as a {@link List}&lt;String&gt;. Null or missing values are excluded.
     *
     * <pre>{@code
     * List<String> emails = users.toStringList("email");
     * // ["alice@example.com", "bob@example.com"]
     * }</pre>
     *
     * @param field the field name to pluck
     * @return list of non-null string values
     */
    public List<String> toStringList(String field) {
        return data.stream()
                .filter(n -> n instanceof JsonObject)
                .map(n -> ((JsonObject) n).getString(field).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Converts this array of {@link JsonObject} elements into a {@link Map},
     * using one field as the key and another as the value.
     *
     * <pre>{@code
     * // [{"id": "u1", "name": "Alice"}, {"id": "u2", "name": "Bob"}]
     * Map<String, String> idToName = users.toMap("id", "name");
     * // {"u1" -> "Alice", "u2" -> "Bob"}
     * }</pre>
     *
     * @param keyField   field whose string value becomes the map key
     * @param valueField field whose string value becomes the map value
     * @return {@link LinkedHashMap} of key → value strings
     */
    public Map<String, String> toMap(String keyField, String valueField) {
        Map<String, String> result = new LinkedHashMap<>();
        for (JsonNode n : data) {
            if (n instanceof JsonObject obj) {
                String k = obj.getString(keyField).orElse(null);
                String v = obj.getString(valueField).orElse(null);
                if (k != null)
                    result.put(k, v);
            }
        }
        return result;
    }

    /**
     * Converts this array of {@link JsonObject} elements into a
     * {@code Map<String, JsonObject>} indexed by the given key field.
     * Useful for O(1) lookup of objects by ID.
     *
     * <pre>{@code
     * Map<String, JsonObject> byId = products.toIndexMap("id");
     * JsonObject product = byId.get("prod-001");
     * }</pre>
     *
     * @param keyField field to use as the map key
     * @return {@link LinkedHashMap} of key → {@link JsonObject}
     */
    public Map<String, JsonObject> toIndexMap(String keyField) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (JsonNode n : data) {
            if (n instanceof JsonObject obj) {
                obj.getString(keyField).ifPresent(k -> result.put(k, obj));
            }
        }
        return result;
    }

    //
    // Aggregation
    //

    /**
     * Sums the numeric values of a given field across all object elements.
     *
     * @param field the numeric field name
     * @return sum as double
     */
    public double sum(String field) {
        return stream()
                .filter(n -> n instanceof JsonObject)
                .mapToDouble(n -> ((JsonObject) n).field(field) != null
                        ? ((JsonObject) n).field(field).asDouble()
                        : 0.0)
                .sum();
    }

    /**
     * Returns the average of a numeric field across all object elements.
     *
     * @param field the numeric field name
     */
    public OptionalDouble avg(String field) {
        return stream()
                .filter(n -> n instanceof JsonObject)
                .mapToDouble(n -> ((JsonObject) n).field(field) != null
                        ? ((JsonObject) n).field(field).asDouble()
                        : 0.0)
                .average();
    }

    /**
     * Returns the minimum of a numeric field across all object elements.
     *
     * @param field the numeric field name
     */
    public OptionalDouble min(String field) {
        return stream()
                .filter(n -> n instanceof JsonObject)
                .mapToDouble(n -> ((JsonObject) n).field(field) != null
                        ? ((JsonObject) n).field(field).asDouble()
                        : Double.MAX_VALUE)
                .min();
    }

    /**
     * Returns the maximum of a numeric field across all object elements.
     *
     * @param field the numeric field name
     */
    public OptionalDouble max(String field) {
        return stream()
                .filter(n -> n instanceof JsonObject)
                .mapToDouble(n -> ((JsonObject) n).field(field) != null
                        ? ((JsonObject) n).field(field).asDouble()
                        : Double.MIN_VALUE)
                .max();
    }

    //
    // Serialization
    //

    /** Returns compact JSON string. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(data.size() * 16 + 2);
        sb.append('[');
        for (int i = 0, size = data.size(); i < size; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(data.get(i));
        }
        return sb.append(']').toString();
    }

    /**
     * Returns a deep copy of this array and all its elements.
     */
    @Override
    public JsonArray deepCopy() {
        JsonArray copy = new JsonArray(data.size());
        data.forEach(n -> copy.add(n.deepCopy()));
        return copy;
    }

    /**
     * Splits this array into a list of smaller arrays of the given size.
     */
    public List<JsonArray> chunk(int size) {
        List<JsonArray> chunks = new ArrayList<>();
        for (int i = 0; i < data.size(); i += size) {
            JsonArray chunk = new JsonArray();
            int end = Math.min(i + size, data.size());
            for (int j = i; j < end; j++) {
                chunk.add(data.get(j));
            }
            chunks.add(chunk);
        }
        return chunks;
    }

    /** Returns a pretty-printed JSON string. */
    public String toPrettyString(int indent) {
        return toPrettyInternal(indent, 0);
    }

    String toPrettyInternal(int indent, int depth) {
        if (data.isEmpty())
            return "[]";
        String pad = " ".repeat(indent * (depth + 1));
        String closePad = " ".repeat(indent * depth);
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < data.size(); i++) {
            sb.append(pad);
            JsonNode n = data.get(i);
            if (n instanceof JsonObject o)
                sb.append(o.toPrettyInternal(indent, depth + 1));
            else if (n instanceof JsonArray a)
                sb.append(a.toPrettyInternal(indent, depth + 1));
            else
                sb.append(n);
            if (i < data.size() - 1)
                sb.append(',');
            sb.append('\n');
        }
        sb.append(closePad).append(']');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof JsonArray other))
            return false;
        return data.equals(other.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
