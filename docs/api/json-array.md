# JsonArray — API Reference

An ordered, mutable list of `JsonNode` elements backed by `ArrayList`.

## Creating

```java
JsonArray arr = new JsonArray();          // empty
JsonArray arr = new JsonArray(100);       // pre-sized
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

## Sub-Arrays

```java
JsonArray sub  = arr.subArray(0, 5);   // indices [0, 5)
JsonArray page = arr.page(2, 10);      // page 2, 10 per page
```

## Stream API

```java
arr.stream()
   .filter(n -> n.asDouble() > 10)
   .mapToDouble(JsonNode::asDouble)
   .sum();

// Parallel for large arrays
arr.parallelStream()
   .filter(n -> n.get("active").asBoolean())
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
double total  = arr.sum("price");
OptionalDouble avg = arr.avg("price");
OptionalDouble min = arr.min("price");
OptionalDouble max = arr.max("price");
```

## Convert to Java List

```java
// Only works for leaf-value arrays
List<Object> values = arr.toList();
// [42, "text", true, null]
```

## Serialization

```java
arr.toString();           // compact: [elem,elem,...]
arr.toPrettyString(2);    // 2-space indented
arr.toPrettyString(4);    // 4-space indented
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
