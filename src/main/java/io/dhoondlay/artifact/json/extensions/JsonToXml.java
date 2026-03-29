package io.dhoondlay.artifact.json.extensions;

import io.dhoondlay.artifact.json.model.*;
import java.util.*;

/**
 * 🔄 JSON to XML Bridge.
 * Effortlessly convert hierarchical JSON structures to standard XML strings.
 */
public class JsonToXml {

    /**
     * Transforms a JsonNode into an XML String.
     * @param root The root JSON object to transform.
     * @param rootName The name of the XML root tag.
     * @return Formatted XML representation.
     */
    public static String transformToXml(JsonNode root, String rootName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(rootName).append(">");
        transformRecursive(root, sb);
        sb.append("</").append(rootName).append(">");
        return sb.toString();
    }

    private static void transformRecursive(JsonNode node, StringBuilder sb) {
        if (node instanceof JsonObject obj) {
            obj.fields().forEach((k, v) -> {
                sb.append("<").append(k).append(">");
                transformRecursive(v, sb);
                sb.append("</").append(k).append(">");
            });
        } else if (node instanceof JsonArray arr) {
            arr.elements().forEach(e -> {
                sb.append("<item>");
                transformRecursive(e, sb);
                sb.append("</item>");
            });
        } else if (node instanceof JsonValue val) {
            sb.append(escapeXml(String.valueOf(val.value())));
        }
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
