package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * Supplies a <b>default value</b> for a field when the JSON key is absent or null
 * during deserialization. This eliminates defensive {@code Optional.orElse()} boilerplate
 * scattered across your business logic.
 *
 * <p>The {@code value()} string is coerced to the field's declared type by
 * {@code CustomObjectMapper} using the same coercion rules as normal deserialization.
 *
 * <pre>{@code
 * public class Config {
 *     @JsonDefault("true")
 *     private boolean enabled;          // → true if key missing
 *
 *     @JsonDefault("100")
 *     private int maxRetries;           // → 100 if key missing
 *
 *     @JsonDefault("USD")
 *     private String currency;          // → "USD" if key missing
 * }
 * }</pre>
 *
 * @author artifact-json
 * @since 2.1
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonDefault {
    /**
     * The default value as a string. Will be type-coerced to match the field type.
     */
    String value();
}
