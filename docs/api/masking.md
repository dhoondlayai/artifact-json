# PII Masking and Data Redaction

Security features run deep in `artifact-json`. If you have enterprise security constraints, GDPR data masking requirements, or HIPAA rules against showing plain-text IDs in logs, the **JsonMasker** combined with **JsonShield** will handle tree-wide data redaction natively!

## JsonMasker

`JsonMasker` accepts a standard masking string (like `****`) and an arbitrary number of sensitive keys. When invoked on a JsonNode, it traverses the *entire* tree (ignoring depth) and automatically redacts any case-insensitive matches. 

Crucially, **the original JSON Tree is never mutated**. It generates a completely new, safe tree.

```java
import io.github.dhoondlayai.artifact.json.extensions.JsonMasker;

JsonMasker masker = new JsonMasker("[REDACTED]")
    .addSensitiveKey("password")
    .addSensitiveKey("ssn")
    .addSensitiveKey("apiToken");

JsonObject basePayload = new JsonObject()
    .put("user", "dhoondlay")
    .put("password", "super_secret!")
    .put("history", new JsonObject().put("ssn", "xxx-xx-xxxx"));

JsonNode safePayload = masker.mask(basePayload);

System.out.println(safePayload);
```

**Output**:
```json
{
  "user": "dhoondlay",
  "password": "[REDACTED]",
  "history": {
    "ssn": "[REDACTED]"
  }
}
```

## JsonShield for In-Place Shielding

If you're already wrapping a tree in a `JsonShield` for safe-getter defaults, you can redact directly on the internal tree representation and pull out the masked tree using `.redact()`:

```java
JsonShield shield = new JsonShield(payloadNode);

// Retrieve safe getters
String id = shield.getString("user.id", "unknown");

// Quick-Redact multiple fields as varargs
JsonNode fullySafeTree = shield.redact("password", "token", "ssn");
```
