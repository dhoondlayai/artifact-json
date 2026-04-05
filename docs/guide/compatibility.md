# Compatibility & Requirements

**artifact-json** is designed for the modern JVM while maintaining broad compatibility with enterprise environments.

---

## Supported Java Versions

| Java Version | Support Status | Minimum Requirement | Notes |
|:---|:---:|:---:|:---|
| **Java 21+** | ✅ Full Support | Recommended | Supports all modern switch patterns and record features. |
| **Java 17** | ✅ Full Support | **Minimum** | Finalized sealed types and pattern matching for `instanceof`. |
| Java 11 | ❌ Not Supported | - | Requires modern language features (Sealed types, Records). |
| Java 8 | ❌ Not Supported | - | Legacy JVM; lacks structural features needed for performance. |

> [!IMPORTANT]
> Starting from version **2.0.2**, the minimum supported Java version is **Java 17**. If you are on an older JVM, we recommend upgrading or using the 1.x branch.

---

## Platform Support

artifact-json is a **pure Java** library with **zero external dependencies**. It runs anywhere a standard JVM is available:

- **Cloud Native**: Optimized for AWS Lambda, Google Cloud Functions, and Azure Functions (fast startup, small binary).
- **Embedded**: Resource-constrained environments (low memory footprint, under 500KB JAR).
- **Big Data**: High-throughput processing with Java Stream API and parallel processing.

---

## Why Java 17+?

By targeting Java 17 as the baseline, artifact-json leverages standard JVM optimizations that older libraries often bypass for backward compatibility:

1. **Sealed Type Hierarchy**: The JVM can devirtualize method calls on `JsonNode`, leading to near-native execution speed.
2. **Pattern Matching**: Reduces boilerplate and improves null-safety during tree traversal.
3. **Records**: `JsonValue` is implemented as a Java `record`, ensuring structural immutability and efficient memory layouts.
4. **MethodHandles**: High-performance serialization via `java.lang.invoke` instead of standard reflection.

---

## Troubleshooting

If you encounter a `java.lang.UnsupportedClassVersionError` or `java.lang.NoSuchFieldError` at startup, please verify your runtime Java version:

```bash
java -version
```

Ensure it reports version **17.x** or higher.
