# DocuRAG PRD

## Overview

DocuRAG is an English-only medical document Retrieval-Augmented Generation application built for document ingestion (**structured exports and raw PDFs**), semantic retrieval, question answering, structured extraction, visualization, and evaluation. The system will use **Spring Modulith** to structure a **modular monolith** (same approach as the **ExpertMatch** reference codebase: `package-info.java` with `@ApplicationModule`, explicit `allowedDependencies`, and cross-module access via `*.api` packages). It will use Spring AI 2.0.0-M4 for GenAI orchestration, PostgreSQL with pgvector for vector storage, Gemma4 31B Cloud served through an OpenAI-compatible Ollama endpoint as the primary chat model, and `nomic-embed-text` with 768-dimensional embeddings for retrieval quality and semantic evaluation.

The product goal is to satisfy the **Practical Task: RAG Creation** with a production-style architecture rather than a notebook prototype. The application must ingest a curated subset of the primary corpus (see [Dataset Strategy](#dataset-strategy) for the canonical link), support document-grounded QA, extract structured data for visualization (chart and/or graph), and include **at least one** evaluation criterion that is **defined, run on a small labeled eval set, and demonstrated** (report + UI or exported results)—this PRD standardizes on quantitative answer-quality metrics while noting acceptable qualitative alternatives below.

### Alignment with Practical Task: RAG Creation

| Program requirement | How DocuRAG addresses it |
|---------------------|---------------------------|
| Preferred GenAI framework | **Spring AI 2.0.0-M4** on Java 21+ (orchestration, RAG advisors, embeddings). The assignment allows any framework; this PRD records the chosen stack. |
| Analyze documents; extract and visualize (graph/diagram, pie chart) | Ingestion + chunking + RAG QA; **FR-5/FR-6**: category pie chart and entity/relation **graph** (or diagram-ready payload); REST + Thymeleaf demo. |
| Link to dataset(s) | **Primary corpus:** [Sagarika-Singh-99/medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus) on Hugging Face. README and submission must repeat this URL and any local snapshot path. **Supplementary:** a small **open English medical PDF demo pack** (official guidelines / public-health sources) documents **PDF ingestion**—see [Dataset Strategy](#dataset-strategy); README must describe both roles (primary vs PDF demo). |
| ≥1 evaluation metric, defined + evaluated + demonstrated | **FR-7** + [Evaluation Recommendations](#evaluation-recommendations): minimum **normalized accuracy** *or* **mean semantic similarity** on a **small** English eval set (see below); UI/API show aggregate and per-case results. |
| Small evaluation set for ≥1 criterion | Versioned eval dataset (e.g. 50–100 cases for iteration; **≥10–20** acceptable for a minimal course demo if documented). |
| Quality and safety of generative outputs | Grounded QA prompts, insufficient-context behavior, UI disclaimer (**NFR-5**); optional explicit safety-oriented checks (e.g. refusals when context is empty) as a qualitative or rubric-based criterion. |

**Quantitative vs qualitative:** The task allows either. This PRD emphasizes **quantitative** metrics (string match, embedding similarity). A valid alternative is a **qualitative** demonstration: e.g. fixed rubric on a small sample (grounded vs hallucinated, completeness), documented with a short results table or screenshots.

**Course submission:** Upload the deliverables (repo link, README, dataset link, eval description, screenshots) to the platform **Answer** block and **Submit**; keep wording aligned with program-level acceptance criteria and Run owner tool guidance.

### Optional “Ninja” extensions (not required for baseline)

| Challenge | Status in this PRD |
|-----------|-------------------|
| Corpus update without full vector DB rebuild | **In scope as API direction:** `POST /api/index/incremental` (see [API Requirements](#api-requirements)); implementation may re-embed only new/changed chunks. |
| Access-control–aware RAG | **Non-goal for v1** ([Non-goals](#non-goals)); architecture note allows future extension. |
| Multi-modal RAG (figures, images) | **Non-goal for v1**; optional future milestone. |
| Precision, recall, faithfulness / groundedness | **Optional eval extensions:** retrieval precision/recall if chunk-level labels exist; **faithfulness** via citation-required answers, NLI, or human/LLM-as-judge on a small set. |

## Product Goals

### Primary goals

- Build an end-to-end RAG application for English medical documents.
- Support ingestion of 2,000 to 5,000 documents across 3 to 5 categories from the selected corpus.
- Answer user questions using retrieved document context through Spring AI RAG components and advisors.
- Extract entities, categories, and relations from the corpus to power visualizations such as pie charts and document/entity graphs (satisfying the assignment’s chart/graph expectation).
- Provide evaluation of final answer quality: **at least one** of normalized accuracy, semantic similarity, or an agreed qualitative rubric on a small eval set, **documented and demonstrable**.
- Offer a simple Thymeleaf UI for demonstration, in addition to REST APIs.

### Secondary goals

- Provide a modular codebase that can be extended to GraphRAG or access-controlled RAG later.
- Enforce **module boundaries with Spring Modulith** (compile/architecture verification), mirroring ExpertMatch-style configuration rather than ad hoc packages.
- Keep the architecture aligned with Java 21+, Spring JDBC, and explicit SQL instead of JPA or Spring Data.
- Ensure local reproducibility under WSL2 with Docker and Testcontainers.

### Non-goals

- Multi-tenant authorization and document-level ACL in the first iteration.
- Multimodal OCR or image understanding in the first iteration.
- Full SPA frontend; the demo UI will remain intentionally simple and server-rendered with Thymeleaf.
- Real-time corpus synchronization from external medical sources.

## Scope

### In scope

- REST API for corpus ingestion (**Hugging Face–style structured files and PDF files**), indexing, question answering, analysis, visualization data, and evaluation.
- Thymeleaf pages for upload/indexing status, QA, analysis results, and charts/graph demo.
- PostgreSQL schema for source documents, chunks, ingestion jobs, evaluation datasets, and evaluation runs.
- pgvector-based semantic search with 768-dimensional vectors aligned to `nomic-embed-text`.
- **Optional multi-endpoint embedding pool** for bulk chunk indexing (ExpertMatch-style); see [Multi-endpoint embedding providers](#multi-endpoint-embedding-providers-expertmatch-aligned).
- **Retrieval** (search top-k chunks) and **LLM** (Spring AI chat, prompt assembly, advisors) as separate Modulith modules; RAG orchestration through Spring AI `QuestionAnswerAdvisor` and optionally `RetrievalAugmentationAdvisor` for extensibility.
- Spring Modulith **architecture verification** (`spring-modulith-starter-test`, `@ApplicationModuleTest` or equivalent) so illegal cross-module dependencies fail the build.
- **Integration tests** on **Testcontainers** (PostgreSQL + pgvector) that exercise a **full application flow** (see [Integration tests](#integration-tests) and **NFR-7**).
- **Mocked LLM (and embedding) model beans** in automated tests so **`mvn verify`** never requires a live Ollama or cloud API (ExpertMatch **`TestAIConfig`** pattern).

### Out of scope

- Full text search engine beyond PostgreSQL and pgvector in phase 1.
- User accounts and authorization.
- Background distributed processing or message brokers.
- Complex frontend frameworks such as Angular or React.

## Users and Usage Scenarios

### Primary user

A course evaluator or developer runs DocuRAG locally, indexes the selected medical corpus subset, asks questions in English, inspects the retrieved source snippets, and views extracted categories and graph/chart visualizations.

### Key demo scenarios

1. Import a selected subset of the medical corpus and build vector embeddings; optionally ingest **open English medical PDFs** to demonstrate PDF parsing on the same pipeline.
2. Ask a medical question and receive a grounded answer with supporting chunks.
3. Run analysis on documents to extract entities or categories and render a pie chart and graph.
4. Run evaluation on a ready-made English eval dataset and inspect normalized accuracy and semantic similarity summary metrics.

## Technology Stack

| Area | Choice | Notes |
|------|--------|-------|
| Base package | `com.berdachuk` | Required by project constraints; application modules live under `com.berdachuk.docurag.<module>`. |
| Language | Java 21+ | Required by project constraints. |
| Modular structure | **Spring Modulith** | `spring-modulith-bom` + `spring-modulith-starter-core`; version aligned with Spring Boot (e.g. **2.0.x** with Spring Boot 3.4+ / 4.x—match BOM to Boot like ExpertMatch). |
| Backend | Spring Boot + Spring AI 2.0.0-M4 | Same integration style as ExpertMatch: **`spring-ai-starter-model-openai`** (OpenAI-compatible HTTP API only). **LLM** module consumes **`ChatModel` / `ChatClient`**; **vector** (and **evaluation**) consume **`EmbeddingModel`**. Retrieval stays non-generative. |
| Persistence | Spring JDBC | Explicit SQL only; no JPA or Spring Data. |
| API style | REST | Required by project constraints. |
| UI | Thymeleaf | Simple server-rendered demo UI. |
| Database | PostgreSQL + pgvector | Supports vector storage and similarity search. |
| Testing infra | Testcontainers | Required for local reproducibility with Docker. |
| LLM | Gemma4 31B Cloud via Ollama OpenAI-compatible endpoint | Primary generation model. |
| Embeddings | `nomic-embed-text` | Recommended embedding model available in Ollama. |
| Embedding size | 768 | Aligned with the selected embedding model. |
| Dataset | `Sagarika-Singh-99/medical-rag-corpus` | English medical corpus with metadata and categories; link: [Hugging Face](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus). |
| PDF text extraction | **Apache PDFBox** (or equivalent JVM library, no cloud OCR in v1) | Required to ingest **`.pdf`** files into the same `source_document` / chunking pipeline as structured exports. |

## Dataset Strategy

**Expanded catalog (download URLs, minimal PDF set, folder examples):** [DocuRAG-Datasets.md](DocuRAG-Datasets.md).

The application will use the Hugging Face dataset **`Sagarika-Singh-99/medical-rag-corpus`** as the primary corpus. **Canonical dataset link (for README and course submission):** [https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus). The implementation will ingest only a controlled subset of 2,000 to 5,000 documents selected from 3 to 5 categories to keep indexing time, storage size, and evaluation turnaround practical for local development and demo runs.

### Dataset selection rules

- English-only content.
- Retain title, text, source, and category fields when available.
- Prefer categories with clear medical semantics and enough examples to support retrieval and charting.
- Exclude corrupted, empty, or ultra-short entries during ingestion.

### Suggested subset strategy

- Select 3 to 5 categories with balanced document counts.
- Cap total indexed documents to about 3,000 for the first full demo.
- Persist the subset selection manifest in the repository for repeatability.

### Supplementary PDF demo corpus (required capability)

The **primary** corpus for scalable indexing, retrieval quality, and evaluation remains **`Sagarika-Singh-99/medical-rag-corpus`** on Hugging Face. **In addition**, the product shall support ingestion of **raw PDF files** and ship documentation for a **small supplementary demo set** of **open, English, medical-relevant PDFs** from **official or clearly licensed public sources** (e.g. EU public health guidance, NHS / government prescribing or data documentation, openly published clinical recommendations).

**Rationale**

- Demonstrates **real PDF parsing → chunking → retrieval → QA**, not only tabular/JSONL rows.
- **Official PDFs** are preferable to random or opaque Kaggle PDF dumps for **defensibility and reproducibility** on demos and coursework; generic non-medical PDF collections prove format support but are a **weak** primary narrative for a medical RAG project.

**Recommended approach**

| Role | Source |
|------|--------|
| **Primary RAG corpus** | [medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus) — curated subset + manifest (scale, categories, eval alignment). |
| **PDF ingestion demo** | **5–15** English PDFs from **public** sites (guidelines, patient-summary standards, prescribing data documentation, etc.). Example of an openly linked English PDF: [EU guidelines — patient summary (PDF)](https://health.ec.europa.eu/system/files/2019-02/guidelines_patient_summary_en_0.pdf). |

**Repository layout (documentation + optional files)**

- Maintain **`data/pdf-demo/README.md`** (or equivalent under the application module) listing **intended sources**, **direct URLs**, and **license / attribution** notes. Operators download PDFs locally if binaries are not committed (to keep the repo small).
- Optional: commit **1–2 small** PDF fixtures **only** if licenses explicitly allow redistribution; otherwise use **Testcontainers / test resources** with **tiny synthetic or minimal PDF bytes** for automated tests.

**README wording (course / demo)**

- *Primary corpus:* curated English medical RAG corpus on Hugging Face for scalable indexing and evaluation.
- *Supplementary raw documents:* a small set of open English medical PDFs to demonstrate **PDF ingestion and text extraction** alongside the primary corpus.

## System Architecture

### Modular monolith (Spring Modulith, ExpertMatch-aligned)

DocuRAG follows the same **structural conventions** as ExpertMatch (`aist-expertmatch`): one deployable Spring Boot application whose **bounded contexts** are **Spring Modulith application modules**, each rooted at `com.berdachuk.docurag.<module>` with a **`package-info.java`** declaring:

- `id` — stable module identifier (used in `allowedDependencies`).
- `displayName` — human-readable label (optional; recommended).
- `allowedDependencies` — other module **ids** this package tree may depend on.
- `type = OPEN` — only where needed for shared infrastructure (typically **core**), matching ExpertMatch’s use of `ApplicationModule.Type.OPEN` for cross-cutting wiring.

**Cross-module access:** Other modules consume only **`*.api`** packages (public facades, DTOs, service interfaces). Implementation types stay in internal packages (ExpertMatch also uses SPI-style patterns where applicable). No direct imports from another module’s internal `impl` or repository packages.

**Maven (minimum):**

- Import **`spring-modulith-bom`** (version pinned with Spring Boot).
- Dependency **`spring-modulith-starter-core`**.
- Test scope: **`spring-modulith-starter-test`** for **`@ApplicationModuleTest`** and Modulith architecture tests so dependency violations fail **`mvn verify`**.
- Optional: generate module docs (ExpertMatch uses Modulith documentation output under `target/modulith-docs`; enable if the chosen Boot/Modulith stack supports it).

Official reference: [Spring Modulith](https://docs.spring.io/spring-modulith/reference/index.html).

### Application modules and packages

| Modulith `id` | Java package root | Responsibility |
|---------------|-------------------|----------------|
| `core` | `com.berdachuk.docurag.core` | Shared infrastructure: JDBC/DataSource wiring, shared exceptions, security-related beans if added later. **OpenAI-compatible Spring AI wiring** (ExpertMatch-style `DocuRagAiConfiguration` / equivalent): builds **`OpenAiApi` + `OpenAiChatModel` + `OpenAiEmbeddingModel`** beans, `@Primary` **`ChatModel`** and **`EmbeddingModel`**, and base **`ChatClient`** unless the **llm** module registers a superset client. **`IdGenerator`** (or equivalent) for **MongoDB-compatible string primary keys** (see [Identifier strategy](#identifier-strategy-mongodb-compatible)). **`Type.OPEN`** if cross-module configuration orchestration is required. |
| `documents` | `com.berdachuk.docurag.documents` | Dataset loading, ingestion, parsing (**JSONL/CSV exports and PDF text extraction**), metadata normalization; persistence for source documents. |
| `chunking` | `com.berdachuk.docurag.chunking` | Chunking strategy and chunk metadata; persists or delegates chunk rows per schema. |
| `vector` | `com.berdachuk.docurag.vector` | Embedding generation (calls to embedding model) and pgvector persistence for chunk vectors. |
| `retrieval` | `com.berdachuk.docurag.retrieval` | Semantic search: top-k chunk retrieval, scores, snippets (no generative LLM in this module). |
| `llm` | `com.berdachuk.docurag.llm` | Spring AI chat client, prompts, **`QuestionAnswerAdvisor`** / optional **`RetrievalAugmentationAdvisor`**, assembly of grounded answers from **retrieval** context. |
| `extraction` | `com.berdachuk.docurag.extraction` | Structured extraction (entities, categories, relations) for visualization and graph payloads. |
| `visualization` | `com.berdachuk.docurag.visualization` | Chart/graph DTOs for REST and Thymeleaf. |
| `evaluation` | `com.berdachuk.docurag.evaluation` | Eval dataset loading, scoring, persistence of runs and per-case results. |
| `web` | `com.berdachuk.docurag.web` | REST controllers and Thymeleaf controllers. Typically **`Type.OPEN`** or allowed to depend on all upstream **api** surfaces for the demo app. |

### Example `allowedDependencies` (initial DAG)

Adjust during implementation if cycles appear; Modulith verification must stay green.

| Module `id` | `allowedDependencies` |
|-------------|-------------------------|
| `core` | `{}` or minimal set required for OPEN orchestration (keep as small as ExpertMatch-equivalent discipline allows). |
| `documents` | `core` |
| `chunking` | `documents`, `core` |
| `vector` | `chunking`, `documents`, `core` |
| `retrieval` | `vector`, `documents`, `core` |
| `llm` | `retrieval`, `core` |
| `extraction` | `llm`, `retrieval`, `documents`, `core` |
| `visualization` | `extraction`, `documents`, `core` |
| `evaluation` | `llm`, `retrieval`, `vector`, `core` |
| `web` | `evaluation`, `visualization`, `extraction`, `llm`, `retrieval`, `vector`, `chunking`, `documents`, `core` |

**Bootstrap class:** `com.berdachuk.docurag.DocuRagApplication` (or `com.berdachuk.DocuRagApplication` per Run owner rules) in a neutral package; keep the Modulith **application root** as the Spring Boot main class package’s parent of `docurag` modules (same pattern as ExpertMatch’s `ExpertMatchApplication` under `com.epam.expertmatch`).

**Example `package-info.java` shape** (mirror ExpertMatch):

```java
@org.springframework.modulith.ApplicationModule(
    id = "retrieval",
    displayName = "Semantic Retrieval",
    allowedDependencies = {"vector", "documents", "core"}
)
package com.berdachuk.docurag.retrieval;
```

### High-level flow

1. Load selected corpus subset from Hugging Face export or prepared local JSONL/CSV snapshot; optionally ingest **supplementary PDFs** from a configured folder (see [Supplementary PDF demo corpus](#supplementary-pdf-demo-corpus-required-capability)).
2. Normalize and persist source documents (**documents**).
3. Split documents into chunks with metadata (**chunking**).
4. Generate embeddings using `nomic-embed-text` through Ollama (**vector**).
5. Store chunks and vectors in PostgreSQL + pgvector (**vector**).
6. For QA, **retrieval** returns top-k chunks; **llm** assembles the prompt and calls Gemma4 31B Cloud through Spring AI.
7. For analysis, **extraction** runs structured prompts (via **llm** / shared clients) on retrieved or selected content; **visualization** aggregates categories/entities into chart and graph data.
8. For **evaluation**, run questions through the **llm** pipeline and compute normalized accuracy plus semantic similarity.

## Functional Requirements

### FR-1 Corpus ingestion

The system shall import:

1. **Primary corpus:** a curated subset from local prepared files exported from Hugging Face (JSONL, CSV, or other documented structured format), preserving title, category, source identifier, and raw text where available.
2. **PDF documents:** files with extension **`.pdf`** from a configured path (including the **supplementary PDF demo pack** described under [Dataset Strategy](#dataset-strategy)). Text shall be extracted with a **JVM PDF library** (default: **Apache PDFBox**). Extracted text is stored like other documents; **`source_url`** and/or **`external_id`** should reflect the file name or canonical download URL when known. **Scanned image-only PDFs without a text layer** may be skipped or logged as unsupported in v1 (no mandatory OCR).

Acceptance criteria:
- An operator can trigger ingestion from a configured folder or command for **both** structured exports **and** PDFs (same or separate configured roots, as documented in README).
- New rows receive **MongoDB-compatible** primary keys via **`IdGenerator.generateId()`** (or equivalent; see [Identifier strategy](#identifier-strategy-mongodb-compatible)).
- The system stores each source document once and avoids duplicate imports using a stable external identifier or content hash (for PDFs: e.g. hash of normalized extracted text plus file path or URL).
- **`source_format`** (or equivalent metadata) distinguishes **`hf_export`** vs **`pdf`** where useful for UI and debugging (see [Suggested schema](#suggested-schema)).
- The UI shows ingestion status, counts, and category distribution.
- Unit or integration tests cover **PDF text extraction** on a **minimal fixture** (no large binaries required in CI).

### FR-2 Chunking

The system shall split each document into semantically useful chunks for retrieval. Chunk metadata shall include document id, chunk index, title, category, and optional source reference.

Acceptance criteria:
- Chunk size and overlap are configurable.
- Chunks below a minimum content threshold are skipped.
- Chunking statistics are logged and visible in the admin/demo page.

### FR-3 Vector indexing

The system shall generate embeddings for all chunks using `nomic-embed-text` through Ollama and store them in pgvector with 768 dimensions. Embedding I/O and vector persistence are owned by the **`vector`** Modulith module; similarity search queries are owned by **`retrieval`**.

Acceptance criteria:
- Embeddings are generated in batches.
- Failed embedding requests are retried with backoff.
- Indexing can be rerun idempotently without duplicating vectors.
- **Optional:** When **multi-endpoint** embedding is configured (see [Multi-endpoint embedding providers](#multi-endpoint-embedding-providers-expertmatch-aligned)), bulk chunk embedding uses the pool; configuration may be empty (single-endpoint via `spring.ai.custom.embedding` only).

### FR-4 Question answering

The system shall expose a REST endpoint and Thymeleaf UI that accept an English question and return a grounded answer together with retrieved supporting chunks. **Retrieval** (semantic search) and **LLM** (Spring AI RAG components and advisors) shall be implemented in separate Modulith modules; the **web** layer orchestrates HTTP only and delegates to published **api** services.

Acceptance criteria:
- The answer includes at least the final response text and supporting chunk metadata.
- The UI shows the question, final answer, and retrieved chunks.
- The system supports configurable `topK` and similarity threshold.

### FR-5 Structured extraction

The system shall extract structured information from corpus content or retrieved context to support visualization. The initial extraction targets are category aggregation and optional entity/relation extraction.

Acceptance criteria:
- The analysis endpoint returns structured JSON.
- A pie chart can be rendered from category counts.
- A graph view can be rendered from entities and relations when extraction is enabled.

### FR-6 Visualization

The system shall provide at least one chart and one graph-ready payload for demo use. The Thymeleaf UI shall render a simple pie chart and a simple graph or relationship list.

Acceptance criteria:
- The pie chart shows document or extracted concept distribution by category.
- The graph visualization shows extracted nodes and edges for a selected document set or question result.
- REST endpoints return the same data used by the UI.

### FR-7 Evaluation

The system shall support answer-quality evaluation against an English eval dataset with reference answers. The **minimum** acceptable implementation for the course task is **one** clearly defined criterion (e.g. normalized accuracy **or** mean semantic similarity **or** a documented qualitative rubric) applied to a **small** labeled set, with results **stored and shown** (UI and/or API/export). The default quantitative bundle computes **normalized accuracy** and **semantic similarity** (cosine on embeddings) between generated and ground-truth answers.

Acceptance criteria:
- Eval input format is versioned and documented.
- At least **one** metric (quantitative or qualitative) is defined in README or `PRD.md` and traceable to eval output.
- An operator can trigger evaluation from REST and from a command-line runner.
- The system stores per-case results and aggregate summaries.
- The UI can display the latest evaluation summary (or exported artifact is described for demo if UI is deferred).

## Non-Functional Requirements

### NFR-1 Maintainability

The codebase shall be organized into **Spring Modulith application modules** with enforced `allowedDependencies`, not merely informal package folders. Implementation shall use Spring JDBC repositories with handwritten SQL. JPA and Spring Data shall not be used anywhere in the project.

### NFR-6 Identifiers (MongoDB-compatible string IDs)

All **application-generated primary keys** for persisted entities (documents, chunks, ingestion jobs, evaluation entities, etc.) shall be **MongoDB-compatible**: **24-character hexadecimal strings** matching the **ObjectId** layout (timestamp, machine, process, counter) and the pattern **`^[0-9a-fA-F]{24}$`**. Generation and validation shall follow the same approach as ExpertMatch’s **`IdGenerator`** (see reference implementation: `aist-expertmatch` / `com.epam.expertmatch.core.util.IdGenerator` — `generateId()` / `isValidId()`). **Do not use UUID** types for these keys in new DocuRAG code; store them as **`text`** or **`varchar(24)`** in PostgreSQL with optional `CHECK` constraints. **External** identifiers from the Hugging Face corpus remain in **`external_id`** (and may be non-ObjectId strings).

### NFR-2 Reproducibility

The system shall run locally under WSL2 with Docker and use Testcontainers for integration tests involving PostgreSQL + pgvector. Environment setup instructions shall explicitly describe WSL2 and Docker Desktop or Docker Engine integration.

### NFR-7 Integration tests (full flow + Testcontainers + mocked AI)

The project shall include **integration tests** that:

1. **Start PostgreSQL with pgvector via Testcontainers** (same image or equivalent approach as local dev). Default **`mvn verify`** assumes **Docker is available**. The README may document an optional Maven profile that **skips** Testcontainers IT on machines without Docker, but the **canonical** CI path runs the full-flow tests.
2. Run **Flyway** (or equivalent) against the container database and exercise a **full vertical slice** of the product, for example: ingest a **small fixture corpus** (structured rows **and/or** text produced from a **tiny PDF fixture**) → chunk → persist **deterministic embedding vectors** (via **mocked `EmbeddingModel`** returning fixed-dimension vectors) → **retrieval** → **LLM** QA path with **`ChatModel` / `ChatClient` mocked** to return stable assistant text → assert HTTP or service-level outcomes (retrieved chunks, answer shape, persisted rows).
3. **Never call a real generative LLM** in the default test suite: **`ChatModel`** (and **`ChatClient`** if applicable) must be **mocks or stubs** (ExpertMatch **`TestAIConfig`** pattern; real AI config **`@Profile("!test")`**).
4. **Mock `EmbeddingModel`** in these full-flow tests unless a separate **documented optional profile** is used for local “live embedding” experiments—baseline CI must stay **offline** for AI endpoints.

Optional additional IT: mini **evaluation** run with mocked chat producing fixed answers, then assert metric computation. **Spring Modulith** architecture tests remain separate but should run in the same **`verify`** phase when enabled.

### NFR-3 Performance

The application shall support indexing 2,000 to 5,000 documents without excessive memory pressure on a developer workstation. The first answer for a warmed index should ideally complete within 5 to 15 seconds depending on the local Ollama/Gemma4 deployment characteristics.

### NFR-4 Observability

The system shall log ingestion, embedding generation, retrieval latency, generation latency, and evaluation summaries. Actuator health endpoints shall be enabled for application and dependency checks.

### NFR-5 Safety

The application shall display a disclaimer in the UI that answers are generated from a medical corpus for educational/demo purposes and are not medical advice.

## Database Design

### Identifier strategy (MongoDB-compatible)

Primary keys are **string IDs** compatible with **MongoDB ObjectId** semantics: **12 bytes → 24 hex characters** (`0-9`, `a-f`). This keeps IDs **URL-safe**, **lexicographically sortable by creation time** (when using the standard ObjectId byte layout), and **aligned with ExpertMatch** for shared patterns across projects.

**Reference implementation:** replicate the algorithm and API surface of ExpertMatch **`IdGenerator`**: `generateId()` produces a new ID; `isValidId(String)` enforces length 24 and hex. Place the utility in **`com.berdachuk.docurag.core`** (e.g. `util/IdGenerator.java`).

### Core tables

- `source_document`
- `document_chunk`
- `ingestion_job`
- `evaluation_dataset`
- `evaluation_case`
- `evaluation_run`
- `evaluation_result`

### Suggested schema

All internal `id` columns and foreign keys referencing them use **`text`** (or **`varchar(24)`**) storing **24 hex characters**. Example constraint: `CHECK (id ~ '^[0-9a-fA-F]{24}$')`.

#### `source_document`

- `id` text primary key (MongoDB-compatible ObjectId hex string)
- `external_id` text unique
- `title` text
- `category` text
- `source_name` text
- `source_url` text nullable
- `content` text
- `content_hash` text
- `source_format` text nullable — e.g. `hf_export`, `pdf` (provenance for primary vs PDF demo ingestion)
- `created_at` timestamptz

#### `document_chunk`

- `id` text primary key (MongoDB-compatible ObjectId hex string)
- `document_id` text references `source_document(id)`
- `chunk_index` integer
- `chunk_text` text
- `token_estimate` integer
- `category` text
- `metadata_json` jsonb
- `embedding` vector(768)
- `created_at` timestamptz

Spring AI and pgvector documentation support storing vectors in PostgreSQL-backed stores and querying them for semantic similarity.

#### `ingestion_job`

- `id` text primary key (MongoDB-compatible ObjectId hex string)
- `started_at` timestamptz
- `finished_at` timestamptz nullable
- `status` text
- `documents_loaded` integer
- `documents_indexed` integer
- `error_message` text nullable

#### `evaluation_dataset`

- `id` text primary key (MongoDB-compatible ObjectId hex string)
- `name` text unique
- `version` text
- `description` text
- `created_at` timestamptz

#### `evaluation_case`

- `id` text primary key (MongoDB-compatible ObjectId hex string)
- `dataset_id` text references `evaluation_dataset(id)`
- `external_case_id` text
- `question` text
- `ground_truth_answer` text
- `category` text
- `difficulty` text nullable
- `metadata_json` jsonb

#### `evaluation_run`

- `id` text primary key (MongoDB-compatible ObjectId hex string)
- `dataset_id` text references `evaluation_dataset(id)`
- `started_at` timestamptz
- `finished_at` timestamptz nullable
- `model_name` text
- `embedding_model_name` text
- `normalized_accuracy` numeric
- `mean_semantic_similarity` numeric
- `semantic_accuracy_at_080` numeric
- `notes` text nullable

#### `evaluation_result`

- `id` text primary key (MongoDB-compatible ObjectId hex string)
- `run_id` text references `evaluation_run(id)`
- `case_id` text references `evaluation_case(id)`
- `predicted_answer` text
- `exact_match` boolean
- `normalized_match` boolean
- `semantic_similarity` numeric
- `semantic_pass` boolean
- `retrieved_chunks_json` jsonb

## API Requirements

### REST endpoints

#### Documents

- `POST /api/documents/ingest`
- `GET /api/documents`
- `GET /api/documents/{id}`
- `GET /api/documents/categories`

#### Indexing

- `POST /api/index/rebuild`
- `POST /api/index/incremental`
- `GET /api/index/status`

#### RAG

- `POST /api/rag/ask`
- `POST /api/rag/analyze`
- `GET /api/rag/history/{id}` optional

#### Visualization

- `GET /api/visualizations/categories/pie`
- `GET /api/visualizations/entities/graph`

#### Evaluation

- `POST /api/evaluation/run`
- `GET /api/evaluation/runs`
- `GET /api/evaluation/runs/{id}`
- `GET /api/evaluation/latest`

### Example request/response shapes

#### `POST /api/rag/ask`

Request:

```json
{
  "question": "What is the first-line treatment for uncomplicated hypertension in adults?",
  "topK": 5,
  "minScore": 0.70
}
```

Response:

```json
{
  "answer": "First-line management typically includes lifestyle modification and guideline-supported antihypertensive therapy such as thiazide diuretics, ACE inhibitors, ARBs, or calcium channel blockers depending on patient factors.",
  "model": "gemma4:31b-cloud",
  "retrievedChunks": [
    {
      "documentId": "674a1b2c3d4e5f6789012345",
      "title": "...",
      "category": "cardiology",
      "score": 0.87,
      "snippet": "..."
    }
  ]
}
```

#### `POST /api/evaluation/run`

Request:

```json
{
  "datasetName": "medical-rag-eval-v1",
  "topK": 5,
  "minScore": 0.70,
  "semanticPassThreshold": 0.80
}
```

Response:

```json
{
  "runId": "674a1b2c3d4e5f6789012346",
  "total": 100,
  "normalizedAccuracy": 0.61,
  "meanSemanticSimilarity": 0.84,
  "semanticAccuracyAt080": 0.76
}
```

## UI Requirements

The Thymeleaf UI is intended only for demonstration and operator convenience, not for full product-grade interaction design. It should remain intentionally simple and stable.

### Required pages

- `/` — dashboard with counts, ingestion status, latest evaluation summary, and links.
- `/qa` — form for asking a question and displaying answer plus supporting chunks.
- `/analysis` — category chart and entity graph view.
- `/documents` — paginated list of ingested documents.
- `/evaluation` — evaluation run form and summary table.

### UI behaviors

- Forms submit via POST and re-render with results.
- Use lightweight JS only for chart rendering and optional graph rendering.
- Display clear model names and dataset information.
- Show a medical disclaimer on every interactive page.

## Spring AI Integration Requirements

Spring AI shall back **chat** in the **`llm`** module and **embedding** usage in the **`vector`** module (ExpertMatch-style split: **`retrieval`** performs semantic search only, without generative calls). The **`evaluation`** module may use **`vector.api`** (or a shared embedding port) for semantic-similarity scoring between answers. The QA pipeline should start with `QuestionAnswerAdvisor` for fast implementation, with the architecture allowing later replacement or extension with `RetrievalAugmentationAdvisor` when finer retrieval control is needed.

### ExpertMatch-aligned LLM and embedding setup (OpenAI-compatible)

DocuRAG shall follow the same **integration pattern** as ExpertMatch (`spring-ai-starter-model-openai`, explicit Java beans, separate chat vs embedding endpoints and options). Vectors are still **persisted with JDBC + pgvector** (this PRD does not require Spring AI `VectorStore`); **`EmbeddingModel`** is used only to **compute** embeddings.

**1. Maven**

- Dependency: **`spring-ai-starter-model-openai`** (version from Spring AI BOM).

**2. Disable Spring AI OpenAI auto-configuration**

- In `application.yml`, set **`spring.autoconfigure.exclude`** to omit at least:
  - `OpenAiChatAutoConfiguration`
  - `OpenAiEmbeddingAutoConfiguration`
- (Optional) Exclude unused OpenAI auto-configurations (audio, image, moderation) as ExpertMatch does, to avoid accidental bean creation.
- Set **`spring.ai.openai.enabled: false`** (or equivalent) so defaults do not silently create duplicate models; **real beans are created only in code** from custom properties.

**3. Configuration properties (mirror ExpertMatch structure)**

- Keep a documented **`spring.ai.openai.*`** block for **reference defaults** (model names, temperatures, embedding **dimensions** = **768** for `nomic-embed-text`, aligned with `vector(768)`).
- Use **`spring.ai.custom.chat.*`** for the **chat** endpoint:
  - `provider` — `openai` (meaning OpenAI-**compatible** HTTP API, including Ollama).
  - `base-url` — e.g. Ollama OpenAI-compatible base URL.
  - `api-key` — often empty for local Ollama; required for cloud providers.
  - `model` — e.g. `gemma4:31b-cloud`.
  - `temperature`, `max-tokens` — tunable.
- Use **`spring.ai.custom.embedding.*`** for the **embedding** endpoint (may be **same host** as chat or **different** provider):
  - `base-url`, `api-key`, `model` (e.g. `nomic-embed-text`), **`dimensions` = `768`**.
- Map properties from environment variables (ExpertMatch style), e.g. `CHAT_BASE_URL`, `CHAT_MODEL`, `EMBEDDING_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS`, so Docker/local/README stay consistent.

**4. Java configuration (`core` module, profile `!test`)**

- Single configuration class (e.g. **`DocuRagAiConfiguration`**) analogous to ExpertMatch’s **`SpringAIConfig`**:
  - If **`spring.ai.custom.chat.base-url`** is set: build **`OpenAiApi`** with that base URL and API key, then **`OpenAiChatModel`** with **`OpenAiChatOptions`** (model, temperature, max tokens).
  - If **`spring.ai.custom.embedding.base-url`** is set: build a separate **`OpenAiApi`** (or shared if intentionally identical), then **`OpenAiEmbeddingModel`** with **`OpenAiEmbeddingOptions`** (model, **dimensions**).
  - Expose **`@Primary` `ChatModel`** (e.g. bean name **`primaryChatModel`**) and **`@Primary` `EmbeddingModel`** (e.g. **`primaryEmbeddingModel`**) so **`ChatClient`**, pgvector-related code, and tests resolve unambiguously.
  - Expose **`ChatClient`** built with **`ChatClient.builder(chatModel).build()`** for use in **`llm`** (unless **`llm`** overrides with an advisor-heavy client; then use **`@Primary`** / **`@Qualifier`** consistently).
- **Provider guard:** restrict to OpenAI-**compatible** HTTP APIs (same rule as ExpertMatch: configure via `OpenAiApi` / `OpenAiChatModel` / `OpenAiEmbeddingModel`).

**5. DocuRAG default targets (Ollama)**

- **Chat:** OpenAI-compatible Ollama endpoint; model **`gemma4:31b-cloud`** (or Run owner default).
- **Embeddings:** same or separate Ollama base URL; model **`nomic-embed-text`**; **768** dimensions matching DB and metrics.

**6. Testing (ExpertMatch-style)**

- **`@Profile("!test")`** on the real AI configuration class.
- Test profile provides **`@Primary`** mock **`ChatModel`** and **`EmbeddingModel`** (and optional **`ChatClient`**) so **`mvn test` / `mvn verify`** never calls a real LLM or embedding API in the default suite. **Integration tests** (Testcontainers full flow) **must** use these mocks (or equivalent test `@Configuration`) so the pipeline is reproducible without Ollama. Optional **`live-ai`** (or similar) profile for local manual runs only—document in README, not required for CI.

**7. Optional extension**

- A second **`ChatModel`** with **`@Qualifier("rerankingChatModel")`** and **`spring.ai.custom.reranking.*`** (ExpertMatch pattern) is **out of scope** for v1 unless added as a ninja; document the same property shape if introduced later.

### Model configuration (summary)

- **Chat** and **embedding** use **separate** `spring.ai.custom.*` blocks so Ollama (or cloud) can use different paths, models, or keys per concern.
- Timeouts, retries, and model names are **fully externalized** (YAML + environment variables).
- **Embedding output dimension** must stay **consistent** with **`document_chunk.embedding`** (`vector(768)`) and evaluation code.

## Multi-endpoint embedding providers (ExpertMatch-aligned)

DocuRAG shall support an **optional multi-endpoint embedding pool** for **bulk indexing** (chunk embedding during ingest/rebuild), following the same **design pattern** as ExpertMatch (`aist-expertmatch`): configuration-driven activation, one **`OpenAiEmbeddingModel` per endpoint URL**, a shared **task queue**, **worker threads per endpoint**, **automatic skip** of failing endpoints, and **batched OpenAI-compatible API calls** to reduce round-trips.

**Reference implementation (ExpertMatch):**

- `MultiEndpointEmbeddingProperties` — `@ConfigurationProperties` for endpoint list and tuning.
- `EmbeddingEndpointPoolConfig` — creates the pool when the first endpoint URL is configured (`@ConditionalOnProperty` on `...endpoints[0].url`).
- `EmbeddingEndpointPool` — `LinkedBlockingQueue` of embedding tasks; workers per endpoint pull tasks; on failure, endpoint marked **skipped** for `skip-duration-min`; failed tasks **re-queued**; `api-batch-size` groups multiple texts per `embedForResponse` call (throughput).
- `MultiEndpointEmbeddingServiceImpl` — `@Primary` `EmbeddingService` when the pool exists; `EmbeddingServiceImpl` remains the fallback when the pool is **not** configured (`@ConditionalOnMissingBean(EmbeddingEndpointPool.class)`).

### DocuRAG configuration namespace

Use a dedicated prefix (mirror ExpertMatch structure under the DocuRAG product key):

```yaml
docurag:
  ingestion:
    embeddings:
      multi-endpoint:
        endpoints: []           # Empty → single-endpoint mode only (spring.ai.custom.embedding)
        skip-duration-min: 10   # Skip endpoint after failure (minutes)
        worker-per-endpoint: 1  # Default worker threads per endpoint
        api-batch-size: 50      # Texts per embedding API call (1 = one HTTP call per text)
        # Example when pool enabled:
        # endpoints:
        #   - url: http://192.168.0.10:11434
        #     model: nomic-embed-text
        #     priority: 1         # Lower number = higher priority (sorted ascending)
        #   - url: http://192.168.0.11:11434
        #     model: nomic-embed-text
        #     priority: 2
        #     workers: 2          # Optional: override worker count for this endpoint
```

Map keys to environment variables in README (e.g. `DOCURAG_INGESTION_EMBEDDINGS_MULTI_ENDPOINT_*`) following the same style as ExpertMatch’s `EXPERTMATCH_INGESTION_EMBEDDINGS_MULTI_ENDPOINT_*`.

**Shared embedding settings:** Per-endpoint **`OpenAiApi`** uses the same **`spring.ai.custom.embedding.api-key`** (and **`spring.ai.custom.embedding.dimensions`**) as the single-endpoint path so **vector width stays 768** across all nodes unless intentionally overridden per endpoint in code (not required for v1).

### Behavioral requirements

1. **Activation:** Multi-endpoint mode is **on** when `docurag.ingestion.embeddings.multi-endpoint.endpoints` contains **at least one** entry with a non-blank **`url`** (same conditional pattern as ExpertMatch `endpoints[0].url`).
2. **Homogeneity:** All endpoints must produce **identical embedding dimension** (**768** for DocuRAG) and **semantically compatible** models (same or equivalent embedder family) so **chunk vectors and query embeddings** are comparable. Mixing unrelated models invalidates retrieval quality.
3. **Scope of use:** The pool targets **high-volume batch embedding** in the **`vector`** / ingestion pipeline. **Query embedding** for retrieval may use either the **primary** `spring.ai.custom.embedding` endpoint **or** delegate to the same pool implementation—document the chosen approach in README; default recommendation: **one logical embedder** for query (single URL) to avoid cross-model mismatch unless all pool endpoints are identical models.
4. **Failover:** Failed batches mark the endpoint skipped; workers **re-offer** tasks to the queue so other endpoints can process them; after `skip-duration-min`, skipped endpoints are eligible again.
5. **Tests:** **`@Primary`** mock **`EmbeddingModel`** / test **`EmbeddingService`** must remain valid when the pool is **disabled** (empty endpoints). When testing the pool, use integration tests with **mock HTTP** or **Testcontainers** mock servers—do not require multiple real Ollama nodes in CI.
6. **Shutdown:** Pool implements graceful **executor shutdown** on context destroy (ExpertMatch `@PreDestroy` pattern).

### Module ownership

- **`vector`** (and/or **`documents`** ingestion orchestration) owns calling **`EmbeddingService`** / pool for **chunk** embedding.
- **`core`** or **`vector`** may host **`DocuRagMultiEndpointEmbeddingProperties`** and **`EmbeddingEndpointPoolConfig`** beans (mirror ExpertMatch package layout: `embedding.config`, `embedding.multiendpoint`).

## JDBC and SQL Constraints

- Use JDBC only (no JPA). Prefer **`NamedParameterJdbcTemplate`** for repositories and **named parameters**
  (avoid positional `?` binds) to keep SQL readable and reduce parameter-order bugs.
- SQL must be explicit and kept in repository classes or dedicated SQL resources.
- No ORM-generated schema from JPA.
- Database migrations should use Flyway or Liquibase; Flyway is preferred for simplicity.

## WSL2 and Docker Requirements

The local development workflow shall support Windows + WSL2 with Docker. PostgreSQL with pgvector must be runnable through Docker Compose or Testcontainers-backed tests from the WSL2 environment.

### Local environment expectations

- JDK 21+ installed in WSL2.
- Maven or Gradle installed in WSL2.
- Docker Desktop with WSL integration enabled, or native Docker Engine inside WSL2.
- `testcontainers` can connect to Docker from test execution.

### Required dev assets

- `compose.yaml` for local PostgreSQL + pgvector.
- `application-local.yml` is **local-only** (gitignored). Provide an **`application-local.example.yml`** template in the repo and document “copy it locally” in the README.
- Integration tests should run with **`test`** profile and use stub AI beans (no live LLM required); a dedicated `application-test.yml` is optional if needed for overrides.
- README section for WSL2 troubleshooting and **required env vars** for chat/embedding endpoints.

## Testing Strategy

### Unit tests

- Chunking service
- **`IdGenerator`** (valid hex length, pattern, monotonic counter behavior where applicable)
- Text normalization utilities
- **PDF text extraction** (PDFBox wrapper): empty PDF, multi-page text, skip or handle PDFs with no extractable text
- SQL repository mapping
- Evaluation metric calculation
- Graph/category aggregation logic
- AI configuration unit tests (property binding / bean presence) where useful; **no real LLM** in default test profile (mock **`ChatModel`** / **`EmbeddingModel`** per ExpertMatch **`TestAIConfig`** pattern).

### Integration tests

- **Full-flow integration tests (required):** at least one test class (or cohesive suite) using **Testcontainers** to spin up **PostgreSQL + pgvector**, apply migrations, then run an **end-to-end path** through the Spring context—e.g. ingest fixture documents → chunk → write vectors (**mock `EmbeddingModel`** with fixed 768-dimensional outputs) → similarity search → **RAG/QA** with **`ChatModel` mocked** to return predictable text → assertions on responses and/or DB state. This validates wiring across **documents**, **chunking**, **vector**, **retrieval**, **llm**, and **web** (or service entrypoints) without external AI services.
- Repository-focused tests against PostgreSQL + pgvector via Testcontainers (where not already covered by the full-flow suite).
- Optional: evaluation run against a **mini in-memory or fixture eval set** with **mocked chat** outputs, asserting computed metrics.
- **Spring Modulith** architecture tests (`spring-modulith-starter-test`) verifying the module graph and forbidden dependencies (ExpertMatch build discipline).

**Mocking rule:** all **LLM (chat)** calls in automated integration tests are **mocked**. **Embedding** calls are **mocked** in the default full-flow tests for determinism and CI; document any optional profile that uses real embeddings locally.

### Demo tests

- Smoke test for `/qa`, `/analysis`, and `/evaluation` pages (may use **`@MockBean`** / test profile with mocked AI).
- Basic controller tests for REST endpoints.

## Evaluation Recommendations

The first evaluation target should be final answer quality, not only retrieval quality. Since the project uses a fixed English corpus and a ready-made English eval dataset, the evaluation should prioritize direct comparison between generated answers and ground-truth answers through normalized accuracy and semantic similarity.

### Recommended metrics

1. **Normalized accuracy**
   - Lowercase both answers.
   - Remove punctuation and extra whitespace.
   - Compare normalized strings.
   - Best for short factual answers.

2. **Mean semantic similarity**
   - Compute embeddings for generated answer and ground truth.
   - Use cosine similarity.
   - Report the mean over the eval set.

3. **Semantic Accuracy@0.80**
   - Mark a case as pass when cosine similarity is at least 0.80.
   - Report the percentage of passed cases.

### Eval dataset recommendations

- Use English QA pairs with ground-truth answers.
- Store dataset as JSON in `src/main/resources/eval/` or import into DB.
- Include metadata fields such as category and difficulty for analysis.
- Start with 50 to 100 eval cases for stable iteration speed.

### Eval process

1. Load eval cases.
2. Run each question through the RAG pipeline.
3. Save retrieved chunks and generated answer.
4. Compute normalized accuracy and semantic similarity.
5. Save aggregate summary and per-case details.
6. Review worst-performing categories and questions.

### Optional eval extensions

- Compare topK values such as 3 vs 5.
- Compare chunk sizes and overlap strategies.
- Compare prompt variants using the same eval set.
- Add manual error taxonomy: missing evidence, partial answer, hallucination, off-topic.
- **Ninja / advanced:** retrieval **precision** and **recall** when gold-relevant chunk ids or spans are available; **faithfulness / groundedness** (e.g. require citations and check support against retrieved text, NLI, or judge on a small sample).

## Prompting Guidelines

### QA prompt principles

- Answer only from retrieved context.
- If the answer is not supported by the retrieved context, say that the information is insufficient.
- Keep answers concise and medically neutral.
- Avoid unsupported medical recommendations.

### Extraction prompt principles

- Return strict JSON.
- Use stable entity and relation labels.
- Do not invent entities not present in the supplied text.

## Security and Compliance Notes

- This is an educational and demo-oriented system, not a certified medical product.
- The UI and API documentation must clearly state that outputs are not medical advice.
- Secrets such as Ollama endpoint keys must be externalized in environment variables.

## Milestones

Target delivery aligns with the program window **Apr 2–Apr 17, 2026** (RAG Creation practical task).

### Milestone 1 — Project bootstrap

- Spring Boot project created.
- Base package `com.berdachuk` configured; **`com.berdachuk.docurag`** application modules scaffolded with **`package-info.java`** and **`@ApplicationModule`** per [Application modules and packages](#application-modules-and-packages).
- **Spring Modulith** BOM + `spring-modulith-starter-core`; test scope `spring-modulith-starter-test`; first **`@ApplicationModuleTest`** (or equivalent) passing.
- Spring AI, Thymeleaf, Spring JDBC, Flyway, PostgreSQL driver, Testcontainers dependencies added.
- Local PostgreSQL + pgvector environment running.

### Milestone 2 — Corpus ingestion and indexing

- Dataset subset prepared and documented (**Hugging Face manifest** + **PDF demo pack** instructions per [Dataset Strategy](#dataset-strategy)).
- Ingestion and chunking completed for **structured exports and PDFs** (FR-1).
- Embeddings generated and stored.
- Index stats visible in UI.

### Milestone 3 — QA and demo UI

- `/api/rag/ask` implemented (delegates to **`retrieval`** + **`llm`** module APIs).
- `/qa` Thymeleaf page working.
- Retrieved chunks displayed.

### Milestone 4 — Extraction and visualization

- Category aggregation and pie chart implemented.
- Entity/relation extraction implemented.
- Graph payload and simple rendering implemented.

### Milestone 5 — Evaluation

- Eval dataset imported.
- Evaluation service and persistence implemented.
- Latest summary visible in UI and API.

### Milestone 6 — Polish and delivery

- README completed.
- Demo flow documented.
- **Full-flow Testcontainers** integration tests stable under WSL2 + Docker; **mocked LLM and embedding** beans in test profile (no Ollama required for **`mvn verify`**).

## Deliverables

- Source code repository structured as a **Spring Modulith** modular monolith (`package-info.java` per module, **`*.api`** for cross-module use, architecture tests passing).
- **Testcontainers**-based **full-flow** integration test(s) with **mocked `ChatModel` / `ChatClient`** (and **mocked `EmbeddingModel`** for deterministic CI), documented in README.
- `PRD.md` (this document or project-local copy as required by the Run owner).
- README with setup and demo instructions, **explicit link(s) to dataset(s)** on Hugging Face or other hosts, **description of the supplementary PDF demo pack** (open English medical PDFs + `data/pdf-demo/README.md`), and **how to run evaluation** plus how to read the metric(s).
- Flyway migrations.
- Docker Compose for PostgreSQL + pgvector.
- Example corpus subset manifest.
- Example eval dataset (small, versioned; meets “at least one criterion”).
- Screenshots of QA, chart, graph, and evaluation pages (evidence that visualization and eval are **demonstrated**).
- **Course platform:** package the above for the **Answer** submission block per program instructions; confirm alignment with program-level acceptance criteria and Run owner technology recommendations.

## Cursor AI Implementation Guidance

Cursor AI should be used as a coding accelerator, but the implementation should remain architecture-first and file-structured. Prompt Cursor with bounded tasks, not broad “build everything” requests.

### Recommended Cursor workflow

1. Generate project skeleton: Spring Boot, **Spring Modulith** BOM + starter, **`package-info.java`** for each DocuRAG module and **`*.api`** stubs.
2. Implement Flyway migrations and JDBC repositories (per module ownership: **documents**, **chunking**, **vector**, **evaluation**).
3. Implement ingestion and chunking services (**structured files + PDF via PDFBox**); add Modulith architecture test when the graph stabilizes.
4. Implement **vector** (embeddings) and **retrieval** (similarity search) without chat generation in retrieval.
5. Implement **llm** module: Spring AI chat/advisors wired to **retrieval** APIs.
6. Implement **web** REST and Thymeleaf controllers delegating to module **api** only.
7. Implement **extraction**, **visualization**, and **evaluation** services plus UI pages.
8. Add **full-flow** integration tests (Testcontainers PostgreSQL + pgvector + mocked AI) and keep **`mvn verify`** green for Modulith rules.

### Recommended Cursor prompts

- “Add `package-info.java` with `@ApplicationModule` for DocuRAG modules `core`, `documents`, `chunking`, `vector`, `retrieval`, `llm`, `extraction`, `visualization`, `evaluation`, `web` using the PRD `allowedDependencies` table.”
- “Create Flyway migration for PostgreSQL + pgvector: **`text`** primary keys and foreign keys (24 hex ObjectId-style, optional `CHECK`); tables per PRD including **`source_document.source_format`**; add **`IdGenerator`** in **core** mirroring ExpertMatch `com.epam.expertmatch.core.util.IdGenerator` (`generateId` / `isValidId`).”
- “In **documents**, add **Apache PDFBox**-based PDF text extraction and wire **`POST /api/documents/ingest`** (or a documented `ingestMode` / path) so both HF-style files and **`.pdf`** under `docurag.ingestion.pdf-demo-path` work; persist **`source_format`**.”
- “Implement Spring JDBC repository for `document_chunk` in the **vector** module with pgvector insert and similarity query; expose only via **vector.api**.”
- “Implement **DocuRagAiConfiguration** in **core** (profile `!test`): exclude OpenAI auto-config, read `spring.ai.custom.chat` / `spring.ai.custom.embedding`, build `OpenAiChatModel` and `OpenAiEmbeddingModel`, `@Primary` beans and `ChatClient`; add **TestAIConfig** mocks for `test` profile (ExpertMatch pattern).”
- “Add `@ApplicationModuleTest` for `DocuRagApplication` to verify Modulith boundaries.”
- “Create Thymeleaf page for QA form with answer and retrieved chunks.”
- “Implement evaluation service that computes normalized accuracy and cosine semantic similarity using the embedding model.”
- “Add **`DocuRagFullFlowIT`**: Testcontainers pgvector, Flyway, fixture ingest → chunk → mock **`EmbeddingModel`** (768-d vectors) → retrieve → mock **`ChatModel`** for QA; assert DB + response.”

## Acceptance Summary

The project meets the **RAG Creation** task when it: (1) uses a stated GenAI stack (here: Spring AI + local models); (2) **analyzes documents** via ingestion, chunking, and retrieval; (3) **extracts** structured signals and **visualizes** them (e.g. pie chart **and** graph/diagram payload); (4) includes a **public link** to the corpus in README/submission; (5) defines **at least one** evaluation measure, runs it on a **small** eval set, and **demonstrates** results (UI, API, or export). DocuRAG additionally requires **PDF ingestion** (text-layer PDFs via **PDFBox** or equivalent) and documentation for a **supplementary open English medical PDF demo pack** alongside the **primary** Hugging Face corpus. The DocuRAG baseline adds **Spring Modulith** structure (ExpertMatch-aligned): enforced modules, **`retrieval`** vs **`llm`** separation, architecture-verifying tests, and **MongoDB-compatible string primary keys** (`IdGenerator` / ObjectId-style 24 hex). Ingest the selected English medical corpus subset, build vector embeddings with `nomic-embed-text`, answer English medical questions with Gemma4 using retrieved context, display a simple Thymeleaf demo UI, ship chart plus graph-ready analysis, and report answer-quality metrics (default: normalized accuracy and semantic similarity, or a documented equivalent).
