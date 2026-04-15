# docu-rag-e2e

Black-box E2E for DocuRAG: **Cucumber** (API + CLI) and **Playwright** (UI). REST calls use the **OpenAPI-generated** okhttp-gson client — do not hand-roll HTTP for covered endpoints (raw **REST Assured** is only used for HTML and paths not in the spec, e.g. `GET /`, `GET /api/rag/history/{id}`).

**Note:** `maven.clean.skip=true` is set for this module so `mvn clean` from the reactor does not delete `docu-rag-e2e/target` (avoids Windows file-lock failures on old Cucumber HTML under `target/cucumber-reports`). Cucumber reports are written under `build/cucumber-reports/`. To wipe E2E outputs completely, delete `docu-rag-e2e/target` and `docu-rag-e2e/build` when no test JVM is running.

## Prerequisites

- **JDK 21**, **Maven 3.9+**
- **Docker** (`docker compose` or `docker-compose`) — brings up Postgres from [`../docu-rag/compose.yaml`](../docu-rag/compose.yaml) (host port **5433**)
- **Playwright browsers** (once per machine), from this directory:

```bash
mvn exec:java -e -Dexec.classpathScope=test -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

If the download is slow or blocked, set a proxy or retry; UI scenarios need Chromium available locally.

## Run (recommended)

Build the app and run all E2E from the **reactor** (ensures the fat JAR exists):

```bash
cd ../docu-rag-parent
mvn verify
```

### Full clean build + E2E + remove DB volumes

From the **repository root**:

```bash
./scripts/full-build-and-e2e.sh --teardown-volumes
```

Windows (PowerShell):

```powershell
.\scripts\full-build-and-e2e.ps1 -TeardownVolumes
```

This runs `docker compose down -v` before and after Maven, `mvn clean verify` on `docu-rag-parent`, prints Surefire/Cucumber report paths, and exits with Maven’s status code. With `--teardown-volumes` / `-TeardownVolumes`, the **`e2e-teardown-volumes`** Maven profile (defined in this module’s `pom.xml`) sets `e2e.compose.down.removeVolumes=true` so the test JVM’s shutdown hook also tears down Compose with **`-v`**.

**Success criteria:** Maven ends with `BUILD SUCCESS` and the script prints `STATUS: SUCCESS (mvn exit 0)`. Cucumber should report **17 scenarios** (all passed) and Surefire **18 tests** (Cucumber runner + `DocuRagClientFactoryTest`). Open `build/cucumber-reports/e2e-report.html` for the HTML report.

### Reports

After a run, open:

- `docu-rag-e2e/target/surefire-reports/` — JUnit / Surefire text and XML
- `docu-rag-e2e/build/cucumber-reports/e2e-report.html` — Cucumber HTML (under `build/`, not `target/`, so `mvn clean` does not fight Windows file locks on the report)
- `docu-rag-e2e/build/cucumber-reports/cucumber.xml` — Cucumber JUnit XML

## Run (E2E module only)

Requires an existing JAR at `../docu-rag/target/docu-rag-0.1.0-SNAPSHOT.jar`:

```bash
mvn verify
```

## Configuration (Maven / JVM system properties)

| Property | Default | Purpose |
|----------|---------|---------|
| `e2e.compose.dir` | `${project.basedir}/../docu-rag` | Directory containing `compose.yaml` |
| `e2e.app.jar` | `../docu-rag/target/docu-rag-0.1.0-SNAPSHOT.jar` | Spring Boot repackaged JAR |
| `e2e.app.port` | `18080` | `server.port` for the app under test (chosen to avoid common local collisions with local default `8084`) |
| `e2e.pg.port` | `5433` | Host port for Postgres (Compose mapping) |
| `e2e.compose.down.removeVolumes` | `false` | If `true`, shutdown hook runs `docker compose down -v --remove-orphans`. Prefer profile **`-Pe2e-teardown-volumes`** (used by full-build scripts) instead of `-D` to avoid Surefire duplicate-property warnings. |
| `e2e.compose.up.timeoutMinutes` | `20` | Max wait for `docker compose up -d` (image pull can be slow) |

Compose and `docker info` output are appended to **`target/docurag-e2e-compose.log`** and **`target/docurag-e2e-docker-info.log`** (under the `docu-rag-e2e` module) so the child process never blocks on a full stdout pipe. A one-time **`target/docurag-e2e-compose-plugin-probe.log`** records whether `docker compose version` succeeded; if the Compose V2 plugin is missing (common on some Windows installs), E2E uses the standalone **`docker-compose`** binary only.

After the DB TCP port is open, the harness runs **`docker compose exec … pg_isready`** (or `docker-compose exec`) so the app JAR is not started before Postgres accepts connections (avoids Flyway/Hikari racing a container that is still initializing).

The suite calls the app at **`http://127.0.0.1:<port>`** (not `localhost`) so health checks avoid Windows IPv6 (`::1`) vs IPv4 listen quirks.

Passed from Surefire via `systemPropertyVariables` in `pom.xml`. Example:

```bash
mvn verify -De2e.app.port=18081
```

The app runs with `--spring.profiles.active=e2e` (stub AI via `TestAIConfig` only; **`DocuRagAiConfiguration` is off** under `e2e` via `@Profile("!test & !e2e")`).

## Tags

| Tag | Meaning |
|-----|---------|
| `@smoke` | Actuator health |
| `@api` | REST / OpenAPI flows |
| `@ui` | Playwright |
| `@cli` | Second JVM: `eval-cli` profile |
| `@optional` | UC-21-style behaviour until API exists |

**UI execution order:** Feature files run in **lexical order** (`01` … `06`). [`02-api.feature`](src/test/resources/features/02-api.feature) seeds the database before Playwright scenarios in [`04-ui.feature`](src/test/resources/features/04-ui.feature) and [`06-ui-use-cases.feature`](src/test/resources/features/06-ui-use-cases.feature).

### UI use-case coverage (`@ui`)

| Feature | What it covers |
|---------|----------------|
| [`04-ui.feature`](src/test/resources/features/04-ui.feature) | Disclaimer + title on all main paths; QA form submit shows **Answer** heading |
| [`06-ui-use-cases.feature`](src/test/resources/features/06-ui-use-cases.feature) | Dashboard index stats; documents table + fixture text; **Ingest configured paths** form; **Analysis** (pie API + `#pieChart`, vis-network canvas); **Evaluation** form run + metrics; QA non-empty answer body; nav link **Documents** |

Filter (Surefire):

```bash
mvn verify -Dcucumber.filter.tags=@smoke
```

## OpenAPI client

- Spec: [`../docu-rag/api/openapi.yaml`](../docu-rag/api/openapi.yaml)
- Regenerate: `mvn generate-test-sources` or `mvn test-compile`
- Packages: `com.berdachuk.docurag.e2e.client.*`

## Fixtures

- `src/test/resources/fixtures/sample.jsonl` — structured ingest (copied next to the JAR under `target/e2e-fixtures/`)
- `src/test/resources/fixtures/tiny.pdf` — PDF ingest; must yield **at least ~20 characters** of extractable text after PDFBox stripping (same rule as `DocumentIngestApiImpl` / production ingest). Image-only or token PDFs will complete with `documentsLoaded: 0` and break the API scenario.

## Layout

- `src/test/resources/features/` — Gherkin (`01-smoke` … `06-ui-use-cases`, lexical order)
- `src/test/java/.../infra/E2eInfraLifecycle.java` — Compose + JAR + health (started from `E2eInfraHooks` `@Before`)
- `src/test/java/.../steps/` — step definitions, `E2eInfraHooks`, `TestContext` (PicoContainer)
- `src/test/java/.../ui/` — Playwright hooks + context

Infrastructure runs on the **first** Cucumber scenario (`@Before` order `Integer.MIN_VALUE`); the JVM **shutdown hook** stops the app and runs `docker compose down`.
