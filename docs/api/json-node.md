# JsonNode — Core Type Reference

`JsonNode` is the sealed base interface for all JSON elements.

## Type Hierarchy

```
JsonNode (sealed)
├── JsonObject   → {"key": value, ...}
├── JsonArray    → [elem, elem, ...]
└── JsonValue    → "string", 42, 3.14, true, false, null
```

## Type Checks

```java
node.isObject()   // true if JsonObject
node.isArray()    // true if JsonArray
node.isValue()    // true if JsonValue
node.isNull()     // true if value is JSON null
node.isString()   // true if String leaf
node.isNumber()   // true if Number leaf
node.isBoolean()  // true if Boolean leaf
```

## Value Extraction

| Method | Returns | Default |
|--------|---------|---------|
| `asText()` | String | `"null"` |
| `asText("default")` | String | given default |
| `asInt()` | int | 0 |
| `asInt(defaultValue)` | int | given default |
| `asLong()` | long | 0L |
| `asDouble()` | double | 0.0 |
| `asBigDecimal()` | BigDecimal | ZERO |
| `asBoolean()` | boolean | false |

## Child Access

```java
// By key (objects)
JsonNode child = node.get("name");

// By index (arrays)
JsonNode elem  = node.get(0);
```

## Path Navigation

```java
Optional<JsonNode> val = node.find("store.books[0].title");
val.ifPresent(t -> System.out.println(t.asText()));
```

Supports:
- Dot separation: `"user.address.city"`
- Array index: `"items[2]"`
- Combined: `"store.books[0].author.name"`

## Find All (Deep Search)

```java
// Find every node with key "price" anywhere in the tree
List<JsonNode> prices = node.findAll("price");
```

## Deep Equality

```java
boolean same = node1.deepEquals(node2);  // structural comparison
```

## Size

```java
node.size();
// JsonObject  → field count
// JsonArray   → element count
// JsonValue   → always 1
```
