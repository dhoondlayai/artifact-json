# JsonQuery — SQL Engine for JSON

The `JsonQuery` engine lets you query `JsonArray` data using a fluent, SQL-like API.
No database, no schema, no external dependencies — just your JSON tree.

## Entry Point

```java
JsonQuery.from(JsonArray array)
```

## SELECT — Field Projection

```java
// SELECT title, price FROM books
JsonArray result = JsonQuery.from(books)
    .select("title", "price")
    .execute();
```

### SELECT AS — Aliases

```java
JsonArray result = JsonQuery.from(books)
    .selectAs("title", "bookName")
    .selectAs("price", "cost")
    .execute();
// [{bookName: "...", cost: 15.99}, ...]
```

## WHERE — Filtering

### Equality & Inequality

```java
.whereEq("status", "ACTIVE")       // field = value
.whereNotEq("type", "DELETED")     // field != value
```

### Numeric Comparisons

```java
.whereGt("price",  10.0)   // >
.whereGte("price", 10.0)   // >=
.whereLt("price",  50.0)   // <
.whereLte("price", 50.0)   // <=
.whereBetween("price", 10.0, 50.0)  // BETWEEN
```

### String Matching

```java
.whereContains("title", "Java")      // LIKE '%Java%'
.whereStartsWith("email", "admin")   // LIKE 'admin%'
.whereEndsWith("email", ".io")       // LIKE '%.io'
.whereMatches("phone", "\\d{10}")    // REGEXP
```

### Set Membership

```java
.whereIn("category", "fiction", "sci-fi", "tech")   // IN (...)
```

### Null Checks

```java
.whereNotNull("email")   // IS NOT NULL
.whereNull("deletedAt")  // IS NULL
```

### Custom Predicate

```java
.where(obj -> obj.field("score").asInt() % 2 == 0)  // arbitrary lambda
```

## ORDER BY / LIMIT / OFFSET / PAGE

```java
.orderBy("price", true)    // DESC
.orderBy("name",  false)   // ASC
.thenBy("id",    false)    // secondary sort key

.limit(10)                 // top 10
.offset(20)                // skip 20
.page(2, 10)               // page 2 (20 rows skipped, 10 returned)
```

## DISTINCT

```java
.distinct()    // removes duplicate results (by JSON string)
```

## GROUP BY

```java
Map<String, JsonArray> byCategory = JsonQuery.from(books)
    .whereNotNull("category")
    .groupBy("category");

byCategory.forEach((cat, group) -> {
    System.out.println(cat + ": " + group.size() + " items");
});
```

## JOIN

Inner join two arrays on a key field:

```java
JsonArray enriched = JsonQuery.from(orders)
    .join(customers, "customerId", "id")  // orders.customerId = customers.id
    .execute();
// Each order object now contains customer fields merged in
```

## Aggregation

These are **terminal operations** — they return a value directly, no `.execute()` needed:

```java
long   count = JsonQuery.from(books).count();
double total = JsonQuery.from(books).sum("price");
OptionalDouble avg = JsonQuery.from(books).avg("price");
OptionalDouble min = JsonQuery.from(books).min("price");
OptionalDouble max = JsonQuery.from(books).max("price");

// Combine with WHERE
double paidTotal = JsonQuery.from(orders)
    .whereEq("status", "PAID")
    .sum("amount");
```

## Parallel Execution

For large arrays (10,000+ elements), enable parallel processing:

```java
JsonArray result = JsonQuery.from(bigArray)
    .parallel()            // uses parallelStream() internally
    .whereGt("score", 90)
    .orderBy("score", true)
    .execute();
```

> **Note:** Parallel mode is best for CPU-bound filtering. For I/O-bound tasks, use `JsonStreamReader`.

## Find First

```java
Optional<JsonObject> admin = JsonQuery.from(users)
    .whereEq("role", "ADMIN")
    .findFirst();

admin.ifPresent(u -> System.out.println(u.getString("name").orElse("?")));
```

## Complete Example

```java
// E-commerce query: top 5 most expensive in-stock tech books
JsonArray result = JsonQuery.from(inventory)
    .selectAs("title",    "productName")
    .selectAs("price",    "listPrice")
    .whereEq("category",  "tech")
    .whereEq("inStock",   true)
    .whereGt("price",     20.0)
    .orderBy("price",     true)    // DESC
    .limit(5)
    .execute();

System.out.println(JsonConverter.toPrettyString(result, 2));
```
