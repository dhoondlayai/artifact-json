package io.github.dhoondlayai.artifact.json;

import io.github.dhoondlayai.artifact.json.annotation.JsonIgnore;
import io.github.dhoondlayai.artifact.json.annotation.JsonNaming;
import io.github.dhoondlayai.artifact.json.annotation.JsonProperty;
import io.github.dhoondlayai.artifact.json.codegen.JsonCodeGenerator;
import io.github.dhoondlayai.artifact.json.databind.CustomObjectMapper;
import io.github.dhoondlayai.artifact.json.model.JsonArray;
import io.github.dhoondlayai.artifact.json.model.JsonMerger;
import io.github.dhoondlayai.artifact.json.model.JsonObject;
import io.github.dhoondlayai.artifact.json.proxy.JsonProxy;
import io.github.dhoondlayai.artifact.json.streaming.JsonStreamWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Advanced Features (Mapping, Proxy, Codegen, Streaming) Tests")
public class AdvancedFeaturesTest {

    @Test
    @DisplayName("Serialization with CustomObjectMapper and Annotations")
    void testSerialization() {
        CustomObjectMapper mapper = new CustomObjectMapper();
        AnnotatedUser user = new AnnotatedUser("john_doe", "secret", "John Doe");
        
        String json = mapper.serialize(user).toString();
        assertTrue(json.contains("\"username\":\"john_doe\""));
        assertTrue(json.contains("\"full_name\":\"John Doe\"")); // Renamed field
        assertFalse(json.contains("password")); // Ignored field
    }

    @Test
    @DisplayName("Zero-Copy Proxy Interface Mapping")
    void testJsonProxy() {
        JsonObject cfg = new JsonObject().put("theme_color", "dark").put("version", 2);
        SystemConfig sysConfig = JsonProxy.create(SystemConfig.class, cfg);
        
        assertEquals("dark", sysConfig.getThemeColor());
        assertEquals(2, sysConfig.getVersion());
        
        sysConfig.setThemeColor("light");
        assertEquals("light", cfg.getString("theme_color").orElse(null));
    }

    @Test
    @DisplayName("Java Records Code Generation")
    void testCodeGeneration() {
        JsonObject data = new JsonObject().put("id", 1).put("name", "Artifact");
        String code = JsonCodeGenerator.generateJavaRecords("MyResponse", data);
        
        assertTrue(code.contains("public record MyResponse"));
        assertTrue(code.contains("int id"));
        assertTrue(code.contains("String name"));
    }

    @Test
    @DisplayName("Event-based Streaming with JsonStreamWriter")
    void testStreamingWriter() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonStreamWriter writer = new JsonStreamWriter(baos, 0)) {
            writer.beginObject()
                  .field("id", 101)
                  .field("msg", "hello")
                  .endObject();
        }
        String result = baos.toString();
        assertEquals("{\"id\":101,\"msg\":\"hello\"\n}", result);
    }

    @Test
    @DisplayName("Deep Merge and Keep-Left Merge")
    void testJsonMerger() {
        JsonObject base = new JsonObject().put("a", 1).put("b", 2);
        JsonObject overlay = new JsonObject().put("b", 3).put("c", 4);
        
        JsonObject merged = (JsonObject) JsonMerger.deepMerge(base, overlay);
        assertEquals(1, merged.get("a").asInt());
        assertEquals(3, merged.get("b").asInt()); // Override
        assertEquals(4, merged.get("c").asInt());
        
        JsonObject keepLeft = (JsonObject) JsonMerger.mergeKeepLeft(base, overlay);
        assertEquals(2, keepLeft.get("b").asInt()); // No override
    }

    // Help Types for Tests

    public interface SystemConfig {
        @JsonProperty("theme_color")
        String getThemeColor();

        @JsonProperty("theme_color")
        void setThemeColor(String value);

        int getVersion();
    }

    @JsonNaming(JsonNaming.NamingStrategy.SNAKE_CASE)
    static class AnnotatedUser {
        private String username;
        @JsonIgnore
        private String password;
        @JsonProperty("full_name")
        private String fullName;

        public AnnotatedUser(String username, String password, String fullName) {
            this.username = username;
            this.password = password;
            this.fullName = fullName;
        }
    }
}
