package io.github.dhoondlayai.artifact.json;

import io.github.dhoondlayai.artifact.json.audit.JsonAuditor;
import io.github.dhoondlayai.artifact.json.convert.JsonConverter;
import io.github.dhoondlayai.artifact.json.databind.FastObjectMapper;
import io.github.dhoondlayai.artifact.json.exception.*;
import io.github.dhoondlayai.artifact.json.model.JsonArray;
import io.github.dhoondlayai.artifact.json.model.JsonObject;
import io.github.dhoondlayai.artifact.json.streaming.JsonStreamReader;
import io.github.dhoondlayai.artifact.json.validation.JsonValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Comprehensive Coverage Extension Tests")
public class ComprehensiveCoverageTest {

    @Test
    @DisplayName("JsonValidator rules and verification")
    void testValidator() {
        JsonObject root = new JsonObject().put("id", 1).put("name", "Artifact");
        JsonValidator validator = new JsonValidator()
                .isNumber("id")
                .isString("name")
                .require("version", n -> false); // Missing
        
        List<String> violations = validator.verify(root);
        assertEquals(1, violations.size());
        assertEquals("version", violations.get(0));
    }

    @Test
    @DisplayName("JsonAuditor logging changes")
    void testAuditor() {
        JsonObject obj = new JsonObject().put("status", "init");
        JsonAuditor auditor = new JsonAuditor();
        
        auditor.trackChange("update", "status", "init", "ready");
        List<JsonAuditor.AuditLog> logs = auditor.getHistory();
        assertFalse(logs.isEmpty());
        assertNotNull(logs.get(0).timestamp());
        assertEquals("update", logs.get(0).action());
        assertEquals("status", logs.get(0).path());
        
        auditor.printReport(); // Hit print coverage
    }

    @Test
    @DisplayName("JsonStreamReader reading content")
    void testStreamReader() throws IOException {
        String json = "[{\"a\": 1}]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
        try (JsonStreamReader reader = new JsonStreamReader(bais)) {
            reader.streamArray(node -> {
                assertEquals(1, ((JsonObject) node).getInt("a").orElse(0));
            });
        }
        
        bais = new ByteArrayInputStream("{\"a\": 1}".getBytes());
        try (JsonStreamReader reader = new JsonStreamReader(bais)) {
            assertEquals(1, ((JsonObject) reader.readFull()).getInt("a").orElse(0));
        }
        
        // Filtered array
        bais = new ByteArrayInputStream("[{\"id\":1},{\"id\":2}]".getBytes());
        try (JsonStreamReader reader = new JsonStreamReader(bais)) {
            reader.streamFilteredArray("id", "1", node -> assertEquals(1, ((JsonObject)node).getInt("id").orElse(0)));
        }
    }

    @Test
    @DisplayName("Exception hierarchy coverage")
    void testExceptions() {
        assertNotNull(new JsonParseException("msg", 1, 1));
        assertNotNull(new JsonPathException("path", "msg"));
        assertNotNull(new JsonTypeException("expected", "actual"));
        assertNotNull(new JsonMappingException("msg", "field", String.class));
        assertNotNull(new JsonConversionException("from", "to", "reason"));
        assertNotNull(new JsonQueryException("msg"));
        assertNotNull(new JsonException("msg"));
    }

    @Test
    @DisplayName("XML conversion coverage")
    void testToXml() {
        JsonObject obj = new JsonObject().put("child", "val");
        String xml = JsonConverter.toXml(obj, "root");
        assertTrue(xml.contains("<root>"));
        assertTrue(xml.contains("<child>val</child>"));
        
        // Extension direct call
        String xml2 = io.github.dhoondlayai.artifact.json.extensions.JsonToXml.transformToXml(obj, "root");
        assertTrue(xml2.contains("<child>val</child>"));

        // YAML coverage
        String yaml = JsonConverter.toYaml(obj);
        assertTrue(yaml.contains("child: val"));
    }

    @Test
    @DisplayName("JsonExpression live evaluation")
    void testExpression() {
        JsonObject data = new JsonObject()
                .put("price", 10.0)
                .put("qty", 5)
                .put("total", "${price * qty}");
        
        JsonObject evaluated = (JsonObject) io.github.dhoondlayai.artifact.json.extensions.JsonExpression.evaluate(data);
        assertEquals(50.0, evaluated.getDouble("total").orElse(0.0));
    }

    @Test
    @DisplayName("FastObjectMapper near-native serialization")
    void testFastMapper() {
        FastObjectMapper mapper = new FastObjectMapper();
        TestUser user = new TestUser("alice", 30);
        
        JsonObject json = (JsonObject) mapper.serialize(user);
        assertEquals("alice", json.getString("name").orElse(null));
        assertEquals(30, json.getInt("age").orElse(0));
    }

    public static class TestUser {
        public String name;
        public int age;
        public TestUser(String name, int age) { this.name = name; this.age = age; }
    }
}
