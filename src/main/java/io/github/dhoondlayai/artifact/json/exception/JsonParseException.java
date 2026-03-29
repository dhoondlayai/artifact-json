package io.github.dhoondlayai.artifact.json.exception;

/**
 * <h2>JsonParseException â€” Syntax Error During Parsing</h2>
 *
 * <p>Thrown when the JSON parser encounters invalid syntax.
 * Includes the line number and column number to pinpoint the error location.</p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * try {
 *     JsonNode node = FastJsonEngine.parse("{\"key\": }"); // syntax error
 * } catch (JsonParseException e) {
 *     System.err.println("Parse failed at line " + e.getLine() + ", col " + e.getCol());
 *     System.err.println(e.getMessage());
 * }
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public class JsonParseException extends JsonException {

    private final int line;
    private final int col;
    private final String snippet;

    public JsonParseException(String message, int line, int col) {
        super(String.format("[line %d, col %d] %s", line, col, message));
        this.line    = line;
        this.col     = col;
        this.snippet = null;
    }

    public JsonParseException(String message, int line, int col, String snippet) {
        super(String.format("[line %d, col %d] %s  near: %s", line, col, message, snippet));
        this.line    = line;
        this.col     = col;
        this.snippet = snippet;
    }

    public JsonParseException(String message) {
        super(message);
        this.line    = -1;
        this.col     = -1;
        this.snippet = null;
    }

    /** Line number where parsing failed (1-based), or {@code -1} if unknown. */
    public int getLine()    { return line; }

    /** Column number where parsing failed (1-based), or {@code -1} if unknown. */
    public int getCol()     { return col; }

    /** Short snippet of the problematic input, or {@code null} if not available. */
    public String getSnippet() { return snippet; }
}
