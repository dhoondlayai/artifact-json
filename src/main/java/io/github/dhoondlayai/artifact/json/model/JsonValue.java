package io.github.dhoondlayai.artifact.json.model;

/**
 * <h2>JsonValue â€” Leaf Node for Primitives</h2>
 *
 * <p>
 * Represents a JSON scalar value: {@link String}, {@link Number},
 * {@link Boolean}, or {@code null}. Implemented as a Java record for
 * structural immutability and value-based equality.
 * </p>
 *
 * <h3>Supported Types:</h3>
 * <ul>
 * <li>{@code null} â†’ JSON {@code null}</li>
 * <li>{@link String} â†’ JSON {@code "text"}</li>
 * <li>{@link Number} â†’ JSON {@code 42}, {@code 3.14}</li>
 * <li>{@link Boolean} â†’ JSON {@code true} / {@code false}</li>
 * </ul>
 *
 * <h3>Examples:</h3>
 * 
 * <pre>{@code
 * JsonValue name = new JsonValue("Alice");
 * JsonValue age = new JsonValue(30);
 * JsonValue active = new JsonValue(true);
 * JsonValue missing = new JsonValue(null);
 *
 * name.isString(); // true
 * age.isNumber(); // true
 * missing.isNull(); // true
 *
 * age.asInt(); // 30
 * name.asText(); // "Alice"
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public record JsonValue(Object value) implements JsonNode {

    /**
     * Compact constructor that validates the type of the wrapped value.
     * Only {@code String}, {@code Number}, {@code Boolean}, and {@code null}
     * are valid JSON primitive types.
     *
     * @throws IllegalArgumentException for unsupported types
     */
    public JsonValue {
        if (value != null && !(value instanceof String || value instanceof Number || value instanceof Boolean)) {
            throw new IllegalArgumentException(
                    "Invalid JSON value type: " + value.getClass().getName()
                            + ". Only String, Number, Boolean or null are allowed.");
        }
    }

    //
    // Serialization
    //

    /**
     * Serializes this value to a JSON-conformant string.
     * Strings are quoted and special characters are escaped.
     * Numbers and booleans are written as-is. {@code null} â†’ {@code null}.
     */
    @Override
    public String toString() {
        if (value == null)
            return "null";
        if (value instanceof String s)
            return "\"" + escapeString(s) + "\"";
        return String.valueOf(value);
    }

    //
    // RFC 8259 String Escaping
    //

    private static String escapeString(String s) {
        if (s.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
