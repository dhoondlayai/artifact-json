package io.github.dhoondlayai.artifact.json;

import io.github.dhoondlayai.artifact.json.model.*;
import io.github.dhoondlayai.artifact.json.query.*;
import io.github.dhoondlayai.artifact.json.codegen.*;
import io.github.dhoondlayai.artifact.json.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Max Coverage Test 1 - Models and Queries")
public class MaxCoverage1Test {

    @Test
    @DisplayName("Exhaustive JsonArray coverage")
    void testJsonArray() {
        JsonArray arr = new JsonArray();
        arr.add("string").add(10).add(true).addNull();
        assertEquals(4, arr.size());

        arr.addAt(1, new JsonValue("inserted"));
        assertEquals("inserted", arr.element(1).asText());

        arr.set(1, new JsonValue("replaced"));
        assertEquals("replaced", arr.element(1).asText());

        JsonNode removed = arr.remove(1);
        assertEquals("replaced", removed.asText());

        assertTrue(arr.remove(new JsonValue(10)));
        assertFalse(arr.remove(new JsonValue(99)));

        List<JsonNode> srcList = new ArrayList<>();
        srcList.add(new JsonValue("a"));
        srcList.add(new JsonValue("b"));
        JsonArray arr2 = new JsonArray(srcList);
        arr.addAll(arr2);
        
        arr2.clear();
        assertEquals(0, arr2.size());

        assertEquals(new JsonValue("default"), arr.elementOrDefault(99, new JsonValue("default")));
        assertNull(arr.elementOrDefault(99, null));

        JsonArray sub = arr.subArray(1, 3);
        assertEquals(2, sub.size());

        JsonArray toSort = new JsonArray().add(3).add(1).add(2);
        toSort.sort(Comparator.comparingInt(n -> n.asInt()));
        assertEquals(1, toSort.element(0).asInt());
        
        toSort.reverse();
        assertEquals(3, toSort.element(0).asInt());
        
        // Pagination
        JsonArray paged = toSort.page(1, 2);
        assertEquals(1, paged.size());
        assertEquals(1, paged.element(0).asInt());

        // Grouping
        JsonArray users = new JsonArray()
            .add(new JsonObject().put("role", "admin").put("id", 1))
            .add(new JsonObject().put("role", "admin").put("id", 2))
            .add(new JsonObject().put("role", "user").put("id", 3));
        Map<String, JsonArray> grouped = users.groupBy("role");
        assertEquals(2, grouped.get("admin").size());
        assertEquals(1, grouped.get("user").size());
        
        // Iteration
        int count = 0;
        for (JsonNode n : toSort) count++;
        assertEquals(3, count);
        
        // Type testing
        assertFalse(toSort.isObject());
        assertTrue(toSort.isArray());
        assertFalse(toSort.isValue());
        assertEquals("[3,2,1]", toSort.toString());
    }

    @Test
    @DisplayName("Exhaustive JsonObject coverage")
    void testJsonObject() {
        JsonObject obj = new JsonObject(10);
        obj.put("str", "value").put("num", 42).put("bool", false).putNull("nl");
        assertEquals(4, obj.size());
        assertFalse(obj.isEmpty());
        
        assertTrue(obj.contains("str"));
        assertFalse(obj.contains("missing"));
        
        JsonNode removed = obj.remove("str");
        assertEquals("value", removed.asText());
        assertFalse(obj.contains("str"));
        
        JsonObject obj2 = new JsonObject();
        obj2.put("new_key", "x");
        obj.merge(obj2);
        assertTrue(obj.contains("new_key"));
        
        assertEquals(new JsonValue("default"), obj.getOrDefault("missing", new JsonValue("default")));
        
        assertEquals("x", obj.getString("new_key").orElse(""));
        assertEquals(42, obj.getInt("num").orElse(0));
        assertEquals(42.0, obj.getDouble("num").orElse(0.0));
        assertEquals(false, obj.getBoolean("bool").orElse(true));
        
        assertFalse(obj.getString("num").isPresent()); // Not a string
        assertFalse(obj.getInt("bool").isPresent());
        assertFalse(obj.getDouble("bool").isPresent());
        assertFalse(obj.getBoolean("num").isPresent());
        
        obj.clear();
        assertEquals(0, obj.size());
        
        // Type testing
        assertTrue(obj.isObject());
        assertFalse(obj.isArray());
        assertFalse(obj.isValue());
    }

    @Test
    @DisplayName("Exhaustive JsonNode general coverage")
    void testJsonNode() {
        JsonNode n1 = new JsonValue("test");
        JsonNode n2 = new JsonValue("test");
        assertEquals(n1, n1);
        assertEquals(n1, n2);
        assertEquals(n1.hashCode(), n2.hashCode());
        
        assertTrue(n1.isString());
        assertFalse(n1.isNumber());
        assertFalse(n1.isBoolean());
        assertFalse(n1.isNull());
        
        JsonNode num = new JsonValue(123.45);
        assertTrue(num.isNumber());
        assertFalse(num.isString());
        
        JsonNode bool = new JsonValue(true);
        assertTrue(bool.isBoolean());
        
        JsonNode nul = new JsonValue(null);
        assertTrue(nul.isNull());
        
        // Find
        JsonObject root = new JsonObject().put("a", new JsonObject().put("b", new JsonArray().add(10).add(20)));
        assertEquals(20, root.find("a.b[1]").get().asInt());
        assertFalse(root.find("a.c").isPresent());
        assertFalse(root.find("a.b[5]").isPresent());
        
        // Equals
        assertNotEquals(root, null);
        assertNotEquals(root, new Object());
        
        JsonObject root2 = new JsonObject().put("a", new JsonObject().put("b", new JsonArray().add(10).add(20)));
        assertEquals(root, root2);
        
        // JsonTraversal
        List<JsonNode> nodes = root.findAll("b");
        assertEquals(1, nodes.size());
        assertEquals(10, ((JsonArray)nodes.get(0)).element(0).asInt());
    }

    @Test
    @DisplayName("Exhaustive JsonQuery coverage")
    void testJsonQuery() {
        JsonArray data = new JsonArray()
            .add(new JsonObject().put("cat", "A").put("val", 10))
            .add(new JsonObject().put("cat", "B").put("val", 20))
            .add(new JsonObject().put("cat", "A").put("val", 30))
            .add(new JsonObject().put("str1", "hello").put("str2", "ello"));
            
        // Filter
        JsonArray q1 = JsonQuery.from(data)
            .whereEq("cat", "A").execute();
        assertEquals(2, q1.size());
        
        JsonArray q2 = JsonQuery.from(data)
            .whereGt("val", 15).execute();
        assertEquals(2, q2.size());
        
        JsonArray q3 = JsonQuery.from(data)
            .whereLt("val", 25).execute();
        assertEquals(3, q3.size());
        
        JsonArray q4 = JsonQuery.from(data)
            .whereGte("val", 30).execute();
        assertEquals(1, q4.size());
        
        JsonArray q5 = JsonQuery.from(data)
            .whereLte("val", 10).execute();
        assertEquals(2, q5.size()); // val=10, and str-only row (numericOf=0.0)
        
        JsonArray q6 = JsonQuery.from(data)
            .whereNotEq("cat", "B").execute();
        assertEquals(3, q6.size());
        
        JsonArray q7 = JsonQuery.from(data)
            .whereContains("str1", "ell").execute();
        assertEquals(1, q7.size());
        
        JsonArray q8 = JsonQuery.from(data)
            .whereStartsWith("str1", "he").execute();
        assertEquals(1, q8.size());
        
        JsonArray q9 = JsonQuery.from(data)
            .whereEndsWith("str1", "lo").execute();
        assertEquals(1, q9.size());
        
        JsonArray q10 = JsonQuery.from(data)
            .whereIn("cat", "A", "C").execute();
        assertEquals(2, q10.size());
        
        // Sort and Page
        JsonArray q11 = JsonQuery.from(data)
            .whereEq("cat", "A")
            .orderBy("val", false) // asc
            .limit(1)
            .execute();
        assertEquals(10, ((JsonObject)q11.element(0)).getInt("val").orElse(0));
        
        JsonArray q12 = JsonQuery.from(data)
            .orderBy("val", true) // desc
            .execute();
        assertEquals(30, ((JsonObject)q12.element(0)).getInt("val").orElse(0));
        
        // Aggregate
        double sum = JsonQuery.from(data).whereEq("cat", "A").sum("val");
        assertEquals(40.0, sum);
        
        double max = JsonQuery.from(data).max("val").orElse(0);
        assertEquals(30.0, max);
        
        double min = JsonQuery.from(data).min("val").orElse(0);
        
        JsonArray trimmed = new JsonArray().add(data.element(0)).add(data.element(1)).add(data.element(2));
        assertEquals(10.0, JsonQuery.from(trimmed).min("val").orElse(0));
        assertEquals(20.0, JsonQuery.from(trimmed).avg("val").orElse(0));
        assertEquals(3, JsonQuery.from(trimmed).count());
        
        // Error cases
        assertThrows(NullPointerException.class, () -> JsonQuery.from(null));
    }

    @Test
    @DisplayName("Exhaustive JsonCodeGenerator coverage")
    void testCodeGenerator() {
        JsonObject schema = new JsonObject()
            .put("id", 1)
            .put("name", "test")
            .put("active", true)
            .put("price", 9.99)
            .put("tags", new JsonArray().add("java"))
            .put("nested", new JsonObject().put("x", 1));
            
        String j17 = JsonCodeGenerator.generateJavaRecords("Root", schema);
        assertTrue(j17.contains("int id"));
        assertTrue(j17.contains("String name"));
        assertTrue(j17.contains("boolean active"));
        assertTrue(j17.contains("double price"));
        assertTrue(j17.contains("List<String> tags"));
        assertTrue(j17.contains("Nested nested"));
        assertTrue(j17.contains("public record Nested"));
    }
}
