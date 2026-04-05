# JsonQuery — SQL Engine for JSON

## The Problem

Modern Java services constantly deal with in-memory JSON collections: API responses, Kafka event batches, deserialized database rows, CSV imports, configuration snapshots, and more.

The moment you need to **filter, sort, aggregate, or join** that data, you face an ugly choice:

| Option | The pain |
|---|---|
| Write stream chains manually | Verbose, hard to read, easy to get wrong |
| Dump to a real database | Over-engineering for transient, in-flight data |
| Pull in a full query library | Heavy external dependency just to filter 10 records |

**`JsonQuery` removes that choice.** You get a fluent, SQL-shaped API directly on any `JsonArray` — no database, no schema, no third-party dependency.

---

## Real-World Use Cases

### 1 — API Response Filtering

Filter 500 product records from a third-party API down to what you actually need:

```java
JsonArray result = JsonQuery.from(response)
    .whereEq("category", "electronics")
    .whereEq("inStock", true)
    .whereLt("price", 50.0)
    .execute();
```

**Output:**
```json
[
  { "id": "E12", "name": "USB Hub", "category": "electronics", "price": 14.99, "inStock": true },
  { "id": "E45", "name": "Cable", "category": "electronics", "price": 7.50, "inStock": true }
]
```

### 2 — Dashboard / Report Generation

```java
Map<String, JsonArray> byRegion = JsonQuery.from(orders)
    .whereEq("status", "COMPLETED")
    .groupBy("region");

byRegion.forEach((region, orders) -> {
    double revenue = JsonQuery.from(orders).sum("amount");
    System.out.printf("%-15s  $%.2f%n", region, revenue);
});
```

**Console output:**
```
North America    $48920.50
Europe           $31244.00
Asia Pacific     $19870.75
```

### 3 — Event Stream Processing

```java
JsonQuery.from(kafkaBatch)
    .whereEq("service",  "payments")
    .whereEq("severity", "HIGH")
    .whereNotNull("traceId")
    .execute()
    .forEach(node -> alertOpsTeam((JsonObject) node));
```

**Filtered result:**
```json
[
  { "service": "payments", "severity": "HIGH", "traceId": "abc-123", "event": "timeout" },
  { "service": "payments", "severity": "HIGH", "traceId": "def-456", "event": "2xx_spike" }
]
```

### 4 — Joining Two Data Sources

```java
JsonArray enriched = JsonQuery.from(orders)
    .join(customers, "customerId", "id")
    .select("orderId", "amount", "name", "email")
    .execute();
```

**Output (merged fields from both arrays):**
```json
[
  { "orderId": "ORD-001", "amount": 120.00, "name": "Alice", "email": "alice@x.com" },
  { "orderId": "ORD-002", "amount": 45.50,  "name": "Bob",   "email": "bob@x.com" }
]
```

### 5 — Paginated Search Endpoints

```java
JsonArray page = JsonQuery.from(cachedProducts)
    .whereContains("title", searchTerm)
    .whereIn("category", selectedCategories)
    .orderBy("rating", true)
    .page(pageNumber, pageSize)
    .execute();
```

**Output for `page(1, 2)` with `searchTerm = "Java"`:**
```json
[
  { "title": "Effective Java", "rating": 4.9, "category": "tech" },
  { "title": "Java Concurrency", "rating": 4.7, "category": "tech" }
]
```

### 6 — Large-File Parallel Aggregation

```java
double avgScore = JsonQuery.from(loyaltyRecords)
    .parallel()
    .whereGt("points", 0)
    .avg("points")
    .orElse(0.0);
// 1247.3
```

---

## Entry Point

```java
JsonQuery.from(JsonArray array)
```

All operations are **lazy** — nothing runs until `.execute()`, `.count()`, `.sum()`, etc.

---

## SELECT — Field Projection

```java
JsonArray result = JsonQuery.from(books)
    .select("title", "price")
    .execute();
```

**Input:**
```json
[{ "title": "Clean Code", "price": 29.99, "author": "Martin" }]
```
**Output (only selected fields):**
```json
[{ "title": "Clean Code", "price": 29.99 }]
```

### SELECT AS — Aliases

```java
JsonArray result = JsonQuery.from(books)
    .selectAs("title", "bookName")
    .selectAs("price", "cost")
    .execute();
```

**Output:**
```json
[{ "bookName": "Clean Code", "cost": 29.99 }]
```

---

## WHERE — Filtering

### Equality & Inequality

```java
.whereEq("status", "ACTIVE")       // field = value
.whereNotEq("type", "DELETED")     // field != value
```

### Numeric Comparisons

```java
.whereGt("price",  10.0)           // >
.whereGte("price", 10.0)           // >=
.whereLt("price",  50.0)           // <
.whereLte("price", 50.0)           // <=
.whereBetween("price", 10.0, 50.0) // BETWEEN (inclusive)
```

**Example — items between $10 and $50:**
```java
JsonArray result = JsonQuery.from(products)
    .whereBetween("price", 10.0, 50.0)
    .execute();
// [{ "name": "Widget", "price": 19.99 }, { "name": "Gadget", "price": 34.50 }]
```

### String Matching

```java
.whereContains("title", "Java")      // LIKE '%Java%'
.whereStartsWith("email", "admin")   // LIKE 'admin%'
.whereEndsWith("email", ".io")       // LIKE '%.io'
.whereMatches("phone", "\\d{10}")    // REGEXP
```

**Example — emails ending in .io:**
```java
JsonArray result = JsonQuery.from(users)
    .whereEndsWith("email", ".io")
    .execute();
// [{ "name": "Dev", "email": "dev@artifact.io" }]
```

### Set Membership

```java
.whereIn("category", "fiction", "sci-fi", "tech")
```

**Output:**
```json
[
  { "title": "Dune", "category": "sci-fi" },
  { "title": "Clean Code", "category": "tech" }
]
```

### Null Checks

```java
.whereNotNull("email")   // IS NOT NULL
.whereNull("deletedAt")  // IS NULL
```

### Custom Predicate

```java
// Keep only objects where score is even
.where(obj -> obj.field("score").asInt() % 2 == 0)
```

---

## ORDER BY / LIMIT / OFFSET / PAGE

```java
.orderBy("price", true)    // DESC
.orderBy("name",  false)   // ASC
.thenBy("id",    false)    // secondary sort key

.limit(10)                 // top 10
.offset(20)                // skip 20
.page(2, 10)               // page 2 (skip 20, return 10)
```

**Example — top 3 most expensive items:**
```java
JsonArray result = JsonQuery.from(products)
    .orderBy("price", true)
    .limit(3)
    .execute();
```
**Output:**
```json
[
  { "name": "Laptop", "price": 1299.00 },
  { "name": "Monitor", "price": 499.00 },
  { "name": "Keyboard", "price": 149.99 }
]
```

---

## DISTINCT

```java
JsonArray unique = JsonQuery.from(arr).distinct().execute();
```

**Input:** `[{"role":"admin"}, {"role":"user"}, {"role":"admin"}]`  
**Output:** `[{"role":"admin"}, {"role":"user"}]`

---

## GROUP BY

```java
Map<String, JsonArray> byCategory = JsonQuery.from(books)
    .whereNotNull("category")
    .groupBy("category");

byCategory.forEach((cat, group) ->
    System.out.println(cat + ": " + group.size() + " items"));
```

**Console output:**
```
tech: 14 items
fiction: 9 items
sci-fi: 6 items
```

---

## JOIN

Inner join two arrays on a key field:

```java
JsonArray enriched = JsonQuery.from(orders)
    .join(customers, "customerId", "id")
    .execute();
```

**orders:** `[{ "orderId": "ORD-1", "customerId": "C1", "amount": 99.0 }]`  
**customers:** `[{ "id": "C1", "name": "Alice", "email": "a@x.com" }]`  
**Output:**
```json
[{
  "orderId": "ORD-1",
  "customerId": "C1",
  "amount": 99.0,
  "name": "Alice",
  "email": "a@x.com"
}]
```

---

## Aggregation

Terminal operations — no `.execute()` needed:

```java
long   count    = JsonQuery.from(books).count();                  // 42
double total    = JsonQuery.from(books).sum("price");             // 683.50
OptionalDouble avg = JsonQuery.from(books).avg("price");          // 16.27
OptionalDouble min = JsonQuery.from(books).min("price");          // 4.99
OptionalDouble max = JsonQuery.from(books).max("price");          // 59.99

// Combined with WHERE
double paidTotal = JsonQuery.from(orders)
    .whereEq("status", "PAID")
    .sum("amount");
// 2841.75
```

---

## Parallel Execution

For large arrays (10,000+ elements), enable parallel processing:

```java
JsonArray result = JsonQuery.from(bigArray)
    .parallel()
    .whereGt("score", 90)
    .orderBy("score", true)
    .execute();
```

> **Note:** Parallel mode uses `parallelStream()` internally. Best for CPU-bound filtering on already-in-memory data. For files not yet loaded, use `JsonStreamReader` instead.

---

## Find First

```java
Optional<JsonObject> admin = JsonQuery.from(users)
    .whereEq("role", "ADMIN")
    .findFirst();

admin.ifPresent(u -> System.out.println(u.getString("name").orElse("?")));
// "Alice"
```

---

## Complete Example

```java
// E-commerce: top 5 most expensive in-stock tech books
JsonArray result = JsonQuery.from(inventory)
    .selectAs("title",   "productName")
    .selectAs("price",   "listPrice")
    .whereEq("category", "tech")
    .whereEq("inStock",  true)
    .whereGt("price",    20.0)
    .orderBy("price",    true)   // DESC
    .limit(5)
    .execute();

System.out.println(JsonConverter.toPrettyString(result, 2));
```

**Output:**
```json
[
  { "productName": "Java Concurrency in Practice", "listPrice": 54.99 },
  { "productName": "Designing Data-Intensive Apps",  "listPrice": 49.99 },
  { "productName": "Clean Architecture",             "listPrice": 39.99 },
  { "productName": "Effective Java",                 "listPrice": 34.99 },
  { "productName": "Spring in Action",               "listPrice": 29.99 }
]
```
