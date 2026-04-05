package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * <b>Unique to artifact-json.</b> Flattens a nested POJO or {@code Map} field
 * into the parent JSON object during serialization, and re-inflates it during
 * deserialization — without any structural nesting in the JSON.
 *
 * <p>This is the clean solution for the "flat-wire, nested-model" pattern that is
 * extremely common in REST API design. It eliminates the need for complex custom
 * serializers for structural flattening.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class Order {
 *     private String orderId;
 *
 *     @JsonFlatten
 *     private Address shippingAddress;  // fields promoted to root level
 * }
 *
 * public class Address {
 *     private String street;
 *     private String city;
 *     private String zip;
 * }
 * }</pre>
 *
 * <b>Serialization output:</b>
 * <pre>{@code
 * {
 *   "orderId": "ORD-001",
 *   "street": "123 Main St",
 *   "city": "Springfield",
 *   "zip": "12345"
 * }
 * }</pre>
 * Instead of the typical nested form:
 * <pre>{@code
 * { "orderId": "ORD-001", "shippingAddress": { "street": "...", ... } }
 * }</pre>
 *
 * <h3>With a prefix (avoids key collisions)</h3>
 * <pre>{@code
 * @JsonFlatten(prefix = "billing_")
 * private Address billingAddress;
 * // → "billing_street", "billing_city", "billing_zip"
 * }</pre>
 *
 * @author artifact-json
 * @since 2.1
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonFlatten {
    /**
     * Optional prefix prepended to each flattened key to prevent collisions.
     * Example: {@code prefix = "address_"} produces {@code "address_street"}.
     */
    String prefix() default "";
}
