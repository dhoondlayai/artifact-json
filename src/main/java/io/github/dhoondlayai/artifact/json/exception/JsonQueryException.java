package io.github.dhoondlayai.artifact.json.exception;

/**
 * <h2>JsonQueryException â€” Query Engine Failure</h2>
 *
 * <p>Thrown when the {@link io.github.dhoondlayai.artifact.json.query.JsonQuery} engine
 * encounters an invalid operation â€” e.g., aggregating over non-numeric fields,
 * invalid join key, or conflicting clauses.</p>
 *
 * @author artifact-json
 * @version 2.0
 */
public class JsonQueryException extends JsonException {

    public JsonQueryException(String message) {
        super(message);
    }

    public JsonQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
