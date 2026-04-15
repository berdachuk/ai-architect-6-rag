# DocuRAG — course submission checklist

Use this list with [DocuRAG-PRD.md](../DocuRAG-PRD.md) deliverables and [DocuRAG-IMPLEMENTATION-PLAN-WBS.md](../DocuRAG-IMPLEMENTATION-PLAN-WBS.md) §7 (Definition of Done).

## Evidence to attach (program-specific)

- [ ] **Course Answer** bundle per your program’s template (zip or portal upload).
- [ ] **Screenshots** (place PNGs under `docs/submission/screenshots/` or attach separately):
  - [ ] **QA** — `/qa` with a question and answer region visible.
  - [ ] **Chart** — category pie (from `/analysis` or visualization flow).
  - [ ] **Graph** — entity graph visualization.
  - [ ] **Evaluation** — `/evaluation` or API output showing a completed run / metrics.

## Technical verification (local or CI)

- [ ] **Reactor build:** `mvn -f docu-rag-parent/pom.xml clean verify` (Docker required for Testcontainers + E2E Compose).
- [ ] **Optional full stack + volume teardown:** from repo root, `.\scripts\full-build-and-e2e.ps1 -TeardownVolumes` (Windows) or `./scripts/full-build-and-e2e.sh --teardown-volumes` (Unix).
- [ ] **Reports after E2E:** `docu-rag-e2e/build/cucumber-reports/e2e-report.html`, `docu-rag-e2e/target/surefire-reports/`.

## Documentation links (for reviewers)

- **Primary corpus (HF):** [Sagarika-Singh-99/medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus) — also in [docu-rag/README.md](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/README.md).
- **Subset manifest example:** [data/corpus/subset-manifest.example.json](https://github.com/berdachuk/ai-architect-6-rag/blob/main/data/corpus/subset-manifest.example.json).
- **PDF demo pack:** [data/pdf-demo/README.md](https://github.com/berdachuk/ai-architect-6-rag/blob/main/data/pdf-demo/README.md).
- **Evaluation:** seeded dataset `medical-rag-eval-v1`; REST `POST /api/evaluation/run`, UI `/evaluation`, CLI `eval-cli` profile (see [docu-rag/README.md](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/README.md)).

## CI

- GitHub Actions workflow [`.github/workflows/docu-rag-verify.yml`](https://github.com/berdachuk/ai-architect-6-rag/blob/main/.github/workflows/docu-rag-verify.yml) runs the same `mvn -f docu-rag-parent/pom.xml verify` on pushes/PRs touching DocuRAG paths.
