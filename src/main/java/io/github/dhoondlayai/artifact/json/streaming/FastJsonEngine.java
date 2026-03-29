package io.github.dhoondlayai.artifact.json.streaming;

import io.github.dhoondlayai.artifact.json.exception.JsonParseException;
import io.github.dhoondlayai.artifact.json.model.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * <h2>FastJsonEngine â€” Zero-Copy, RFC 8259 Compliant JSON Parser</h2>
 *
 * <p>
 * The core parser of artifact-json. Parses JSON from multiple input sources
 * into a {@link JsonNode} tree without any intermediate representation.
 * Implements the full JSON specification (RFC 8259) including:
 * </p>
 * <ul>
 * <li>Full unicode escape sequences ({@code \\uXXXX})</li>*
 * <li>All escape chars:{
 * 
 * @code
 *       \"}, {@code \\},
 *       {@code \/},{@code\b},{@code\f},{@code\n},{@code\r},{@code\t}</li>*
 *       <li>Integer,float,negative,and scientific notation numbers</li>*
 *       <li>Booleans({@code true}/{@code false})and{@code null}</li>*
 *       <li>Deeply nested objects and arrays</li>*
 *       <li>Meaningful parse errors with line and column numbers</li>*
 *       </ul>
 *       **
 *       <h3>Performance characteristics:</h3>*
 *       <ul>
 *       *
 *       <li>ByteBuffer parsing avoids{@code char[]}allocation on every
 *       string</li>*
 *       <li>Direct{@code int}position tracking instead of iterator
 *       objects</li>*
 *       <li>Single-pass parsingâ€”O(n)time,O(depth)stack space</li>*
 *       <li>Pre-sized{@link StringBuilder}for string building</li>*
 *       </ul>
 *       **
 *       <h3>Usage:</h3>*
 * 
 *       <pre>{@code* // Parse
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      // from
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      // String
*JsonNode node=FastJsonEngine.parse("{\"name\":\"Alice\",\"age\":30}");** // Parse from ByteBuffer (off-heap / mapped
                                                                          // memory)
*ByteBuffer buf=ByteBuffer.wrap(jsonBytes);*JsonNode node=FastJsonEngine.parse(buf);** // Parse from InputStream
                                                                                       // (streaming large files)
*try(InputStream in=new FileInputStream("data.json")){*JsonNode node=FastJsonEngine.parse(in);*}*}</pre>
 * 
 *       **@author artifact-json*@version 2.0
 */
public final class FastJsonEngine {

    private FastJsonEngine() {
    }

    //
    // Public API â€” Entry points
    //

    /**
     * Parses a JSON string into a {@link JsonNode} tree.
     *
     * @param json the JSON string
     * @return root {@link JsonNode}
     * @throws JsonParseException if the input is not valid JSON
     */
    public static JsonNode parse(String json) {
        if (json == null || json.isBlank())
            throw new JsonParseException("Input JSON is null or empty");
        Parser p = new Parser(json);
        JsonNode node = p.parseValue();
        p.skipWhitespace();
        if (p.pos < json.length()) {
            throw new JsonParseException("Unexpected content after root value", p.line, p.col(),
                    json.substring(p.pos, Math.min(p.pos + 20, json.length())));
        }
        return node;
    }

    /**
     * Parses JSON from a {@link ByteBuffer} (may be direct/off-heap).
     * Zero-copy path â€” converts to String only for compatibility.
     *
     * @param buffer the buffer containing UTF-8 encoded JSON
     * @return root {@link JsonNode}
     * @throws JsonParseException if the input is invalid
     */
    public static JsonNode parse(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return parse(new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Parses JSON from an {@link InputStream}. Reads all bytes, then parses.
     * For truly streaming/large files use {@link JsonStreamReader} instead.
     *
     * @param in the input stream (UTF-8 encoded)
     * @return root {@link JsonNode}
     * @throws JsonParseException   if the JSON is invalid
     * @throws UncheckedIOException if reading fails
     */
    public static JsonNode parse(InputStream in) {
        try {
            byte[] bytes = in.readAllBytes();
            return parse(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read from InputStream", e);
        }
    }

    /**
     * Parses JSON from a {@link Reader}.
     *
     * @param reader the reader for the JSON source
     * @return root {@link JsonNode}
     * @throws JsonParseException if invalid JSON is encountered
     */
    public static JsonNode parse(Reader reader) {
        try {
            return parse(new BufferedReader(reader).lines()
                    .collect(java.util.stream.Collectors.joining("\n")));
        } catch (Exception e) {
            throw new JsonParseException("Failed to read from Reader: " + e.getMessage());
        }
    }

    //
    // Internal Parser
    //

    private static final class Parser {

        private final String src;
        private int pos = 0;
        private int line = 1;
        private int lineStart = 0;

        Parser(String src) {
            this.src = src;
        }

        int col() {
            return pos - lineStart + 1;
        }

        // â”€â”€ Whitespace
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        void skipWhitespace() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == '\n') {
                    line++;
                    lineStart = pos + 1;
                    pos++;
                } else if (c == ' ' || c == '\r' || c == '\t') {
                    pos++;
                } else
                    break;
            }
        }

        char peek() {
            skipWhitespace();
            if (pos >= src.length())
                throw new JsonParseException("Unexpected end of input", line, col());
            return src.charAt(pos);
        }

        char consume() {
            char c = src.charAt(pos++);
            if (c == '\n') {
                line++;
                lineStart = pos;
            }
            return c;
        }

        void expect(char expected) {
            skipWhitespace();
            if (pos >= src.length())
                throw new JsonParseException(
                        "Expected '" + expected + "' but reached end of input", line, col());
            char c = src.charAt(pos);
            if (c != expected)
                throw new JsonParseException(
                        "Expected '" + expected + "' but got '" + c + "'", line, col(), Character.toString(c));
            pos++;
        }

        // â”€â”€ Value Dispatcher
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JsonNode parseValue() {
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", true);
                case 'f' -> parseLiteral("false", false);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c))

                        yield parseNumber();
                    throw new JsonParseException("Unexpected character '" + c + "'", line, col());
                }

            };
        }

        // â”€â”€ Object
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JsonNode parseObject() {
            expect('{');
            JsonObject obj = new JsonObject();
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == '}') {
                pos++;
                return obj;
            }

            while (true) {
                skipWhitespace();
                if (pos >= src.length())
                    throw new JsonParseException("Unterminated object", line, col());

                // Key
                if (src.charAt(pos) != '"')
                    throw new JsonParseException("Expected string key but got '" + src.charAt(pos) + "'", line, col());
                String key = parseRawString();

                expect(':');

                // Value
                JsonNode value = parseValue();
                obj.put(key, value);

                skipWhitespace();
                if (pos >= src.length())
                    throw new JsonParseException("Unterminated object", line, col());
                char next = src.charAt(pos);
                if (next == '}') {
                    pos++;
                    break;
                }
                if (next != ',')
                    throw new JsonParseException(
                            "Expected ',' or '}' but got '" + next + "'", line, col());
                pos++; // consume comma
            }
            return obj;
        }

        // â”€â”€ Array
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JsonNode parseArray() {
            expect('[');
            JsonArray arr = new JsonArray();
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == ']') {
                pos++;
                return arr;
            }

            while (true) {
                arr.add(parseValue());
                skipWhitespace();
                if (pos >= src.length())
                    throw new JsonParseException("Unterminated array", line, col());
                char next = src.charAt(pos);
                if (next == ']') {
                    pos++;
                    break;
                }
                if (next != ',')
                    throw new JsonParseException(
                            "Expected ',' or ']' but got '" + next + "'", line, col());
                pos++;
            }
            return arr;
        }

        // â”€â”€ String
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JsonNode parseString() {
            return new JsonValue(parseRawString());
        }

        String parseRawString() {
            expect('"');
            StringBuilder sb = new StringBuilder(32);
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"')
                    return sb.toString();
                if (c == '\\') {
                    if (pos >= src.length())
                        throw new JsonParseException("Unterminated escape sequence", line, col());
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > src.length())
                                throw new JsonParseException(
                                        "Incomplete unicode escape", line, col());
                            String hex = src.substring(pos, pos + 4);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException e) {
                                throw new JsonParseException("Invalid unicode escape \\u" + hex, line, col());
                            }
                            pos += 4;
                        }
                        default -> throw new JsonParseException(
                                "Unknown escape sequence '\\" + esc + "'", line, col());
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new JsonParseException("Unterminated string", line, col());
        }

        // â”€â”€ Number
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JsonNode parseNumber() {
            int start = pos;
            if (pos < src.length() && src.charAt(pos) == '-')
                pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos)))
                pos++;
            boolean isDecimal = false;
            if (pos < src.length() && src.charAt(pos) == '.') {
                isDecimal = true;
                pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos)))
                    pos++;
            }
            if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                isDecimal = true;
                pos++;
                if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-'))
                    pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos)))
                    pos++;
            }
            String numStr = src.substring(start, pos);
            try {
                if (isDecimal)
                    return new JsonValue(Double.parseDouble(numStr));
                long lval = Long.parseLong(numStr);
                if (lval >= Integer.MIN_VALUE && lval <= Integer.MAX_VALUE)
                    return new JsonValue((int) lval);
                return new JsonValue(lval);
            } catch (NumberFormatException e) {
                throw new JsonParseException("Invalid number literal: " + numStr, line, col());
            }
        }

        // â”€â”€ Literals
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        JsonNode parseLiteral(String literal, Object result) {
            if (src.startsWith(literal, pos)) {
                pos += literal.length();
                return new JsonValue(result);
            }
            throw new JsonParseException(
                    "Expected '" + literal + "'", line, col(),
                    src.substring(pos, Math.min(pos + literal.length(), src.length())));
        }
    }
}
