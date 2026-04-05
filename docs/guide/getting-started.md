# Getting Started

Welcome to **artifact-json**! This guide will help you install and run your first code.

## Installation

Add the single dependency to your `pom.xml`. There are **no transitive dependencies**.

```xml
<dependency>
    <groupId>io.github.dhoondlayai</groupId>
    <artifactId>artifact-json</artifactId>
    <version>2.0.2</version>
</dependency>
```

> **Note:** Requires Java 17 or higher.

## Your First JSON Code

```java
import io.github.dhoondlayai.artifact.json.streaming.FastJsonEngine;
import io.github.dhoondlayai.artifact.json.model.*;

public class Main {
    public static void main(String[] args) {
        // Parse raw string
        String raw = "{\"company\": \"Dhoondlay\", \"active\": true, \"users\": [\"Alice\", \"Bob\"]}";
        JsonObject data = FastJsonEngine.parse(raw).asObject();
        
        // Read typesafely
        String name = data.getString("company").orElse("Unknown");
        System.out.println("Company: " + name);
        
        // Modify
        data.put("version", new JsonValue(2.0));
        
        // Print
        System.out.println(data.toPrettyString());
    }
}
```
