# Introduction

**artifact-json** is a next-generation JSON processing library designed entirely for modern Java (17+).

## The Philosophy

Most existing Java JSON libraries were built in the Java 6-8 era. They carry immense amounts of legacy code, rely heavily on reflection, and force you into complex data-binding setups just to filter a few fields.

We built `artifact-json` with three core principles:
1. **Zero Dependencies**: Keep the application footprint under 500KB.
2. **Data-Centric Querying**: Treating JSON as a querying candidate, allowing SQL-like operations in-memory natively.
3. **Modern Java Features**: Sealed interfaces and Pattern Matching make traversing nodes safer and substantially faster.

## Features at a Glance

* **FastJsonEngine**: Zero-copy, high-performance RFC-8259 compliant parsing.
* **JsonQuery**: Execute `SELECT`, `WHERE`, `GROUP BY` safely over JSON arrays in streams using `.parallel()`.
* **JsonConverter**: Bidirectional, native conversion across **8 distinct formats**.
* **JsonCodeGenerator**: Instantly reverse-engineer massive remote API payloads into POJO Java records.
* **JsonProxy**: Abstract heavy configuration trees into zero-cost interfaces.
* **JsonStreamReader**: Pull-based file parsing for massive gigabyte files using constant (~8KB) memory.
