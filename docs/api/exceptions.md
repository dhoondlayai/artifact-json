# Exception Handling

artifact-json uses a typed exception hierarchy so you can catch errors
at exactly the right granularity.

## Hierarchy

```
RuntimeException
└── JsonException                  (catch-all)
    ├── JsonParseException         (syntax errors)
    ├── JsonMappingException       (POJO serialization)
    ├── JsonTypeException          (wrong type access)
    ├── JsonPathException          (invalid path)
    ├── JsonQueryException         (query engine errors)
    └── JsonConversionException    (format conversion)
```

All exceptions are **unchecked** (`RuntimeException`) — your code stays clean.

## JsonParseException

Includes line number, column number, and a snippet of the offending input:

```java
try {
    FastJsonEngine.parse("{\"key\": }");
} catch (JsonParseException e) {
    System.err.println(e.getMessage());
    // [line 1, col 9] Expected start of value but got '}'  near "}"

    int line    = e.getLine();     // 1
    int col     = e.getCol();      // 9
    String snip = e.getSnippet();  // near text
}
```

## JsonMappingException

Thrown when `CustomObjectMapper` can't serialize or deserialize:

```java
try {
    mapper.deserialize(node, MyDto.class);
} catch (JsonMappingException e) {
    System.err.println(e.getMessage());
    // Missing required field [field=email, targetType=String]

    String field = e.getFieldName();     // "email"
    Class<?> t   = e.getTargetType();   // String.class
}
```

## JsonTypeException

Thrown on invalid type coercion:

```java
try {
    throw new JsonTypeException("Number", "JsonObject");
} catch (JsonTypeException e) {
    System.err.println(e.getMessage());
    // Type mismatch: expected Number but node is JsonObject
}
```

## JsonPathException

```java
try {
    throw new JsonPathException("store.books[999].title", "index out of bounds");
} catch (JsonPathException e) {
    System.err.println(e.getMessage());
    // Invalid path 'store.books[999].title': index out of bounds

    String path = e.getPath();  // "store.books[999].title"
}
```

## JsonQueryException

```java
try {
    // sum on non-numeric field
    JsonQuery.from(books).sum("category");
} catch (JsonQueryException e) {
    System.err.println(e.getMessage());
}
```

## JsonConversionException

```java
try {
    JsonConverter.toCsv(singleObject); // needs array
} catch (JsonConversionException e) {
    System.err.println(e.getMessage());
    // Conversion from JSON to CSV failed: Array elements must be objects

    String from = e.getSourceFormat();  // "JSON"
    String to   = e.getTargetFormat(); // "CSV"
}
```

## Catch All

Use `JsonException` to catch any library error in one block:

```java
try {
    JsonNode n = FastJsonEngine.parse(input);
    JsonArray result = JsonQuery.from((JsonArray) n).whereGt("price", 10).execute();
    String csv = JsonConverter.toCsv(result);
} catch (JsonException e) {
    log.error("JSON operation failed", e);
}
```
