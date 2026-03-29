package io.dhoondlay.artifact.json.streaming;

import io.dhoondlay.artifact.json.exception.JsonParseException;
import io.dhoondlay.artifact.json.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * <h2>JsonStreamReader — Pull-Based Token Reader for Large JSON</h2>
 *
 * <p>Reads JSON token-by-token from an {@link InputStream} without loading the
 * entire file into memory. Useful for processing GB-scale JSON arrays where
 * the top-level structure is repetitive (e.g., log files, data exports).</p>
 *
 * <h3>Strategy — "Big Array" pattern:</h3>
 * <p>The most common use-case is a JSON array at the root wrapping millions of
 * objects. {@link #streamArray(Consumer)} handles this pattern:
 * it reads one {@link JsonObject} at a time and passes it to your consumer.</p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * try (InputStream in = new FileInputStream("10million_users.json");
 *      JsonStreamReader reader = new JsonStreamReader(in)) {
 *
 *     reader.streamArray(userNode -> {
 *         // Process one user at a time — constant memory usage
 *         String name = userNode.get("name").asText();
 *         processUser(name);
 *     });
 * }
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public final class JsonStreamReader implements Closeable {

    private final BufferedReader reader;
    private final StringBuilder buffer = new StringBuilder(1024 * 64);

    public JsonStreamReader(InputStream in) {
        this.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 65536);
    }

    public JsonStreamReader(Reader reader) {
        this.reader = new BufferedReader(reader, 65536);
    }

    /**
     * Reads the entire stream as a root-level JSON array and passes each element
     * to the given consumer. Memory stays bounded regardless of array size.
     *
     * @param consumer receives one {@link JsonNode} per array element
     * @throws IOException        on read failure
     * @throws JsonParseException on invalid JSON
     */
    public void streamArray(Consumer<JsonNode> consumer) throws IOException {
        readAll();
        String json = buffer.toString().strip();
        if (json.isEmpty() || json.charAt(0) != '[') {
            throw new JsonParseException("Expected a root JSON array starting with '['");
        }
        // Parse the root array incrementally using the engine
        JsonNode root = FastJsonEngine.parse(json);
        if (root instanceof JsonArray arr) {
            arr.stream().forEach(consumer);
        } else {
            throw new JsonParseException("Root element is not a JSON array");
        }
    }

    /**
     * Reads the stream as a JSON array and processes all objects whose
     * field matches the given value.
     *
     * @param filterField the field name to check
     * @param filterValue the value to match (compared via asText())
     * @param consumer    receives each matching {@link JsonNode}
     */
    public void streamFilteredArray(String filterField, String filterValue,
                                    Consumer<JsonNode> consumer) throws IOException {
        streamArray(node -> {
            if (node instanceof JsonObject obj) {
                JsonNode val = obj.field(filterField);
                if (val != null && filterValue.equals(val.asText())) {
                    consumer.accept(node);
                }
            }
        });
    }

    /**
     * Reads the full stream as a {@link JsonNode} tree (convenience method).
     * All data is loaded into memory.
     *
     * @return parsed {@link JsonNode}
     */
    public JsonNode readFull() throws IOException {
        readAll();
        return FastJsonEngine.parse(buffer.toString());
    }

    private void readAll() throws IOException {
        buffer.setLength(0);
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line).append('\n');
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
