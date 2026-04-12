# API Design (DocuRAG)

## Description

Conventions for DocuRAG’s REST + Thymeleaf edge (`web` module) and how to keep the OpenAPI contract and E2E suite aligned.

## When to use

- Adding/changing REST endpoints under `com.berdachuk.docurag.web.rest`
- Updating DTOs used by `/api/**`
- Wiring demo pages under `com.berdachuk.docurag.web.pages` / `src/main/resources/templates`

## Instructions

- Treat `docu-rag/docs/openapi.yaml` as the **contract** for `/api/**`.
  - If you change controllers or DTOs, update the YAML.
  - Re-run `docu-rag-e2e` generation (`mvn generate-test-sources` / `mvn verify`).
- Keep REST and page concerns separate:
  - REST: JSON DTOs only, stable paths under `/api/...`
  - Pages: server-rendered Thymeleaf demo UX under `/...`
- Prefer explicit request/response DTOs (records) in the owning module’s `*.api` package.

## Boundaries

- Don’t change REST paths/DTOs without updating OpenAPI (E2E depends on it).
- Don’t add authentication/authorization semantics without explicit human approval (out of PRD v1).

