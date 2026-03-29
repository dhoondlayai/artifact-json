package io.dhoondlay.artifact.json.exception;

/**
 * <h2>JsonPathException — Invalid Path Expression</h2>
 *
 * <p>Thrown when a path expression like {@code "store.books[0].title"}
 * is malformed or references a node that is not traversable.</p>
 *
 * @author artifact-json
 * @version 2.0
 */
public class JsonPathException extends JsonException {

    private final String path;

    public JsonPathException(String path, String reason) {
        super(String.format("Invalid path '%s': %s", path, reason));
        this.path = path;
    }

    /** The path expression that caused the failure. */
    public String getPath() { return path; }
}
