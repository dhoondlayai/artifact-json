package io.dhoondlay.artifact.json.model;

/**
 * <h2>JsonMerger — Deep Merge Utility</h2>
 *
 * <p>Merges two {@link JsonNode} trees with configurable conflict resolution.
 * All merges produce a <b>new</b> tree — original inputs are never mutated.</p>
 *
 * <h3>Merge rules:</h3>
 * <ul>
 *   <li>{@link JsonObject} + {@link JsonObject} → keys merged recursively; right-side wins conflicts</li>
 *   <li>{@link JsonArray} + {@link JsonArray} → arrays concatenated (all left elements, then all right)</li>
 *   <li>Any other combination → right-side value replaces left</li>
 * </ul>
 *
 * <h3>Examples:</h3>
 * <pre>{@code
 * JsonObject base    = FastJsonEngine.parse("{\"theme\":\"dark\",\"lang\":\"en\"}").asObject();
 * JsonObject overlay = FastJsonEngine.parse("{\"lang\":\"fr\",\"timeout\":5000}").asObject();
 *
 * JsonNode merged = JsonMerger.deepMerge(base, overlay);
 * // {"theme":"dark","lang":"fr","timeout":5000}
 *
 * // Arrays are concatenated
 * JsonArray a = new JsonArray().add("x");
 * JsonArray b = new JsonArray().add("y");
 * JsonNode both = JsonMerger.deepMerge(a, b);
 * // ["x","y"]
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public final class JsonMerger {

    private JsonMerger() {}

    /**
     * Performs a deep merge of {@code target} and {@code update}.
     * Fields in {@code update} take precedence over fields in {@code target}.
     *
     * @param target the base node
     * @param update the override node
     * @return a new merged {@link JsonNode}
     */
    public static JsonNode deepMerge(JsonNode target, JsonNode update) {
        if (target == null) return update;
        if (update == null) return target;

        if (target instanceof JsonObject tObj && update instanceof JsonObject uObj) {
            JsonObject merged = new JsonObject(tObj.size() + uObj.size());
            // Copy all from target
            tObj.fields().forEach(merged::put);
            // Merge/override from update
            uObj.fields().forEach((k, v) -> {
                JsonNode existing = merged.field(k);
                merged.put(k, (existing != null) ? deepMerge(existing, v) : v);
            });
            return merged;
        }

        if (target instanceof JsonArray tArr && update instanceof JsonArray uArr) {
            JsonArray merged = new JsonArray(tArr.size() + uArr.size());
            tArr.elements().forEach(merged::add);
            uArr.elements().forEach(merged::add);
            return merged;
        }

        // Scalar conflict → update wins
        return update;
    }

    /**
     * Merges {@code update} into {@code target} but keeps the left value on conflicts
     * (non-overwriting merge).
     *
     * @param target the base node (wins on conflict)
     * @param update the supplier node (only adds new keys)
     * @return merged node
     */
    public static JsonNode mergeKeepLeft(JsonNode target, JsonNode update) {
        if (target == null) return update;
        if (update == null) return target;

        if (target instanceof JsonObject tObj && update instanceof JsonObject uObj) {
            JsonObject merged = new JsonObject(tObj.size() + uObj.size());
            tObj.fields().forEach(merged::put);
            uObj.fields().forEach((k, v) -> {
                if (!merged.contains(k)) merged.put(k, v);
            });
            return merged;
        }
        // left wins for all other types
        return target;
    }
}
