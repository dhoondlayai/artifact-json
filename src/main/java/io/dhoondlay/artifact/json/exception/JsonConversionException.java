package io.dhoondlay.artifact.json.exception;

/**
 * <h2>JsonConversionException — Format Conversion Failure</h2>
 *
 * <p>Thrown when conversion between JSON and another format (CSV, XML, YAML,
 * Properties, HTML, Markdown) fails due to incompatible structure or invalid input.</p>
 *
 * @author artifact-json
 * @version 2.0
 */
public class JsonConversionException extends JsonException {

    private final String sourceFormat;
    private final String targetFormat;

    public JsonConversionException(String sourceFormat, String targetFormat, String reason) {
        super(String.format("Conversion from %s to %s failed: %s", sourceFormat, targetFormat, reason));
        this.sourceFormat = sourceFormat;
        this.targetFormat = targetFormat;
    }

    public JsonConversionException(String message, Throwable cause) {
        super(message, cause);
        this.sourceFormat = null;
        this.targetFormat = null;
    }

    /** The source format (e.g., {@code "JSON"}, {@code "CSV"}). */
    public String getSourceFormat() { return sourceFormat; }

    /** The target format (e.g., {@code "XML"}, {@code "YAML"}). */
    public String getTargetFormat() { return targetFormat; }
}
