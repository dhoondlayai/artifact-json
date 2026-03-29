package io.github.dhoondlayai.artifact.json.streaming;

import io.github.dhoondlayai.artifact.json.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * <h2>JsonStreamWriter â€” Zero-Allocation Event-Based JSON Writer</h2>
 *
 * <p>
 * A pull-based JSON generator inspired by Jackson's {@code JsonGenerator}.
 * Writes JSON tokens directly to an {@link OutputStream} or {@link Writer}
 * without building an in-memory
 * {@link io.github.dhoondlayai.artifact.json.model.JsonNode} tree.
 * This is the recommended approach for serializing very large datasets.
 * </p>
 *
 * <h3>When to use JsonStreamWriter:</h3>
 * <ul>
 * <li>Generating JSON responses for APIs without loading all data in
 * memory</li>
 * <li>Streaming millions of records from a database to JSON file</li>
 * <li>Building large JSON arrays incrementally</li>
 * </ul>
 *
 * <h3>Example â€” streaming array:</h3>
 * 
 * <pre>{@code
 * try (var out = new FileOutputStream("output.json");
 *         var writer = new JsonStreamWriter(out)) {
 *
 *     writer.beginArray();
 *     for (Record r : database.fetchAll()) {
 *         writer.beginObject()
 *                 .field("id", r.getId())
 *                 .field("name", r.getName())
 *                 .field("score", r.getScore())
 *                 .endObject();
 *     }
 *     writer.endArray();
 * }
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public final class JsonStreamWriter implements Closeable {

    private final Writer writer;
    private final boolean prettyPrint;
    private final int indentSize;

    private int depth = 0;
    private final boolean[] firstItem = new boolean[256];

    //
    // Constructors
    //

    public JsonStreamWriter(OutputStream out) {
        this(new OutputStreamWriter(out, StandardCharsets.UTF_8), false, 0);
    }

    public JsonStreamWriter(Writer writer) {
        this(writer, false, 0);
    }

    /** Creates a pretty-printing writer with the given indent. */
    public JsonStreamWriter(OutputStream out, int indent) {
        this(new OutputStreamWriter(out, StandardCharsets.UTF_8), indent > 0, indent);
    }

    private JsonStreamWriter(Writer writer, boolean prettyPrint, int indentSize) {
        this.writer = new BufferedWriter(writer, 8192);
        this.prettyPrint = prettyPrint;
        this.indentSize = indentSize;
        firstItem[0] = true;
    }

    //
    // Structure
    //

    /** Opens a JSON object {@code {}. */
    public JsonStreamWriter beginObject() throws IOException {
        writeSeparatorAndIndent();
        writer.write('{');
        depth++;
        firstItem[depth] = true;
        return this;
    }

    /** Closes a JSON object {@code }. */
    public JsonStreamWriter endObject() throws IOException {
        depth--;
        if (!firstItem[depth + 1])
            newlineAndIndent();
        writer.write('}');
        firstItem[depth] = false;
        return this;
    }

    /** Opens a JSON array {@code [}. */
    public JsonStreamWriter beginArray() throws IOException {
        writeSeparatorAndIndent();
        writer.write('[');
        depth++;
        firstItem[depth] = true;
        return this;
    }

    /** Closes a JSON array {@code ]}. */
    public JsonStreamWriter endArray() throws IOException {
        depth--;
        if (!firstItem[depth + 1])
            newlineAndIndent();
        writer.write(']');
        firstItem[depth] = false;
        return this;
    }

    //
    // Fields â€” for object contexts
    //

    /** Writes a string field ({@code "key":"value"}). */
    public JsonStreamWriter field(String key, String value) throws IOException {
        writeKey(key);
        writeStringValue(value);
        return this;
    }

    /** Writes a numeric field ({@code "key":42}). */
    public JsonStreamWriter field(String key, Number value) throws IOException {
        writeKey(key);
        writer.write(value == null ? "null" : value.toString());
        return this;
    }

    /** Writes a boolean field ({@code "key":true}). */
    public JsonStreamWriter field(String key, boolean value) throws IOException {
        writeKey(key);
        writer.write(value ? "true" : "false");
        return this;
    }

    /** Writes a null field ({@code "key":null}). */
    public JsonStreamWriter nullField(String key) throws IOException {
        writeKey(key);
        writer.write("null");
        return this;
    }

    /** Writes a raw pre-serialized JSON value as a field ({@code "key":{...}}). */
    public JsonStreamWriter rawField(String key, String rawJson) throws IOException {
        writeKey(key);
        writer.write(rawJson);
        return this;
    }

    //
    // Values â€” for array contexts
    //

    /** Writes a string value in an array context. */
    public JsonStreamWriter value(String v) throws IOException {
        writeSeparatorAndIndent();
        writeStringValue(v);
        firstItem[depth] = false;
        return this;
    }

    /** Writes a number value in an array context. */
    public JsonStreamWriter value(Number v) throws IOException {
        writeSeparatorAndIndent();
        writer.write(v == null ? "null" : v.toString());
        firstItem[depth] = false;
        return this;
    }

    /** Writes a boolean value in an array context. */
    public JsonStreamWriter value(boolean v) throws IOException {
        writeSeparatorAndIndent();
        writer.write(v ? "true" : "false");
        firstItem[depth] = false;
        return this;
    }

    /** Writes a null value in an array context. */
    public JsonStreamWriter nullValue() throws IOException {
        writeSeparatorAndIndent();
        writer.write("null");
        firstItem[depth] = false;
        return this;
    }

    /** Serializes any {@link JsonNode} directly. */
    public JsonStreamWriter writeNode(JsonNode node) throws IOException {
        writeSeparatorAndIndent();
        writer.write(node.toString());
        firstItem[depth] = false;
        return this;
    }

    //
    // Flush / Close
    //

    /** Flushes the internal writer buffer. */
    public JsonStreamWriter flush() throws IOException {
        writer.flush();
        return this;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    //
    // Internal Helpers
    //

    private void writeKey(String key) throws IOException {
        writeSeparatorAndIndent();
        writeStringValue(key);
        writer.write(':');
        if (prettyPrint)
            writer.write(' ');
        firstItem[depth] = false;
    }

    private void writeSeparatorAndIndent() throws IOException {
        if (!firstItem[depth])
            writer.write(',');
        if (prettyPrint && depth > 0)
            newlineAndIndent();
    }

    private void newlineAndIndent() throws IOException {
        writer.write('\n');
        writer.write(" ".repeat(indentSize * depth));
    }

    private void writeStringValue(String s) throws IOException {
        writer.write('"');
        if (s == null) {
            writer.write("null\"");
            return;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> writer.write("\\\"");
                case '\\' -> writer.write("\\\\");
                case '\n' -> writer.write("\\n");
                case '\r' -> writer.write("\\r");
                case '\t' -> writer.write("\\t");
                case '\b' -> writer.write("\\b");
                case '\f' -> writer.write("\\f");
                default -> {
                    if (c < 0x20)
                        writer.write(String.format("\\u%04x", (int) c));
                    else
                        writer.write(c);
                }
            }
        }
        writer.write('"');
    }
}
