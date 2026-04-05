package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * Controls which values are <b>included</b> or <b>excluded</b> during serialization.
 * Features a simple, zero-dependency API for clean JSON results.
 *
 * <p>By default, {@code CustomObjectMapper} serializes every field, including nulls and
 * empty collections. Apply {@code @JsonInclude} to skip unwanted values and produce
 * cleaner, leaner JSON payloads.
 *
 * <pre>{@code
 * @JsonInclude(JsonInclude.Include.NON_NULL)  // class-level: applies to all fields
 * public class ApiResponse {
 *     private String data;
 *
 *     @JsonInclude(JsonInclude.Include.NON_EMPTY)  // field-level override
 *     private List<String> errors;
 *
 *     @JsonInclude(JsonInclude.Include.ALWAYS)     // always include, even if null
 *     private String status;
 * }
 * // If data=null and errors=[], output: {"status": null}
 * }</pre>
 *
 * @author artifact-json
 * @since 2.1
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonInclude {

    Include value() default Include.ALWAYS;

    enum Include {
        /** Always include the field (default behavior). */
        ALWAYS,

        /** Skip the field if its value is {@code null}. */
        NON_NULL,

        /**
         * Skip the field if its value is {@code null}, an empty String {@code ""},
         * an empty Collection/Map, or an empty array/JsonArray.
         */
        NON_EMPTY,

        /**
         * Skip the field if its value equals the Java default for that type
         * ({@code null} for objects, {@code 0} for numbers, {@code false} for booleans).
         */
        NON_DEFAULT
    }
}
