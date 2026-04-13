# DocuRAG — Use cases

This document lists **all product use cases** derived from [DocuRAG-PRD.md](DocuRAG-PRD.md). For API shapes and schema, use the PRD; this file is the **scenario catalog** for implementation, tests, and demos.

**Primary actor:** *Operator* — a developer or course evaluator running DocuRAG locally (English UI, medical disclaimer on interactive pages).

**System:** DocuRAG (Spring Boot, Modulith, PostgreSQL + pgvector, Spring AI).

---

## Summary table

| ID | Name | Channel | PRD trace |
|----|------|---------|-----------|
| UC-01 | Ingest corpus subset | REST, UI, CLI | FR-1 |
| UC-01b | Ingest supplementary PDF demo pack | REST, UI, CLI | FR-1 |
| UC-02 | View ingestion status & category mix | UI, API (dashboard) | FR-1 |
| UC-03 | List & paginate ingested documents | REST, UI | FR-1 |
| UC-04 | Open document detail | REST | FR-1 |
| UC-05 | List corpus categories | REST | FR-1 |
| UC-06 | Rebuild full vector index | REST | FR-2, FR-3 |
| UC-07 | Incremental index update | REST | API (ninja-aligned) |
| UC-08 | View indexing status | REST, UI | FR-2, FR-3 |
| UC-09 | Ask grounded medical question | REST | FR-4 |
| UC-10 | Ask grounded medical question (web) | UI `/qa` | FR-4, NFR-5 |
| UC-11 | Run document analysis / extraction | REST | FR-5 |
| UC-12 | Fetch category pie data | REST | FR-6 |
| UC-13 | Fetch entity graph data | REST | FR-6 |
| UC-14 | View charts & graph (analysis page) | UI `/analysis` | FR-5, FR-6 |
| UC-15 | Run evaluation on eval dataset | REST | FR-7 |
| UC-16 | Run evaluation (command-line) | CLI | FR-7 |
| UC-17 | List evaluation runs | REST | FR-7 |
| UC-18 | Open evaluation run detail | REST | FR-7 |
| UC-19 | Get latest evaluation summary | REST, UI | FR-7 |
| UC-20 | Dashboard overview | UI `/` | UI |
| UC-21 | Optional RAG session history | REST | API optional |

**Future / optional (PRD non-goals or ninja):** ACL-aware RAG, multimodal ingestion, advanced retrieval metrics — not baseline UCs; see [§ Optional extensions](#optional-extensions-not-in-baseline).

---

## UC-01 — Ingest corpus subset

| Field | Description |
|-------|-------------|
| **Goal** | Load a curated English medical corpus from local files (Hugging Face export / JSONL / CSV per PRD) and/or **PDFs** from a configured directory (UC-01b). |
| **Actor** | Operator |
| **Trigger** | `POST /api/documents/ingest` or equivalent UI/CLI action |
| **Preconditions** | Configured import path; DB reachable; subset manifest may exist |
| **Main flow** | 1) Read files. 2) Normalize metadata (title, category, source, text). 3) Assign **ObjectId-style** `id` via `IdGenerator`. 4) Skip duplicates by `external_id` / content hash. 5) Persist `source_document`. |
| **Postconditions** | New or updated documents stored; ingestion job / counts updated |
| **FR** | FR-1 |
| **API** | `POST /api/documents/ingest` |

---

## UC-01b — Ingest supplementary PDF demo pack

| Field | Description |
|-------|-------------|
| **Goal** | Load **5–15 open English medical PDFs** (public guidelines / health-authority documents) from a configured directory to demonstrate **raw PDF ingestion** alongside the primary Hugging Face corpus. |
| **Actor** | Operator |
| **Trigger** | Same as UC-01 (`POST /api/documents/ingest` with PDF path / mode, or separate documented property such as `docurag.ingestion.pdf-demo-path`) |
| **Preconditions** | PDFs downloaded per **`docu-rag/data/pdf-demo/README.md`**; DB reachable |
| **Main flow** | 1) Discover **`.pdf`** files. 2) Extract text (**PDFBox**). 3) Set **`source_format`** = `pdf`, **`external_id`** / **`source_url`** from file name or manifest. 4) Skip duplicates (hash). 5) Persist **`source_document`**. |
| **Postconditions** | PDF-derived documents available for chunking and indexing |
| **FR** | FR-1 |
| **API** | `POST /api/documents/ingest` (implementation may use request body or config to select PDF root vs HF export root) |

*Prefer **official** open PDFs over opaque generic PDF dumps for demo credibility (PRD [Dataset Strategy](DocuRAG-PRD.md#dataset-strategy)).*

---

## UC-02 — View ingestion status & category distribution

| Field | Description |
|-------|-------------|
| **Goal** | See progress, counts, and category distribution after or during ingest. |
| **Actor** | Operator |
| **Trigger** | Open dashboard `/` or admin section |
| **Preconditions** | At least one ingest run or documents present |
| **Main flow** | Load aggregates from DB or last job; render counts and category breakdown |
| **FR** | FR-1 (acceptance: UI shows status, counts, category distribution) |
| **UI** | `/` (partial), ingestion status area |

---

## UC-03 — List & paginate ingested documents

| Field | Description |
|-------|-------------|
| **Goal** | Browse indexed source documents. |
| **Actor** | Operator |
| **Trigger** | `GET /api/documents` or UI `/documents` |
| **Preconditions** | Documents ingested |
| **Main flow** | Return paginated list with key metadata |
| **FR** | FR-1 |
| **API** | `GET /api/documents` |
| **UI** | `/documents` |

---

## UC-04 — Open document detail

| Field | Description |
|-------|-------------|
| **Goal** | Inspect one document by internal id. |
| **Actor** | Operator |
| **Trigger** | `GET /api/documents/{id}` |
| **Preconditions** | Valid 24-hex id |
| **Main flow** | Load `source_document` row (and optionally chunk summary if exposed later) |
| **FR** | FR-1 |
| **API** | `GET /api/documents/{id}` |

---

## UC-05 — List corpus categories

| Field | Description |
|-------|-------------|
| **Goal** | Enumerate categories for filters, charts, or ingest validation. |
| **Actor** | Operator / system |
| **Trigger** | `GET /api/documents/categories` |
| **Preconditions** | Documents with category field present |
| **Main flow** | Distinct categories from stored documents |
| **FR** | FR-1 |
| **API** | `GET /api/documents/categories` |

---

## UC-06 — Rebuild full vector index

| Field | Description |
|-------|-------------|
| **Goal** | Chunk all (or selected) documents, embed with `nomic-embed-text`, store `vector(768)`. |
| **Actor** | Operator |
| **Trigger** | `POST /api/index/rebuild` |
| **Preconditions** | Documents present; embedding provider available (non-test) |
| **Main flow** | 1) Chunk per FR-2 (configurable size/overlap, skip tiny chunks). 2) Batch embed with retry/backoff. 3) Upsert chunks + vectors idempotently. |
| **Postconditions** | Index stats updated; UI/API reflect status |
| **FR** | FR-2, FR-3 |
| **API** | `POST /api/index/rebuild` |

---

## UC-07 — Incremental index update

| Field | Description |
|-------|-------------|
| **Goal** | Add or update vectors for new/changed content without full rebuild. |
| **Actor** | Operator |
| **Trigger** | `POST /api/index/incremental` |
| **Preconditions** | Baseline index exists; changed set identified (implementation-specific) |
| **Main flow** | Re-chunk/re-embed only affected documents or chunks |
| **PRD** | Ninja-aligned corpus update |
| **API** | `POST /api/index/incremental` |

---

## UC-08 — View indexing status

| Field | Description |
|-------|-------------|
| **Goal** | Know whether indexing is idle, running, failed; chunk/embed stats. |
| **Actor** | Operator |
| **Trigger** | `GET /api/index/status` or dashboard |
| **Preconditions** | — |
| **Main flow** | Read job/status tables or derived metrics |
| **FR** | FR-2, FR-3 (logging/stats on demo page) |
| **API** | `GET /api/index/status` |

---

## UC-09 — Ask grounded medical question (REST)

| Field | Description |
|-------|-------------|
| **Goal** | Answer an English question using **retrieved chunks** + LLM; return citations/snippets. |
| **Actor** | Operator or API client |
| **Trigger** | `POST /api/rag/ask` with `question`, optional `topK`, `minScore` |
| **Preconditions** | Index built; LLM configured |
| **Main flow** | 1) **retrieval**: top-k similarity search. 2) **llm**: RAG prompt + advisor. 3) Return answer + `retrievedChunks`. If context insufficient, answer reflects PRD prompt rules. |
| **FR** | FR-4 |
| **API** | `POST /api/rag/ask` |

---

## UC-10 — Ask grounded medical question (web)

| Field | Description |
|-------|-------------|
| **Goal** | Same as UC-09 via Thymeleaf demo. |
| **Actor** | Operator |
| **Trigger** | Submit form on `/qa` |
| **Preconditions** | Same as UC-09 |
| **Main flow** | POST → render question, answer, chunks |
| **FR** | FR-4, **NFR-5** (disclaimer on page) |
| **UI** | `/qa` |

---

## UC-11 — Run document analysis / extraction

| Field | Description |
|-------|-------------|
| **Goal** | Extract structured signals (categories, entities, relations) for viz or downstream use. |
| **Actor** | Operator |
| **Trigger** | `POST /api/rag/analyze` |
| **Preconditions** | Corpus or retrieval context available |
| **Main flow** | LLM/structured extraction per PRD; return JSON |
| **FR** | FR-5 |
| **API** | `POST /api/rag/analyze` |

---

## UC-12 — Fetch category pie data

| Field | Description |
|-------|-------------|
| **Goal** | JSON for pie chart (document or concept counts by category). |
| **Actor** | Operator / UI |
| **Trigger** | `GET /api/visualizations/categories/pie` |
| **Preconditions** | Data from ingest and/or extraction |
| **Main flow** | Aggregate and return DTO |
| **FR** | FR-6 |
| **API** | `GET /api/visualizations/categories/pie` |

---

## UC-13 — Fetch entity graph data

| Field | Description |
|-------|-------------|
| **Goal** | JSON nodes/edges for graph or diagram. |
| **Actor** | Operator / UI |
| **Trigger** | `GET /api/visualizations/entities/graph` |
| **Preconditions** | Extraction enabled / run |
| **Main flow** | Return graph payload |
| **FR** | FR-6 |
| **API** | `GET /api/visualizations/entities/graph` |

---

## UC-14 — View charts & graph (analysis page)

| Field | Description |
|-------|-------------|
| **Goal** | Demo pie + graph/list in browser. |
| **Actor** | Operator |
| **Trigger** | Open `/analysis` |
| **Preconditions** | Same as UC-12/UC-13 |
| **Main flow** | Load visualization DTOs; lightweight JS rendering |
| **FR** | FR-5, FR-6, **NFR-5** |
| **UI** | `/analysis` |

---

## UC-15 — Run evaluation on eval dataset

| Field | Description |
|-------|-------------|
| **Goal** | Batch-run RAG on labeled questions; compute **≥1** metric (e.g. normalized accuracy, semantic similarity). |
| **Actor** | Operator |
| **Trigger** | `POST /api/evaluation/run` with dataset name and thresholds |
| **Preconditions** | Eval dataset loaded/versioned; models configured |
| **Main flow** | For each case: retrieve → generate → score → persist `evaluation_result`; save `evaluation_run` summary |
| **Postconditions** | Run id + aggregates available |
| **FR** | FR-7 |
| **API** | `POST /api/evaluation/run` |

---

## UC-16 — Run evaluation (command-line)

| Field | Description |
|-------|-------------|
| **Goal** | Same outcomes as UC-15 without using HTTP (CI, ops). |
| **Actor** | Operator |
| **Trigger** | Spring Boot `ApplicationRunner` / CLI profile |
| **FR** | FR-7 |
| **API** | N/A (programmatic entrypoint) |

---

## UC-17 — List evaluation runs

| Field | Description |
|-------|-------------|
| **Goal** | Audit past eval executions. |
| **Actor** | Operator |
| **Trigger** | `GET /api/evaluation/runs` |
| **FR** | FR-7 |
| **API** | `GET /api/evaluation/runs` |

---

## UC-18 — Open evaluation run detail

| Field | Description |
|-------|-------------|
| **Goal** | Per-run aggregates and access to per-case results (as designed). |
| **Actor** | Operator |
| **Trigger** | `GET /api/evaluation/runs/{id}` |
| **FR** | FR-7 |
| **API** | `GET /api/evaluation/runs/{id}` |

---

## UC-19 — Get latest evaluation summary

| Field | Description |
|-------|-------------|
| **Goal** | Quick view of most recent metrics for dashboard or reporting. |
| **Actor** | Operator |
| **Trigger** | `GET /api/evaluation/latest` or `/` widget |
| **FR** | FR-7 |
| **API** | `GET /api/evaluation/latest` |
| **UI** | `/`, `/evaluation` |

---

## UC-20 — Dashboard overview

| Field | Description |
|-------|-------------|
| **Goal** | Landing page: counts, ingest/index status, link to QA/analysis/documents/evaluation, latest eval snippet. |
| **Actor** | Operator |
| **Trigger** | `GET /` |
| **FR** | UI Requirements |
| **UI** | `/` |
| **NFR** | NFR-5 disclaimer |

---

## UC-21 — Optional RAG session history

| Field | Description |
|-------|-------------|
| **Goal** | Retrieve prior QA context by session id if implemented. |
| **Actor** | Operator |
| **Trigger** | `GET /api/rag/history/{id}` |
| **Preconditions** | Feature enabled; history persisted |
| **PRD** | Optional API |
| **API** | `GET /api/rag/history/{id}` |

---

## Cross-cutting use cases

| ID | Name | Notes |
|----|------|--------|
| XC-01 | **Show medical disclaimer** | Every interactive Thymeleaf page (NFR-5). |
| XC-02 | **Health / readiness** | Actuator health for DB and dependencies (NFR-4). |
| XC-03 | **Structured logging** | Ingest, embed, retrieve, generate, eval timings (NFR-4). |
| XC-04 | **Modulith boundary compliance** | No illegal cross-module imports; verified in build (NFR-1). |
| XC-05 | **Deterministic automated tests** | Full-flow IT with Testcontainers + mocked `ChatModel` / `EmbeddingModel` (NFR-7). |

---

## Optional extensions (not in baseline)

| Theme | PRD pointer | Note |
|-------|-------------|------|
| Access-control–aware retrieval | Non-goal v1 | Future UC family: filter chunks by principal |
| Multimodal RAG | Non-goal v1 | Future: figures / OCR |
| Retrieval precision/recall | Ninja eval | Needs labeled relevant chunks |
| Faithfulness / groundedness scoring | Ninja eval | Citations, NLI, or judge |
| Live LLM in CI | — | Explicit opt-in profile only; not default |

---

## UI ↔ use case map

| Page | Use cases |
|------|-----------|
| `/` | UC-02, UC-19, UC-20, XC-01 |
| `/qa` | UC-10, XC-01 |
| `/analysis` | UC-14, XC-01 |
| `/documents` | UC-03, XC-01 |
| `/evaluation` | UC-15, UC-19, XC-01 |

---

## Dataset & compliance reminder

- **Corpus:** [Sagarika-Singh-99/medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus) (PRD).
- Outputs are **not medical advice**; educational/demo use only.

---

**Document version:** 1.0 (aligned with [DocuRAG-PRD.md](DocuRAG-PRD.md) as of creation). Update this file when FRs or APIs change.
