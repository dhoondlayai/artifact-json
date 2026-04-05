package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * Marks a <b>getter method</b> as a "virtual" JSON field.
 * The method's return value will be included in the JSON output,
 * even if no backing field exists.
 *
 * <p>Ideal for calculated properties like {@code fullName} or {@code totalCost}.</p>
 *
 * <pre>{@code
 * public class Invoice {
 *     private double subtotal;
 *     private double taxRate;
 *
 *     @JsonVirtual("total_amount")
 *     public double calculateTotal() {
 *         return subtotal * (1 + taxRate);
 *     }
 * }
 *
 * // Output: {"subtotal": 100, "taxRate": 0.08, "total_amount": 108.0}
 * }</pre>
 *
 * @author artifact-json
 * @since 2.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonVirtual {
    /**
     * The name of the field in the JSON output.
     * If left empty, the method name (stripped of 'get'/'is') is used.
     */
    String value() default "";
}
