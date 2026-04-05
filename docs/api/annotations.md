# Pro-Level Annotations

Artifact-JSON provides a set of powerful annotations to control serialization and deserialization without writing complex boilerplate.

## @JsonUnwrapped
Flatten a nested object's fields into the parent.

```java
public class Profile {
    private String name;
    
    @JsonUnwrapped(prefix = "addr_")
    private Address location;
}

// Result: {"name": "John", "addr_city": "NY", "addr_zip": "10001"}
```

## @JsonVirtual
Include the output of a method as a JSON field.

```java
public class ShoppingCart {
    private double itemPrice;
    private int quantity;

    @JsonVirtual("final_total")
    public double getTotal() {
        return itemPrice * quantity;
    }
}

// Result: {"itemPrice": 10.5, "quantity": 2, "final_total": 21.0}
```

## @JsonValidate
Enforce constraints on fields during deserialization.

```java
public class SignupRequest {
    @JsonValidate(required = true, regex = "^[A-Za-z0-9]+$")
    private String username;

    @JsonValidate(min = 1, max = 500)
    private int stockCount;
}

// If schema is violated, CustomObjectMapper throws JsonMappingException.
```

## Other Supported Annotations
- `JsonProperty`: Renaming fields.
- `JsonIgnore`: Hiding fields.
- `JsonInclude`: Controlling null/empty serialization.
- `JsonNaming`: Class-level strategies (Snake Case, etc.).
- `PII`: Masking sensitive data.
- `JsonDefault`: Fallback values for missing JSON keys.
- `JsonReadOnly` / `JsonWriteOnly`: Access control.
- `JsonAlias`: Alternate names for deserialization.
