package io.github.dhoondlayai.artifact.json.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonObject and JsonArray (Core model) Tests")
public class JsonObjectArrayTest {

    @Test
    @DisplayName("JsonObject manipulation: put, get, rename, remove")
    void testJsonObjectManipulation() {
        JsonObject obj = new JsonObject();
        obj.put("id", 101L);
        obj.put("user", "testUser");
        obj.put("active", true);
        
        assertEquals(101L, obj.getLong("id").orElse(0L));
        assertEquals("testUser", obj.getString("user").orElse("?"));
        assertTrue(obj.getBoolean("active").orElse(false));
        
        obj.rename("user", "username");
        assertTrue(obj.contains("username"));
        assertFalse(obj.contains("user"));
        
        obj.remove("id");
        assertFalse(obj.contains("id"));
        
        obj.putIfAbsent("username", new JsonValue("duplicate"));
        assertEquals("testUser", obj.getString("username").orElse("?")); // No change
    }

    @Test
    @DisplayName("JsonArray manipulation: add, sort, reverse, distinct, page")
    void testJsonArrayManipulation() {
        JsonArray arr = new JsonArray();
        arr.add("apple").add("banana").add("apple").add("cherry");
        
        assertEquals(4, arr.size());
        assertEquals(3, arr.distinct().size());
        
        arr = new JsonArray().add(30).add(10).add(50).add(20);
        JsonArray sorted = arr.sort(Comparator.comparingInt(JsonNode::asInt));
        assertEquals(10, sorted.get(0).asInt());
        assertEquals(50, sorted.get(3).asInt());
        
        JsonArray reversed = sorted.reverse();
        assertEquals(50, reversed.get(0).asInt());
        assertEquals(10, reversed.get(3).asInt());
        
        JsonArray paged = reversed.page(1, 2);
        assertEquals(2, paged.size());
        assertEquals(20, paged.get(0).asInt());
    }

    @Test
    @DisplayName("JsonNode safe conversion methods")
    void testJsonNodeMethods() {
        JsonValue valNum = new JsonValue(123);
        JsonValue valStr = new JsonValue("hello");
        JsonValue valBool = new JsonValue(true);
        JsonValue valNull = new JsonValue(null);
        
        assertEquals(123, valNum.asInt());
        assertEquals("hello", valStr.asText());
        assertTrue(valBool.asBoolean());
        assertTrue(valNull.isNull());
        
        assertFalse(valNum.isObject());
        assertFalse(valNum.isArray());
    }

    @Test
    @DisplayName("Path finding in JSON structure")
    void testPathFinding() {
        JsonObject child = new JsonObject().put("leaf", "final");
        JsonObject parent = new JsonObject().put("branch", child);
        JsonObject root = new JsonObject().put("tree", parent);
        
        Optional<JsonNode> result = root.find("tree.branch.leaf");
        assertTrue(result.isPresent());
        assertEquals("final", result.get().asText());
        
        assertFalse(root.find("tree.missing.leaf").isPresent());
    }
}
