package io.dhoondlay.artifact.json;

import io.dhoondlay.artifact.json.annotation.*;
import io.dhoondlay.artifact.json.codegen.JsonCodeGenerator;
import io.dhoondlay.artifact.json.convert.JsonConverter;
import io.dhoondlay.artifact.json.databind.CustomObjectMapper;
import io.dhoondlay.artifact.json.exception.*;
import io.dhoondlay.artifact.json.extensions.*;
import io.dhoondlay.artifact.json.model.*;
import io.dhoondlay.artifact.json.proxy.JsonProxy;
import io.dhoondlay.artifact.json.query.JsonQuery;
import io.dhoondlay.artifact.json.streaming.FastJsonEngine;
import io.dhoondlay.artifact.json.streaming.JsonStreamWriter;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * <h2>ArtifactJsonProjectDemo — Full Feature Showcase</h2>
 *
 * <p>Demonstrates every major feature of artifact-json 2.0, including:
 * the full parser, query engine, conversion suite, streaming writer,
 * traversal, proxy, code generation, annotations, and exception handling.</p>
 *
 * @author artifact-json
 * @version 2.0
 */
public class ArtifactJsonProjectDemo {

    public static void main(String[] args) throws Exception {

        banner("artifact-json 2.0 — High-Performance JSON Library Demo");

        // ──────────────────────────────────────────────────────────────────────
        // 1. PARSING — RFC 8259 compliant, all types
        // ──────────────────────────────────────────────────────────────────────
        section("1. PARSING (FastJsonEngine)");

        String raw = """
                {
                  "store": {
                    "name": "Artifact Books",
                    "location": "Silicon Valley",
                    "books": [
                      {"title": "The Great Gatsby",  "author": "F. Scott Fitzgerald", "price": 15.99, "inStock": true,  "category": "fiction"},
                      {"title": "1984",              "author": "George Orwell",        "price": 12.50, "inStock": true,  "category": "fiction"},
                      {"title": "Mastering Java",    "author": "AG Team",              "price": 45.00, "inStock": false, "category": "tech"},
                      {"title": "Clean Code",        "author": "Robert Martin",        "price": 35.00, "inStock": true,  "category": "tech"},
                      {"title": "Dune",              "author": "Frank Herbert",        "price": 18.99, "inStock": true,  "category": "sci-fi"},
                      {"title": "Neuromancer",       "author": "William Gibson",       "price": 14.00, "inStock": false, "category": "sci-fi"}
                    ]
                  },
                  "id": 1001,
                  "rating": 4.8,
                  "active": true,
                  "owner": null
                }
                """;

        JsonNode root = FastJsonEngine.parse(raw);
        print("Name:   ", root.find("store.name").map(JsonNode::asText).orElse("?"));
        print("ID:     ", root.get("id").asInt());
        print("Rating: ", root.get("rating").asDouble());
        print("Active: ", root.get("active").asBoolean());
        print("Owner:  ", root.get("owner").isNull() + " (is null)");
        print("Books:  ", root.find("store.books").map(JsonNode::size).orElse(0) + " items");

        // ──────────────────────────────────────────────────────────────────────
        // 2. JsonObject & JsonArray — Rich API
        // ──────────────────────────────────────────────────────────────────────
        section("2. JsonObject & JsonArray API");

        JsonObject obj = new JsonObject()
                .put("name", "Alice")
                .put("age", 30)
                .put("active", true);

        print("getString: ", obj.getString("name").orElse("?"));
        print("getInt:    ", obj.getInt("age").orElse(0));
        print("contains:  ", obj.contains("name"));
        obj.rename("name", "fullName");
        print("renamed:   ", obj.getString("fullName").orElse("?"));
        obj.putIfAbsent("country", new JsonValue("India"));
        print("putIfAbsent:", obj.getString("country").orElse("?"));

        JsonArray prices = new JsonArray().add(10.5).add(25.0).add(7.99).add(19.0);
        print("sum via stream: ", prices.stream().mapToDouble(JsonNode::asDouble).sum());
        print("sorted:         ", prices.sort(Comparator.comparingDouble(JsonNode::asDouble)));
        print("reversed:       ", prices.reverse());
        print("distinct:       ", new JsonArray().add("a").add("b").add("a").distinct());
        print("page(0,2):      ", prices.page(0, 2));

        // Aggregation
        JsonArray booksArr = (JsonArray) root.find("store.books").orElseThrow();
        print("sum(price):     ", booksArr.sum("price"));
        print("avg(price):     ", booksArr.avg("price").orElse(0));
        print("max(price):     ", booksArr.max("price").orElse(0));
        print("min(price):     ", booksArr.min("price").orElse(0));

        // ──────────────────────────────────────────────────────────────────────
        // 3. JSONQUERY — Extended SQL Engine
        // ──────────────────────────────────────────────────────────────────────
        section("3. JsonQuery — Extended SQL Engine");

        // WHERE + SELECT + ORDER BY + LIMIT
        JsonArray expensive = JsonQuery.from(booksArr)
                .select("title", "price")
                .whereGt("price", 14.0)
                .whereEq("inStock", true)
                .orderBy("price", true)
                .limit(3)
                .execute();
        print("Expensive inStock books (desc, top 3):", expensive.toPrettyString(2));

        // WHERE CONTAINS
        JsonArray fiction = JsonQuery.from(booksArr)
                .whereEq("category", "fiction")
                .execute();
        print("Fiction books: ", fiction.size() + " results");

        // WHERE BETWEEN
        JsonArray midRange = JsonQuery.from(booksArr)
                .whereBetween("price", 14.0, 25.0)
                .orderBy("price", false)
                .execute();
        print("Mid-range (14-25):", midRange);

        // GROUP BY
        Map<String, JsonArray> byCategory = JsonQuery.from(booksArr).groupBy("category");
        byCategory.forEach((cat, books) ->
                print("  Category [" + cat + "]:", books.size() + " books"));

        // AGGREGATION
        print("COUNT all:        ", JsonQuery.from(booksArr).count());
        print("COUNT fiction:    ", JsonQuery.from(booksArr).whereEq("category","fiction").count());
        print("SUM price:        ", JsonQuery.from(booksArr).sum("price"));
        print("AVG price:        ", JsonQuery.from(booksArr).avg("price").orElse(0));

        // SELECT AS (aliasing)
        JsonArray aliased = JsonQuery.from(booksArr)
                .selectAs("title", "bookName")
                .selectAs("price", "cost")
                .limit(2)
                .execute();
        print("SelectAs aliases: ", aliased);

        // JOIN (simulate customers + orders)
        JsonArray customers = new JsonArray();
        customers.add(new JsonObject().put("id", "C1").put("country", "IN"));
        customers.add(new JsonObject().put("id", "C2").put("country", "US"));

        JsonArray orders = new JsonArray();
        orders.add(new JsonObject().put("orderId", "O1").put("customerId", "C1").put("amount", 500));
        orders.add(new JsonObject().put("orderId", "O2").put("customerId", "C2").put("amount", 200));
        orders.add(new JsonObject().put("orderId", "O3").put("customerId", "C1").put("amount", 750));

        JsonArray joined = JsonQuery.from(orders)
                .join(customers, "customerId", "id")
                .execute();
        print("JOIN result:", joined.toPrettyString(2));

        // WHERE IN
        JsonArray selectedCats = JsonQuery.from(booksArr)
                .whereIn("category", "fiction", "sci-fi")
                .execute();
        print("WHERE IN [fiction, sci-fi]:", selectedCats.size() + " results");

        // PARALLEL mode
        long parallelCount = JsonQuery.from(booksArr)
                .parallel()
                .whereGt("price", 10.0)
                .count();
        print("Parallel count (price>10):", parallelCount);

        // ──────────────────────────────────────────────────────────────────────
        // 4. TRAVERSAL — dfs, bfs, flatten, transform
        // ──────────────────────────────────────────────────────────────────────
        section("4. JsonTraversal");

        Map<String, Object> flat = JsonTraversal.flatten(root);
        print("Flattened leaf count: ", flat.size());
        flat.entrySet().stream().limit(5).forEach(e -> print("  " + e.getKey() + " = ", e.getValue()));

        int nodeCount = JsonTraversal.countNodes(root);
        int depth     = JsonTraversal.maxDepth(root);
        print("Total nodes: ", nodeCount);
        print("Max depth:   ", depth);

        List<JsonValue> leaves = JsonTraversal.collectLeaves(root);
        print("Leaf count:  ", leaves.size());

        // Transform: set all prices to 0 (audit mode)
        JsonNode zeroPriced = JsonTraversal.transform(root, n -> {
            if (n instanceof JsonObject o && o.contains("price")) {
                return new JsonObject(o.fields()).put("price", new JsonValue(0.0));
            }
            return n;
        });
        print("Zeroed prices check:", zeroPriced.find("store.books[0].price").map(JsonNode::asText).orElse("?"));

        // ──────────────────────────────────────────────────────────────────────
        // 5. CONVERTERS — JsonConverter suite
        // ──────────────────────────────────────────────────────────────────────
        section("5. Conversion Suite (JsonConverter)");

        // Pretty JSON
        String pretty = JsonConverter.toPrettyString(root.find("store.books").orElseThrow(), 2);
        print("Pretty JSON (first 200 chars):", pretty.substring(0, Math.min(200, pretty.length())) + "...");

        // JSON → CSV
        String csv = JsonConverter.toCsv(booksArr);
        print("CSV (first 3 lines):", csv.lines().limit(3).toList());

        // CSV → JSON (round-trip)
        JsonArray backFromCsv = JsonConverter.fromCsv(csv);
        print("CSV→JSON round-trip size:", backFromCsv.size());
        print("First title back:", backFromCsv.element(0) instanceof JsonObject o
                ? o.getString("title").orElse("?") : "?");

        // JSON → XML
        String xml = JsonConverter.toXml(root.find("store").orElseThrow(), "store");
        print("XML (first 250 chars):", xml.substring(0, Math.min(250, xml.length())) + "...");

        // JSON → YAML
        JsonObject simpleStore = new JsonObject()
                .put("name", "Artifact Books")
                .put("location", "Silicon Valley")
                .put("rating", 4.8);
        String yaml = JsonConverter.toYaml(simpleStore);
        print("YAML:\n", yaml);

        // YAML → JSON
        JsonNode fromYaml = JsonConverter.fromYaml(yaml);
        print("YAML→JSON:", fromYaml);

        // JSON → Properties
        String props = JsonConverter.toProperties(simpleStore);
        print("Properties:", props);

        // JSON → Markdown Table
        String md = JsonConverter.toMarkdownTable(booksArr);
        print("Markdown Table (first 3 lines):", md.lines().limit(3).toList());

        // JSON → HTML Table
        String html = JsonConverter.toHtmlTable(booksArr, "data-table");
        print("HTML (first 200 chars):", html.substring(0, Math.min(200, html.length())) + "...");

        // ──────────────────────────────────────────────────────────────────────
        // 6. STREAMING WRITER
        // ──────────────────────────────────────────────────────────────────────
        section("6. JsonStreamWriter (event-based, no in-memory tree)");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonStreamWriter writer = new JsonStreamWriter(baos, 2)) {
            writer.beginArray();
            for (int i = 1; i <= 3; i++) {
                writer.beginObject()
                        .field("id",    i)
                        .field("name",  "User-" + i)
                        .field("score", i * 30.5)
                        .field("active", i % 2 == 0)
                        .endObject();
            }
            writer.endArray();
        }
        print("StreamWriter output:", baos.toString());

        // ──────────────────────────────────────────────────────────────────────
        // 7. PATH NAVIGATION
        // ──────────────────────────────────────────────────────────────────────
        section("7. Path Navigation (find / findAll)");

        print("find store.name:         ", root.find("store.name").map(JsonNode::asText).orElse("missing"));
        print("find store.books[2].title:", root.find("store.books[2].title").map(JsonNode::asText).orElse("?"));
        print("find missing.key:         ", root.find("missing.key").isPresent());

        List<JsonNode> allTitles = root.findAll("title");
        print("findAll('title') count:", allTitles.size());
        allTitles.forEach(t -> print("  -", t.asText()));

        // ──────────────────────────────────────────────────────────────────────
        // 8. WILDCARD SEARCH
        // ──────────────────────────────────────────────────────────────────────
        section("8. Wildcard Search");

        JsonExtensions ext = new JsonExtensions();
        List<JsonNode> allPrices = ext.wildcardFind(root, "store.books[*].price");
        print("Wildcard store.books[*].price:", allPrices);

        List<JsonNode> byKey = ext.searchByKey(root, "author");
        print("searchByKey('author'):", byKey.size() + " found");

        List<JsonNode> byVal = ext.searchByValue(root, "George Orwell");
        print("searchByValue('George Orwell'):", !byVal.isEmpty() ? "FOUND" : "NOT FOUND");

        // ──────────────────────────────────────────────────────────────────────
        // 9. PII MASKING
        // ──────────────────────────────────────────────────────────────────────
        section("9. Enterprise: PII Masking");

        JsonObject userProfile = new JsonObject()
                .put("username", "admin")
                .put("email",    "admin@dhoondlay.io")
                .put("password", "s3cr3tP@ss")
                .put("ssn",      "123-45-6789");

        JsonMasker masker = new JsonMasker("****")
                .addSensitiveKey("password")
                .addSensitiveKey("ssn");
        print("Original:", userProfile);
        print("Masked:  ", masker.mask(userProfile));

        // ──────────────────────────────────────────────────────────────────────
        // 10. RFC 6902 PATCH GENERATION
        // ──────────────────────────────────────────────────────────────────────
        section("10. RFC 6902 JSON Patch");

        JsonObject v1 = new JsonObject().put("name", "Bob").put("age", 25);
        JsonObject v2 = new JsonObject().put("name", "Robert").put("age", 25).put("title", "Engineer");
        JsonArray patch = JsonPatchGenerator.generatePatch(v1, v2);
        print("Patch instructions:", patch);

        // ──────────────────────────────────────────────────────────────────────
        // 11. ANNOTATIONS + SERIALIZATION
        // ──────────────────────────────────────────────────────────────────────
        section("11. Annotations (@JsonNaming, @JsonProperty, @JsonIgnore)");

        CustomObjectMapper mapper = new CustomObjectMapper();
        AnnotatedUser user = new AnnotatedUser("john_doe", "superSecret", "Johnathan Doe", "Engineering");
        print("Serialized:", mapper.serialize(user));

        // ──────────────────────────────────────────────────────────────────────
        // 12. PROXY INTERFACE MAPPING
        // ──────────────────────────────────────────────────────────────────────
        section("12. Zero-Copy Proxy Interface Mapping");

        JsonObject cfg = new JsonObject().put("theme_color", "dark_mode").put("version", 2);
        SystemConfig sysConfig = JsonProxy.create(SystemConfig.class, cfg);
        print("Proxy get:", sysConfig.getThemeColor());
        sysConfig.setThemeColor("light_mode");
        print("After set:", cfg.getString("theme_color").orElse("?"));

        // ──────────────────────────────────────────────────────────────────────
        // 13. CODE GENERATION (JSON → Java Records)
        // ──────────────────────────────────────────────────────────────────────
        section("13. Code Generation (JsonNode → Java Records)");

        String javaCode = JsonCodeGenerator.generateJavaRecords("StoreResponse", root);
        print("Generated code (first 400 chars):", javaCode.substring(0, Math.min(400, javaCode.length())) + "...");

        // ──────────────────────────────────────────────────────────────────────
        // 14. EXCEPTION HANDLING
        // ──────────────────────────────────────────────────────────────────────
        section("14. Exception Hierarchy");

        // Parse error with location
        try {
            FastJsonEngine.parse("{\"key\": }");
        } catch (JsonParseException e) {
            print("JsonParseException:", e.getMessage());
        }

        // Path exception
        try {
            throw new JsonPathException("store.books[999].title", "index out of bounds");
        } catch (JsonPathException e) {
            print("JsonPathException:", e.getMessage());
        }

        // Type exception
        try {
            throw new JsonTypeException("Number", "JsonObject");
        } catch (JsonTypeException e) {
            print("JsonTypeException:", e.getMessage());
        }

        // Mapping exception
        try {
            throw new JsonMappingException("Missing required field", "email", String.class);
        } catch (JsonMappingException e) {
            print("JsonMappingException:", e.getMessage());
        }

        // Conversion exception
        try {
            throw new JsonConversionException("JSON", "CSV", "root is not a JsonArray");
        } catch (JsonConversionException e) {
            print("JsonConversionException:", e.getMessage());
        }

        // Query exception
        try {
            throw new JsonQueryException("Cannot aggregate on non-numeric field 'category'");
        } catch (JsonQueryException e) {
            print("JsonQueryException:", e.getMessage());
        }

        // ──────────────────────────────────────────────────────────────────────
        // 15. DEEP MERGE
        // ──────────────────────────────────────────────────────────────────────
        section("15. Deep Merge");

        JsonObject base = new JsonObject().put("theme","dark").put("lang","en").put("version",1);
        JsonObject overlay = new JsonObject().put("lang","fr").put("timeout",5000);
        print("Base:    ", base);
        print("Overlay: ", overlay);
        print("Merged:  ", JsonMerger.deepMerge(base, overlay));
        print("KeepLeft:", JsonMerger.mergeKeepLeft(base, overlay));

        // ──────────────────────────────────────────────────────────────────────
        // 16. JSON SHIELD (safe defaulting)
        // ──────────────────────────────────────────────────────────────────────
        section("16. JsonShield (safe access with defaults)");

        JsonShield shield = new JsonShield(root);
        print("Existing:  ", shield.getString("store.name", "N/A"));
        print("Missing:   ", shield.getString("store.address.zip", "N/A"));
        print("Int safe:  ", shield.getInt("id", -1));
        print("Bool safe: ", shield.getBoolean("active", false));

        banner("Demo Complete — artifact-json 2.0");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    static void banner(String title) {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("  💎  " + title);
        System.out.println("═".repeat(70) + "\n");
    }

    static void section(String title) {
        System.out.println("\n── " + title + " " + "─".repeat(Math.max(0, 60 - title.length())));
    }

    static void print(String label, Object value) {
        System.out.printf("   %-35s %s%n", label, value);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Demo Types
    // ─────────────────────────────────────────────────────────────────────────

    public interface SystemConfig {
        @JsonProperty("theme_color")
        String getThemeColor();

        @JsonProperty("theme_color")
        void setThemeColor(String value);

        int getVersion();
    }

    @JsonNaming(JsonNaming.NamingStrategy.SNAKE_CASE)
    static class AnnotatedUser {
        private String username;

        @JsonIgnore
        private String password;

        @JsonProperty(value = "full_name")
        private String fullName;

        private String department;

        public AnnotatedUser(String username, String password, String fullName, String department) {
            this.username   = username;
            this.password   = password;
            this.fullName   = fullName;
            this.department = department;
        }
    }
}
