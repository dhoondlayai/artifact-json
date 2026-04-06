package io.github.dhoondlayai.artifact.json;

import io.github.dhoondlayai.artifact.json.annotation.JsonProperty;
import io.github.dhoondlayai.artifact.json.convert.JsonConverter;
import io.github.dhoondlayai.artifact.json.exception.JsonParseException;
import io.github.dhoondlayai.artifact.json.model.*;
import io.github.dhoondlayai.artifact.json.proxy.JsonProxy;
import io.github.dhoondlayai.artifact.json.streaming.FastJsonEngine;
import io.github.dhoondlayai.artifact.json.streaming.JsonStreamReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MaxCoverage6Test {

    // --- JsonProxy Testing ---
    public interface UserProfile {
        @JsonProperty("user_id")
        String getId();
        void setId(String id);
        
        String getName();
        void setName(String name);
        
        boolean isActive();
        void setActive(boolean active);
        
        AddressInfo getAddress();
    }
    
    public interface AddressInfo {
        String getCity();
        void setCity(String city);
    }

    @Test
    void testJsonProxy() {
        JsonObject json = new JsonObject().put("user_id", "U1").put("name", "Bob").put("active", true)
                .put("address", new JsonObject().put("city", "London"));
        
        UserProfile proxy = JsonProxy.create(UserProfile.class, json);
        assertEquals("U1", proxy.getId());
        assertEquals("Bob", proxy.getName());
        assertTrue(proxy.isActive());
        assertEquals("London", proxy.getAddress().getCity());
        
        proxy.setName("Alice");
        assertEquals("Alice", json.getString("name").orElse(""));
        
        proxy.getAddress().setCity("Paris");
        assertEquals("Paris", json.getObject("address").get().getString("city").orElse(""));
        
        proxy.setActive(false);
        assertFalse(json.getBoolean("active").orElse(true));
        
        assertEquals(json.toString(), proxy.toString());
        
        assertThrows(UnsupportedOperationException.class, () -> proxy.hashCode());
    }

    // --- JsonConverter Testing ---
    @Test
    void testJsonConverterExhaustive() {
        JsonArray arr = new JsonArray()
            .add(new JsonObject().put("name", "A").put("val", 10))
            .add(new JsonObject().put("name", "B").put("val", 20));
        
        // CSV
        String csv = JsonConverter.toCsv(arr, ';', true);
        assertTrue(csv.contains("name;val"));
        JsonArray fromCsv = JsonConverter.fromCsv(csv, ';');
        assertEquals(2, fromCsv.size());
        assertEquals("A", ((JsonObject)fromCsv.element(0)).getString("name").orElse(""));
        
        // XML
        String xml = JsonConverter.toXml(arr, "data");
        assertTrue(xml.contains("<name>A</name>"));
        assertTrue(xml.contains("<val>10</val>"));
        
        // YAML
        String yaml = JsonConverter.toYaml(arr);
        assertTrue(yaml.contains("name: A"));
        JsonNode fromYaml = JsonConverter.fromYaml("name: A\nval: 10");
        assertTrue(fromYaml.isObject());
        
        // Properties
        String props = JsonConverter.toProperties(arr);
        assertTrue(props.contains("0.name=A"));
        assertTrue(props.contains("1.val=20"));
        
        // Tables
        String md = JsonConverter.toMarkdownTable(arr);
        assertTrue(md.contains("| name | val |"));
        String html = JsonConverter.toHtmlTable(arr, "my-table");
        assertTrue(html.contains("<table class=\"my-table\">"));
    }

    // --- FastJsonEngine Testing ---
    @Test
    void testParserEscapesAndErrors() {
        // Escapes
        String json = "{\"msg\": \"Line1\\nLine2\\tTab\\\\Backslash\\\"Quote\\/Slash\\u0041\"}";
        JsonNode node = FastJsonEngine.parse(json);
        assertEquals("Line1\nLine2\tTab\\Backslash\"Quote/SlashA", ((JsonObject)node).getString("msg").orElse(""));
        
        // Numbers
        assertEquals(123.456, FastJsonEngine.parse("123.456").asDouble());
        assertEquals(-1e2, FastJsonEngine.parse("-1e2").asDouble());
        assertEquals(1.2e+3, FastJsonEngine.parse("1.2e+3").asDouble());
        
        // Errors
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("{\"a\": 1} extra"));
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("{\"a\": \\uZZZZ}"));
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("{\"a\": \"\\z\"}"));
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("{\"a\": 1"));
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("[1, 2"));
    }

    // --- JsonStreamReader Testing ---
    @Test
    void testStreamReader() throws Exception {
        String json = "[{\"id\":1}, {\"id\":2}]";
        try (JsonStreamReader r = new JsonStreamReader(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            AtomicInteger count = new AtomicInteger();
            r.streamArray(n -> count.incrementAndGet());
            assertEquals(2, count.get());
        }
        
        try (JsonStreamReader r = new JsonStreamReader(new StringReader(json))) {
            AtomicInteger count = new AtomicInteger();
            r.streamFilteredArray("id", "2", n -> count.incrementAndGet());
            assertEquals(1, count.get());
        }
    }
}
