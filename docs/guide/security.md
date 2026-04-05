# Security Report

Artifact-JSON is designed with a **Security-First** mindset. As a zero-dependency library, we eliminate the primary vector for supply-chain attacks (transitive vulnerabilities).

## Core Security Pillars

### 1. Minimal Attack Surface
By avoiding external libraries or logging frameworks, Artifact-JSON has no external vulnerabilities to inherit. The entire codebase is under ~20k lines, making it easy to audit.

### 2. Reflection Safety
While we use reflection for POJO mapping, we strictly control visibility:
- Fields are only accessed if they are explicitly part of the class hierarchy.
- No support for executing arbitrary code or complex logic during deserialization (unlike Java's native serialization).

### 3. PII Protection (Data Masking)
The `@PII` annotation allows developers to mark sensitive fields (passwords, SSNs, phone numbers) to be automatically masked during serialization. This prevents sensitive data from accidentally leaking into logs or external APIs.

### 4. Recursion & DOS Protection
To prevent Denial-of-Service (DOS) attacks via maliciously deep JSON structures, `CustomObjectMapper` enforces a **MAX_DEPTH** (default: 100). Any structure deeper than this will trigger a `JsonMappingException` instead of causing a `StackOverflowError`.

### 5. Input Validation
The `@JsonValidate` annotation allows you to enforce strict constraints on incoming JSON data:
- **Required fields**: Prevent `NullPointerException` later in your business logic.
- **Regex matching**: Ensure strings (like emails or IDs) follow expected patterns.
- **Numeric ranges**: Prevent overflows or invalid states (e.g., age must be 18-120).

## Vulnerability Scan Results

| Check Category | Status | Details |
| :--- | :--- | :--- |
| **Transitive Dependencies** | Pass | 0 external dependencies found. |
| **Insecure Deserialization** | Pass | No execution of arbitrary methods; strict type mapping only. |
| **Stack Overflow (DOS)** | Pass | ENFORCED: Max recursion depth of 100. |
| **Data Leakage** | Pass | SUPPORTED: `@PII` for automatic masking. |
| **Java Version Security** | Pass | Built on Java 17+ using modern security standards. |

## Reporting a Vulnerability
If you discover a security issue, please contact the maintainers at [security@dhoondlayai.github.io](mailto:security@dhoondlayai.github.io).
