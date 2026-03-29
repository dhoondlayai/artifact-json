package io.dhoondlay.artifact.json.exception;

/**
 * <h2>JsonQueryException — Query Engine Failure</h2>
 *
 * <p>Thrown when the {@link io.dhoondlay.artifact.json.query.JsonQuery} engine
 * encounters an invalid operation — e.g., aggregating over non-numeric fields,
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
