package io.dhoondlay.artifact.json.exception;

/**
 * <h2>JsonMappingException — POJO Serialization / Deserialization Failure</h2>
 *
 * <p>Thrown when {@code CustomObjectMapper} or {@code FastObjectMapper}
 * cannot map between a Java object and a {@link io.dhoondlay.artifact.json.model.JsonNode}.</p>
 *
 * <h3>Common causes:</h3>
 * <ul>
 *   <li>Missing required field annotated with {@code @JsonProperty(required = true)}</li>
 *   <li>Type mismatch during deserialization</li>
 *   <li>No-arg constructor missing on the target class</li>
 *   <li>Inaccessible private fields with no accessor</li>
 * </ul>
 *
 * @author artifact-json
 * @version 2.0
 */
public class JsonMappingException extends JsonException {

    private final String fieldName;
    private final Class<?> targetType;

    public JsonMappingException(String message) {
        super(message);
        this.fieldName  = null;
        this.targetType = null;
    }

    public JsonMappingException(String message, String fieldName, Class<?> targetType) {
        super(String.format("%s [field=%s, targetType=%s]", message,
                fieldName, targetType != null ? targetType.getSimpleName() : "unknown"));
        this.fieldName  = fieldName;
        this.targetType = targetType;
    }

    public JsonMappingException(String message, Throwable cause) {
        super(message, cause);
        this.fieldName  = null;
        this.targetType = null;
    }

    /** The field name that caused the mapping failure, or {@code null}. */
    public String getFieldName()    { return fieldName; }

    /** The Java class being mapped to, or {@code null}. */
    public Class<?> getTargetType() { return targetType; }
}
