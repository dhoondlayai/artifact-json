# Converters — Format Conversion Suite

`JsonConverter` is a unified facade for all format conversions.
All conversions are **zero-dependency** — no external libraries required.

```java
import io.github.dhoondlayai.artifact.json.convert.JsonConverter;
```

## JSON → String (Pretty / Compact / Minified)

```java
// Pretty (2-space indent)
String pretty = JsonConverter.toPrettyString(node, 2);

// Compact (default .toString())
String compact = JsonConverter.toCompactString(node);

// Minified (same as compact — no whitespace)
String min = JsonConverter.toMinified(node);
```

**Pretty output example:**
```json
{
  "name": "Alice",
  "scores": [
    95,
    87,
    100
  ]
}
```

## JSON → CSV

```java
// Default: comma delimiter, with header row
String csv = JsonConverter.toCsv(usersArray);

// Custom delimiter (TSV)
String tsv = JsonConverter.toCsv(usersArray, '\t', true);
```

**Output:**
```
name,age,role
Alice,30,admin
Bob,25,user
```

## CSV → JSON

```java
// Auto-detects types (int, double, boolean, null, String)
JsonArray users = JsonConverter.fromCsv(csvString);

// Custom delimiter
JsonArray data  = JsonConverter.fromCsv(tsvString, '\t');
```

Values like `"true"`, `"false"`, `"null"`, `"42"`, `"3.14"` are automatically
typed — no manual conversion needed.

## JSON → XML

```java
String xml = JsonConverter.toXml(root, "response");
```

**Output:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<response>
  <store>
    <name>Artifact Books</name>
    <books>
      <title>1984</title>
      <price>12.5</price>
    </books>
  </store>
</response>
```

Arrays produce repeated elements using the parent field name.

## JSON → YAML

```java
String yaml = JsonConverter.toYaml(node);
```

**Output:**
```yaml
name: Artifact Books
location: Silicon Valley
rating: 4.8
active: true
```

## YAML → JSON

```java
JsonNode node = JsonConverter.fromYaml(yamlString);
```

Supports flat key-value YAML and simple nested objects.

## JSON → Java Properties

Nested keys are flattened with dot-notation:

```java
String props = JsonConverter.toProperties(node);
```

**Output:**
```properties
store.name=Artifact Books
store.location=Silicon Valley
store.books.0.title=1984
```

## JSON → Markdown Table

```java
String md = JsonConverter.toMarkdownTable(usersArray);
```

**Output:**
```markdown
| name  | age | role  |
| ---   | --- | ---   |
| Alice | 30  | admin |
| Bob   | 25  | user  |
```

Paste directly into GitHub README or any Markdown renderer.

## JSON → HTML Table

```java
// With CSS class
String html = JsonConverter.toHtmlTable(usersArray, "data-table");

// Without CSS class
String html = JsonConverter.toHtmlTable(usersArray);
```

**Output:**
```html
<table class="data-table">
<thead>
<tr><th>name</th><th>age</th><th>role</th></tr>
</thead>
<tbody>
<tr><td>Alice</td><td>30</td><td>admin</td></tr>
<tr><td>Bob</td><td>25</td><td>user</td></tr>
</tbody>
</table>
```

## Conversion Summary Table

| From | To | Method |
|------|----|--------|
| JsonNode | Pretty String | `toPrettyString(node, 2)` |
| JsonNode | Compact JSON | `toCompactString(node)` |
| JsonArray | CSV | `toCsv(array)` |
| CSV String | JsonArray | `fromCsv(csv)` |
| JsonNode | XML | `toXml(node, "root")` |
| JsonNode | YAML | `toYaml(node)` |
| YAML String | JsonNode | `fromYaml(yaml)` |
| JsonNode | `.properties` | `toProperties(node)` |
| JsonArray | Markdown Table | `toMarkdownTable(array)` |
| JsonArray | HTML Table | `toHtmlTable(array)` |
