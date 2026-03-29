# Advanced Features — Traversal, Proxy, Masking, Annotations

## JsonTraversal

Safe, iterative, non-recursive tree traversal — handles 10,000+ level nesting.

```java
import io.github.dhoondlayai.artifact.json.model.JsonTraversal;
```

### DFS (Depth-First)

```java
// Visit every node, pre-order
JsonTraversal.dfs(root, node -> System.out.println(node.asText()));
```

### BFS (Breadth-First)

```java
// Visit layer-by-layer
JsonTraversal.bfs(root, node -> System.out.println(node.asText()));
```

### Traverse with Paths

```java
// Each node with its dot-path
JsonTraversal.traverseWithPaths(root, (path, node) -> {
    if (node.isString()) {
        System.out.printf("%s = %s%n", path, node.asText());
    }
});
// Output:
// store.name = Artifact Books
// store.books[0].title = 1984
// store.books[0].author = George Orwell
```

### Flatten / Unflatten

```java
// Flatten to dot-path → value map
Map<String, Object> flat = JsonTraversal.flatten(root);
// {"store.name": "Artifact Books", "store.books[0].title": "1984", ...}

// Reconstruct from flat map
JsonNode rebuilt = JsonTraversal.unflatten(flat);
```

### Transform (Deep Clone + Modify)

```java
// Uppercase all string values
JsonNode result = JsonTraversal.transform(root, n -> {
    if (n instanceof JsonValue v && v.isString()) {
        return new JsonValue(v.asText().toUpperCase());
    }
    return n; // unchanged
});
```

### Stats

```java
int   nodes = JsonTraversal.countNodes(root);  // total node count
int   depth = JsonTraversal.maxDepth(root);    // max nesting depth
List<JsonValue> leaves = JsonTraversal.collectLeaves(root);
```

---

## Dynamic Proxy Interface Mapping

Map any `JsonObject` to a typed Java interface — **zero deserialization, zero copies**.
Reads and writes go directly to the underlying JSON tree.

```java
// Define your interface
public interface ServerConfig {
    @JsonProperty("host_name")
    String getHostName();

    @JsonProperty("host_name")
    void setHostName(String value);

    int getPort();
    void setPort(int port);

    boolean isDebugEnabled();
}

// Create proxy over a JsonObject
JsonObject cfg = FastJsonEngine.parse("{\"host_name\":\"localhost\",\"port\":8080,\"debugEnabled\":false}").asObject();
ServerConfig server = JsonProxy.create(ServerConfig.class, cfg);

// Reads directly from JsonObject
String host = server.getHostName(); // "localhost"
int    port = server.getPort();     // 8080

// Writes directly to JsonObject — no intermediate objects
server.setHostName("prod.dhoondlay.io");
System.out.println(cfg.get("host_name").asText()); // prod.dhoondlay.io
```

**Nested proxies:** If a getter returns another interface, it creates a nested proxy automatically:

```java
public interface AppConfig {
    ServerConfig getServer();   // returns nested proxy
    DatabaseConfig getDatabase();
}
```

---

## PII Masking (JsonMasker)

Masks sensitive fields in the JSON tree before logging or storing.
Returns a **new** tree — the original is never mutated.

```java
JsonMasker masker = new JsonMasker("****")
    .addSensitiveKey("password")
    .addSensitiveKey("ssn")
    .addSensitiveKey("creditCard")
    .addSensitiveKey("token");

JsonObject payload = buildPayload(); // has password, ssn fields
JsonNode masked = masker.mask(payload);

System.out.println(masked);
// {"username":"alice","password":"****","ssn":"****","email":"alice@example.com"}
```

Keys are matched **case-insensitively** at any nesting level.

---

## JsonShield — Safe Access with Defaults

```java
JsonShield shield = new JsonShield(configNode);

// All returner use defaults if path is missing or wrong type
String host    = shield.getString("server.host",      "localhost");
int    port    = shield.getInt   ("server.port",      8080);
long   timeout = shield.getLong  ("server.timeout",   30_000L);
double ratio   = shield.getDouble("sampling.ratio",   0.1);
boolean debug  = shield.getBoolean("logging.debug",   false);

// Existence check
if (shield.exists("feature.flags.newUI")) { ... }

// Raw node access
Optional<JsonNode> raw = shield.get("server");

// Redact fields from the tree (returns new tree)
JsonNode safe = shield.redact("password", "token", "apiKey");
```

---

## Annotations

### @JsonProperty

```java
public class User {
    @JsonProperty("full_name")          // rename in JSON
    private String fullName;

    @JsonProperty(value = "e_mail", required = true)  // required + renamed
    private String email;
}
```

### @JsonIgnore

```java
public class User {
    @JsonIgnore
    private String password;   // never serialized or deserialized

    @JsonIgnore
    private transient String sessionToken;
}
```

### @JsonNaming

Applies a naming strategy to **all fields** in a class:

```java
@JsonNaming(JsonNaming.NamingStrategy.SNAKE_CASE)
public class OrderConfig {
    private int maxRetries;     // → max_retries in JSON
    private String baseUrl;     // → base_url in JSON
}
```

| Strategy | Example |
|----------|---------|
| `CAMEL_CASE` (default) | `myFieldName` |
| `SNAKE_CASE` | `my_field_name` |
| `KEBAB_CASE` | `my-field-name` |
| `PASCAL_CASE` | `MyFieldName` |

---

## Code Generator

Generate Java Records directly from any JSON structure:

```java
String code = JsonCodeGenerator.generateJavaRecords("ApiResponse", root);
System.out.println(code);
```

**Output:**
```java
// ⚡ Auto-generated by artifact-json

public record StoreItem (
    String title,
    String author,
    double price,
    boolean inStock
) {}

public record ApiResponse (
    List<StoreItem> books,
    String name,
    String location
) {}
```

Use for scaffolding DTOs and ViewModels from any real API response — no external tools needed.
