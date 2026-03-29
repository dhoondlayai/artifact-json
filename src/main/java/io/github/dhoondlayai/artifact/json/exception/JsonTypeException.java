package io.github.dhoondlayai.artifact.json.exception;

/**
 * <h2>JsonTypeException â€” Wrong Type Access</h2>
 *
 * <p>Thrown when a caller tries to access a {@link io.github.dhoondlayai.artifact.json.model.JsonNode}
 * as the wrong type (e.g., calling {@code asInt()} on an object node).</p>
 *
 * @author artifact-json
 * @version 2.0
 */
public class JsonTypeException extends JsonException {

    private final String expectedType;
    private final String actualType;

    public JsonTypeException(String expectedType, String actualType) {
        super(String.format("Type mismatch: expected %s but node is %s", expectedType, actualType));
        this.expectedType = expectedType;
        this.actualType   = actualType;
    }

    /** The type the caller expected (e.g., {@code "Number"}). */
    public String getExpectedType() { return expectedType; }

    /** The actual type of the node (e.g., {@code "JsonObject"}). */
    public String getActualType()   { return actualType; }
}
