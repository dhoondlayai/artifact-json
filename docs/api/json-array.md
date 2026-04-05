# JsonArray — API Reference

An ordered, mutable list of `JsonNode` elements backed by `ArrayList`.

## Creating

```java
JsonArray arr = new JsonArray();           // empty
JsonArray arr = new JsonArray(100);        // pre-sized
JsonArray arr = new JsonArray(existingList);
```

## Adding Elements

```java
arr.add("text")            // String
   .add(42)                // Number
   .add(3.14)              // Number
   .add(true)              // boolean
   .addNull()              // null
   .add(new JsonObject())  // nested object
   .addAt(0, newNode)      // insert at position
   .addAll(otherArray);    // append all from another array
```

## Accessing Elements

```java
JsonNode elem = arr.element(0);                        // or throw
JsonNode safe = arr.elementOrDefault(0, fallback);     // safe
Optional<JsonNode> first = arr.first();
Optional<JsonNode> last  = arr.last();

arr.size();
arr.isEmpty();
arr.contains(node);
arr.indexOf(node);
```

## Modifying

```java
arr.set(1, replacement);   // replace at index
arr.remove(0);             // remove by index
arr.remove(node);          // remove by value
arr.clear();               // remove all
```

## Sub-Arrays / Pagination

```java
JsonArray sub  = arr.subArray(0, 5);   // indices [0, 5)
JsonArray page = arr.page(2, 10);      // page 2, 10 per page → skips 20
```

## Stream API

```java
arr.stream()
   .filter(n -> n.asDouble() > 10)
   .mapToDouble(JsonNode::asDouble)
   .sum();

// Parallel for large arrays
arr.parallelStream()
   .filter(n -> n.asDouble() > 50)
   .count();
```

## Transformation

```java
// map — apply function to each element
JsonArray doubled = arr.map(n -> new JsonValue(n.asDouble() * 2));

// filter — keep only matching elements
JsonArray filtered = arr.filter(n -> n.asDouble() > 10);

// distinct — remove duplicates
JsonArray unique = arr.distinct();

// sort in-place
arr.sort(Comparator.comparingDouble(JsonNode::asDouble));

// reverse in-place
arr.reverse();
```

## Group By

```java
// Group object elements by a field value
Map<String, JsonArray> byRole = users.groupBy("role");

byRole.get("admin"); // → JsonArray of admin users
byRole.get("user");  // → JsonArray of regular users
```

## Aggregation

```java
double total       = arr.sum("price");
OptionalDouble avg = arr.avg("price");
OptionalDouble min = arr.min("price");
OptionalDouble max = arr.max("price");
```

---

## Converting to Java Collections

### `toList()` — raw values

Unwraps leaf `JsonValue` elements into a `List<Object>`.

```java
// arr = ["java", "json", 42]
List<Object> values = arr.toList();
// ["java", "json", 42]
```

### `toList(mapper)` — typed list

Converts each element using your own function. The most flexible option.

```java
// Extract the "name" field from every object
List<String> names = users.toList(n -> ((JsonObject) n).getString("name").orElse(""));
// ["Alice", "Bob", "Charlie"]

// Extract the "price" field as double
List<Double> prices = products.toList(n -> ((JsonObject) n).getDouble("price").orElse(0.0));
// [9.99, 24.99, 4.49]
```

### `toSet()` — deduplicated raw values

Same as `toList()` but returns a `LinkedHashSet`, removing duplicates while preserving order.

```java
// arr = ["java", "json", "java", "java"]
Set<Object> unique = arr.toSet();
// {"java", "json"}
```

### `toSet(mapper)` — typed set

```java
// Get all unique roles from a user array
Set<String> roles = users.toSet(n -> ((JsonObject) n).getString("role").orElse(""));
// {"admin", "user", "moderator"}
```

### `toStringList(field)` — pluck a string field

Extracts a single string field from every object in the array. Null/missing values are excluded automatically.

```java
// users = [{name:"Alice", email:"a@x.com"}, {name:"Bob", email:"b@x.com"}, ...]
List<String> emails = users.toStringList("email");
// ["a@x.com", "b@x.com"]

List<String> ids = orders.toStringList("orderId");
// ["ORD-001", "ORD-002", "ORD-003"]
```

### `toMap(keyField, valueField)` — two-field lookup map

Converts an array of objects into a `Map<String, String>` using one field as key and another as value.

```java
// [{id:"u1", name:"Alice"}, {id:"u2", name:"Bob"}]
Map<String, String> idToName = users.toMap("id", "name");
// {"u1" → "Alice", "u2" → "Bob"}

// Fast email → role lookup
Map<String, String> emailToRole = users.toMap("email", "role");
// {"alice@x.com" → "admin", "bob@x.com" → "user"}
```

### `toIndexMap(keyField)` — object index for O(1) lookup

Creates a `Map<String, JsonObject>` for instant lookup of full objects by a key field. Ideal when you frequently need to find an object by its ID.

```java
// [{id:"prod-001", name:"...", price:9.99}, {id:"prod-002", ...}]
Map<String, JsonObject> byId = products.toIndexMap("id");

JsonObject product = byId.get("prod-001");
// {"id": "prod-001", "name": "Widget", "price": 9.99}

double price = product.getDouble("price").orElse(0.0);
// 9.99
```

### Collection conversion cheat sheet

| Method | Returns | Use when... |
|---|---|---|
| `toList()` | `List<Object>` | Simple leaf-value arrays |
| `toList(mapper)` | `List<T>` | Need typed values from objects |
| `toSet()` | `Set<Object>` | Leaf values, duplicates possible |
| `toSet(mapper)` | `Set<T>` | Typed values, need deduplication |
| `toStringList(field)` | `List<String>` | Pluck one field from all objects |
| `toMap(k,v)` | `Map<String,String>` | Key-value lookups between two fields |
| `toIndexMap(key)` | `Map<String,JsonObject>` | O(1) full-object lookup by ID |

---

## Serialization

```java
arr.toString();           // compact: [elem,elem,...]
arr.toPrettyString(2);    // 2-space indented
arr.toPrettyString(4);    // 4-space indented
```

**Example output** for `arr.toPrettyString(2)`:
```json
[
  {
    "id": "u1",
    "name": "Alice"
  },
  {
    "id": "u2",
    "name": "Bob"
  }
]
```

## Iteration

```java
// for-each loop (implements Iterable)
for (JsonNode node : arr) {
    System.out.println(node.asText());
}

// iterator
Iterator<JsonNode> it = arr.iterator();
```
