package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * Controls serialization and deserialization format for a field.
 * Most useful for managing dates, numbers, and custom string representations
 * in a standardized way.
 *
 * <p>This annotation is unique to artifact-json — it provides a lightweight,
 * typed way to manage serialization schemas.
 *
 * <h3>Date / Time Fields</h3>
 * <pre>{@code
 * public class Order {
 *     @JsonFormat(pattern = "yyyy-MM-dd")
 *     private LocalDate orderDate;          // serializes as "2024-01-15"
 *
 *     @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
 *     private LocalDateTime createdAt;      // serializes as "2024-01-15T09:30:00"
 *
 *     @JsonFormat(shape = JsonFormat.Shape.NUMBER)
 *     private Instant timestamp;            // serializes as epoch millis: 1705312200000
 * }
 * }</pre>
 *
 * <h3>Number Formatting</h3>
 * <pre>{@code
 * public class Invoice {
 *     @JsonFormat(pattern = "#,##0.00")
 *     private double amount;   // serializes as "1,234.50" (String shape)
 * }
 * }</pre>
 *
 * @author artifact-json
 * @since 2.1
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonFormat {

    /**
     * Format pattern string. For dates: {@code "yyyy-MM-dd"}.
     * For numbers: {@code "#,##0.00"}. Empty means use type default.
     */
    String pattern() default "";

    /**
     * The output shape to use during serialization.
     */
    Shape shape() default Shape.STRING;

    enum Shape {
        /** Serialize as a JSON string (default for dates). */
        STRING,
        /** Serialize as a JSON number (e.g., epoch millis for Instant). */
        NUMBER,
        /** Serialize as a JSON boolean. */
        BOOLEAN
    }
}
