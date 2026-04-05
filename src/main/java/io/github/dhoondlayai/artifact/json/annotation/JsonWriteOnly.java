package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * <b>Unique to artifact-json.</b> Marks a field as <b>write-only</b>: it is
 * deserialized (read from JSON input) but <b>never emitted during serialization</b>.
 *
 * <p>The canonical use case is passwords and secrets: you accept them in a registration
 * or login request body, but you must never echo them back in any API response.
 *
 * <pre>{@code
 * public class RegisterRequest {
 *     private String email;
 *
 *     @JsonWriteOnly
 *     private String password;      // accepted in request body, never in response
 *
 *     @JsonWriteOnly
 *     private String secretAnswer;  // same pattern
 * }
 * }</pre>
 *
 * <p>Compare with {@code @JsonIgnore} (both directions suppressed) and
 * {@code @PII} (serialized but masked). {@code @JsonWriteOnly} is the strict
 * zero-emission option.
 *
 * @author artifact-json
 * @since 2.1
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonWriteOnly {
}
