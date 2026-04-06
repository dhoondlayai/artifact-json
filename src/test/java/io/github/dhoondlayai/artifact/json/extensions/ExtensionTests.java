package io.github.dhoondlayai.artifact.json.extensions;

import io.github.dhoondlayai.artifact.json.model.JsonArray;
import io.github.dhoondlayai.artifact.json.model.JsonNode;
import io.github.dhoondlayai.artifact.json.model.JsonObject;
import io.github.dhoondlayai.artifact.json.model.JsonValue;
import io.github.dhoondlayai.artifact.json.model.JsonTraversal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Utility Extensions (Masking, Patching, Shield, Traversal) Tests")
public class ExtensionTests {

    @Test
    @DisplayName("PII Masking with JsonMasker")
    void testJsonMasker() {
        JsonObject user = new JsonObject()
                .put("username", "admin")
                .put("password", "s3cr3t")
                .put("ssn", "123-456");
        
        JsonMasker masker = new JsonMasker("****")
                .addSensitiveKey("password")
                .addSensitiveKey("ssn");
        
        JsonObject masked = (JsonObject) masker.mask(user);
        assertEquals("admin", masked.getString("username").orElse(null));
        assertEquals("****", masked.getString("password").orElse(null));
        assertEquals("****", masked.getString("ssn").orElse(null));
    }

    @Test
    @DisplayName("RFC 6902 Patch generation")
    void testJsonPatchGenerator() {
        JsonObject v1 = new JsonObject().put("name", "Bob").put("age", 25);
        JsonObject v2 = new JsonObject().put("name", "Robert").put("age", 25).put("title", "Engineer");
        
        JsonArray patch = JsonPatchGenerator.generatePatch(v1, v2);
        assertTrue(patch.size() > 0);
        
        // Find 'replace' for name
        boolean foundReplace = patch.stream().anyMatch(n -> 
            n.get("op").asText().equals("replace") && n.get("path").asText().equals("/name") && n.get("value").asText().equals("Robert"));
        assertTrue(foundReplace);

        // Find 'add' for title
        boolean foundAdd = patch.stream().anyMatch(n -> 
            n.get("op").asText().equals("add") && n.get("path").asText().equals("/title") && n.get("value").asText().equals("Engineer"));
        assertTrue(foundAdd);
    }

    @Test
    @DisplayName("Safe access with JsonShield and defaults")
    void testJsonShield() {
        JsonObject root = new JsonObject().put("id", 1).put("meta", new JsonObject().put("version", "v1"));
        JsonShield shield = new JsonShield(root);
        
        assertEquals("admin", shield.getString("user.name", "admin"));
        assertEquals("v1", shield.getString("meta.version", "v2"));
        assertEquals(1, shield.getInt("id", 0));
        assertFalse(shield.getBoolean("active", false));
    }

    @Test
    @DisplayName("Traversal methods: flatten, transform, count")
    void testJsonTraversal() {
        JsonObject root = new JsonObject()
                .put("a", new JsonObject().put("b", 1))
                .put("c", new JsonArray().add(2).add(3));
        
        Map<String, Object> flattened = JsonTraversal.flatten(root);
        assertEquals(1, flattened.get("a.b"));
        assertEquals(2, flattened.get("c[0]"));
        assertEquals(3, flattened.get("c[1]"));
        
        assertEquals(6, JsonTraversal.countNodes(root)); // {a:..}, a, b, {c:..}, c, 2, 3... wait, counting depends on implementation
        assertTrue(JsonTraversal.countNodes(root) > 0);
        assertEquals(2, JsonTraversal.maxDepth(root)); // root -> a -> b
    }

    @Test
    @DisplayName("Wildcard search in structure")
    void testWildcardFind() {
        JsonObject root = new JsonObject()
                .put("users", new JsonArray()
                        .add(new JsonObject().put("email", "e1"))
                        .add(new JsonObject().put("email", "e2")));
        
        JsonExtensions ext = new JsonExtensions();
        List<JsonNode> emails = ext.wildcardFind(root, "users[*].email");
        assertEquals(2, emails.size());
        assertEquals("e1", emails.get(0).asText());
    }
}
