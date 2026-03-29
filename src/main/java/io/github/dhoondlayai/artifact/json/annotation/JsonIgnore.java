package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * Marker annotation that indicates that the property is to be ignored 
 * during serialization and deserialization.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonIgnore {
    /**
     * Optional boolean defining whether this annotation is active.
     */
    boolean value() default true;
}
