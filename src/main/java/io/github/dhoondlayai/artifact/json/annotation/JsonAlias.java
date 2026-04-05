package io.github.dhoondlayai.artifact.json.annotation;

import java.lang.annotation.*;

/**
 * Provides one or more alternative JSON key names that map to this field during
 * <b>deserialization</b>. The primary field name (or {@code @JsonProperty} value)
 * is still the only name used for <b>serialization</b>.
 *
 * <p>Solves real-world API versioning problems — e.g., an external API changed
 * {@code "user_name"} to {@code "username"}, and you want to accept both without
 * writing a custom deserializer.
 *
 * <pre>{@code
 * public class User {
 *     @JsonAlias({"user_name", "userName", "login"})
 *     private String username;
 * }
 * // All of the following JSON inputs will deserialize correctly:
 * // {"username": "alice"}  {"user_name": "alice"}  {"login": "alice"}
 * }</pre>
 *
 * @author artifact-json
 * @since 2.1
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonAlias {
    /** One or more alternative names accepted during deserialization. */
    String[] value();
}
