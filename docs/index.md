---
layout: home

# SEO Page Overrides
title: "artifact-json | World's Fastest JSON Library for Java 21+"
titleTemplate: false
description: "The ultimate high-performance, zero-dependency JSON framework for Java 21+. Built for raw speed with SQL querying, universal conversion, and zero-cost proxy mapping."

# Social/Search Meta
head:
  - - meta
    - name: "description"
      content: "Stop using heavy JSON libraries. artifact-json is a zero-dependency Java 21+ JSON utility offering SQL-like queries, 8-format conversion, and ultra-fast parsing with non-blocking NIO."
  - - meta
    - name: "keywords"
      content: "Java JSON library, Java 21, Fast JSON, Zero Dependency, JsonQuery, SQL JSON, Jackson Alternative"

hero:
  name: "artifact-json"
  text: "High-Performance JSON for Java 21+"
  tagline: "Zero dependencies. Full RFC 8259. Faster than Jackson. SQL-like query engine. 8-format converter."
  image:
    src: /logo.png
    alt: artifact-json logo
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
    details: Full RFC 8259 JSON parser with unicode escapes, scientific notation, and line/column error reporting. No Jackson, no Gson, zero bloat.

  - icon: 🔍
    title: SQL-Like JsonQuery Engine
    details: 15+ WHERE operators, ORDER BY, LIMIT, and aggregations (SUM, AVG) — execute SQL-like queries directly on live JSON trees.

  - icon: 🌊
    title: Stream API First
    details: Optimized for the modern JVM. Process millions of records with constant memory using non-blocking JsonStreamReader and parallel streams.

  - icon: 🔄
    title: 8-Format Universal Converter
    details: JSON ↔ CSV, XML, YAML, Properties, Markdown, and HTML Table. One library to rule all your data transformations.

  - icon: 🛡️
    title: Enterprise Security & Masking
    details: Native PII masking, RFC 6902 patch generation, and deep merge. Built for high-security, high-throughput enterprise gateways.

  - icon: 🚀
    title: Dynamic Proxy Mapping
    details: Bind any JsonObject to a typed Java interface at runtime with zero reflection and zero deserialization overhead.
---

<div align="center">
  <br/>
  <h2>Why Choose artifact-json?</h2>
  <p>Built for the 2026+ Java ecosystem, <strong>artifact-json</strong> leverages modern JVM features like <em>Sealed Interfaces</em>, <em>MethodHandles</em>, and <em>SIMD-friendly loops</em> to deliver performance that legacy libraries cannot match.</p>
</div>

