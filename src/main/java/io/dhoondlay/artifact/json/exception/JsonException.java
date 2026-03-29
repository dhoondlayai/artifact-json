package io.dhoondlay.artifact.json.exception;

/**
 * <h2>JsonException — Base Exception</h2>
 *
 * <p>Root of the artifact-json exception hierarchy.
 * All library-specific exceptions extend this class, so callers can catch
 * everything with a single {@code catch (JsonException e)} block.</p>
 *
 * <h3>Exception Hierarchy:</h3>
 * <pre>
 *   JsonException
 *   ├── JsonParseException     — syntax errors during parsing (with line/column)
 *   ├── JsonMappingException   — POJO serialization / deserialization failures
 *   ├── JsonTypeException      — wrong type access (e.g. calling asInt() on object)
 *   ├── JsonPathException      — invalid path expressions
 *   ├── JsonQueryException     — query engine failures
 *   └── JsonConversionException — format conversion failures (CSV, XML, YAML, etc.)
 * </pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public class JsonException extends RuntimeException {

    /** Constructs with message only. */
    public JsonException(String message) {
        super(message);
    }

    /** Constructs with message and root cause. */
    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
