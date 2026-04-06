package io.github.dhoondlayai.artifact.json;

import io.github.dhoondlayai.artifact.json.audit.JsonAuditor;
import io.github.dhoondlayai.artifact.json.extensions.*;
import io.github.dhoondlayai.artifact.json.model.*;
import io.github.dhoondlayai.artifact.json.proxy.JsonProxy;
import io.github.dhoondlayai.artifact.json.annotation.JsonProperty;
import io.github.dhoondlayai.artifact.json.validation.JsonValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Coverage Boost – Extensions, Proxy, Validation, Audit, Xml")
public class CoverageBoostTest {

    // ─── JsonExtensions ────────────────────────────────────────────────────

    @Test
    @DisplayName("JsonExtensions – all wildcard branches")
    void testJsonExtensionsWildcards() {
        JsonExtensions ext = new JsonExtensions();

        // '*' on a JsonObject – covers the 'star on object' branch
        JsonObject root = new JsonObject()
                .put("a", 1)
                .put("b", 2);
        List<JsonNode> all = ext.wildcardFind(root, "*");
        assertEquals(2, all.size());

        // '*' on a JsonArray – covers the 'star on array' branch
        JsonArray arr = new JsonArray().add("x").add("y");
        JsonObject wrapper = new JsonObject().put("items", arr);
        // direct wildcard on array node:  put array as root for one-segment path
        List<JsonNode> arrAll = ext.wildcardFind(arr, "*");
        assertEquals(2, arrAll.size());

        // '[*]' with explicit key — covers the segment.endsWith("[*]") branch
        List<JsonNode> items = ext.wildcardFind(wrapper, "items[*]");
        assertEquals(2, items.size());

        // '[*]' with empty key prefix (root is array) — covers key.isEmpty() branch
        List<JsonNode> direct = ext.wildcardFind(arr, "[*]");
        assertEquals(2, direct.size());

        // plain segment miss — covers the null-return branch
        List<JsonNode> miss = ext.wildcardFind(root, "missing");
        assertEquals(0, miss.size());

        // searchByKey
        JsonObject deep = new JsonObject()
                .put("user", new JsonObject().put("email", "a@b.com"))
                .put("email", "root@b.com");
        List<JsonNode> emails = ext.searchByKey(deep, "email");
        assertEquals(2, emails.size());

        // searchByValue
        List<JsonNode> found = ext.searchByValue(deep, "a@b.com");
        assertEquals(1, found.size());

        // snakeToCamel / camelToSnake
        assertEquals("helloWorld", ext.snakeToCamel("hello_world"));
        assertEquals("hello_world", ext.camelToSnake("helloWorld"));

        // inferSchema
        JsonNode schema = ext.inferSchema(SampleDto.class);
        assertTrue(schema.isObject());
        assertTrue(schema.find("properties").isPresent());

        // fixToJson
        JsonNode fix = ext.fixToJson("35=D|49=SENDER|56=TARGET");
        assertTrue(fix.find("tag_35").isPresent());

        // parseSelfHealing – valid after healing
        JsonNode healed = ext.parseSelfHealing("{name: 'Alice',}");
        assertTrue(healed.isObject());

        // parseSelfHealing – null input
        JsonNode nullResult = ext.parseSelfHealing(null);
        assertTrue(nullResult.isNull());

        // parseSelfHealing – truly unparseable fallback
        JsonNode failed = ext.parseSelfHealing("{{{{{{{");
        assertTrue(failed.isValue());
        assertTrue(failed.asText().startsWith("PARSE_FAILED"));
    }

    static class SampleDto {
        public String name;
        public int age;
        public boolean active;
        public double score;
        public Object misc;
    }

    // ─── JsonShield ────────────────────────────────────────────────────────

    @Test
    @DisplayName("JsonShield – all typed getters + redact")
    void testJsonShieldFull() {
        JsonObject cfg = new JsonObject()
                .put("host", "localhost")
                .put("port", 8080)
                .put("ratio", 1.5)
                .put("debug", true)
                .put("secret", "abc");
        JsonShield shield = new JsonShield(cfg);

        // Existing paths
        assertEquals("localhost", shield.getString("host", "x"));
        assertEquals(8080, shield.getInt("port", 0));
        assertEquals(1.5, shield.getDouble("ratio", 0.0));
        assertTrue(shield.getBoolean("debug", false));
        assertEquals(8080L, shield.getLong("port", 0L));

        // Missing paths → defaults
        assertEquals("def", shield.getString("missing", "def"));
        assertEquals(99, shield.getInt("missing", 99));
        assertEquals(3.14, shield.getDouble("missing", 3.14));
        assertFalse(shield.getBoolean("missing", false));
        assertEquals(0L, shield.getLong("missing", 0L));

        // get() + exists()
        assertTrue(shield.get("host").isPresent());
        assertFalse(shield.get("nope").isPresent());
        assertTrue(shield.exists("port"));
        assertFalse(shield.exists("nope"));

        // redact – covers JsonObject and array branches
        JsonObject withArr = new JsonObject()
                .put("password", "s3cr3t")
                .put("tags", new JsonArray().add("a").add("b"))
                .put("safe", "ok");
        JsonShield s = new JsonShield(withArr);
        JsonNode redacted = s.redact("password");
        assertEquals("[REDACTED]", redacted.find("password").map(JsonNode::asText).orElse("x"));
        assertEquals("ok", redacted.find("safe").map(JsonNode::asText).orElse("x"));
    }

    // ─── JsonMasker ────────────────────────────────────────────────────────

    @Test
    @DisplayName("JsonMasker – nested object and array masking")
    void testJsonMaskerDeep() {
        JsonMasker masker = new JsonMasker("[X]").addSensitiveKey("pin");

        // Nested masking inside a JsonObject
        JsonObject data = new JsonObject()
                .put("user", new JsonObject().put("pin", "1234").put("name", "Bob"))
                .put("pin", "9999");
        JsonNode masked = masker.mask(data);
        assertEquals("[X]", masked.find("pin").map(JsonNode::asText).orElse("?"));

        // Array elements are traversed
        JsonArray arr = new JsonArray()
                .add(new JsonObject().put("pin", "111").put("x", 1))
                .add(new JsonValue("leaf"));
        JsonNode maskedArr = masker.mask(arr);
        assertTrue(maskedArr.isArray());

        // Leaf value passthrough (non-sensitive)
        JsonNode leaf = masker.mask(new JsonValue("hello"));
        assertEquals("hello", leaf.asText());
    }

    // ─── JsonPatchGenerator ────────────────────────────────────────────────

    @Test
    @DisplayName("JsonPatchGenerator – remove, array-replace, primitive-replace branches")
    void testPatchGeneratorBranches() {
        // Remove branch: key in source not in target
        JsonObject src = new JsonObject().put("a", 1).put("removed", 2);
        JsonObject tgt = new JsonObject().put("a", 1);
        JsonArray patch = JsonPatchGenerator.generatePatch(src, tgt);
        assertTrue(patch.stream().anyMatch(n -> n.get("op").asText().equals("remove")));

        // Array-replace branch
        JsonArray a1 = new JsonArray().add(1).add(2);
        JsonArray a2 = new JsonArray().add(3);
        JsonArray arrPatch = JsonPatchGenerator.generatePatch(a1, a2);
        assertTrue(arrPatch.stream().anyMatch(n -> n.get("op").asText().equals("replace")));

        // Primitive-replace branch (value diff at root)
        JsonNode v1 = new JsonValue(1);
        JsonNode v2 = new JsonValue(2);
        JsonArray primPatch = JsonPatchGenerator.generatePatch(v1, v2);
        assertEquals(1, primPatch.size());
        assertEquals("replace", primPatch.element(0).get("op").asText());

        // Equal nodes → empty patch
        JsonArray emptyPatch = JsonPatchGenerator.generatePatch(new JsonValue("x"), new JsonValue("x"));
        assertEquals(0, emptyPatch.size());

        // path escaping: key with ~ and /
        JsonObject with = new JsonObject().put("a~b/c", "old");
        JsonObject without = new JsonObject().put("a~b/c", "new");
        JsonArray escaped = JsonPatchGenerator.generatePatch(with, without);
        assertTrue(escaped.stream().anyMatch(n -> n.get("path").asText().contains("~0")));
    }

    // ─── JsonToXml ────────────────────────────────────────────────────────

    @Test
    @DisplayName("JsonToXml – object, array, value, and escape branches")
    void testJsonToXml() {
        // Object branch
        JsonObject obj = new JsonObject().put("name", "Alice").put("age", 30);
        String xml = JsonToXml.transformToXml(obj, "user");
        assertTrue(xml.contains("<user>"));
        assertTrue(xml.contains("<name>Alice</name>"));
        assertTrue(xml.contains("</user>"));

        // Array branch (items tag)
        JsonArray arr = new JsonArray().add("x").add("y");
        String arrXml = JsonToXml.transformToXml(arr, "list");
        assertTrue(arrXml.contains("<item>"));

        // Value with XML-special chars – escaping branch
        JsonObject special = new JsonObject().put("v", "<a & 'b' \"c\">");
        String escXml = JsonToXml.transformToXml(special, "r");
        assertTrue(escXml.contains("&lt;"));
        assertTrue(escXml.contains("&amp;"));
    }

    // ─── JsonExpression ────────────────────────────────────────────────────

    @Test
    @DisplayName("JsonExpression – object, array, multiplication, path-miss branches")
    void testJsonExpression() {
        // Object with expression multiplication
        JsonObject ctx = new JsonObject()
                .put("price", 10)
                .put("qty", 3)
                .put("total", "${price * qty}");
        JsonNode result = JsonExpression.evaluate(ctx);
        assertEquals(30.0, ((JsonObject) result).getDouble("total").orElse(0.0));

        // Path lookup (no *)
        JsonObject ctx2 = new JsonObject()
                .put("val", 42)
                .put("ref", "${val}");
        JsonNode r2 = JsonExpression.evaluate(ctx2);
        assertEquals(42, ((JsonObject) r2).getInt("ref").orElse(0));

        // Array branch
        JsonArray arr = new JsonArray()
                .add(new JsonObject().put("n", "${x}"))
                .add(new JsonValue("literal"));
        JsonNode arrResult = JsonExpression.evaluate(arr);
        assertTrue(arrResult.isArray());

        // Non-expression value (no match) – passthrough branch
        JsonObject plain = new JsonObject().put("k", "hello");
        JsonNode plainResult = JsonExpression.evaluate(plain);
        assertEquals("hello", ((JsonObject) plainResult).getString("k").orElse(""));
    }

    // ─── JsonAuditor ──────────────────────────────────────────────────────

    @Test
    @DisplayName("JsonAuditor – trackChange, getHistory, printReport")
    void testJsonAuditor() {
        JsonAuditor auditor = new JsonAuditor();
        auditor.trackChange("UPDATE", "/name", "Alice", "Bob");
        auditor.trackChange("DELETE", "/age", 30, null);

        List<JsonAuditor.AuditLog> history = auditor.getHistory();
        assertEquals(2, history.size());
        assertEquals("UPDATE", history.get(0).action());
        assertEquals("/name", history.get(0).path());
        assertEquals("Alice", history.get(0).oldValue());
        assertEquals("Bob", history.get(0).newValue());
        assertNotNull(history.get(0).timestamp());

        // printReport just prints – ensure it doesn't throw
        assertDoesNotThrow(auditor::printReport);
    }

    // ─── JsonValidator ────────────────────────────────────────────────────

    @Test
    @DisplayName("JsonValidator – isString, isNumber, require, missing path")
    void testJsonValidator() {
        JsonObject data = new JsonObject()
                .put("name", "Alice")
                .put("age", 30);

        JsonValidator validator = new JsonValidator()
                .isString("name")
                .isNumber("age")
                .isString("missing"); // should fail

        List<String> violations = validator.verify(data);
        assertEquals(1, violations.size());
        assertEquals("missing", violations.get(0));

        // All pass
        List<String> ok = new JsonValidator()
                .isString("name")
                .isNumber("age")
                .verify(data);
        assertTrue(ok.isEmpty());

        // Failing type check (age is a number, not string)
        List<String> typeViolation = new JsonValidator()
                .isString("age")
                .verify(data);
        assertEquals(1, typeViolation.size());

        // require() with custom predicate
        List<String> custom = new JsonValidator()
                .require("name", n -> n.asText().equals("Bob"))
                .verify(data);
        assertEquals(1, custom.size());
    }

    // ─── JsonProxy ────────────────────────────────────────────────────────

    interface UserProxy {
        String getName();
        void setName(String name);
        int getAge();
        void setAge(int age);
        boolean isActive();
        double getScore();
        String toString();
        @JsonProperty("email_address")
        String getEmail();
    }

    interface AddressProxy {
        String getStreet();
    }

    interface CustomOp {
        void execute();
    }

    @Test
    @DisplayName("JsonProxy – getters, setters, toString, nested proxy, JsonProperty")
    void testJsonProxy() {
        JsonObject node = new JsonObject()
                .put("name", "Alice")
                .put("age", 30)
                .put("active", true)
                .put("score", 9.5)
                .put("email_address", "alice@example.com");

        UserProxy user = JsonProxy.create(UserProxy.class, node);

        // Basic getters
        assertEquals("Alice", user.getName());
        assertEquals(30, user.getAge());
        assertTrue(user.isActive());
        assertEquals(9.5, user.getScore());

        // @JsonProperty mapping
        assertEquals("alice@example.com", user.getEmail());

        // toString delegates to backingNode
        assertTrue(user.toString().contains("Alice"));

        // Setters – String
        user.setName("Bob");
        assertEquals("Bob", user.getName());

        // Setters – Number
        user.setAge(25);
        assertEquals(25, user.getAge());

        // Setter with null
        user.setName(null);
        assertNull(user.getName());

        // Nested interface proxy
        JsonObject addrNode = new JsonObject().put("street", "Main St");
        node.put("address", addrNode);
        // Can't call getAddress() via UserProxy but create one directly
        AddressProxy addr = JsonProxy.create(AddressProxy.class, addrNode);
        assertEquals("Main St", addr.getStreet());

        // UnsupportedOperationException for unknown methods (not get/set/is/toString)
        CustomOp cp = JsonProxy.create(CustomOp.class, new JsonObject());
        assertThrows(UnsupportedOperationException.class, cp::execute);
    }

    // ─── Exceptions ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Exceptions – trigger all constructors")
    void testExceptions() {
        // JsonMappingException
        io.github.dhoondlayai.artifact.json.exception.JsonMappingException m1 = new io.github.dhoondlayai.artifact.json.exception.JsonMappingException("basic");
        assertNull(m1.getFieldName());
        assertNull(m1.getTargetType());

        io.github.dhoondlayai.artifact.json.exception.JsonMappingException m2 = new io.github.dhoondlayai.artifact.json.exception.JsonMappingException("full", "field1", String.class);
        assertEquals("field1", m2.getFieldName());
        assertEquals(String.class, m2.getTargetType());

        io.github.dhoondlayai.artifact.json.exception.JsonMappingException m3 = new io.github.dhoondlayai.artifact.json.exception.JsonMappingException("cause", new RuntimeException());
        assertNotNull(m3.getCause());
        assertNull(m3.getFieldName());

        // JsonConversionException
        io.github.dhoondlayai.artifact.json.exception.JsonConversionException c1 = new io.github.dhoondlayai.artifact.json.exception.JsonConversionException("JSON", "XML", "failure");
        assertEquals("JSON", c1.getSourceFormat());
        assertEquals("XML", c1.getTargetFormat());

        io.github.dhoondlayai.artifact.json.exception.JsonConversionException c2 = new io.github.dhoondlayai.artifact.json.exception.JsonConversionException("cause", new RuntimeException());
        assertNull(c2.getSourceFormat());

        // Exception base
        io.github.dhoondlayai.artifact.json.exception.JsonException e1 = new io.github.dhoondlayai.artifact.json.exception.JsonException("test");
        io.github.dhoondlayai.artifact.json.exception.JsonException e2 = new io.github.dhoondlayai.artifact.json.exception.JsonException("test", new Exception());
        assertEquals("test", e1.getMessage());
        assertEquals("test", e2.getMessage());
    }

    // ─── JsonCodeGenerator ──────────────────────────────────────────────────

    @Test
    @DisplayName("JsonCodeGenerator – array edge cases")
    void testJsonCodeGenerator() {
        // Array of value types
        JsonObject valArr = new JsonObject().put("list", new JsonArray().add("string"));
        String cl1 = io.github.dhoondlayai.artifact.json.codegen.JsonCodeGenerator.generateJavaRecords("Cl1", valArr);
        assertTrue(cl1.contains("List<String>"));

        // Empty array
        JsonObject emptyArr = new JsonObject().put("list", new JsonArray());
        String cl2 = io.github.dhoondlayai.artifact.json.codegen.JsonCodeGenerator.generateJavaRecords("Cl2", emptyArr);
        assertTrue(cl2.contains("List<Object>"));

        // Null value
        JsonObject hasNull = new JsonObject().put("val", new JsonValue(null));
        String cl3 = io.github.dhoondlayai.artifact.json.codegen.JsonCodeGenerator.generateJavaRecords("Cl3", hasNull);
        assertTrue(cl3.contains("Object val"));

        // Array root
        JsonArray rootArr = new JsonArray().add(new JsonObject().put("id", 1));
        String cl4 = io.github.dhoondlayai.artifact.json.codegen.JsonCodeGenerator.generateJavaRecords("Cl4", rootArr);
        assertTrue(cl4.contains("Auto-generated"));

        // Snake case property names to camelCase record names
        JsonObject snake = new JsonObject().put("my_field_name", 123);
        String cl5 = io.github.dhoondlayai.artifact.json.codegen.JsonCodeGenerator.generateJavaRecords("Cl5", snake);
        assertTrue(cl5.contains("int myFieldName"));
    }
}
