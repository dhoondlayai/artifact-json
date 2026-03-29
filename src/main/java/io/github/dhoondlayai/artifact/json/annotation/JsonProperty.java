package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * Defines a logical name for a property to be used during JSON serialization/deserialization.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonProperty {
    /**
     * The name to use for the JSON property. If empty, defaults to the field name.
     */
    String value() default "";

    /**
     * Whether the property is required. Defaults to false.
     */
    boolean required() default false;
}
