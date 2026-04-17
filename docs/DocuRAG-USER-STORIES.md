# DocuRAG — User stories

Agile-style **user stories** for DocuRAG, aligned with [DocuRAG-PRD.md](DocuRAG-PRD.md), [DocuRAG-USE-CASES.md](DocuRAG-USE-CASES.md), and [DocuRAG-FORMS-AND-FLOWS.md](DocuRAG-FORMS-AND-FLOWS.md).

**Primary persona:** **Operator** — developer or course evaluator running DocuRAG locally (English UI).

**Story ID prefix:** `US-xx` — traceability to use cases `UC-xx` where applicable.

---

## Summary backlog

| ID | Epic | Short title |
|----|------|-------------|
| US-01 | Corpus | Ingest medical corpus (HF export + **PDF**) |
| US-26 | Corpus | Document open medical PDF demo sources |
| US-02 | Corpus | See ingestion status and categories |
| US-03 | Corpus | Browse documents (paginated) |
| US-04 | Corpus | Open one document |
| US-05 | Corpus | List categories |
| US-06 | Index | Rebuild full vector index |
| US-07 | Index | Incremental index update |
| US-08 | Index | See indexing status |
| US-09 | RAG | Ask question via API |
| US-10 | RAG | Ask question via web |
| US-11 | RAG | Tune retrieval (topK, threshold) |
| US-12 | Analysis | Run extraction / analysis |
| US-13 | Viz | Category pie data (API) |
| US-14 | Viz | Entity graph data (API) |
| US-15 | Viz | View analysis page |
| US-16 | Eval | Run evaluation via API |
| US-17 | Eval | Run evaluation via CLI |
| US-18 | Eval | List and inspect eval runs |
| US-19 | Eval | See latest eval summary |
| US-20 | Shell | Dashboard home |
| US-21 | Shell | Navigate demo app |
| US-22 | Compliance | See medical disclaimer |
| US-23 | Ops | Health and logs |
| US-24 | Optional | RAG session history |
| US-25 | Optional | Multi-endpoint bulk embedding |

**Out of scope (v1 backlog / future):** US-F01 ACL-aware RAG; US-F02 multimodal; US-F03 advanced eval metrics — see PRD non-goals and ninja items.

---

## Epic: Corpus ingestion & documents

### US-01 — Ingest corpus subset

**As an** operator,  
**I want** to import a curated English medical corpus from local prepared files (**Hugging Face export** and **PDF**),  
**So that** the system has source documents to chunk, embed, and retrieve—including **real PDFs** for demo credibility.

**Acceptance criteria**

- Importer supports **structured** exports (JSONL/CSV per PRD) **and** **`.pdf`** via **PDFBox** (text-layer PDFs; scanned-only PDFs may be skipped with a clear log).
- Importer preserves title, category, source identifier, and raw text where available; **`source_format`** distinguishes **`hf_export`** vs **`pdf`**.
- New rows get MongoDB-compatible **ObjectId-style** primary keys (`IdGenerator`).
- Duplicates are avoided via stable **external_id** and/or **content hash**.
- Ingestion can be triggered from REST, UI shortcut, or CLI per PRD.
- **Unit tests** cover PDF extraction on a **minimal** in-repo PDF fixture.

**Maps to:** UC-01, UC-01b, FR-1

---

### US-26 — Document open medical PDF demo sources

**As an** operator or reviewer,  
**I want** a maintained list of **open English medical PDFs** (URLs, titles, attribution) in the repository,  
**So that** the PDF ingestion demo is **reproducible and defensible** without relying on opaque third-party PDF dumps.

**Acceptance criteria**

- **`data/pdf-demo/README.md`** explains primary vs supplementary corpus, lists **example sources** (e.g. EU public health PDFs), and states that operators download files locally when binaries are not committed.
- Top-level / module README repeats the **two-sentence** positioning: primary HF corpus + supplementary PDF demo (per PRD).

**Maps to:** UC-01b, FR-1 (documentation deliverable)

---

### US-02 — Ingestion status and category mix

**As an** operator,  
**I want** to see ingestion progress, counts, and category distribution,  
**So that** I can confirm the corpus loaded correctly before indexing.

**Acceptance criteria**

- Dashboard (or equivalent) shows status, document counts, and category breakdown.
- Aligns with FR-1 UI acceptance.

**Maps to:** UC-02, FR-1

---

### US-03 — Browse ingested documents

**As an** operator,  
**I want** a paginated list of ingested documents with key metadata,  
**So that** I can sanity-check what is indexed.

**Acceptance criteria**

- `GET` list + UI `/documents` with pagination (and optional filters per PRD).
- Table shows at least title, category, source identifiers as available.

**Maps to:** UC-03, FR-1

---

### US-04 — Document detail

**As an** operator,  
**I want** to open a single document by id,  
**So that** I can verify content and metadata.

**Acceptance criteria**

- REST `GET /api/documents/{id}` returns document fields; optional UI detail view.
- Id is 24-hex string per NFR-6.

**Maps to:** UC-04, FR-1

---

### US-05 — List categories

**As an** operator,  
**I want** to retrieve distinct corpus categories,  
**So that** I can filter UI or validate ingest coverage.

**Acceptance criteria**

- `GET /api/documents/categories` returns category list derived from stored documents.

**Maps to:** UC-05, FR-1

---

## Epic: Chunking & vector index

### US-06 — Rebuild full index

**As an** operator,  
**I want** to rebuild chunking and embeddings for the corpus,  
**So that** retrieval uses up-to-date vectors in pgvector.

**Acceptance criteria**

- Chunk size and overlap are configurable; tiny chunks skipped (FR-2).
- Embeddings use configured model (e.g. `nomic-embed-text`), **768** dimensions (FR-3).
- Batched embedding with retry/backoff; idempotent re-run without duplicate vectors (FR-3).
- Trigger via `POST /api/index/rebuild` (or documented equivalent).

**Maps to:** UC-06, FR-2, FR-3

---

### US-07 — Incremental index update

**As an** operator,  
**I want** to update embeddings for new or changed documents without a full rebuild,  
**So that** large corpora can be maintained faster (when implemented).

**Acceptance criteria**

- `POST /api/index/incremental` (or equivalent) documented; behavior matches PRD ninja direction.

**Maps to:** UC-07, PRD API

---

### US-08 — Indexing status

**As an** operator,  
**I want** to see whether indexing is idle, running, or failed, plus basic stats,  
**So that** I know when Q&A is safe to run.

**Acceptance criteria**

- `GET /api/index/status` and/or dashboard reflects job state and chunk/embed stats (FR-2/3 logging/stats visibility).

**Maps to:** UC-08, FR-2, FR-3

---

## Epic: Grounded Q&A (RAG)

### US-09 — Ask via REST

**As an** operator (or API client),  
**I want** to `POST` an English question and receive a grounded answer with source chunks,  
**So that** I can integrate DocuRAG into scripts or tools.

**Acceptance criteria**

- Response includes answer text, model id, and **retrievedChunks** with scores and snippets (PRD example).
- Retrieval and LLM remain separate Modulith modules; **web** delegates to **api** (FR-4).

**Maps to:** UC-09, FR-4

---

### US-10 — Ask via web UI

**As an** operator,  
**I want** to submit a question on `/qa` and see the answer plus supporting chunks,  
**So that** I can demo the system without curl.

**Acceptance criteria**

- Form POST re-renders with question, answer, chunk list (PRD UI).
- **Medical disclaimer** visible (NFR-5).

**Maps to:** UC-10, FR-4, NFR-5

---

### US-11 — Tune retrieval parameters

**As an** operator,  
**I want** to set **topK** and **minimum similarity** for a question,  
**So that** I can balance recall and precision for demos.

**Acceptance criteria**

- API and `/qa` form accept `topK` and `minScore` (or equivalent names); values applied in retrieval (FR-4).

**Maps to:** UC-09, UC-10, FR-4

---

## Epic: Analysis & visualization

### US-12 — Run analysis / extraction

**As an** operator,  
**I want** to run structured extraction over corpus or selected scope,  
**So that** categories, entities, and relations feed charts and graphs (FR-5).

**Acceptance criteria**

- `POST /api/rag/analyze` returns structured JSON; prompts follow extraction guidelines (strict JSON, no invented entities).

**Maps to:** UC-11, FR-5

---

### US-13 — Category pie API

**As an** operator or UI,  
**I want** JSON for category distribution,  
**So that** a pie chart can be rendered (FR-6).

**Acceptance criteria**

- `GET /api/visualizations/categories/pie` returns data consistent with UI pie.

**Maps to:** UC-12, FR-6

---

### US-14 — Entity graph API

**As an** operator or UI,  
**I want** JSON nodes and edges (or list) for relationships,  
**So that** a graph or diagram can be rendered (FR-6).

**Acceptance criteria**

- `GET /api/visualizations/entities/graph` returns graph-ready payload.

**Maps to:** UC-13, FR-6

---

### US-15 — Analysis page

**As an** operator,  
**I want** to open `/analysis` and see pie + graph (or list fallback),  
**So that** I can satisfy the course visualization requirement visually.

**Acceptance criteria**

- Lightweight JS for rendering; same data as REST endpoints (FR-6); disclaimer on page (NFR-5).

**Maps to:** UC-14, FR-5, FR-6

---

## Epic: Evaluation

### US-16 — Run evaluation (REST)

**As an** operator,  
**I want** to run the RAG pipeline over a versioned eval dataset and persist scores,  
**So that** I can report at least one quantitative (or qualitative) metric for the assignment.

**Acceptance criteria**

- `POST /api/evaluation/run` with dataset name and thresholds; stores **evaluation_run** and **evaluation_result** (FR-7).
- At least one metric defined and visible (normalized accuracy and/or semantic similarity by default).

**Maps to:** UC-15, FR-7

---

### US-17 — Run evaluation (CLI)

**As an** operator,  
**I want** to trigger evaluation without the HTTP UI,  
**So that** CI or scripts can reproduce metrics.

**Acceptance criteria**

- Command-line runner (e.g. Spring `ApplicationRunner`) documented in README (FR-7).

**Maps to:** UC-16, FR-7

---

### US-18 — Inspect evaluation history

**As an** operator,  
**I want** to list runs and open one run’s aggregate and per-case results,  
**So that** I can debug poor categories or questions.

**Acceptance criteria**

- `GET /api/evaluation/runs`, `GET /api/evaluation/runs/{id}` (FR-7).

**Maps to:** UC-17, UC-18, FR-7

---

### US-19 — Latest evaluation snapshot

**As an** operator,  
**I want** a quick “latest metrics” view on the dashboard or `/evaluation`,  
**So that** I see the most recent run without drilling into ids.

**Acceptance criteria**

- `GET /api/evaluation/latest` and/or UI widget (FR-7).

**Maps to:** UC-19, FR-7

---

## Epic: Application shell & compliance

### US-20 — Dashboard

**As an** operator,  
**I want** a home page with counts, ingest/index hints, latest eval, and links to Q&A, analysis, documents, evaluation,  
**So that** I can orient and start a demo quickly.

**Acceptance criteria**

- `/` matches PRD “Required pages”; shows dataset/model hints where feasible.

**Maps to:** UC-20, PRD UI

---

### US-21 — Global navigation

**As an** operator,  
**I want** consistent navigation between main pages,  
**So that** I never get stuck on a leaf page.

**Acceptance criteria**

- Nav links: Home, Q&A, Analysis, Documents, Evaluation ([DocuRAG-WIREFRAMES.md](DocuRAG-WIREFRAMES.md)).

**Maps to:** UC-20, UI

---

### US-22 — Medical disclaimer

**As a** course stakeholder,  
**I want** every interactive page to state outputs are not medical advice,  
**So that** demo use meets safety expectations (NFR-5).

**Acceptance criteria**

- Disclaimer on `/`, `/qa`, `/analysis`, `/documents`, `/evaluation` (and any new interactive page).

**Maps to:** NFR-5, PRD UI behaviors

---

### US-23 — Operability

**As an** operator,  
**I want** health checks and structured logs for ingest, embed, retrieve, generate, and eval,  
**So that** I can diagnose failures (NFR-4).

**Acceptance criteria**

- Actuator health enabled; log fields/latencies per PRD NFR-4.

**Maps to:** NFR-4

---

## Epic: Optional / advanced

### US-24 — RAG session history (optional)

**As an** operator,  
**I want** to retrieve prior Q&A by session id **if** the feature is implemented,  
**So that** I can continue a thread-style demo.

**Acceptance criteria**

- `GET /api/rag/history/{id}` documented and tested when enabled; harmless omission if not built.

**Maps to:** UC-21, PRD optional API

---

### US-25 — Multi-endpoint bulk embedding (optional)

**As an** operator with multiple Ollama/embedding nodes,  
**I want** optional **multi-endpoint** bulk embedding with failover and batching,  
**So that** large ingest runs finish faster and survive single-node outages.

**Acceptance criteria**

- `docurag.ingestion.embeddings.multi-endpoint` per PRD; empty list = off; all endpoints **768-d** compatible models; tests stay green without real cluster.

**Maps to:** PRD § Multi-endpoint embedding providers

---

## Future backlog (not v1)

| ID | Story (summary) | PRD |
|----|-----------------|-----|
| US-F01 | As a tenant user, I want document-level access control on retrieval… | Non-goal v1 |
| US-F02 | As an operator, I want to ingest figures and OCR text… | Non-goal v1 |
| US-F03 | As an evaluator, I want retrieval precision/recall and faithfulness scores… | Ninja eval |

---

## Traceability matrix

| US | UC | FR / NFR |
|----|-----|----------|
| US-01 | UC-01, UC-01b | FR-1 |
| US-26 | UC-01b | FR-1 |
| US-02 | UC-02 | FR-1 |
| US-03 | UC-03 | FR-1 |
| US-04 | UC-04 | FR-1 |
| US-05 | UC-05 | FR-1 |
| US-06 | UC-06 | FR-2, FR-3 |
| US-07 | UC-07 | API |
| US-08 | UC-08 | FR-2, FR-3 |
| US-09 | UC-09 | FR-4 |
| US-10 | UC-10 | FR-4, NFR-5 |
| US-11 | UC-09/10 | FR-4 |
| US-12 | UC-11 | FR-5 |
| US-13 | UC-12 | FR-6 |
| US-14 | UC-13 | FR-6 |
| US-15 | UC-14 | FR-5, FR-6, NFR-5 |
| US-16 | UC-15 | FR-7 |
| US-17 | UC-16 | FR-7 |
| US-18 | UC-17, UC-18 | FR-7 |
| US-19 | UC-19 | FR-7 |
| US-20 | UC-20 | UI |
| US-21 | UC-20 | UI |
| US-22 | XC-01 | NFR-5 |
| US-23 | XC-02 | NFR-4 |
| US-24 | UC-21 | Optional |
| US-25 | — | PRD multi-endpoint |

---

**Document version:** 1.0. Update when PRD or use cases change.
