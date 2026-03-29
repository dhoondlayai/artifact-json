# JsonObject — API Reference

An ordered JSON object backed by `LinkedHashMap` with O(1) access.

## Creating

```java
JsonObject obj = new JsonObject();              // empty
JsonObject obj = new JsonObject(16);            // pre-sized capacity
JsonObject obj = new JsonObject(existingMap);   // from Map
```

## Writing (all return `this` for chaining)

```java
obj.put("name",   "Alice")          // String
   .put("age",    30)               // Number
   .put("active", true)             // boolean
   .putNull("deletedAt")            // null value
   .put("profile", new JsonObject())// nested object
   .put("tags",    new JsonArray()); // nested array
```

```java
obj.putIfAbsent("role", new JsonValue("user")); // only if absent
obj.computeIfAbsent("history", k -> new JsonArray());
obj.remove("password");             // remove a field
obj.rename("name", "fullName");     // rename a key
obj.clear();                        // remove all fields
obj.merge(otherObj);                // deep merge (returns new object)
```

## Reading

```java
JsonNode val = obj.field("name");           // may return null
JsonNode def = obj.getOrDefault("x", fallback); // with fallback
boolean  has = obj.contains("name");        // existence check
int      n   = obj.size();                  // field count
Set<String> keys = obj.keys();             // key set
Map<String, JsonNode> all = obj.fields();  // full field map
```

## Typed Getters

```java
Optional<String>  name = obj.getString("name");
Optional<Integer> age  = obj.getInt("age");
Optional<Long>    id   = obj.getLong("id");
Optional<Double>  rate = obj.getDouble("rating");
Optional<Boolean> on   = obj.getBoolean("active");
Optional<JsonObject> nested = obj.getObject("address");
Optional<JsonArray>  list   = obj.getArray("tags");
```

## Stream API

```java
// Sequential
obj.stream().filter(e -> e.getValue().isString())
            .forEach(e -> System.out.println(e.getKey() + "=" + e.getValue().asText()));

// Parallel (for large objects)
obj.parallelStream()
   .filter(e -> e.getValue().asDouble() > 0)
   .count();

// Key stream
obj.keyStream().filter(k -> k.startsWith("user_")).toList();

// Value stream
obj.valueStream().filter(JsonNode::isNumber).toList();
```

## Transformation

```java
// Transform all values
JsonObject upper = obj.mapValues(v ->
        v.isString() ? new JsonValue(v.asText().toUpperCase()) : v);

// Convert to plain Java Map
Map<String, Object> map = obj.toMap(); // only leaf JsonValue fields

// Iterate
obj.forEach((key, value) -> System.out.println(key + "=" + value));
```

## Serialization

```java
obj.toString();            // compact: {"key":"value",...}
obj.toPrettyString(2);     // 2-space indented
obj.toPrettyString(4);     // 4-space indented
```

## equals / hashCode

Both are structurally based (delegated to the underlying `LinkedHashMap`).
