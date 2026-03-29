package io.dhoondlay.artifact.json.streaming;

import io.dhoondlay.artifact.json.model.*;

/**
 * Core Visitor Pattern for high-performance tree traversal.
 */
public interface JsonNodeVisitor<R> {
    R visitObject(JsonObject node);
    R visitArray(JsonArray node);
    R visitValue(JsonValue node);

    /**
     * Entry point using Java 21+ pattern matching.
     */
    default R visit(JsonNode node) {
        if (node instanceof JsonObject obj) return visitObject(obj);
        if (node instanceof JsonArray arr) return visitArray(arr);
        if (node instanceof JsonValue val) return visitValue(val);
        return null; // or throw exception
    }
}
