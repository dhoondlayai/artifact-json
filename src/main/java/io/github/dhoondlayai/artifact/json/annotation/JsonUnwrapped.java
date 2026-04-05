package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * <b>Unwraps</b> a complex object's fields into the parent during serialization.
 * Maintains zero external dependencies and high reflection safety.
 *
 * <p>Use this to avoid redundant nested structures when mapping legacy models
 * or DDD value objects.</p>
 *
 * <pre>{@code
 * public class User {
 *     private String id;
 *
 *     @JsonUnwrapped(prefix = "addr_")
 *     private Address address;
 * }
 *
 * public class Address {
 *     private String city;
 *     private String zip;
 * }
 *
 * // Output:
 * {
 *   "id": "123",
 *   "addr_city": "New York",
 *   "addr_zip": "10001"
 * }
 * }</pre>
 *
 * @author artifact-json
 * @since 2.1
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonUnwrapped {
    /**
     * Optional prefix to prepend to all unwrapped field names.
     */
    String prefix() default "";

    /**
     * Optional suffix to append to all unwrapped field names.
     */
    String suffix() default "";
}
