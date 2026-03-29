package io.github.dhoondlayai.artifact.json.query;

import io.github.dhoondlayai.artifact.json.exception.JsonQueryException;
import io.github.dhoondlayai.artifact.json.model.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.*;

/**
 * <h2>JsonQuery â€” In-Memory SQL Engine for JSON Arrays</h2>
 *
 * <p>
 * Perform full SQL-like query operations natively on {@link JsonArray} trees
 * without casting, deserializing, or using any external query language.
 * All operations work on the live {@link JsonNode} tree, so no copies are made
 * until {@link #execute()} is called.
 * </p>
 *
 * <h3>Supported operations:</h3>
 * <table border="1">
 * <tr>
 * <th>SQL Equivalent</th>
 * <th>JsonQuery Method</th>
 * </tr>
 * <tr>
 * <td>SELECT col1, col2</td>
 * <td>{@link #select(String...)}</td>
 * </tr>
 * <tr>
 * <td>SELECT col AS alias</td>
 * <td>{@link #selectAs(String, String)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col = val</td>
 * <td>{@link #whereEq(String, Object)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col != val</td>
 * <td>{@link #whereNotEq(String, Object)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col > val</td>
 * <td>{@link #whereGt(String, double)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col >= val</td>
 * <td>{@link #whereGte(String, double)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col &lt; val</td>
 * <td>{@link #whereLt(String, double)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col &lt;= val</td>
 * <td>{@link #whereLte(String, double)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col LIKE '%x%'</td>
 * <td>{@link #whereContains(String, String)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col LIKE 'x%'</td>
 * <td>{@link #whereStartsWith(String, String)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col LIKE '%x'</td>
 * <td>{@link #whereEndsWith(String, String)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col REGEXP</td>
 * <td>{@link #whereMatches(String, String)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col IN (...)</td>
 * <td>{@link #whereIn(String, Object...)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col IS NOT NULL</td>
 * <td>{@link #whereNotNull(String)}</td>
 * </tr>
 * <tr>
 * <td>WHERE col IS NULL</td>
 * <td>{@link #whereNull(String)}</td>
 * </tr>
 * <tr>
 * <td>ORDER BY col</td>
 * <td>{@link #orderBy(String, boolean)}</td>
 * </tr>
 * <tr>
 * <td>LIMIT n</td>
 * <td>{@link #limit(int)}</td>
 * </tr>
 * <tr>
 * <td>OFFSET n</td>
 * <td>{@link #offset(int)}</td>
 * </tr>
 * <tr>
 * <td>PAGE n SIZE s</td>
 * <td>{@link #page(int, int)}</td>
 * </tr>
 * <tr>
 * <td>DISTINCT</td>
 * <td>{@link #distinct()}</td>
 * </tr>
 * <tr>
 * <td>GROUP BY col</td>
 * <td>{@link #groupBy(String)}</td>
 * </tr>
 * <tr>
 * <td>JOIN other ON key</td>
 * <td>{@link #join(JsonArray, String, String)}</td>
 * </tr>
 * <tr>
 * <td>COUNT / SUM / AVG / MIN / MAX</td>
 * <td>{@link #count()}, {@link #sum(String)}, {@link #avg(String)},
 * {@link #min(String)}, {@link #max(String)}</td>
 * </tr>
 * </table>
 *
 * <h3>Examples:</h3>
 * 
 * <pre>{@code
 * JsonArray books = ...;  // [{title:"A", price:15, category:"sci-fi"}, ...]
 *
 * // â”€â”€ Basic query â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * JsonArray result = JsonQuery.from(books)
 *     .select("title", "price")
 *     .whereGt("price", 10.0)
 *     .whereContains("title", "Java")
 *     .orderBy("price", false)
 *     .limit(5)
 *     .execute();
 *
 * // â”€â”€ Aggregation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * double totalRevenue = JsonQuery.from(orders)
 *     .whereEq("status", "PAID")
 *     .sum("amount");
 *
 * // â”€â”€ GroupBy â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * Map<String, JsonArray> byCategory = JsonQuery.from(books)
 *     .groupBy("category");
 *
 * // â”€â”€ Join â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * JsonArray enriched = JsonQuery.from(orders)
 *     .join(customers, "customerId", "id")
 *     .execute();
 *
 * // â”€â”€ Parallel execution for large datasets â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * JsonArray result = JsonQuery.from(millions).parallel()
 *     .whereGt("score", 90)
 *     .execute();
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public final class JsonQuery {

    private final JsonArray source;
    private final List<String> selectFields = new ArrayList<>();
    private final Map<String, String> aliases = new LinkedHashMap<>();
    private Predicate<JsonObject> whereClause = obj -> true;
    private Comparator<JsonObject> comparator = null;
    private int limitSize = -1;
    private int offsetSize = 0;
    private boolean distinctMode = false;
    private boolean parallelMode = false;

    // Right-side array for JOIN
    private JsonArray joinArray = null;
    private String joinLeftKey = null;
    private String joinRightKey = null;

    private JsonQuery(JsonArray source) {
        this.source = Objects.requireNonNull(source, "source JsonArray must not be null");
    }

    //
    // Entry Point
    //

    /**
     * Creates a new query over the given {@link JsonArray}.
     *
     * @param array the source array (typically containing {@link JsonObject}
     *              elements)
     */
    public static JsonQuery from(JsonArray array) {
        return new JsonQuery(array);
    }

    //
    // SELECT
    //

    /**
     * Projects results to the specified fields only (SQL
     * {@code SELECT col1, col2}).
     * If not called, all fields are returned.
     *
     * @param fields field names to include in results
     */
    public JsonQuery select(String... fields) {
        Collections.addAll(selectFields, fields);
        return this;
    }

    /**
     * Projects a field with an alias in the output (SQL
     * {@code SELECT col AS alias}).
     *
     * @param field the original field name
     * @param alias the name in the output
     */
    public JsonQuery selectAs(String field, String alias) {
        selectFields.add(field);
        aliases.put(field, alias);
        return this;
    }

    //
    // WHERE Predicates
    //

    /** Adds a custom predicate. All WHERE clauses are AND-ed together. */
    public JsonQuery where(Predicate<JsonObject> predicate) {
        this.whereClause = this.whereClause.and(predicate);
        return this;
    }

    /** WHERE field = value (exact match). */
    public JsonQuery whereEq(String field, Object value) {
        return where(obj -> {
            JsonNode f = obj.field(field);
            return f instanceof JsonValue v && Objects.equals(v.value(), value);
        });
    }

    /** WHERE field != value. */
    public JsonQuery whereNotEq(String field, Object value) {
        return where(obj -> {
            JsonNode f = obj.field(field);
            return !(f instanceof JsonValue v && Objects.equals(v.value(), value));
        });
    }

    /** WHERE field > value (numeric). */
    public JsonQuery whereGt(String field, double value) {
        return where(obj -> numericOf(obj, field) > value);
    }

    /** WHERE field >= value (numeric). */
    public JsonQuery whereGte(String field, double value) {
        return where(obj -> numericOf(obj, field) >= value);
    }

    /** WHERE field < value (numeric). */
    public JsonQuery whereLt(String field, double value) {
        return where(obj -> numericOf(obj, field) < value);
    }

    /** WHERE field <= value (numeric). */
    public JsonQuery whereLte(String field, double value) {
        return where(obj -> numericOf(obj, field) <= value);
    }

    /** WHERE field LIKE '%text%' â€” case-sensitive substring match. */
    public JsonQuery whereContains(String field, String text) {
        return where(obj -> textOf(obj, field).contains(text));
    }

    /** WHERE field LIKE 'prefix%' â€” case-sensitive prefix match. */
    public JsonQuery whereStartsWith(String field, String prefix) {
        return where(obj -> textOf(obj, field).startsWith(prefix));
    }

    /** WHERE field LIKE '%suffix' â€” case-sensitive suffix match. */
    public JsonQuery whereEndsWith(String field, String suffix) {
        return where(obj -> textOf(obj, field).endsWith(suffix));
    }

    /** WHERE field REGEXP regex â€” Java regex match. */
    public JsonQuery whereMatches(String field, String regex) {
        return where(obj -> textOf(obj, field).matches(regex));
    }

    /** WHERE field IN (values...) â€” membership test. */
    public JsonQuery whereIn(String field, Object... values) {
        Set<Object> valueSet = new HashSet<>(Arrays.asList(values));
        return where(obj -> {
            JsonNode f = obj.field(field);
            return f instanceof JsonValue v && valueSet.contains(v.value());
        });
    }

    /** WHERE field IS NOT NULL â€” field must be present and non-null. */
    public JsonQuery whereNotNull(String field) {
        return where(obj -> {
            JsonNode f = obj.field(field);
            return f != null && !f.isNull();
        });
    }

    /** WHERE field IS NULL â€” field absent or explicitly null. */
    public JsonQuery whereNull(String field) {
        return where(obj -> {
            JsonNode f = obj.field(field);
            return f == null || f.isNull();
        });
    }

    /** WHERE field value is between low and high (inclusive). */
    public JsonQuery whereBetween(String field, double low, double high) {
        return where(obj -> {
            double v = numericOf(obj, field);
            return v >= low && v <= high;
        });
    }

    //
    // ORDER BY / LIMIT / OFFSET / PAGE / DISTINCT
    //

    /**
     * Sorts results by the given field.
     *
     * @param field      field name to sort by
     * @param descending {@code true} for descending order
     */
    public JsonQuery orderBy(String field, boolean descending) {
        this.comparator = (o1, o2) -> {
            JsonNode v1 = o1.field(field);
            JsonNode v2 = o2.field(field);
            if (v1 instanceof JsonValue val1 && v2 instanceof JsonValue val2) {
                if (val1.value() instanceof Number n1 && val2.value() instanceof Number n2) {
                    int cmp = Double.compare(n1.doubleValue(), n2.doubleValue());
                    return descending ? -cmp : cmp;
                }
                if (val1.value() instanceof Comparable<?> c1 && val2.value() instanceof Comparable<?> c2) {
                    @SuppressWarnings("unchecked")
                    int cmp = ((Comparable<Object>) c1).compareTo(c2);
                    return descending ? -cmp : cmp;
                }
            }
            return 0;
        };
        return this;
    }

    /** Adds a secondary sort key (thenComparing). */
    public JsonQuery thenBy(String field, boolean descending) {
        if (this.comparator == null)
            return orderBy(field, descending);
        Comparator<JsonObject> secondary = (o1, o2) -> {
            JsonNode v1 = o1.field(field), v2 = o2.field(field);
            if (v1 instanceof JsonValue val1 && v2 instanceof JsonValue val2
                    && val1.value() instanceof Number n1 && val2.value() instanceof Number n2) {
                int cmp = Double.compare(n1.doubleValue(), n2.doubleValue());
                return descending ? -cmp : cmp;
            }
            return 0;
        };
        this.comparator = this.comparator.thenComparing(secondary);
        return this;
    }

    /** Limits the number of results (SQL {@code LIMIT n}). */
    public JsonQuery limit(int n) {
        this.limitSize = n;
        return this;
    }

    /** Skips the first n results (SQL {@code OFFSET n}). */
    public JsonQuery offset(int n) {
        this.offsetSize = n;
        return this;
    }

    /** Applies pagination â€” equivalent to {@code LIMIT size OFFSET page*size}. */
    public JsonQuery page(int page, int size) {
        this.offsetSize = page * size;
        this.limitSize = size;
        return this;
    }

    /** Removes duplicate results (compared by JSON string). */
    public JsonQuery distinct() {
        this.distinctMode = true;
        return this;
    }

    /** Uses {@code parallelStream()} for execution â€” ideal for large arrays. */
    public JsonQuery parallel() {
        this.parallelMode = true;
        return this;
    }

    //
    // JOIN
    //

    /**
     * Performs an inner join with another {@link JsonArray}.
     * Each left element is merged with the matching right element.
     *
     * <h4>Example:</h4>
     * 
     * <pre>{@code
     * JsonArray enriched = JsonQuery.from(orders)
     *         .join(customers, "customerId", "id")
     *         .execute();
     * // Each order now contains all customer fields merged in
     * }</pre>
     *
     * @param right    the right-side array
     * @param leftKey  field name in left array to join on
     * @param rightKey field name in right array to match
     */
    public JsonQuery join(JsonArray right, String leftKey, String rightKey) {
        this.joinArray = right;
        this.joinLeftKey = leftKey;
        this.joinRightKey = rightKey;
        return this;
    }

    //
    // EXECUTE
    //

    /**
     * Executes the query and returns the result as a {@link JsonArray}.
     *
     * @return result {@link JsonArray}
     * @throws JsonQueryException if execution fails
     */
    public JsonArray execute() {
        try {
            Stream<JsonObject> stream = (parallelMode
                    ? source.parallelStream()
                    : source.stream())
                    .filter(n -> n instanceof JsonObject)
                    .map(n -> (JsonObject) n)
                    .filter(whereClause);

            if (comparator != null)
                stream = stream.sorted(comparator);
            if (offsetSize > 0)
                stream = stream.skip(offsetSize);
            if (limitSize > 0)
                stream = stream.limit(limitSize);

            // Build index for JOIN if needed
            Map<String, JsonObject> rightIndex = buildJoinIndex();

            // Collect using sequential collector (parallel used above, collect
            // sequentially)
            List<JsonObject> rows = stream.collect(Collectors.toList());

            // Apply distinct
            if (distinctMode) {
                Set<String> seen = new LinkedHashSet<>();
                rows = rows.stream()
                        .filter(o -> seen.add(o.toString()))
                        .collect(Collectors.toList());
            }

            JsonArray result = new JsonArray(rows.size());
            for (JsonObject obj : rows) {
                JsonObject projected = project(obj, rightIndex);
                result.add(projected);
            }
            return result;

        } catch (JsonQueryException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonQueryException("Query execution failed: " + e.getMessage(), e);
        }
    }

    private JsonObject project(JsonObject obj, Map<String, JsonObject> rightIndex) {
        // Apply JOIN
        JsonObject merged = obj;
        if (rightIndex != null) {
            JsonNode leftKeyNode = obj.field(joinLeftKey);
            if (leftKeyNode != null) {
                JsonObject right = rightIndex.get(leftKeyNode.asText());
                if (right != null)
                    merged = obj.merge(right);
            }
        }

        // Apply SELECT projection
        if (selectFields.isEmpty())
            return merged;
        JsonObject result = new JsonObject(selectFields.size());
        for (String f : selectFields) {
            JsonNode val = merged.field(f);
            if (val != null) {
                String outKey = aliases.getOrDefault(f, f);
                result.put(outKey, val);
            }
        }
        return result;
    }

    private Map<String, JsonObject> buildJoinIndex() {
        if (joinArray == null)
            return null;
        Map<String, JsonObject> index = new HashMap<>();
        joinArray.stream()
                .filter(n -> n instanceof JsonObject)
                .map(n -> (JsonObject) n)
                .forEach(obj -> {
                    JsonNode keyNode = obj.field(joinRightKey);
                    if (keyNode != null)
                        index.put(keyNode.asText(), obj);
                });
        return index;
    }

    //
    // AGGREGATION (terminal operations â€” do not need execute())
    //

    /**
     * Counts the elements matching the WHERE clause.
     *
     * @return count
     */
    public long count() {
        return filteredStream().count();
    }

    /**
     * Sums a numeric field across all matching elements.
     *
     * @param field the numeric field name
     * @return sum as double
     */
    public double sum(String field) {
        return filteredStream().mapToDouble(obj -> numericOf(obj, field)).sum();
    }

    /**
     * Computes the average of a numeric field.
     *
     * @param field the numeric field name
     */
    public OptionalDouble avg(String field) {
        return filteredStream().mapToDouble(obj -> numericOf(obj, field)).average();
    }

    /**
     * Returns the minimum value of a numeric field.
     *
     * @param field the numeric field name
     */
    public OptionalDouble min(String field) {
        return filteredStream().mapToDouble(obj -> numericOf(obj, field)).min();
    }

    /**
     * Returns the maximum value of a numeric field.
     *
     * @param field the numeric field name
     */
    public OptionalDouble max(String field) {
        return filteredStream().mapToDouble(obj -> numericOf(obj, field)).max();
    }

    /**
     * Groups matching elements by the string value of the given field.
     *
     * @param field field to group by
     * @return map from group value â†’ {@link JsonArray}
     */
    public Map<String, JsonArray> groupBy(String field) {
        Map<String, JsonArray> groups = new LinkedHashMap<>();
        filteredStream().forEach(obj -> {
            String key = Optional.ofNullable(obj.field(field))
                    .map(JsonNode::asText).orElse("__null__");
            groups.computeIfAbsent(key, k -> new JsonArray()).add(obj);
        });
        return groups;
    }

    /**
     * Returns the first matching element, or empty.
     *
     * @return {@link Optional} of the first matching {@link JsonObject}
     */
    public Optional<JsonObject> findFirst() {
        return filteredStream().findFirst();
    }

    //
    // Internals
    //

    private Stream<JsonObject> filteredStream() {
        return source.stream()
                .filter(n -> n instanceof JsonObject)
                .map(n -> (JsonObject) n)
                .filter(whereClause);
    }

    private static double numericOf(JsonObject obj, String field) {
        JsonNode n = obj.field(field);
        return (n instanceof JsonValue v && v.value() instanceof Number num)
                ? num.doubleValue()
                : 0.0;
    }

    private static String textOf(JsonObject obj, String field) {
        JsonNode n = obj.field(field);
        return n == null ? "" : n.asText();
    }
}
