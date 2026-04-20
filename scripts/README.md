# Scripts

| Script | Purpose |
|--------|---------|
| [`full-build-and-e2e.sh`](full-build-and-e2e.sh) | Unix/Git Bash: `mvn clean verify` at repository root, optional `docker compose down -v` before/after, prints E2E report paths. With `--teardown-volumes`, activates Maven profile **`e2e-teardown-volumes`** on the E2E module (sets compose shutdown hook to `down -v`). |
| [`full-build-and-e2e.ps1`](full-build-and-e2e.ps1) | Windows PowerShell: same as above. Use `-TeardownVolumes` for volume removal. |

Examples:

```bash
chmod +x scripts/full-build-and-e2e.sh   # once, on Linux/macOS/Git Bash
./scripts/full-build-and-e2e.sh --teardown-volumes
```

```powershell
.\scripts\full-build-and-e2e.ps1 -TeardownVolumes
```

Requires Docker running, JDK 21, Maven, and (for UI tests) Playwright Chromium installed in `docu-rag-e2e` as documented in `docu-rag-e2e/README.md`.

On **Windows CMD**, quote other Maven defines if needed (e.g. `mvn verify "-De2e.app.port=18081"`). Prefer **`-Pe2e-teardown-volumes`** for volume teardown instead of `-De2e.compose.down.removeVolumes=true`.

### Reports and E2E diagnostics

- Surefire: `docu-rag-e2e/target/surefire-reports/`
- Cucumber HTML/XML: `docu-rag-e2e/build/cucumber-reports/`
- Compose / Docker: `docu-rag-e2e/target/docurag-e2e-compose.log`, `docurag-e2e-docker-info.log`, `docurag-e2e-compose-plugin-probe.log`
- App subprocess (fat JAR): `docu-rag-e2e/target/docurag-e2e-app.log`

The full-build scripts print a short summary (including tails of the logs above when present).

### Interpreting the run

- **Green run:** last lines include `[INFO] BUILD SUCCESS` and `STATUS: SUCCESS (mvn exit 0)`. Cucumber summary: **17 scenarios (17 passed)**; Surefire **18 tests** in the E2E module.
- **Reports:** HTML at `docu-rag-e2e/build/cucumber-reports/e2e-report.html`; JUnit XML at `cucumber.xml` in the same folder.
- **Docker noise:** `docker info` may print a daemon deprecation about an unencrypted API port; that comes from the local Docker install, not DocuRAG.
