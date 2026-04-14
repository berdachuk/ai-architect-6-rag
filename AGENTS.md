# AI Context (Root) — DocuRAG

Repo purpose: **DocuRAG** is a medical-document Retrieval Augmented Generation (RAG) demo: ingest JSONL/CSV + PDFs, chunk, embed into **PostgreSQL + pgvector**, retrieve, and answer via **Spring AI**. It also includes a black-box E2E suite and a MkDocs documentation site.

**Stack:** Java 21, Maven, Spring Boot 4, Spring Modulith, JDBC (no JPA), Flyway, Testcontainers, PostgreSQL+pgvector, Thymeleaf demo UI.

## Repo Map

```text
.
├── AGENTS.md
├── .claude/skills/                 # single source of truth for agent skills
├── .run/                           # IntelliJ run/debug configs
├── docu-rag/                       # Spring Boot app (Modulith modules live under com.berdachuk.docurag.*)
├── docu-rag-e2e/                   # Cucumber + Playwright black-box E2E (OpenAPI client)
├── docu-rag-parent/                # Maven reactor: docu-rag + docu-rag-e2e
├── docs/                           # PRD, architecture, dev guide (MkDocs source)
└── scripts/                        # full-build-and-e2e helpers
```

## Architecture (High Level)

DocuRAG is a **Modulith modular monolith** (module boundaries via `package-info.java`). Public surfaces:

- Web UI: Thymeleaf pages (`/`, `/qa`, `/documents`, `/evaluation`)
- REST API: `/api/**` (source of truth: `docu-rag/api/openapi.yaml`)

Conceptual flow: **documents → chunking → vector → retrieval → llm**, with **evaluation/extraction/visualization** as sidecar capabilities and **web** as the edge.

## Commands

```bash
# App (unit + integration + Modulith checks; Docker required for Testcontainers)
cd docu-rag && mvn verify

# E2E (reactor builds app JAR then runs Cucumber/Playwright)
cd docu-rag-parent && mvn verify

# One-shot full build + E2E (+ optional docker down -v)
./scripts/full-build-and-e2e.sh --teardown-volumes

# Docs
mkdocs build -s
```

## Global Boundaries

- ✅ OK: add/modify Modulith modules, REST endpoints, ingestion, chunking, retrieval, evaluation, docs.
- ⚠️ Must update OpenAPI: any change to `/api/**` controllers or DTOs must also update `docu-rag/api/openapi.yaml` (E2E client depends on it).
- ⚠️ DB changes: schema changes must be versioned Flyway migrations (`docu-rag/src/main/resources/db/migration`).
- 🚫 Do not introduce JPA/Hibernate (JDBC only).
- 🚫 Do not call a live LLM/embedding provider from automated tests (use `TestAIConfig` via `test`/`e2e` profiles).

## Skills (Single Source Of Truth)

Skills live only under `.claude/skills/**/SKILL.md`. Load the smallest skill(s) that cover the task.

| Skill | When to use |
|------|-------------|
| `core-architecture` | module ownership, Modulith boundaries, dependency decisions |
| `domain-modeling` | documents/chunks/evaluation domain changes, invariants, IDs |
| `repository-design` | SQL/JDBC, pgvector, migrations, named parameters |
| `api-design` | REST + Thymeleaf wiring, DTOs, OpenAPI alignment |
| `ai-provider` | Spring AI custom endpoints, profiles, TestAIConfig behavior |
| `testing` | unit/IT/E2E strategy, fixtures, Testcontainers, Playwright |

## Module Guidance

- App: `docu-rag/AGENTS.md`
- E2E: `docu-rag-e2e/AGENTS.md`
- Docs: `docs/AGENTS.md`

## Links

- PRD: `docs/DocuRAG-PRD.md`
- Architecture: `docs/DocuRAG-ARCHITECTURE.md`
- Developer guide: `docs/DocuRAG-DEVELOPER-GUIDE.md`
- AI context strategy: `docs/ai-context-strategy.md`

