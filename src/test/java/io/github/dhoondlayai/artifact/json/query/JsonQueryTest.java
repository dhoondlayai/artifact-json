package io.github.dhoondlayai.artifact.json.query;

import io.github.dhoondlayai.artifact.json.model.JsonArray;
import io.github.dhoondlayai.artifact.json.model.JsonObject;
import io.github.dhoondlayai.artifact.json.streaming.FastJsonEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonQuery (SQL Engine) Tests")
public class JsonQueryTest {

    private JsonArray books;

    @BeforeEach
    void setup() {
        String json = """
                [
                  {"title": "The Great Gatsby",  "author": "F. Scott Fitzgerald", "price": 15.99, "inStock": true,  "category": "fiction"},
                  {"title": "1984",              "author": "George Orwell",        "price": 12.50, "inStock": true,  "category": "fiction"},
                  {"title": "Mastering Java",    "author": "AG Team",              "price": 45.00, "inStock": false, "category": "tech"},
                  {"title": "Clean Code",        "author": "Robert Martin",        "price": 35.00, "inStock": true,  "category": "tech"},
                  {"title": "Dune",              "author": "Frank Herbert",        "price": 18.99, "inStock": true,  "category": "sci-fi"},
                  {"title": "Neuromancer",       "author": "William Gibson",       "price": 14.00, "inStock": false, "category": "sci-fi"}
                ]
                """;
        books = (JsonArray) FastJsonEngine.parse(json);
    }

    @Test
    @DisplayName("Basic SELECT and WHERE filtering")
    void testBasicSelectWhere() {
        JsonArray result = JsonQuery.from(books)
                .select("title", "price")
                .whereGt("price", 14.0)
                .whereEq("inStock", true)
                .execute();
        
        assertEquals(3, result.size());
        JsonObject first = (JsonObject) result.get(0);
        assertTrue(first.contains("title"));
        assertTrue(first.contains("price"));
        assertFalse(first.contains("author"));
    }

    @Test
    @DisplayName("ORDER BY sorting")
    void testOrderBy() {
        JsonArray sorted = JsonQuery.from(books)
                .orderBy("price", false) // ascending
                .execute();
        
        assertEquals(12.50, ((JsonObject) sorted.get(0)).getDouble("price").orElse(0.0));
        assertEquals(45.00, ((JsonObject) sorted.get(5)).getDouble("price").orElse(0.0));
    }

    @Test
    @DisplayName("GROUP BY category")
    void testGroupBy() {
        Map<String, JsonArray> grouped = JsonQuery.from(books).groupBy("category");
        
        assertTrue(grouped.containsKey("fiction"));
        assertTrue(grouped.containsKey("tech"));
        assertTrue(grouped.containsKey("sci-fi"));
        assertEquals(2, grouped.get("fiction").size());
    }

    @Test
    @DisplayName("Aggregations: SUM, AVG, MIN, MAX")
    void testAggregations() {
        double sum = JsonQuery.from(books).sum("price");
        double avg = JsonQuery.from(books).avg("price").orElse(0.0);
        double min = JsonQuery.from(books).min("price").orElse(0.0);
        double max = JsonQuery.from(books).max("price").orElse(0.0);
        
        assertTrue(sum > 0);
        assertEquals(12.50, min);
        assertEquals(45.00, max);
    }

    @Test
    @DisplayName("LIMIT result set")
    void testLimit() {
        JsonArray limited = JsonQuery.from(books).limit(3).execute();
        assertEquals(3, limited.size());
    }

    @Test
    @DisplayName("WHERE BETWEEN filtering")
    void testBetween() {
        JsonArray result = JsonQuery.from(books)
                .whereBetween("price", 15.0, 30.0)
                .execute();
        assertEquals(2, result.size()); // Gatsby (15.99) and Dune (18.99)
    }

    @Test
    @DisplayName("Parallel execution support")
    void testParallel() {
        long count = JsonQuery.from(books)
                .parallel()
                .whereGt("price", 10.0)
                .count();
        assertEquals(6, count);
    }

    @Test
    @DisplayName("JOIN two data sets")
    void testJoin() {
        JsonArray customers = new JsonArray();
        customers.add(new JsonObject().put("cid", "C1").put("name", "Alice"));
        
        JsonArray orders = new JsonArray();
        orders.add(new JsonObject().put("oid", "O1").put("customerId", "C1").put("amt", 100));
        
        JsonArray joined = JsonQuery.from(orders)
                .join(customers, "customerId", "cid")
                .execute();
        
        assertEquals(1, joined.size());
        JsonObject first = (JsonObject) joined.get(0);
        assertEquals("Alice", first.getString("name").orElse("?"));
        assertEquals(100, first.get("amt").asInt());
    }
}
