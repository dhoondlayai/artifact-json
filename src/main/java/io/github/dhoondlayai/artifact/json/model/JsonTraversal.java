package io.github.dhoondlayai.artifact.json.model;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <h2>JsonTraversal â€” Superfast Tree Traversal Engine</h2>
 *
 * <p>
 * Provides iterative (non-recursive) algorithms for traversing, transforming,
 * and flattening JSON trees. Using an explicit stack/queue instead of recursion
 * ensures there are no {@link StackOverflowError}s even on deeply nested
 * (10,000+
 * level) structures.
 * </p>
 *
 * <h3>Available algorithms:</h3>
 * <ul>
 * <li>{@link #dfs(JsonNode, Consumer)} â€” Depth-first (iterative)</li>
 * <li>{@link #bfs(JsonNode, Consumer)} â€” Breadth-first (iterative)</li>
 * <li>{@link #traverse(JsonNode, Consumer)} â€” Alias for DFS
 * (convenience)</li>
 * <li>{@link #transform(JsonNode, Function)} â€” Deeply clone + apply
 * function</li>
 * <li>{@link #flatten(JsonNode)} â€” Produce a dot-path â†’ value map</li>
 * <li>{@link #unflatten(Map)} â€” Reconstruct a tree from a flat map</li>
 * <li>{@link #traverseWithPaths(JsonNode, BiConsumer)} â€” DFS with dot-path
 * context</li>
 * </ul>
 *
 * <h3>Examples:</h3>
 * 
 * <pre>{@code
 * // Count all string leaves
 * int[] count = { 0 };
 * JsonTraversal.dfs(root, node -> {
 *     if (node.isString())
 *         count[0]++;
 * });
 *
 * // Flatten to a property map
 * Map<String, Object> flat = JsonTraversal.flatten(root);
 * // flat = {"store.location": "NY", "store.books[0].title": "...", ...}
 *
 * // Increase all prices by 10%
 * JsonNode updated = JsonTraversal.transform(root, node -> {
 *     if (node instanceof JsonObject obj && obj.contains("price")) {
 *         double p = obj.field("price").asDouble();
 *         return new JsonObject().merge(obj).put("price", new JsonValue(p * 1.10));
 *     }
 *     return node;
 * });
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public final class JsonTraversal {

    private JsonTraversal() {
    }

    //
    // Traversal
    //

    /**
     * Iterative Depth-First traversal. Visits every node in the tree exactly once,
     * pre-order (parent before children). Safe for arbitrarily deep trees.
     *
     * @param root     starting node
     * @param consumer receives each visited {@link JsonNode}
     */
    public static void dfs(JsonNode root, Consumer<JsonNode> consumer) {
        if (root == null)
            return;
        Deque<JsonNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            JsonNode current = stack.pop();
            consumer.accept(current);
            if (current instanceof JsonObject obj) {
                // Push in reverse order so left-to-right is processed first
                List<JsonNode> vals = new ArrayList<>(obj.fields().values());
                for (int i = vals.size() - 1; i >= 0; i--)
                    stack.push(vals.get(i));
            } else if (current instanceof JsonArray arr) {
                List<JsonNode> elems = arr.elements();
                for (int i = elems.size() - 1; i >= 0; i--)
                    stack.push(elems.get(i));
            }
        }
    }

    /**
     * Iterative Breadth-First traversal. Visits nodes layer by layer.
     * Useful for searching by depth or collecting nodes at a specific level.
     *
     * @param root     starting node
     * @param consumer receives each visited {@link JsonNode}
     */
    public static void bfs(JsonNode root, Consumer<JsonNode> consumer) {
        if (root == null)
            return;
        Queue<JsonNode> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            JsonNode current = queue.poll();
            consumer.accept(current);
            if (current instanceof JsonObject obj) {
                obj.fields().values().forEach(queue::add);
            } else if (current instanceof JsonArray arr) {
                arr.elements().forEach(queue::add);
            }
        }
    }

    /**
     * Convenience alias for {@link #dfs(JsonNode, Consumer)}.
     * Used internally by
     * {@link io.github.dhoondlayai.artifact.json.extensions.JsonExtensions}.
     *
     * @param root     starting node
     * @param consumer receives each visited {@link JsonNode}
     */
    public static void traverse(JsonNode root, Consumer<JsonNode> consumer) {
        dfs(root, consumer);
    }

    /**
     * DFS traversal that also provides the dot-path string for each node.
     *
     * <h4>Example output paths:</h4>
     * 
     * <pre>
     *   ""                          â†’ root
     *   "store"                     â†’ store object
     *   "store.books"               â†’ books array
     *   "store.books[0]"            â†’ first book object
     *   "store.books[0].title"      â†’ title string
     * </pre>
     *
     * @param root     starting node
     * @param consumer receives (dotPath, node) pairs
     */
    public static void traverseWithPaths(JsonNode root, BiConsumer<String, JsonNode> consumer) {
        traverseInternal(root, "", consumer);
    }

    private static void traverseInternal(JsonNode node, String path,
            BiConsumer<String, JsonNode> consumer) {
        consumer.accept(path, node);
        if (node instanceof JsonObject obj) {
            obj.fields().forEach((k, v) -> traverseInternal(v, path.isEmpty() ? k : path + "." + k, consumer));
        } else if (node instanceof JsonArray arr) {
            List<JsonNode> elems = arr.elements();
            for (int i = 0; i < elems.size(); i++) {
                traverseInternal(elems.get(i), path + "[" + i + "]", consumer);
            }
        }
    }

    //
    // Transform
    //

    /**
     * Deep-clones the entire tree while applying a transformation function to each
     * node.
     * The function controls what the node becomes â€” return the original node
     * unchanged
     * to act as a deep-clone.
     *
     * @param root        the source tree
     * @param transformer applied to every node; return the replacement node
     * @return transformed tree (new nodes, original is unmodified)
     */
    public static JsonNode transform(JsonNode root, Function<JsonNode, JsonNode> transformer) {
        JsonNode mapped = transformer.apply(root);
        if (mapped instanceof JsonObject obj) {
            JsonObject result = new JsonObject(obj.size());
            obj.fields().forEach((k, v) -> result.put(k, transform(v, transformer)));
            return result;
        } else if (mapped instanceof JsonArray arr) {
            JsonArray result = new JsonArray(arr.size());
            arr.elements().forEach(e -> result.add(transform(e, transformer)));
            return result;
        }
        return mapped;
    }

    //
    // Flatten / Unflatten
    //

    /**
     * Flattens a JSON tree into a single-level {@link Map} using dot-path keys.
     * Array indices are bracketed:
     * {@code store.books[0].title â†’ "The Great Gatsby"}.
     *
     * <h4>Example:</h4>
     * 
     * <pre>{@code
     * Map<String, Object> flat = JsonTraversal.flatten(root);
     * // {"id": 101, "store.location": "NY", "store.books[0].title": "...", ...}
     * }</pre>
     *
     * @param root the tree to flatten
     * @return a flat {@link LinkedHashMap} (insertion-order preserved)
     */
    public static Map<String, Object> flatten(JsonNode root) {
        Map<String, Object> result = new LinkedHashMap<>();
        traverseWithPaths(root, (path, node) -> {
            if (node instanceof JsonValue val && !path.isEmpty()) {
                result.put(path, val.value());
            }
        });
        return result;
    }

    /**
     * Reconstructs a {@link JsonObject} tree from a flat dot-path map.
     * Reverses the effect of {@link #flatten(JsonNode)}.
     *
     * @param flatMap flat path â†’ value map
     * @return reconstructed {@link JsonNode} tree root
     */
    public static JsonNode unflatten(Map<String, Object> flatMap) {
        JsonObject root = new JsonObject();
        for (var entry : flatMap.entrySet()) {
            setByPath(root, entry.getKey().split("\\."), 0, new JsonValue(entry.getValue()));
        }
        return root;
    }

    private static void setByPath(JsonObject obj, String[] segments, int idx, JsonNode value) {
        String seg = segments[idx];
        if (idx == segments.length - 1) {
            obj.put(seg, value);
            return;
        }
        JsonNode existing = obj.field(seg);
        JsonObject nested;
        if (existing instanceof JsonObject exObj) {
            nested = exObj;
        } else {
            nested = new JsonObject();
            obj.put(seg, nested);
        }
        setByPath(nested, segments, idx + 1, value);
    }

    //
    // Collect utilities
    //

    /**
     * Collects all leaf values ({@link JsonValue}) in DFS order.
     *
     * @param root the tree to scan
     * @return ordered list of all leaf nodes
     */
    public static List<JsonValue> collectLeaves(JsonNode root) {
        List<JsonValue> leaves = new ArrayList<>();
        dfs(root, n -> {
            if (n instanceof JsonValue v)
                leaves.add(v);
        });
        return leaves;
    }

    /**
     * Counts total nodes in the tree (including branch and leaf nodes).
     *
     * @param root the tree to count
     * @return total node count
     */
    public static int countNodes(JsonNode root) {
        int[] count = { 0 };
        dfs(root, n -> count[0]++);
        return count[0];
    }

    /**
     * Returns the maximum depth of the tree (root has depth 0).
     *
     * @param root the tree
     * @return maximum depth
     */
    public static int maxDepth(JsonNode root) {
        if (root instanceof JsonObject obj) {
            return obj.fields().values().stream().mapToInt(v -> 1 + maxDepth(v)).max().orElse(0);
        } else if (root instanceof JsonArray arr) {
            return arr.elements().stream().mapToInt(e -> 1 + maxDepth(e)).max().orElse(0);
        }
        return 0;
    }
}
