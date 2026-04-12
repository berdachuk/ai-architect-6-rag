# Repository Design (DocuRAG)

## Description

JDBC + SQL conventions for DocuRAG, including pgvector access patterns and migration discipline.

## When to use

- Writing or refactoring SQL repositories/services
- Adding/changing Flyway migrations
- Touching pgvector columns, casts, or similarity queries

## Instructions

- Prefer **`NamedParameterJdbcTemplate`** and **named parameters** (`:documentId`, `:limit`). Avoid positional `?`.
- Keep SQL explicit (text blocks or `.sql` resources). Avoid “SQL built by concatenation”.
- Use small, readable casts:
  - `CAST(:vectorLiteral AS vector)`
  - `CAST(:json AS jsonb)`
- Keep ownership clear:
  - `documents` owns `source_document` + ingestion tracking
  - `chunking` creates `document_chunk` text rows
  - `vector` writes `document_chunk.embedding`
  - `retrieval` owns similarity query shapes

### Migrations

- All schema changes go through Flyway under `docu-rag/src/main/resources/db/migration/`.
- Don’t edit already-applied migrations; add a new `V<N>__...sql`.

## Boundaries

- Do not introduce JPA/Hibernate.
- Do not change embedding dimensionality without coordinating schema + config + health checks + fixtures.

