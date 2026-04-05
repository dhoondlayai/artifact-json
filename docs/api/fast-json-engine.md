# FastJsonEngine — Parser Reference

The heart of artifact-json. A single-pass, zero-copy RFC 8259 compliant JSON parser
written in 100% pure Java 17+.

## Basic Usage

```java
import io.github.dhoondlayai.artifact.json.streaming.FastJsonEngine;
import io.github.dhoondlayai.artifact.json.model.*;

// Parse from String
JsonNode root = FastJsonEngine.parse(jsonString);

// Parse from InputStream
JsonNode root = FastJsonEngine.parse(inputStream, StandardCharsets.UTF_8);
```

## Parse Results

```java
// Type check via pattern matching (Java 17+)
if (root instanceof JsonObject obj) handleObject(obj);
else if (root instanceof JsonArray arr) handleArray(arr);
else if (root instanceof JsonValue val) handleValue(val);

// Or use type checks
if (root.isObject()) { ... }
if (root.isArray())  { ... }
if (root.isValue())  { ... }
```

## What It Handles

| Feature | Supported |
|---------|-----------|
| Objects, arrays, strings, numbers, booleans, null | ✅ |
| Escaped strings (`\n`, `\t`, `\\`, `\/`, `\"`) | ✅ |
| Unicode escapes (`\u0041`) | ✅ |
| Scientific notation (`1.5e10`, `3.14E-2`) | ✅ |
| Negative numbers | ✅ |
| Deeply nested structures | ✅ |
| Empty objects `{}` and arrays `[]` | ✅ |
| RFC 8259 compliant whitespace | ✅ |

## Error Reporting

Parse errors include line number, column, and a context snippet:

```java
try {
    FastJsonEngine.parse("{\"key\": }");
} catch (JsonParseException e) {
    // [line 1, col 9] Expected start of value but found '}'
    int    line = e.getLine();
    int    col  = e.getCol();
    String snip = e.getSnippet();
}
```

## Self-Healing Parser

The `JsonExtensions` class provides a self-healing wrapper that fixes common
mistakes before trying to parse:

```java
JsonExtensions ext = new JsonExtensions();

// Fixes: trailing commas, unquoted keys, single-quoted strings
JsonNode node = ext.parseSelfHealing("{key: 'value', items: [1, 2, 3,]}");
```

## Performance Tips

- **Pre-size your objects:** `new JsonObject(expectedFieldCount)` avoids rehashing
- **Pre-size your arrays:** `new JsonArray(expectedElementCount)` avoids resizing
- **Reuse the engine:** `FastJsonEngine` is stateless — all methods are static
- **For big files:** Use `JsonStreamReader` instead

## Stream-Based Parsing (Massive Files)

```java
// Process millions of records in constant memory
try (JsonStreamReader reader = new JsonStreamReader(new FileInputStream("data.json"))) {
    reader.streamArray(node -> {
        // called once per object in the top-level array
        String id = node.get("id").asText();
        process(id);
    });
}
```
