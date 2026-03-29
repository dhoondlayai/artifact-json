package io.github.dhoondlayai.artifact.json.extensions;

import io.github.dhoondlayai.artifact.json.model.*;

/**
 * RFC 6902 standard JSON Patch generator.
 * Produces standard patches instead of proprietary diffs so they can be 
 * interpreted by any other system or language.
 */
public class JsonPatchGenerator {

    /**
     * Calculates an RFC 6902 compliant JSON Patch between two nodes.
     * Example Patch Output: 
     * [ { "op": "replace", "path": "/baz", "value": "boo" }, 
     *   { "op": "add", "path": "/hello", "value": ["world"] } ]
     */
    public static JsonArray generatePatch(JsonNode source, JsonNode target) {
        JsonArray patches = new JsonArray();
        compareRecursive("", source, target, patches);
        return patches;
    }

    private static void compareRecursive(String path, JsonNode source, JsonNode target, JsonArray patches) {
        if (source.equals(target)) return;

        if (source instanceof JsonObject sObj && target instanceof JsonObject tObj) {
            // Check for added or modified fields
            tObj.fields().forEach((k, v) -> {
                String childPath = path + "/" + escapePath(k);
                JsonNode sVal = sObj.field(k);
                if (sVal == null) {
                    patches.add(createOperation("add", childPath, v));
                } else if (!sVal.equals(v)) {
                    compareRecursive(childPath, sVal, v, patches);
                }
            });

            // Check for removed fields
            sObj.fields().forEach((k, v) -> {
                if (tObj.field(k) == null) {
                    String childPath = path + "/" + escapePath(k);
                    patches.add(createOperation("remove", childPath, null));
                }
            });
        } else if (source instanceof JsonArray && target instanceof JsonArray) {
            // Simplified array patching: replace the whole array if different for high-speed operation
            patches.add(createOperation("replace", path.isEmpty() ? "/" : path, target));
        } else {
            // Primitive change or type change
            patches.add(createOperation("replace", path.isEmpty() ? "/" : path, target));
        }
    }

    private static JsonObject createOperation(String op, String path, JsonNode value) {
        JsonObject operation = new JsonObject();
        operation.put("op", new JsonValue(op));
        operation.put("path", new JsonValue(path));
        if (value != null) {
            operation.put("value", value);
        }
        return operation;
    }

    private static String escapePath(String path) {
        return path.replace("~", "~0").replace("/", "~1");
    }
}
