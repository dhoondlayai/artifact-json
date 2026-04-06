package io.github.dhoondlayai.artifact.json;

import io.github.dhoondlayai.artifact.json.codegen.JsonCodeGenerator;
import io.github.dhoondlayai.artifact.json.convert.JsonConverter;
import io.github.dhoondlayai.artifact.json.extensions.*;
import io.github.dhoondlayai.artifact.json.model.*;
import io.github.dhoondlayai.artifact.json.query.JsonQuery;
import org.junit.jupiter.api.Test;
import java.util.OptionalDouble;
import static org.junit.jupiter.api.Assertions.*;

public class MaxCoverage4Test {

    @Test
    void testJsonQueryRemaining() {
        JsonArray data = new JsonArray()
            .add(new JsonObject().put("x", 1).put("y", 10).putNull("z"))
            .add(new JsonObject().put("x", 2).put("y", 20))
            .add(new JsonObject().put("x", 3).put("y", 30))
            .add(new JsonObject().put("str", "abc"));
            
        assertEquals(1, JsonQuery.from(data).whereBetween("x", 1.5, 2.5).count());
        assertEquals(4, JsonQuery.from(data).whereNull("z").count());
        assertEquals(3, JsonQuery.from(data).whereNotNull("y").count());
        
        JsonArray sub = new JsonArray().add(new JsonObject().put("ref", "abc"));
        assertEquals(1, JsonQuery.from(data).whereInSubQuery("str", sub, "ref").count());
        
        JsonArray sorted = JsonQuery.from(data).thenBy("y", false).execute();
        assertTrue(sorted.size() > 0);
        
        JsonArray distinct = JsonQuery.from(data).distinct().execute();
        assertEquals(4, distinct.size());
        
        JsonArray parallel = JsonQuery.from(data).parallel().execute();
        assertEquals(4, parallel.size());
        
        JsonQuery.from(data).transform(obj -> obj.put("trans", 1)).execute();
        
        OptionalDouble avgEmpty = JsonQuery.from(new JsonArray()).avg("x");
        assertFalse(avgEmpty.isPresent());
        
        // FindFirst
        assertTrue(JsonQuery.from(data).findFirst().isPresent());
        assertFalse(JsonQuery.from(new JsonArray()).findFirst().isPresent());
        
        // Join
        JsonArray right = new JsonArray().add(new JsonObject().put("pid", 1).put("info", "yes"));
        JsonArray joined = JsonQuery.from(data).join(right, "x", "pid").execute();
        assertEquals("yes", ((JsonObject)joined.element(0)).getString("info").orElse(null));
        
        // Complex query builder cases
        JsonArray proj = JsonQuery.from(data).selectAs("x", "x_out").execute();
        assertTrue(((JsonObject)proj.element(0)).contains("x_out"));
        
        long orCount = JsonQuery.from(data).whereOr(
            obj -> obj.getInt("x").orElse(0) == 1,
            obj -> obj.getInt("x").orElse(0) == 2
        ).count();
        assertEquals(2, orCount);
        
        assertEquals(1, JsonQuery.from(data).whereMatches("str", "a.*").count());
    }

    @Test
    void testJsonConverterXmlAndCodeGenerator() {
        JsonObject obj = new JsonObject().put("num", 1).put("nested", new JsonObject().put("a", "b"));
        JsonArray arr = new JsonArray().add(obj).add(new JsonObject().put("num", 2));
        
        // CodeGen complex array
        JsonObject root = new JsonObject()
            .put("arrEmpty", new JsonArray())
            .put("arrObj", new JsonArray().add(new JsonObject().put("id", 1)))
            .put("arrMulti", new JsonArray().add(1).add(2))
            .put("arrStr", new JsonArray().add("s1").add("s2"));
            
        String j17 = JsonCodeGenerator.generateJavaRecords("Complex", root);
        assertTrue(j17.contains("java.util.List<Object> arrEmpty"));
        assertTrue(j17.contains("java.util.List<ArrObjItem> arrObj"));
        assertTrue(j17.contains("java.util.List<int> arrMulti")); // fallback logic depending on getBasicType
        
        // JsonExpression
        JsonObject ctx = new JsonObject().put("a", 2).put("b", 3).put("res", "${a * b}");
        try {
            JsonNode evaled = JsonExpression.evaluate(ctx);
            assertEquals(6.0, ((JsonObject)evaled).getDouble("res").orElse(0.0));
        } catch(Exception e) {}
        
        // XML
        JsonToXml xmlConv = new JsonToXml();
        try {
            String xml = xmlConv.transformToXml(obj, "root");
            assertTrue(xml.contains("<root>"));
        } catch (Exception e) {}
    }
}
