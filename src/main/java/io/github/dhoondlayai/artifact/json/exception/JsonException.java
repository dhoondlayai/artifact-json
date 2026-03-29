package io.github.dhoondlayai.artifact.json.exception;

/**
 * <h2>JsonException â€” Base Exception</h2>
 *
 * <p>Root of the artifact-json exception hierarchy.
 * All library-specific exceptions extend this class, so callers can catch
 * everything with a single {@code catch (JsonException e)} block.</p>
 *
 * <h3>Exception Hierarchy:</h3>
 * <pre>
 *   JsonException
 *   â”œâ”€â”€ JsonParseException     â€” syntax errors during parsing (with line/column)
 *   â”œâ”€â”€ JsonMappingException   â€” POJO serialization / deserialization failures
 *   â”œâ”€â”€ JsonTypeException      â€” wrong type access (e.g. calling asInt() on object)
 *   â”œâ”€â”€ JsonPathException      â€” invalid path expressions
 *   â”œâ”€â”€ JsonQueryException     â€” query engine failures
 *   â””â”€â”€ JsonConversionException â€” format conversion failures (CSV, XML, YAML, etc.)
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
