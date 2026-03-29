# artifact-json vs Jackson

An honest feature-by-feature comparison between **artifact-json 2.0**
and **Jackson 2.x** — the most widely used Java JSON library.

## Summary

| Criterion | artifact-json | Jackson |
|-----------|:---:|:---:|
| Zero runtime dependencies | ✅ | ❌ `jackson-core` + `jackson-databind` + `jackson-annotations` |
| Built-in SQL query engine | ✅ | ❌ use JOOQ / QueryDSL externally |
| Stream API support | ✅ native | ❌ requires custom iteration |
| Parallel stream queries | ✅ `.parallel()` | ❌ manual `ForkJoinPool` |
| In-memory format conversions | ✅ 8 formats | ❌ need `jackson-dataformat-csv`, etc. |
| Dynamic proxy mapping | ✅ built-in | ❌ not available |
| Self-healing parser | ✅ | ❌ |
| PII masking | ✅ built-in | ❌ |
| Java 21 sealed types | ✅ | ❌ |
| Pattern matching support | ✅ | ❌ |
| Parse error line/column | ✅ | ✅ |
| POJO databinding | ✅ | ✅ (more complete) |
| Schema validation | ❌ (roadmap) | ❌ (use JSON-Schema libs) |
| Spring Boot integration | ❌ (roadmap) | ✅ |
| Tree model (all nodes) | ✅ | ✅ `JsonNode` |

## Why Zero Dependencies Matters

Jackson requires at minimum 3 jars (~3MB) to function. Add CSv, XML, or YAML support
and it balloons to 5–8 jars. artifact-json ships as a **single sub-500KB jar**.

```xml
<!-- Jackson: 3-8 jars depending on features -->
<dependency><groupId>com.fasterxml.jackson.core</groupId>...</dependency>
<dependency><groupId>com.fasterxml.jackson.databind</groupId>...</dependency>
<dependency><groupId>com.fasterxml.jackson.dataformat</groupId>...</dependency>

<!-- artifact-json: single jar, zero transitive deps -->
<dependency>
    <groupId>io.github.dhoondlayai.artifact</groupId>
    <artifactId>artifact-json</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Query Power

Jackson has no built-in query engine. Equivalent code requires JOOQ, Jayway JsonPath,
or custom iteration:

### Jackson (verbose, no SQL operators)
```java
// Jackson — manual filter + sort + collect
List<JsonNode> result = new ArrayList<>();
for (JsonNode n : root.get("books")) {
    if (n.get("price").asDouble() > 10.0
            && n.get("inStock").asBoolean()
            && n.get("category").asText().equals("fiction")) {
        result.add(n);
    }
}
result.sort(Comparator.comparingDouble(n -> n.get("price").asDouble()));
result = result.subList(0, Math.min(5, result.size()));
```

### artifact-json (declarative, SQL-like)
```java
JsonArray result = JsonQuery.from(books)
    .whereGt("price", 10.0)
    .whereEq("inStock", true)
    .whereEq("category", "fiction")
    .orderBy("price", false)
    .limit(5)
    .execute();
```

## Conversion Suite

| Format | artifact-json | Jackson |
|--------|:---:|:---:|
| CSV | ✅ built-in | requires `jackson-dataformat-csv` |
| XML | ✅ built-in | requires `jackson-dataformat-xml` |
| YAML | ✅ built-in | requires `jackson-dataformat-yaml` + SnakeYAML |
| Properties | ✅ built-in | no support |
| Markdown Table | ✅ built-in | no support |
| HTML Table | ✅ built-in | no support |
| Pretty JSON | ✅ built-in | ✅ `ObjectMapper.writerWithDefaultPrettyPrinter()` |

## Stream API

```java
// artifact-json — Stream API built-in
double total = JsonQuery.from(orders)
    .whereEq("status", "PAID")
    .sum("amount");

// Parallel processing with one method call
long count = JsonQuery.from(bigArray)
    .parallel()
    .whereGt("score", 90)
    .count();
```

## When to Choose Jackson

Jackson remains the right choice if you need:
- Deep Spring Boot auto-configuration
- Complex polymorphic deserialization with type IDs
- A mature ecosystem with 10+ years of community support
- Specific format modules (Protobuf, CBOR, Smile, etc.)

## When to Choose artifact-json

Choose artifact-json when you need:
- Zero-dependency deployment (embedded systems, AWS Lambda)
- In-memory SQL-like querying on JSON data
- Multi-format conversion without extra dependencies
- Stream API first-class support
- Java 21 pattern matching and sealed types
- PII masking and RFC 6902 patch generation
