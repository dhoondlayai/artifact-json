package io.github.dhoondlayai.artifact.json;

import io.github.dhoondlayai.artifact.json.annotation.*;
import io.github.dhoondlayai.artifact.json.databind.CustomObjectMapper;
import io.github.dhoondlayai.artifact.json.model.JsonObject;
import io.github.dhoondlayai.artifact.json.model.JsonValue;
import io.github.dhoondlayai.artifact.json.streaming.FastJsonEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Exhaustive CustomObjectMapper Branch Coverage")
public class CustomObjectMapperExhaustiveTest {

    @Test
    @DisplayName("Tests all annotations and edge cases in CustomObjectMapper")
    void testExhaustive() {
        CustomObjectMapper mapper = new CustomObjectMapper();
        
        // 1. Test @JsonUnwrapped
        Parent p = new Parent();
        p.id = "p1";
        p.child = new Child();
        p.child.name = "c1";
        JsonObject json = (JsonObject) mapper.serialize(p);
        assertTrue(json.contains("id"));
        assertTrue(json.contains("child_name")); // Unwrapped with prefix
        
        // 2. Test @JsonAlias and @JsonDefault
        String input = "{\"user_id\": \"u1\"}";
        User u = mapper.deserialize(FastJsonEngine.parse(input), User.class);
        assertEquals("u1", u.id); // Matched alias "user_id"
        assertEquals("Guest", u.role); // Default value
        
        // 3. Test @JsonValidate
        Product prod = new Product();
        prod.price = -10;
        assertThrows(Exception.class, () -> mapper.deserialize(mapper.serialize(prod), Product.class));
        
        // 4. Test @JsonVirtual
        VirtualObj v = new VirtualObj();
        JsonObject vJson = (JsonObject) mapper.serialize(v);
        assertTrue(vJson.contains("Calculated"));
        
        // 5. Test @PII and @JsonInclude
        SecureUser s = new SecureUser();
        s.email = "test@test.com";
        s.ssn = "111-222";
        s.optional = null;
        JsonObject sJson = (JsonObject) mapper.serialize(s);
        assertEquals("****", sJson.getString("ssn").orElse(""));
        assertFalse(sJson.contains("optional")); // NON_NULL
        
        // 6. Test Arrays and Primitives
        int[] nums = {1, 2, 3};
        assertEquals(3, mapper.serialize(nums).size());
        assertEquals(true, mapper.serialize(true).asBoolean());
    }

    public static class Parent {
        public String id;
        @JsonUnwrapped(prefix = "child_")
        public Child child;
    }

    public static class Child {
        public String name;
    }

    public static class User {
        @JsonAlias("user_id")
        public String id;
        @JsonDefault("Guest")
        public String role;
        public User() {}
    }

    public static class Product {
        @JsonValidate(min = 0)
        public double price;
        public Product() {}
    }

    public static class VirtualObj {
        @JsonVirtual
        public String getCalculated() { return "dynamic"; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SecureUser {
        public String email;
        @PII
        public String ssn;
        public String optional;
    }
}
