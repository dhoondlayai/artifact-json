package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * Marks a POJO field as containing <b>Personally Identifiable Information (PII)</b>
 * or other sensitive data (SSN, passwords, tokens, card numbers, etc.).
 *
 * <p>When {@link io.github.dhoondlayai.artifact.json.databind.CustomObjectMapper}
 * serializes an object annotated with {@code @PII}, the field value is automatically
 * replaced by the configured mask string — no manual {@code JsonMasker} wiring required.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * public class Patient {
 *     private String name;
 *
 *     @PII
 *     private String ssn;          // masked with default "****"
 *
 *     @PII("***-**-####")
 *     private String creditCard;   // masked with custom pattern
 *
 *     @PII(mask = "[REDACTED]", audit = true)
 *     private String medicalRecord; // masked + access logged
 * }
 * }</pre>
 *
 * <p>Fields annotated with {@code @PII} are <em>never</em> deserialized back into the
 * POJO from JSON; they are always set to {@code null} on the deserialized side to prevent
 * sensitive data from flowing in via the wire unexpectedly.
 *
 * @author artifact-json
 * @since 2.1
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PII {

    /**
     * The mask string to replace the field value with during serialization.
     * Defaults to {@code "****"}.
     */
    String mask() default "****";

    /**
     * Optional shorthand alias for {@link #mask()}.
     * When both {@code value} and {@code mask} are provided, {@code mask} takes precedence.
     */
    String value() default "";

    /**
     * When {@code true}, the serialization event is logged to the audit trail
     * (if an audit sink is configured on the mapper). Defaults to {@code false}.
     */
    boolean audit() default false;
}
