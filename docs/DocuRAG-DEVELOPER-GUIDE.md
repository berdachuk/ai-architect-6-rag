# DocuRAG — developer guide

How to **build**, **run Postgres in Docker**, **start the Spring Boot app**, and **manually verify** the main flows. For product scope and milestones, see [DocuRAG-PRD.md](DocuRAG-PRD.md) and [DocuRAG-IMPLEMENTATION-PLAN-WBS.md](DocuRAG-IMPLEMENTATION-PLAN-WBS.md).

---

## Prerequisites

| Tool                                        | Notes                                                          |
|---------------------------------------------|----------------------------------------------------------------|
| **JDK 21**                                  | `java -version`                                                |
| **Maven 3.9+**                              | `mvn -version`                                                 |
| **Docker**                                  | Engine + Compose v2 (`docker compose version`)                 |
| **Ollama** (or other OpenAI-compatible API) | Chat + embeddings; defaults assume `http://localhost:11434/v1` |

---

## 1. Database with Docker Compose

The app expects **PostgreSQL 16 with pgvector**. A Compose file is already in the module:

**File:** [`docu-rag/compose.yaml`](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/compose.yaml)

| Setting         | Value                                                                                |
|-----------------|--------------------------------------------------------------------------------------|
| Image           | `pgvector/pgvector:pg16`                                                             |
| Database        | `docurag`                                                                            |
| User / password | `docurag` / `docurag`                                                                |
| Host port       | **5433** (mapped to container `5432`, avoids clashing with a local Postgres on 5432) |

### Commands (from `docu-rag/`)

Start in the background:

```bash
cd docu-rag
docker compose up -d
```

Check health (Compose defines a `healthcheck`):

```bash
docker compose ps
```

Stop and **remove containers** (data volume kept unless you add `-v`):

```bash
docker compose down
```

Stop and **delete the database volume** (fresh DB next time):

```bash
docker compose down -v
```

Logs:

```bash
docker compose logs -f postgres
```

### JDBC URL for local profile

Match Compose: **`localhost:5433`**, database **`docurag`**.

Local profile config is intentionally **not committed** (to keep machine-specific URLs and secrets out of git). Copy the template:

- `docu-rag/src/main/resources/application-local.example.yml` → `docu-rag/src/main/resources/application-local.yml`

Then edit `spring.datasource.*` if your ports/credentials differ.

---

## 2. Build

From the **DocuRAG module** (fat JAR for demos / E2E):

```bash
cd docu-rag
mvn clean package -DskipTests
```

JAR output: `docu-rag/target/docu-rag-0.1.0-SNAPSHOT.jar`

Full **unit + integration + Modulith** (requires Docker for Testcontainers):

```bash
cd docu-rag
mvn verify
```

**Reactor** (app + black-box E2E against Compose + JAR):

```bash
cd docu-rag-parent
mvn clean verify
```

One-shot **clean build + E2E + Compose teardown** from repo root (see [scripts/README.md](https://github.com/berdachuk/ai-architect-6-rag/blob/main/scripts/README.md)): stops the stack with `docker compose down -v` before and after, rebuilds the reactor, runs the full Cucumber/Playwright suite, prints report paths, and ends with **`STATUS: SUCCESS`** or **`STATUS: FAILED`**. Volume teardown uses Maven profile **`e2e-teardown-volumes`** (not a raw `-D` flag) so Surefire stays quiet.

```bash
./scripts/full-build-and-e2e.sh --teardown-volumes
```

Windows:

```powershell
.\scripts\full-build-and-e2e.ps1 -TeardownVolumes
```

Manual equivalent (same profile the scripts activate):

```bash
mvn -f docu-rag-parent/pom.xml clean verify -Pe2e-teardown-volumes
```

---

## 3. Start the application

1. **Postgres** must be up (`docker compose up -d` in `docu-rag/`).
2. **Ollama** (or your provider) must expose chat + embedding APIs compatible with Spring AI `custom` client settings in [`application.yml`](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/src/main/resources/application.yml).

### Run with Maven (typical)

From `docu-rag/`:

```bash
cd docu-rag
export CHAT_MODEL=gemma4:31b-cloud
export EMBEDDING_MODEL=nomic-embed-text
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Windows (cmd):

```text
set CHAT_MODEL=gemma4:31b-cloud
set EMBEDDING_MODEL=nomic-embed-text
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Profile `local`** activates `application-local.yml` (created from the template above). If that file contains machine-specific IPs (embedding URLs), override with environment variables, for example:

```bash
export EMBEDDING_BASE_URL=http://localhost:11434/v1
export CHAT_BASE_URL=http://localhost:11434/v1
```

Then run with `local` as above.

### Run the packaged JAR

```bash
cd docu-rag
java -jar target/docu-rag-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Default HTTP port: **8080**.

Tip: if you want to run without a live Ollama/LLM, start with profile **`e2e`** instead (it uses stub AI beans and port **18080**):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=e2e
```

---

## 4. Manual testing (happy path)

Use the **Thymeleaf UI** at `http://localhost:8080/` or the REST API (OpenAPI: [`docu-rag/docs/openapi.yaml`](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/docs/openapi.yaml)).

### 4.1 Health

```bash
curl -s http://localhost:8080/actuator/health | jq .
```

Expect top-level `"status":"UP"` and DB/vector components healthy after Flyway has run.

### 4.2 Ingest documents

Point ingestion at a **JSONL/CSV directory or file** and/or **PDF folder** via env (see [`application.yml`](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/src/main/resources/application.yml) `docurag.ingestion.*`):

```bash
export DOCURAG_CORPUS_PATH=/path/to/export.jsonl
export DOCURAG_PDF_DEMO_PATH=/path/to/pdf-folder
```

Restart the app if you changed env vars, or call the API with explicit paths:

```bash
curl -s -X POST http://localhost:8080/api/documents/ingest \
  -H "Content-Type: application/json" \
  -d '{"paths":["/absolute/path/to/sample.jsonl"]}'
```

Subset policy example: [`docu-rag/data/corpus/README.md`](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/data/corpus/README.md).

#### PDF demo (local-only binaries)

This repo treats PDF binaries as **local-only** (gitignored). A suggested folder is:

- `docu-rag/data/pdf-demo/downloaded/`

Ingest PDFs by passing the folder path:

```bash
curl -s -X POST http://localhost:8080/api/documents/ingest \
  -H "Content-Type: application/json" \
  -d '{"paths":["/absolute/path/to/docu-rag/data/pdf-demo/downloaded"]}' | jq .
```

Verify the PDF documents exist (look for `"sourceFormat":"pdf"`):

```bash
curl -s "http://localhost:8080/api/documents?page=25&size=20" | jq .
```

### 4.3 Build the vector index

```bash
curl -s -X POST http://localhost:8080/api/index/rebuild
```

### 4.4 Index status

```bash
curl -s http://localhost:8080/api/index/status | jq .
```

Confirm non-zero document/chunk/embedded counts when indexing finished.

### 4.5 RAG question (REST)

```bash
curl -s -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"What is hypertension?","topK":5,"minScore":0.3}' | jq .
```

### 4.6 UI smoke

| Page | Path |
|------|------|
| Dashboard | `/` |
| QA | `/qa` |
| Documents | `/documents` |
| Analysis | `/analysis` |
| Evaluation | `/evaluation` |

Medical **disclaimer** should appear on interactive pages (NFR).

### 4.7 Evaluation (API)

Dataset name is seeded (e.g. `medical-rag-eval-v1`):

```bash
curl -s -X POST http://localhost:8080/api/evaluation/run \
  -H "Content-Type: application/json" \
  -d '{"datasetName":"medical-rag-eval-v1","topK":3,"minScore":0.0,"semanticPassThreshold":0.5}' | jq .
```

---

## 5. Evaluation CLI (optional)

Runs evaluation without serving the web UI (still needs DB + AI). From `docu-rag/`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local,eval-cli -- --dataset=medical-rag-eval-v1
```

---

## 6. Stop everything

1. Stop the Spring Boot process (Ctrl+C in the terminal where it runs).
2. Stop Postgres:

```bash
cd docu-rag
docker compose down
```

Use `docker compose down -v` only when you want a **wiped** database volume.

---

## 7. Troubleshooting

| Symptom | Check |
|---------|--------|
| App cannot connect to DB | `docker compose ps`; JDBC uses **`localhost:5433`** for Compose mapping. |
| Flyway / “Unsupported Database: PostgreSQL” | Use Boot Flyway starter + `flyway-database-postgresql` (see [`docu-rag/README.md`](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/README.md)). |
| Embeddings/chat fail | `CHAT_BASE_URL` / `EMBEDDING_BASE_URL` end with **`/v1`** for Ollama-style servers; models pulled in Ollama. |
| Port 8080 in use | `--server.port=8081` on the command line or `SERVER_PORT` env. |

---

## Related links

| Doc | Purpose |
|-----|---------|
| [`docu-rag/README.md`](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/README.md) | Module overview, REST summary, E2E pointer |
| [`docu-rag/AGENTS.md`](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/AGENTS.md) | Agent/convention entry |
| [`docu-rag-e2e/README.md`](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag-e2e/README.md) | Automated black-box E2E |
| [`docs/submission/README.md`](submission/README.md) | Course submission checklist |
