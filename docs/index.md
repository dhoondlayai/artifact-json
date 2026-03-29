---
layout: home

hero:
  name: "artifact-json"
  text: "High-Performance JSON for Java 21+"
  tagline: "Zero dependencies. Full RFC 8259. Faster than Jackson. SQL-like query engine. 8-format converter."
  image:
    src: /logo.svg
    alt: artifact-json
  actions:
    - theme: brand
      text: Get Started →
      link: /guide/getting-started
    - theme: alt
      text: View on GitHub
      link: https://github.com/dhoondlay/artifact-json

features:
  - icon: ⚡
    title: Zero-Dependency Parser
    details: Full RFC 8259 JSON parser with unicode escapes, scientific notation, and line/column error reporting. No Jackson, no Gson needed.

  - icon: 🔍
    title: SQL-Like JsonQuery Engine
    details: 15+ WHERE operators, ORDER BY, LIMIT, OFFSET, PAGE, GROUP BY, JOIN, DISTINCT, and aggregation (SUM, AVG, MIN, MAX, COUNT) — all on live JSON trees.

  - icon: 🌊
    title: Stream API First
    details: Every JsonObject and JsonArray exposes stream() and parallelStream(). Process millions of records with constant memory using JsonStreamReader.

  - icon: 🔄
    title: 8-Format Converter
    details: JSON ↔ CSV, XML, YAML, Properties, Markdown Table, HTML Table, compact string and pretty-print — zero external libraries.

  - icon: 🛡️
    title: Enterprise Ready
    details: PII masking, RFC 6902 patch generation, deep merge, self-healing parser, JsonShield safe access, full exception hierarchy with context.

  - icon: 🏷️
    title: Jackson-Compatible Annotations
    details: "@JsonProperty, @JsonIgnore, @JsonNaming with SNAKE_CASE, KEBAB_CASE, and PASCAL_CASE conventions."

  - icon: 🚀
    title: Dynamic Proxy Mapping
    details: Map any JsonObject to a typed Java interface at runtime — zero reflection, zero deserialization cost.

  - icon: 🛠️
    title: Code Generator
    details: Reverse-engineer any JSON tree into Java Record source code automatically.
---
