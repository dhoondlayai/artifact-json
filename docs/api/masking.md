# PII Masking and Data Redaction

`artifact-json` gives you two complementary layers of PII protection:

| Layer | When to use |
|---|---|
| **`@PII` annotation** | You own the POJO — mark the field once, it's always masked |
| **`JsonMasker` / `JsonShield`** | You're working with a raw `JsonNode` tree you don't own a POJO for |

---

## Recommended: `@PII` Annotation

The cleanest way to protect sensitive fields is to annotate them directly on your POJO. `CustomObjectMapper` will auto-mask them on every `.serialize()` call — no extra wiring required.

```java
public class UserProfile {
    private String username;
    private String email;

    @PII
    private String password;        // → "****"

    @PII("[REDACTED]")
    private String ssn;             // → "[REDACTED]"

    @PII(mask = "***", audit = true)
    private String apiToken;        // → "***" and logged to audit trail
}
```

```java
CustomObjectMapper mapper = new CustomObjectMapper();
JsonNode safeJson = mapper.serialize(userProfile);
// password, ssn, and apiToken are automatically masked — always
```

See the full `@PII` reference in [Annotations](./annotations.md#pii--automatic-pii-masking).

---

## Runtime Option: `JsonMasker`

Use `JsonMasker` when you have a `JsonNode` tree at runtime and no POJO — for example, masking a third-party API response before logging it.

`JsonMasker` traverses the **entire** tree (case-insensitive key matching, any depth) and returns a **new, safe tree** — the original is never mutated.

```java
import io.github.dhoondlayai.artifact.json.extensions.JsonMasker;

JsonMasker masker = new JsonMasker("[REDACTED]")
    .addSensitiveKey("password")
    .addSensitiveKey("ssn")
    .addSensitiveKey("apiToken");

JsonNode safePayload = masker.mask(incomingPayload);
```

**Input:**
```json
{ "user": "dhoondlay", "password": "super_secret!", "history": { "ssn": "xxx-xx-xxxx" } }
```

**Output:**
```json
{
  "user": "dhoondlay",
  "password": "[REDACTED]",
  "history": { "ssn": "[REDACTED]" }
}
```

---

## Runtime Option: `JsonShield.redact()`

If you're already using `JsonShield` for safe dotted-path access, you can redact multiple fields in one varargs call:

```java
JsonShield shield = new JsonShield(payloadNode);

String userId = shield.getString("user.id", "unknown");  // safe dotted-path read

// Redact before logging / forwarding
JsonNode safe = shield.redact("password", "token", "ssn");
```

---

## Decision Guide

```
Do you own a POJO?
  ├─ YES → use @PII on the field  (compile-time guarantee, always safe)
  └─ NO  → working with raw JsonNode tree?
              ├─ Simple masking   → JsonMasker
              └─ Already using JsonShield → shield.redact(...)
```
