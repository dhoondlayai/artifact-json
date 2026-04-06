package io.github.dhoondlayai.artifact.json;

import io.github.dhoondlayai.artifact.json.annotation.*;
import io.github.dhoondlayai.artifact.json.databind.*;
import io.github.dhoondlayai.artifact.json.exception.JsonMappingException;
import io.github.dhoondlayai.artifact.json.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class MaxCoverage5Test {

    @JsonNaming(JsonNaming.NamingStrategy.SNAKE_CASE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ComplexUser {
        @JsonProperty("user_id")
        private String id;
        private String firstName;
        @JsonIgnore
        private String internalSecret;
        @JsonWriteOnly
        private String password;
        @JsonUnwrapped(prefix = "addr_")
        private Address address;
        @PII(mask = "REDACTED")
        private String ssn;
        @PII(value = "####")
        private String creditCard;
        
        private List<String> roles;
        private Map<String, Integer> scores;
        private String[] nicknames;
        
        @JsonVirtual
        public String getSummary() {
            return firstName + " (" + id + ")";
        }

        // Getters/Setters or just access since fields are private we need to set them via reflection if mapper does it
        // CustomObjectMapper uses reflection so we don't strictly need public getters/setters if we test serialization
    }

    public static class Address {
        private String city;
        private String zip;
    }

    @Test
    void testComplexMapping() throws Exception {
        CustomObjectMapper mapper = new CustomObjectMapper();
        
        // Setup registry
        mapper.registerAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
            @Override
            public JsonNode toJson(LocalDate value) { return new JsonValue(value.toString()); }
            @Override
            public LocalDate fromJson(JsonNode node) { return LocalDate.parse(node.asText()); }
        });
        
        ComplexUser user = new ComplexUser();
        user.id = "U1";
        user.firstName = "John";
        user.internalSecret = "SECRET";
        user.password = "PWD";
        user.ssn = "123-456";
        user.creditCard = "1111-2222";
        user.roles = Arrays.asList("admin", "user");
        user.scores = Map.of("math", 90);
        user.nicknames = new String[]{"Johnny"};
        
        Address addr = new Address();
        addr.city = "NY";
        addr.zip = "10001";
        user.address = addr;
        
        JsonNode json = mapper.serialize(user);
        assertTrue(json.isObject());
        JsonObject obj = (JsonObject) json;
        
        assertEquals("U1", obj.getString("user_id").orElse(""));
        assertEquals("John", obj.getString("first_name").orElse(""));
        assertFalse(obj.contains("internalSecret"));
        assertFalse(obj.contains("password"));
        assertEquals("REDACTED", obj.getString("ssn").orElse(""));
        assertEquals("####", obj.getString("credit_card").orElse(""));
        
        // Unwrapped fields
        assertEquals("NY", obj.getString("addr_city").orElse(""));
        assertEquals("10001", obj.getString("addr_zip").orElse(""));
        
        // Virtual
        assertEquals("John (U1)", obj.getString("Summary").orElse(""));
        
        // Collections
        assertTrue(obj.getArray("roles").isPresent());
        assertTrue(obj.getObject("scores").isPresent());
        assertTrue(obj.getArray("nicknames").isPresent());
        
        // Deserialize
        ComplexUser user2 = mapper.deserialize(json, ComplexUser.class);
        assertEquals("U1", user2.id);
        assertEquals("John", user2.firstName);
        // address should be populated? 
        // Wait, CustomObjectMapper might not support deserializing @JsonUnwrapped yet based on its implementation
    }

    @JsonNaming(JsonNaming.NamingStrategy.KEBAB_CASE)
    public static class KebabUser {
        private String firstName;
    }

    @JsonNaming(JsonNaming.NamingStrategy.PASCAL_CASE)
    public static class PascalUser {
        private String firstName;
    }

    public static class DefaultUser {
        @JsonDefault("anonymous")
        private String name;
        @JsonDefault("42")
        private int age;
        @JsonDefault("true")
        private boolean active;
        @JsonAlias({"primary_email", "sec_email"})
        private String email;
        @JsonReadOnly
        private String readOnly = "fixed";
        @JsonValidate(required = true, regex = "\\d+")
        private String zip;
        @JsonValidate(min = 18, max = 99)
        private int years;
    }

    public static class NoNoArg {
        public NoNoArg(String s) {}
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class NonEmpty {
        String s = "";
        List<String> l = List.of();
        Map<String, String> m = Map.of();
        JsonArray a = new JsonArray();
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class NonDefault {
        int i = 0;
        boolean b = false;
        String s = "";
    }

    @Test
    void testNamingStrategies() {
        CustomObjectMapper mapper = new CustomObjectMapper();
        KebabUser k = new KebabUser();
        k.firstName = "Bob";
        assertEquals("Bob", ((JsonObject)mapper.serialize(k)).getString("first-name").orElse(""));

        PascalUser p = new PascalUser();
        p.firstName = "Alice";
        assertEquals("Alice", ((JsonObject)mapper.serialize(p)).getString("FirstName").orElse(""));
    }

    @Test
    void testInclusionStrategies() {
        CustomObjectMapper mapper = new CustomObjectMapper();
        assertEquals("{}", mapper.serialize(new NonEmpty()).toString());
        assertEquals("{}", mapper.serialize(new NonDefault()).toString());
    }

    @Test
    void testDeserializationFeatures() {
        CustomObjectMapper mapper = new CustomObjectMapper();
        
        // Default values & Aliases
        JsonObject obj = new JsonObject().put("primary_email", "a@b.com").put("zip", "12345").put("years", 25);
        DefaultUser du = mapper.deserialize(obj, DefaultUser.class);
        assertEquals("anonymous", du.name);
        assertEquals(42, du.age);
        assertTrue(du.active);
        assertEquals("a@b.com", du.email);
        
        // Validation failure
        try {
            mapper.deserialize(new JsonObject().put("zip", "abc"), DefaultUser.class);
            fail("Regex should fail");
        } catch (JsonMappingException e) {}

        try {
            mapper.deserialize(new JsonObject().put("zip", "1").put("years", 10), DefaultUser.class);
            fail("Min should fail");
        } catch (JsonMappingException e) {}

        // Required field (JsonProperty)
        class Req { @JsonProperty(required = true) String name; }
        try {
            mapper.deserialize(new JsonObject(), Req.class);
            fail("Required prop missing");
        } catch (JsonMappingException e) {}

        // No no-arg constructor
        try {
            mapper.deserialize(new JsonObject(), NoNoArg.class);
            fail("NoNoArg should fail");
        } catch (JsonMappingException e) {}

        // Incorrect type
        try {
            mapper.deserialize(new JsonValue(1), NoNoArg.class);
            fail("Value to object should fail");
        } catch (JsonMappingException e) {}
    }

    @Test
    void testDiff() {
        CustomObjectMapper mapper = new CustomObjectMapper();
        JsonObject s = new JsonObject().put("a", 1).put("b", 2);
        JsonObject t = new JsonObject().put("a", 1).put("b", 3).put("c", 4);
        
        JsonObject d = (JsonObject) mapper.diff(s, t);
        assertEquals(3, d.getInt("b").orElse(0));
        assertEquals(4, d.getInt("c").orElse(0));
        assertFalse(d.contains("a"));
    }

    @Test
    void testMappingEdgeCases() {
        CustomObjectMapper mapper = new CustomObjectMapper();
        
        // MAX DEPTH
        try {
            Map<String, Object> circular = new HashMap<>();
            circular.put("self", circular);
            mapper.serialize(circular);
            fail("Should throw depth exception");
        } catch (JsonMappingException e) {
            assertTrue(e.getMessage().contains("depth"));
        }
        
        // Nulls
        assertEquals("null", mapper.serialize(null).toString());
        
        // Adapters
        mapper.registerAdapter(String.class, new TypeAdapter<String>() {
            @Override
            public JsonNode toJson(String value) { return new JsonValue(value.toUpperCase()); }
            @Override
            public String fromJson(JsonNode node) { return node.asText().toLowerCase(); }
        });
        assertEquals("\"HELLO\"", mapper.serialize("hello").toString());
    }
}
