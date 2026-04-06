package io.github.dhoondlayai.artifact.json;

import io.github.dhoondlayai.artifact.json.model.*;
import io.github.dhoondlayai.artifact.json.streaming.FastJsonEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MaxCoverage7Test {

    @Test
    void testModelMethodsExhaustive() {
        JsonObject obj = new JsonObject();
        obj.put("a", 1);
        obj.put("b", "test");
        obj.put("c", new JsonArray().add(1));
        
        assertTrue(obj.containsKey("a"));
        assertFalse(obj.containsKey("z"));
        assertEquals(3, obj.size());
        assertFalse(obj.isEmpty());
        
        obj.remove("b");
        assertEquals(2, obj.size());
        
        JsonObject copy = obj.deepCopy();
        assertEquals(obj, copy);
        assertNotSame(obj, copy);
        assertNotSame(obj.field("c"), copy.field("c"));
        
        JsonObject other = new JsonObject().put("d", 4);
        obj.merge(other);
        assertTrue(obj.containsKey("d"));
        
        // JsonArray
        JsonArray arr = new JsonArray();
        arr.add(10).add(20);
        assertEquals(2, arr.size());
        arr.set(0, new JsonValue(100));
        assertEquals(100, arr.element(0).asInt());
        
        JsonArray extra = new JsonArray(); extra.add(30); extra.add(40);
        arr.addAll(extra);
        assertEquals(4, arr.size());
        
        arr.remove(1);
        assertEquals(3, arr.size());
        
        JsonArray arrCopy = arr.deepCopy();
        assertEquals(arr, arrCopy);
        
        arr.clear();
        assertTrue(arr.isEmpty());
        
        // JsonValue equals/hashCode
        JsonValue v1 = new JsonValue(100);
        JsonValue v2 = new JsonValue(100);
        JsonValue v3 = new JsonValue("100");
        JsonValue vNull = new JsonValue(null);
        
        assertEquals(v1, v2);
        assertNotEquals(v1, v3);
        assertNotEquals(v1, vNull);
        assertEquals(v1.hashCode(), v2.hashCode());
        assertNotEquals(v1.hashCode(), v3.hashCode());
    }

    @Test
    void testFastJsonEngineEdgeCases() {
        // Empty
        assertTrue(FastJsonEngine.parse("{}").isObject());
        assertTrue(FastJsonEngine.parse("[]").isArray());
        
        // Whitespace
        assertEquals(1, FastJsonEngine.parse(" \n\r\t 1 \n\r\t ").asInt());
        
        // Numbers
        assertEquals(0, FastJsonEngine.parse("0").asInt());
        assertEquals(-0.0, FastJsonEngine.parse("-0.0").asDouble());
        assertEquals(Long.MAX_VALUE, FastJsonEngine.parse(String.valueOf(Long.MAX_VALUE)).asLong());
        
        // Comma edge cases
        assertThrows(RuntimeException.class, () -> FastJsonEngine.parse("[1,,2]"));
        assertThrows(RuntimeException.class, () -> FastJsonEngine.parse("{\"a\":1,,}"));
        
        // Unicode escapes
        assertEquals("\u1234", FastJsonEngine.parse("\"\\u1234\"").asText());
    }
}
