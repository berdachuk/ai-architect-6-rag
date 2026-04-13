# DocuRAG (App) — Agent Guide

DocuRAG is the Spring Boot application under `docu-rag/` (Java 21, Spring Boot 4, Spring Modulith, JDBC + Flyway, PostgreSQL+pgvector, Spring AI, Thymeleaf demo UI).

## Module Boundaries (Modulith)

Application modules are under `com.berdachuk.docurag.<module>` and declared via `package-info.java`.

- Edge: `web` (Thymeleaf + REST controllers)
- Core modules: `documents`, `chunking`, `vector`, `retrieval`, `llm`
- Sidecar modules: `evaluation`, `extraction`, `visualization`
- Shared: `core` (OPEN)

Rule: other modules should only depend on `...<module>.api.*` where possible.

## Profiles / Runtime

- `local`: typical dev run on **8080** (expects local-only `application-local.yml`, created from the template)
- `test`: automated tests (stub AI beans)
- `e2e`: black-box run (stub AI beans, port **18080**, datasource `localhost:5433`)
- `eval-cli`: runs one evaluation and exits (combine with `local` or `e2e`)

Local config template:

- `src/main/resources/application-local.example.yml` → `src/main/resources/application-local.yml` (gitignored)

## Key Contracts

- REST surface: `docs/openapi.yaml` (keep aligned with controllers under `com.berdachuk.docurag.web.rest`)
- DB schema: Flyway migrations under `src/main/resources/db/migration`
- IDs: 24-hex ObjectId-style strings (`IdGenerator`)

## Commands

```bash
cd docu-rag
mvn test
mvn verify
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Constraints

- JDBC only (no JPA). Prefer `NamedParameterJdbcTemplate` and named bind variables.
- Do not call live AI in tests; use `TestAIConfig` (`test`/`e2e` profiles).

## Skills (Canonical)

Skills live at repo root: `../.claude/skills/`

- `core-architecture`, `domain-modeling`, `repository-design`, `api-design`, `ai-provider`, `testing`

