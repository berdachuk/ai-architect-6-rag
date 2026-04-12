# Domain Modeling (DocuRAG)

## Description

How to evolve DocuRAG’s domain concepts (documents, chunks, indexing, evaluation) while keeping clear ownership between Modulith modules and stable external contracts.

## When to use

- Adding/renaming fields in API DTOs under `com.berdachuk.docurag.*.api`
- Changing ingestion/chunking/indexing behavior or persistence
- Changing evaluation metrics or evaluation storage
- Clarifying which module owns which table/record/invariant

## Instructions

### Module ownership of core concepts

- **Documents** (`documents`): structured+PDF ingestion; owns `source_document` and `ingestion_job`
- **Chunks** (`chunking` + `vector`): `document_chunk` is shared:
  - `chunking` owns chunk text creation (`chunk_text`, `chunk_index`, `token_estimate`, category propagation)
  - `vector` owns embedding writes (`embedding`) and index status reporting
- **Retrieval** (`retrieval`): similarity search over embedded chunks (no generation)
- **LLM** (`llm`): grounded answer generation + response DTOs
- **Evaluation** (`evaluation`): datasets/cases/runs/results (stored in DB and exposed via API/UI)
- **Extraction/Visualization**: LLM-assisted extraction and DTOs for charts/graphs

### Main domain models (what to keep stable)

- `SourceDocumentSummary`, `SourceDocumentDetail` (documents API)
- `IndexStatus` (vector API)
- `ChunkHit` (retrieval API)
- `RagAskRequest`, `RagAskResponse`, `RetrievedChunkDto` (llm API)
- `EvaluationRun*`, `EvaluationCaseResult` (evaluation API)

### Cross-cutting invariants

- IDs are **24-hex ObjectId-style strings** (see `IdGenerator`).
- Embeddings are assumed **768-dim** unless changed end-to-end (schema + config + code + health checks + fixtures).
- When adding new fields to API DTOs, consider:
  - OpenAPI update (`docu-rag/docs/openapi.yaml`)
  - E2E client regeneration (`docu-rag-e2e`)

## Boundaries

- Don’t change ID format without updating DB schema assumptions, fixtures, and docs.
- Don’t change embedding dimensionality unless you update schema (`vector(N)`), config defaults, and health indicator checks.
- Don’t “leak” internal persistence records across module boundaries; prefer `*.api` DTOs/interfaces.

