# JsonStreamReader — Pull-Based Streaming Parsing

`JsonStreamReader` parses large JSON files in **constant memory** by processing
one element at a time — no tree is ever built for the full file.

## Basic Usage

```java
import io.dhoondlay.artifact.json.streaming.JsonStreamReader;

// Stream a top-level JSON array of objects
try (JsonStreamReader reader = new JsonStreamReader(new FileInputStream("1million.json"))) {
    reader.streamArray(node -> {
        // This lambda is called once per element
        JsonObject obj = (JsonObject) node;
        process(obj.getString("id").orElse("?"));
    });
}
```

## With Filtering

```java
try (JsonStreamReader reader = new JsonStreamReader(inputStream)) {
    reader.streamArray(node -> {
        if (node instanceof JsonObject obj) {
            double price = obj.getDouble("price").orElse(0);
            if (price > 100.0) {
                saveToDatabase(obj);
            }
        }
    });
}
```

## Memory Profile

At any point in time, only **one deserialized JsonNode** is held in memory.
Previous nodes are immediately eligible for GC after the lambda returns.

| File Size | Peak Heap | Approach |
|-----------|-----------|----------|
| 10MB JSON | ~50MB | `FastJsonEngine.parse()` once |
| 10MB JSON | ~20KB | `JsonStreamReader.streamArray()` |
| 1GB JSON  | OutOfMemoryError | `FastJsonEngine.parse()` |
| 1GB JSON  | ~20KB | `JsonStreamReader.streamArray()` |

## Comparison with FastJsonEngine

| | `FastJsonEngine.parse()` | `JsonStreamReader` |
|--|--|--|
| Builds full tree | ✅ | ❌ |
| Random access | ✅ | ❌ |
| Constant memory | ❌ | ✅ |
| Best for | < 50MB JSON | Any size |

## When to Use

- Importing large data files (CSV → JSON → DB)
- Processing API responses with thousands of elements
- ETL pipelines
- Log file processing
- Any case where you never need random access into the full array
