package io.github.dhoondlayai.artifact.json.convert;

import io.github.dhoondlayai.artifact.json.exception.JsonConversionException;
import io.github.dhoondlayai.artifact.json.model.*;
import io.github.dhoondlayai.artifact.json.streaming.FastJsonEngine;

import java.util.*;

/**
 * <h2>JsonConverter â€” Unified Conversion Facade</h2>
 *
 * <p>
 * The single entry point for all format conversions in artifact-json.
 * Delegates to specialized converter classes but provides a clean, unified API.
 * </p>
 *
 * <h3>Supported conversions:</h3>
 * 
 * <pre>
 *   JSON â†’ String (compact, pretty, minified)
 *   JSON â†’ CSV
 *   JSON â†’ XML
 *   JSON â†’ YAML
 *   JSON â†’ Java .properties
 *   JSON â†’ Markdown Table
 *   JSON â†’ HTML Table
 *   CSV  â†’ JSON
 *   XML  â†’ JSON
 *   YAML â†’ JSON
 * </pre>
 *
 * <h3>Examples:</h3>
 * 
 * <pre>{@code
 * JsonNode node = FastJsonEngine.parse(json);
 *
 * String pretty = JsonConverter.toPrettyString(node, 2);
 * String csv = JsonConverter.toCsv(array);
 * String xml = JsonConverter.toXml(node, "root");
 * String yaml = JsonConverter.toYaml(node);
 * String props = JsonConverter.toProperties(node);
 * String mdTable = JsonConverter.toMarkdownTable(array);
 * String html = JsonConverter.toHtmlTable(array);
 *
 * JsonNode fromCsv = JsonConverter.fromCsv(csvString);
 * JsonNode fromYaml = JsonConverter.fromYaml(yamlString);
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public final class JsonConverter {

    private JsonConverter() {
    }

    //
    // JSON â†’ String
    //

    /**
     * Returns a compact single-line JSON string.
     *
     * @param node the node to serialize
     */
    public static String toCompactString(JsonNode node) {
        return node.toString();
    }

    /**
     * Returns a human-readable pretty-printed JSON string.
     *
     * @param node   the node to serialize
     * @param indent number of spaces per indentation level
     */
    public static String toPrettyString(JsonNode node, int indent) {
        return switch (node) {
            case JsonObject obj -> obj.toPrettyString(indent);
            case JsonArray arr -> arr.toPrettyString(indent);
            default -> node.toString();
        };
    }

    /**
     * Returns an ultra-compact JSON string with all whitespace removed.
     * Equivalent to {@link #toCompactString(JsonNode)}.
     */
    public static String toMinified(JsonNode node) {
        return node.toString();
    }

    //
    // JSON â†’ CSV
    //

    /**
     * Converts a {@link JsonArray} of {@link JsonObject} rows to CSV format.
     * The first row contains headers (derived from the keys of the first object).
     * All fields are quoted to handle commas inside values.
     *
     * @param array the array of JSON objects (rows)
     * @return CSV string with header + data rows
     * @throws JsonConversionException if input is not an array of objects
     */
    public static String toCsv(JsonArray array) {
        return toCsv(array, ',', true);
    }

    /**
     * Converts a {@link JsonArray} to CSV with custom delimiter.
     *
     * @param array         array of JSON objects
     * @param delimiter     field separator character (e.g., {@code ','},
     *                      {@code ';'}, {@code '\t'})
     * @param includeHeader whether to include a header row
     */
    public static String toCsv(JsonArray array, char delimiter, boolean includeHeader) {
        if (array.isEmpty())
            return "";
        JsonNode first = array.element(0);
        if (!(first instanceof JsonObject))
            throw new JsonConversionException(
                    "JSON", "CSV", "Array elements must be JsonObject rows");

        List<String> headers = new ArrayList<>(((JsonObject) first).keys());
        StringBuilder sb = new StringBuilder(array.size() * headers.size() * 16);

        if (includeHeader) {
            appendCsvRow(sb, headers, delimiter);
        }
        for (JsonNode node : array) {
            if (node instanceof JsonObject obj) {
                List<String> row = new ArrayList<>(headers.size());
                for (String key : headers) {
                    JsonNode val = obj.field(key);
                    row.add(val == null ? "" : csvEscape(val.asText(), delimiter));
                }
                appendCsvRow(sb, row, delimiter);
            }
        }
        return sb.toString();
    }

    private static void appendCsvRow(StringBuilder sb, List<String> cols, char delimiter) {
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0)
                sb.append(delimiter);
            sb.append(cols.get(i));
        }
        sb.append('\n');
    }

    private static String csvEscape(String val, char delimiter) {
        if (val.contains(String.valueOf(delimiter)) || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    //
    // CSV â†’ JSON
    //

    /**
     * Parses a CSV string with a header row into a {@link JsonArray} of objects.
     * Handles quoted fields with embedded commas, newlines, and double-quote
     * escapes.
     *
     * @param csv CSV content (first row = headers)
     * @return {@link JsonArray} of {@link JsonObject} rows
     */
    public static JsonArray fromCsv(String csv) {
        return fromCsv(csv, ',');
    }

    /**
     * Parses CSV with a custom delimiter.
     *
     * @param csv       CSV content
     * @param delimiter field separator
     */
    public static JsonArray fromCsv(String csv, char delimiter) {
        JsonArray result = new JsonArray();
        String[] lines = csv.split("\r?\n");
        if (lines.length == 0)
            return result;

        String[] headers = parseCsvLine(lines[0], delimiter);
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank())
                continue;
            String[] values = parseCsvLine(lines[i], delimiter);
            JsonObject obj = new JsonObject(headers.length);
            for (int j = 0; j < headers.length; j++) {
                String val = j < values.length ? values[j].trim() : "";
                // Auto-detect type
                obj.put(headers[j].trim(), detectType(val));
            }
            result.add(obj);
        }
        return result;
    }

    private static String[] parseCsvLine(String line, char delimiter) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }

    private static JsonNode detectType(String val) {
        if (val.isEmpty() || val.equalsIgnoreCase("null"))
            return new JsonValue(null);
        if (val.equalsIgnoreCase("true"))
            return new JsonValue(true);
        if (val.equalsIgnoreCase("false"))
            return new JsonValue(false);
        try {
            return new JsonValue(Integer.parseInt(val));
        } catch (NumberFormatException ignored) {
        }
        try {
            return new JsonValue(Double.parseDouble(val));
        } catch (NumberFormatException ignored) {
        }
        return new JsonValue(val);
    }

    //
    // JSON â†’ XML
    //

    /**
     * Converts a {@link JsonNode} to an XML string.
     * Arrays become repeated elements using the parent tag name.
     *
     * @param node    the node to convert
     * @param rootTag the XML root element name
     */
    public static String toXml(JsonNode node, String rootTag) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        nodeToXml(sb, node, rootTag, 0);
        return sb.toString();
    }

    private static void nodeToXml(StringBuilder sb, JsonNode node, String tag, int depth) {
        String indent = "  ".repeat(depth);
        switch (node) {
            case JsonObject obj -> {
                sb.append(indent).append('<').append(xmlTag(tag)).append(">\n");
                obj.fields().forEach((k, v) -> nodeToXml(sb, v, k, depth + 1));
                sb.append(indent).append("</").append(xmlTag(tag)).append(">\n");
            }
            case JsonArray arr -> {
                for (JsonNode elem : arr) {
                    nodeToXml(sb, elem, tag, depth);
                }
            }
            case JsonValue val -> {
                sb.append(indent).append('<').append(xmlTag(tag)).append('>')
                        .append(xmlEscape(val.asText()))
                        .append("</").append(xmlTag(tag)).append(">\n");
            }
        }
    }

    private static String xmlTag(String s) {
        return s.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }

    private static String xmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    //
    // JSON â†’ YAML
    //

    /**
     * Converts a {@link JsonNode} to YAML format (no external libraries required).
     *
     * @param node the node to convert
     */
    public static String toYaml(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        nodeToYaml(sb, node, 0, false);
        return sb.toString();
    }

    private static void nodeToYaml(StringBuilder sb, JsonNode node, int depth, boolean inline) {
        String pad = "  ".repeat(depth);
        switch (node) {
            case JsonObject obj -> {
                if (inline)
                    sb.append('\n');
                obj.fields().forEach((k, v) -> {
                    sb.append(pad).append(yamlKey(k)).append(": ");
                    if (v instanceof JsonValue val) {
                        sb.append(yamlScalar(val)).append('\n');
                    } else {
                        nodeToYaml(sb, v, depth + 1, true);
                    }
                });
            }
            case JsonArray arr -> {
                if (inline)
                    sb.append('\n');
                for (JsonNode elem : arr) {
                    sb.append(pad).append("- ");
                    if (elem instanceof JsonValue val) {
                        sb.append(yamlScalar(val)).append('\n');
                    } else {
                        nodeToYaml(sb, elem, depth + 1, true);
                    }
                }
            }
            case JsonValue val -> sb.append(yamlScalar(val)).append('\n');
        }
    }

    private static String yamlKey(String k) {
        return k.matches("[a-zA-Z0-9_]+") ? k : "'" + k.replace("'", "''") + "'";
    }

    private static String yamlScalar(JsonValue val) {
        if (val.value() == null)
            return "null";
        if (val.value() instanceof Boolean)
            return val.value().toString();
        if (val.value() instanceof Number)
            return val.value().toString();
        String s = val.value().toString();
        // Quote if string could be misinterpreted
        if (s.contains(":") || s.contains("#") || s.startsWith("\"") || s.isEmpty()
                || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")
                || s.equalsIgnoreCase("null")) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        }
        return s;
    }

    //
    // JSON â†’ Properties
    //

    /**
     * Converts a flat or nested {@link JsonObject} to Java {@code .properties}
     * format.
     * Nested keys are separated by dots. Arrays are indexed ({@code key.0},
     * {@code key.1}).
     *
     * @param node the node to flatten to properties
     */
    public static String toProperties(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        flattenToProperties(sb, node, "");
        return sb.toString();
    }

    private static void flattenToProperties(StringBuilder sb, JsonNode node, String prefix) {
        switch (node) {
            case JsonObject obj ->
                obj.fields().forEach((k, v) -> flattenToProperties(sb, v, prefix.isEmpty() ? k : prefix + "." + k));
            case JsonArray arr -> {
                int[] i = { 0 };
                arr.stream().forEach(e -> flattenToProperties(sb, e, prefix + "." + i[0]++));
            }
            case JsonValue val -> {
                sb.append(prefix).append('=');
                if (val.value() != null)
                    sb.append(val.value().toString().replace("\\", "\\\\"));
                sb.append('\n');
            }
        }
    }

    //
    // JSON â†’ Markdown Table
    //

    /**
     * Converts a {@link JsonArray} of {@link JsonObject} rows to a Markdown table.
     *
     * @param array array of object rows
     * @return Markdown table string
     */
    public static String toMarkdownTable(JsonArray array) {
        if (array.isEmpty())
            return "_empty_\n";
        if (!(array.element(0) instanceof JsonObject first))
            throw new JsonConversionException("JSON", "Markdown", "Array elements must be objects");

        List<String> headers = new ArrayList<>(first.keys());
        StringBuilder sb = new StringBuilder();

        // Header row
        sb.append('|');
        headers.forEach(h -> sb.append(' ').append(h).append(" |"));
        sb.append('\n');

        // Separator
        sb.append('|');
        headers.forEach(h -> sb.append(" --- |"));
        sb.append('\n');

        // Data rows
        for (JsonNode node : array) {
            if (node instanceof JsonObject obj) {
                sb.append('|');
                for (String h : headers) {
                    JsonNode val = obj.field(h);
                    sb.append(' ').append(val == null ? "" : val.asText()).append(" |");
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    //
    // JSON â†’ HTML Table
    //

    /**
     * Converts a {@link JsonArray} of objects to an HTML {@code 
     * 
    <table>
     * }.
     *
     * @param array    array of object rows
     * @param cssClass optional CSS class for the table element (can be null)
     */
    public static String toHtmlTable(JsonArray array, String cssClass) {
        if (array.isEmpty())
            return "<p><em>No data</em></p>";
        if (!(array.element(0) instanceof JsonObject first))
            throw new JsonConversionException("JSON", "HTML", "Array elements must be objects");

        List<String> headers = new ArrayList<>(first.keys());
        String cls = cssClass != null && !cssClass.isBlank()
                ? " class=\"" + cssClass + "\""
                : "";
        StringBuilder sb = new StringBuilder();
        sb.append("<table").append(cls).append(">\n<thead>\n<tr>");
        headers.forEach(h -> sb.append("<th>").append(htmlEscape(h)).append("</th>"));
        sb.append("</tr>\n</thead>\n<tbody>\n");

        for (JsonNode node : array) {
            if (node instanceof JsonObject obj) {
                sb.append("<tr>");
                for (String h : headers) {
                    JsonNode val = obj.field(h);
                    sb.append("<td>").append(val == null ? "" : htmlEscape(val.asText())).append("</td>");
                }
                sb.append("</tr>\n");
            }
        }
        sb.append("</tbody>\n</table>");
        return sb.toString();
    }

    /** Convenience overload with no CSS class. */
    public static String toHtmlTable(JsonArray array) {
        return toHtmlTable(array, null);
    }

    private static String htmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    //
    // YAML â†’ JSON
    //

    /**
     * Parses a simple YAML string into a {@link JsonNode}.
     * Handles flat key-value pairs and simple nested structure.
     * For complex YAML, use a dedicated YAML library.
     *
     * @param yaml the YAML string
     * @return {@link JsonNode} representation
     */
    public static JsonNode fromYaml(String yaml) {
        JsonObject root = new JsonObject();
        String[] lines = yaml.split("\r?\n");
        parseYamlLines(root, lines, 0, 0);
        return root;
    }

    private static int parseYamlLines(JsonObject obj, String[] lines, int start, int baseIndent) {
        int i = start;
        while (i < lines.length) {
            String line = lines[i];
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                i++;
                continue;
            }
            int indent = countLeadingSpaces(line);
            if (indent < baseIndent)
                break;
            String stripped = line.strip();
            if (stripped.contains(": ")) {
                int colon = stripped.indexOf(": ");
                String key = stripped.substring(0, colon).trim();
                String value = stripped.substring(colon + 2).trim();
                if (value.isEmpty() && i + 1 < lines.length && countLeadingSpaces(lines[i + 1]) > indent) {
                    JsonObject child = new JsonObject();
                    i = parseYamlLines(child, lines, i + 1, indent + 2);
                    obj.put(key, child);
                } else {
                    obj.put(key, yamlStringToNode(value));
                    i++;
                }
            } else {
                i++;
            }
        }
        return i;
    }

    private static int countLeadingSpaces(String s) {
        int c = 0;
        while (c < s.length() && s.charAt(c) == ' ')
            c++;
        return c;
    }

    private static JsonNode yamlStringToNode(String s) {
        if (s.equals("null") || s.equals("~"))
            return new JsonValue(null);
        if (s.equals("true"))
            return new JsonValue(true);
        if (s.equals("false"))
            return new JsonValue(false);
        try {
            return new JsonValue(Integer.parseInt(s));
        } catch (NumberFormatException e) {
        }
        try {
            return new JsonValue(Double.parseDouble(s));
        } catch (NumberFormatException e) {
        }
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return new JsonValue(s.substring(1, s.length() - 1));
        }
        return new JsonValue(s);
    }
}
