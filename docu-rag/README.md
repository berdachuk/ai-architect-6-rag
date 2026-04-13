# DocuRAG

**Developer guide (build, Docker DB, manual test):** [`docs/DocuRAG-DEVELOPER-GUIDE.md`](../docs/DocuRAG-DEVELOPER-GUIDE.md)

Medical document **RAG** application: Spring Boot 4, Spring Modulith, PostgreSQL + **pgvector**, Spring AI 2.0.0-M4 (OpenAI-compatible HTTP), Thymeleaf demo UI, JDBC + Flyway (no JPA). **Spring Boot 4** uses **`spring-boot-starter-flyway`** for migrations; PostgreSQL also needs **`flyway-database-postgresql`** (Flyway 10+), or startup fails with `Unsupported Database: PostgreSQL …`.

## Data sources

- **Primary corpus:** [Sagarika-Singh-99/medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus) — export JSONL/JSON (or CSV) and point `DOCURAG_CORPUS_PATH` at the file or directory. **Subset policy + example manifest:** [`data/corpus/README.md`](data/corpus/README.md) and [`data/corpus/subset-manifest.example.json`](data/corpus/subset-manifest.example.json).
- **PDF demo:** open English medical PDFs; see [`data/pdf-demo/README.md`](data/pdf-demo/README.md). Set `DOCURAG_PDF_DEMO_PATH` to a folder containing `.pdf` files (recommended local-only folder: `data/pdf-demo/downloaded/`, PDFs are gitignored).

## Requirements

- **JDK 21**, **Maven 3.9+**, **Docker** (for Testcontainers during `mvn verify` and optional local Postgres via Compose).

## Quick start (local Postgres + Ollama)

1. Start database: `docker compose -f compose.yaml up -d`
2. Start [Ollama](https://ollama.com/) locally (default OpenAI-compatible base URL is already **`http://localhost:11434/v1`** in `application.yml`). Override with **`OLLAMA_BASE_URL`**, or set **`CHAT_BASE_URL`** / **`EMBEDDING_BASE_URL`** separately if chat and embeddings use different hosts.
3. Create local profile config (gitignored): copy `src/main/resources/application-local.example.yml` to `src/main/resources/application-local.yml`. The defaults match Compose credentials (`docurag` / `docurag` on host port **`localhost:5433`**).
4. Run the app with profile **`local`**:

```text
set CHAT_MODEL=gemma4:31b-cloud
set EMBEDDING_MODEL=nomic-embed-text
mvn -q spring-boot:run -Dspring-boot.run.profiles=local
```

Optional: `CHAT_API_KEY` / `EMBEDDING_API_KEY` (often empty for Ollama).

5. Open `http://localhost:8080/` — ingest via **Documents** (uses `docurag.ingestion.*` paths from `application.yml` / env), then call **`POST /api/index/rebuild`**, then use **QA**.

## Run / debug in IntelliJ IDEA

This repo includes IntelliJ run configurations under `.run/` at the repository root:

- **DocuRAG (local)**: `SPRING_PROFILES_ACTIVE=local` (expects you copied `src/main/resources/application-local.example.yml` → `application-local.yml`)
- **DocuRAG (e2e stub AI)**: `SPRING_PROFILES_ACTIVE=e2e` (stub AI, port **18080**, points at the local JSONL sample if present)
- **DocuRAG (eval-cli, stub AI)**: `SPRING_PROFILES_ACTIVE=e2e,eval-cli` (runs one evaluation and exits)

Steps:

1. Open the repository in IntelliJ IDEA (JDK **21**).
2. Start Postgres: `docker compose -f compose.yaml up -d` (from `docu-rag/`).
3. Select a **DocuRAG** run configuration from the Run/Debug dropdown.
4. Click **Debug** (bug icon) to attach the debugger and use breakpoints as usual.

Ports:

- `local` profile: `http://localhost:8080/`
- `e2e` profile: `http://localhost:18080/`

## REST (summary)

| Area          | Examples                                                                                                              |
|---------------|-----------------------------------------------------------------------------------------------------------------------|
| Documents     | `POST /api/documents/ingest`, `GET /api/documents`, `GET /api/documents/{id}`, `GET /api/documents/categories`        |
| Index         | `POST /api/index/rebuild`, `GET /api/index/status` (`POST /api/index/incremental` returns 501)                        |
| RAG           | `POST /api/rag/ask`, `POST /api/rag/analyze`                                                                          |
| Visualization | `GET /api/visualizations/categories/pie`, `GET /api/visualizations/entities/graph`                                    |
| Evaluation    | `POST /api/evaluation/run`, `GET /api/evaluation/runs`, `GET /api/evaluation/runs/{id}`, `GET /api/evaluation/latest` |

**OpenAPI:** machine-readable contract for the REST surface lives in [`docs/openapi.yaml`](docs/openapi.yaml). The sibling module [`../docu-rag-e2e`](../docu-rag-e2e) generates a Java client from that file (`mvn generate-test-sources` in `docu-rag-e2e`). Update the YAML when controllers or DTOs change, then regenerate.

## Evaluation CLI (no web port)

With DB and AI env vars set as above:

```text
mvn -q spring-boot:run -Dspring-boot.run.profiles=local,eval-cli -- --dataset=medical-rag-eval-v1
```

Optional: `--topK=5`, `--minScore=0.5`, `--semanticPassThreshold=0.8`. The process runs one evaluation and exits.

## Build & tests

```text
mvn verify
```

Uses **Testcontainers** (`pgvector/pgvector:pg16`), Flyway, stub **ChatModel** / **EmbeddingModel** (`@Profile("test")`). **No live Ollama required.**

To skip tests: `mvn -q package -DskipTests` (not recommended for CI).

### Black-box E2E (reactor: Compose + fat JAR + Cucumber)

End-to-end tests live in [`../docu-rag-e2e`](../docu-rag-e2e). Run them **after** packaging this module from the parent reactor:

```text
cd ../docu-rag-parent
mvn verify
```

**One-shot full build + E2E + volume teardown** (from repo root; Git Bash / Linux / macOS):

```text
./scripts/full-build-and-e2e.sh --teardown-volumes
```

On Windows PowerShell:

```text
.\scripts\full-build-and-e2e.ps1 -TeardownVolumes
```

You need **Docker** for `docker compose` (Postgres on host port **5433** by default). UI scenarios use **Playwright**; install browsers once as described in the E2E module README. By default, E2E uses app port **18080** to avoid common collisions on `8080`. Optional Maven overrides: `-De2e.app.port=18081`, `-De2e.pg.port=5433`, `-De2e.compose.dir=...`, `-De2e.app.jar=...`. For `docker compose down -v` on E2E JVM shutdown, use profile **`-Pe2e-teardown-volumes`** (defined in `docu-rag-e2e/pom.xml`); the full-build scripts activate it when using `--teardown-volumes` / `-TeardownVolumes`.

## Product definition

[`docs/DocuRAG-PRD.md`](../docs/DocuRAG-PRD.md)

## Agent context

- **[AGENTS.md](AGENTS.md)**
- **[`.claude/skills/`](../.claude/skills/index.md)**

## Layout

```
docu-rag/
├── compose.yaml
├── pom.xml
├── data/pdf-demo/
├── src/main/java/com/berdachuk/docurag/   # Modulith modules + *.api named interfaces
├── src/main/resources/db/migration/
└── src/test/resources/fixtures/           # sample.jsonl for IT
```

Package root: **`com.berdachuk.docurag`**.
