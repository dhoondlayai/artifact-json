# Annotations (@JsonProperty, @JsonIgnore, etc.)

artifact-json's mapper suite supports three native annotations for guiding Java POJO Serialization and Deserialization. These annotations require the `CustomObjectMapper` to take effect.

```java
import io.dhoondlay.artifact.json.databind.CustomObjectMapper;
```

## @JsonProperty

Allows you to override the JSON key name associated with a Java property, and optionally enforcing required data validation.

```java
public class User {
    // Will be serialized as {"fullName": "..."} instead of "name"
    @JsonProperty("fullName")
    private String name;

    // Forces CustomObjectMapper to throw JsonMappingException if the 
    // JSON response does not contain an "email" key
    @JsonProperty(value = "email", required = true)
    private String email; 
}
```

## @JsonIgnore

Prevents a field from **ever** being serialized or deserialized. Excellent for passwords, transient internal flags, or computed caching values.

```java
public class SecurityProfile {
    private String username;

    @JsonIgnore
    private String hashPswd;   // This will never appear in JSON
}
```

## @JsonNaming (Class Level)

Applies a universal naming strategy to **all** properties inside the class simultaneously. It eliminates the need to attach `@JsonProperty` individually to every single field!

```java
import io.dhoondlay.artifact.json.annotation.JsonNaming;

@JsonNaming(JsonNaming.NamingStrategy.SNAKE_CASE)
public class StripePaymentResponse {
    
    // Will translate to "payment_id"
    private String paymentId;       
    
    // Will translate to "customer_identity_token"
    private String customerIdentityToken; 

    // Can still override strategies when needed
    @JsonProperty("amount_cents")
    private int amount;
}
```

### Strategy Types:
| Strategy | Java Field | JSON Output |
|----------|------------|-------------|
| `CAMEL_CASE` (Default) | `myPropName` | `"myPropName"` |
| `SNAKE_CASE` | `myPropName` | `"my_prop_name"` |
| `KEBAB_CASE` | `myPropName` | `"my-prop-name"` |
| `PASCAL_CASE`| `myPropName` | `"MyPropName"` |
