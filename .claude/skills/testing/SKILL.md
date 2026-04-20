# Testing (DocuRAG)

## Description

How to verify DocuRAG safely and deterministically: unit tests, Testcontainers integration tests, and the black-box E2E module.

## When to use

- Adding/changing ingestion, chunking, vector indexing, retrieval, LLM, evaluation
- Debugging failures in `mvn test` / `mvn verify`
- Updating OpenAPI and ensuring E2E stays green

## Instructions

### Test layers

- **Unit tests** (`mvn test`)
  - Pure logic: chunking algorithm, parsing utilities, DTO mapping
- **Integration tests** (`mvn verify`)
  - Testcontainers Postgres+pgvector + Flyway migrations + Spring context wiring
  - Runs with **stub AI** beans under profile `test` (see `TestAIConfig`)
- **Black-box E2E** (`docu-rag-e2e`)
  - Recommended: `mvn verify` from repository root (reactor `pom.xml`)
  - Uses OpenAPI-generated client + Playwright UI checks

### Non-negotiables

- Automated tests must not require a live chat/embedding provider.
- Any `/api/**` change requires updating `docu-rag/docs/openapi.yaml` and re-running E2E.

## Boundaries

- Don’t introduce calls to real AI providers in default test profiles.
- Don’t commit large binary fixtures; keep PDF fixtures tiny and deterministic.

