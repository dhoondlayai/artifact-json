package io.github.dhoondlayai.artifact.json;

import io.github.dhoondlayai.artifact.json.annotation.*;
import io.github.dhoondlayai.artifact.json.convert.*;
import io.github.dhoondlayai.artifact.json.databind.*;
import io.github.dhoondlayai.artifact.json.extensions.*;
import io.github.dhoondlayai.artifact.json.model.*;
import io.github.dhoondlayai.artifact.json.streaming.*;
import io.github.dhoondlayai.artifact.json.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Max Coverage Test 2 - Streaming, Binding, Extensions")
public class MaxCoverage2Test {

    @Test
    @DisplayName("Exhaustive Streaming Coverage")
    void testStreaming() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonStreamWriter writer = new JsonStreamWriter(baos)) {
            writer.beginObject()
                .field("str", "abc\\\"\n\r\t\b\f\u0000") // Escapes
                .field("num", 123)
                .field("num_null", (Number)null)
                .field("bool", true)
                .nullField("nl")
                .rawField("raw", "{}")
                .field("obj_str", "str") // to exercise different paths
                .beginObject()
                .endObject();
            writer.flush();
        }
        
        baos = new ByteArrayOutputStream();
        try (JsonStreamWriter w = new JsonStreamWriter(baos)) {
            w.beginArray()
                .value("v")
                .value((Number)1)
                .value((Number)null)
                .value(true)
                .value(false)
                .nullValue()
                .writeNode(new JsonArray())
                .beginObject()
                .endObject()
                .endArray();
            w.flush();
        }
        
        JsonNode parsed = FastJsonEngine.parse(baos.toString());
        assertTrue(parsed.isArray());
        assertEquals(8, ((JsonArray)parsed).size());
        
        // fastjson engine edge cases
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("invalid"));
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("{\"a\":}"));
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("[1, 2, "));
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("\"unclosed"));
        try {
            FastJsonEngine.parse("1e9999");
        } catch (Exception e) {}
    }

    @Test
    @DisplayName("Exhaustive FastObjectMapper Coverage")
    void testFastObjectMapper() {
        FastObjectMapper mapper = new FastObjectMapper();
        
        // Primitives
        assertEquals("test", mapper.serialize("test").asText());
        assertEquals(1, mapper.serialize(1).asInt());
        assertEquals(true, mapper.serialize(true).asBoolean());
        assertTrue(mapper.serialize(null).isNull());
        
        // Collections
        List<String> list = Arrays.asList("a", "b");
        assertEquals(2, ((JsonArray)mapper.serialize(list)).size());
        
        // Maps
        Map<String, Object> map = new HashMap<>();
        map.put("k", "v");
        assertTrue(((JsonObject)mapper.serialize(map)).contains("k"));
        
        // Standard Object
        Obj o = new Obj();
        o.id = 1;
        o.name = "Test";
        o.ignore = "ignored";
        o.camelCase = "camel";
        JsonObject json = (JsonObject) mapper.serialize(o);
        assertEquals(1, json.getInt("ident").orElse(0)); // @JsonProperty
        assertFalse(json.contains("ignore")); // @JsonIgnore
        assertTrue(json.contains("camel_case")); // @JsonNaming(SNAKE_CASE)
    }

    @JsonNaming(JsonNaming.NamingStrategy.SNAKE_CASE)
    public static class Obj {
        @JsonProperty("ident")
        public int id;
        public String name;
        @JsonIgnore
        public String ignore;
        public String camelCase;
        @JsonProperty("") // empty property fallback
        public String emptyProp = "e";
    }

    @Test
    @DisplayName("Exhaustive JsonConverter Coverage")
    void testConverter() {
        JsonObject obj = new JsonObject().put("a", 1).put("b", "test\nline");
        JsonArray arr = new JsonArray().add(obj).add(new JsonObject().put("a", 2));
        
        assertEquals("{\"a\":1,\"b\":\"test\\nline\"}", JsonConverter.toCompactString(obj));
        assertEquals("{\"a\":1,\"b\":\"test\\nline\"}", JsonConverter.toMinified(obj));
        
        String yaml = JsonConverter.toYaml(obj);
        assertTrue(yaml.contains("a: 1"));
        
        String props = JsonConverter.toProperties(obj);
        assertTrue(props.contains("a=1"));
        
        String md = JsonConverter.toMarkdownTable(arr);
        assertTrue(md != null && md.contains("|"));
        
        String html = JsonConverter.toHtmlTable(arr);
        assertTrue(html != null && html.contains("th"));
        
        // Edge cases
        assertThrows(JsonConversionException.class, () -> JsonConverter.toCsv(new JsonArray().add(1))); // not obj inside array
        assertThrows(JsonConversionException.class, () -> JsonConverter.toMarkdownTable(new JsonArray().add(1)));
        assertThrows(JsonConversionException.class, () -> JsonConverter.toHtmlTable(new JsonArray().add(1)));
    }
    
    @Test
    @DisplayName("Exhaustive Shield, Masker, Patch")
    void testExtensions() {
        JsonObject root = new JsonObject().put("nested", new JsonObject().put("a", 1));
        JsonShield shield = new JsonShield(root);
        
        assertEquals(1, shield.getInt("nested.a", 0));
        assertEquals(0, shield.getInt("nested.b", 0));
        assertEquals("default", shield.getString("x.y", "default"));
        
        assertTrue(shield.exists("nested.a"));
        assertFalse(shield.exists("missing"));
        
        JsonMasker masker = new JsonMasker("***").addSensitiveKey("ssn");
        JsonObject pii = new JsonObject().put("ssn", "123").put("public", "abc");
        JsonObject masked = (JsonObject) masker.mask(pii);
        assertEquals("***", masked.getString("ssn").orElse(""));
        assertEquals("abc", masked.getString("public").orElse(""));
        
        // Patch operations
        JsonObject v1 = new JsonObject().put("a", 1);
        JsonObject v2 = new JsonObject().put("a", 2).put("b", 1);
        JsonArray patch = JsonPatchGenerator.generatePatch(v1, v2);
        assertTrue(patch.size() > 0);
    }
}
