package io.github.dhoondlayai.artifact.json.streaming;

import io.github.dhoondlayai.artifact.json.model.JsonArray;
import io.github.dhoondlayai.artifact.json.model.JsonNode;
import io.github.dhoondlayai.artifact.json.model.JsonObject;
import io.github.dhoondlayai.artifact.json.exception.JsonParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FastJsonEngine (RFC 8259 Parser) Tests")
public class FastJsonEngineTest {

    @Test
    @DisplayName("Parse simple JSON object")
    void testParseSimpleObject() {
        String json = "{\"name\":\"Artifact\", \"version\": 2.0, \"active\": true}";
        JsonNode node = FastJsonEngine.parse(json);
        
        assertTrue(node instanceof JsonObject);
        JsonObject obj = (JsonObject) node;
        assertEquals("Artifact", obj.getString("name").orElse(null));
        assertEquals(2.0, obj.getDouble("version").orElse(0.0));
        assertTrue(obj.getBoolean("active").orElse(false));
    }

    @Test
    @DisplayName("Parse complex nested structure")
    void testParseComplexNested() {
        String json = """
                {
                  "library": "artifact-json",
                  "features": ["parsing", "querying", "converting"],
                  "specs": {
                    "zeroDependency": true,
                    "performance": "high"
                  }
                }
                """;
        JsonNode node = FastJsonEngine.parse(json);
        
        assertEquals("artifact-json", node.get("library").asText());
        assertTrue(node.get("features") instanceof JsonArray);
        assertEquals(3, node.get("features").size());
        assertEquals("parsing", node.get("features").get(0).asText());
        assertTrue(node.get("specs").get("zeroDependency").asBoolean());
    }

    @Test
    @DisplayName("Parse empty array and object")
    void testParseEmpty() {
        assertEquals(0, FastJsonEngine.parse("{}").size());
        assertEquals(0, FastJsonEngine.parse("[]").size());
    }

    @Test
    @DisplayName("Handle null in JSON")
    void testParseNull() {
        String json = "{\"owner\": null}";
        JsonNode node = FastJsonEngine.parse(json);
        assertTrue(node.get("owner").isNull());
    }

    @Test
    @DisplayName("Malformed JSON should throw JsonParseException")
    void testMalformedJson() {
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("{key: \"missing quotes\"}"));
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("{\"key\": }"));
        assertThrows(JsonParseException.class, () -> FastJsonEngine.parse("{\"key\": \"unclosed\""));
    }

    @Test
    @DisplayName("Large number support")
    void testLargeNumbers() {
        String json = "{\"id\": 9223372036854775807}";
        JsonNode node = FastJsonEngine.parse(json);
        assertEquals(9223372036854775807L, node.get("id").asLong());
    }
}
