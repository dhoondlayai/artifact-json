# JsonStreamWriter — Event-Based Streaming Output

`JsonStreamWriter` writes JSON incrementally — no in-memory tree is built.
Use for generating **large JSON arrays or objects** from database cursors,
file streams, or any iterative data source.

## Basic Usage

```java
import io.github.dhoondlayai.artifact.json.streaming.JsonStreamWriter;
import java.io.*;

// Write to a file (pretty-printed with 2-space indent)
try (JsonStreamWriter writer = new JsonStreamWriter(new FileOutputStream("output.json"), 2)) {
    writer.beginArray();

    for (Record r : database.query("SELECT * FROM orders")) {
        writer.beginObject()
              .field("id",       r.getId())
              .field("amount",   r.getAmount())
              .field("currency", r.getCurrency())
              .field("paid",     r.isPaid())
              .endObject();
    }

    writer.endArray();
}
// File is flushed and closed automatically
```

## Write to String

```java
ByteArrayOutputStream baos = new ByteArrayOutputStream();
try (JsonStreamWriter w = new JsonStreamWriter(baos, 0)) {
    w.beginObject()
     .field("status", "ok")
     .field("count",  42)
     .endObject();
}
String json = baos.toString(StandardCharsets.UTF_8);
// {"status":"ok","count":42}
```

## Nested Structures

```java
writer.beginObject()
      .key("meta")
        .beginObject()
          .field("page",  1)
          .field("total", 100)
        .endObject()
      .key("items")
        .beginArray()
          .value("alpha")
          .value("beta")
          .value("gamma")
        .endArray()
      .endObject();
```

## Writing null and Boolean

```java
writer.field("name",    (String) null);  // "name": null
writer.field("active",  true);           // "active": true
writer.field("deleted", false);          // "deleted": false
```

## Memory Profile

`JsonStreamWriter` maintains essentially **zero heap** beyond what Java's
`BufferedOutputStream` needs. At 1 million records, the heap usage is constant
at approximately the size of one output buffer (~8KB).

## Comparison

| Approach | 1M records peak heap |
|----------|---------------------|
| `new JsonArray()` | ~400MB |
| `JsonStreamWriter` | ~8KB |
