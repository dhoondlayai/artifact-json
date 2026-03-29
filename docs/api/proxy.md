# Dynamic Proxy Interface Mapping

If you don't want to deserialize a massive JSON configuration into a POJO, you can use **Zero-Cost Dynamic Proxy Mapping**. 

`JsonProxy` creates a Java interface proxy that reads and writes **directly** to the underlying `JsonObject` map. There is no reflection overhead after proxy creation, no GC allocations for data-binding objects, and memory usage goes way down!

```java
import io.github.dhoondlayai.artifact.json.proxy.JsonProxy;
import io.github.dhoondlayai.artifact.json.annotation.JsonProperty;
```

## Example Walkthrough

**1. Create your Java Interface.** You can annotate getter/setters exactly like you would on a POJO!

```java
public interface SystemConfig {
    @JsonProperty("theme_color")
    String getThemeColor();

    @JsonProperty("theme_color")
    void setThemeColor(String value);

    int getVersion();
    void setVersion(int value);
}
```

**2. Parse the payload and create proxy.**

```java
JsonObject backingJson = FastJsonEngine.parse("{\"theme_color\": \"dark\", \"version\": 2}").asObject();

SystemConfig config = JsonProxy.create(SystemConfig.class, backingJson);
```

**3. Read and write!**

```java
System.out.println(config.getThemeColor()); // Prints "dark"
System.out.println(config.getVersion());    // Prints 2

config.setThemeColor("light");              

// The original backing JsonObject was modified directly!
System.out.println(backingJson.toString()); 
// {"theme_color": "light", "version": 2}
```

## Nested Proxies (Interfaces returning Interfaces)

If your interface has a method that returns another proxy interface, `JsonProxy` automatically wraps the inner `JsonObject` on-the-fly!

```java
public interface ServerPayload {
    String getMode();
    DatabaseConfig getDatabase(); // Nested Proxy
}

public interface DatabaseConfig {
    String getHost();
    int getPort();
}
```

```java
ServerPayload server = JsonProxy.create(ServerPayload.class, jsonPayload);

// Automatically wraps the "database" JsonObject in a sub-proxy transparently
System.out.println( server.getDatabase().getHost() ); 
```
