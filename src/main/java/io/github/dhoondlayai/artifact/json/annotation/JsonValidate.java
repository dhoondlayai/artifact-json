package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * Adds lightweight <b>validation constraints</b> to fields during deserialization.
 *
 * <p>Supports regex matching, mandatory checks, and basic numeric ranges.
 * If validation fails, {@code CustomObjectMapper} throws a {@code JsonValidationException}.</p>
 *
 * <pre>{@code
 * public class User {
 *     @JsonValidate(required = true, regex = "^[A-Za-z0-9]+$")
 *     private String username;
 *
 *     @JsonValidate(min = 18, max = 120)
 *     private int age;
 * }
 * }</pre>
 *
 * @author artifact-json
 * @since 2.1
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonValidate {
    /** Whether the field must be present and non-null. */
    boolean required() default false;

    /** Regular expression the field value must match. */
    String regex() default "";

    /** Minimum numeric value allowed (inclusive). */
    double min() default Double.NEGATIVE_INFINITY;

    /** Maximum numeric value allowed (inclusive). */
    double max() default Double.POSITIVE_INFINITY;
}
