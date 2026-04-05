package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * <b>Unique to artifact-json.</b> Marks a field as <b>read-only</b>: it is
 * serialized (written to JSON) but <b>never populated during deserialization</b>.
 *
 * <p>This solves the common API security problem where certain fields (like
 * {@code createdAt}, {@code id}, or {@code role}) should be returned in responses
 * but must not be settable by clients sending JSON bodies.
 *
 * <pre>{@code
 * public class User {
 *     @JsonReadOnly
 *     private String id;           // returned in GET, ignored in POST/PUT body
 *
 *     @JsonReadOnly
 *     private Instant createdAt;   // server-managed, clients can't override it
 *
 *     private String name;         // normal read-write field
 * }
 * }</pre>
 *
 * <p>Compare with {@code @JsonIgnore} which suppresses the field entirely in both
 * directions. {@code @JsonReadOnly} is the one-way alternative: out only.
 *
 * @author artifact-json
 * @since 2.1
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonReadOnly {
}
