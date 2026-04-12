# Core Architecture (DocuRAG)

## Description

Explains the real project architecture for **DocuRAG**: Spring Modulith modules, their responsibilities, and how they interact through `*.api` contracts.

## When to use

- Choosing which module owns a feature
- Adding a new Modulith module or updating `allowedDependencies`
- Refactoring code that crosses module boundaries
- Debugging illegal cross-module imports (Modulith tests)

## Instructions

- Treat `com.berdachuk.docurag.<module>` as the unit of ownership (Modulith module).
- Prefer cross-module calls through **`com.berdachuk.docurag.<module>.api.*`**.
- Keep the edge surface in `web`:
  - Thymeleaf pages (`/`, `/qa`, `/documents`, `/evaluation`)
  - REST controllers under `com.berdachuk.docurag.web.rest` (`/api/**`)

### Modules (today)

- `core` (OPEN): shared config, IDs, health, utilities
- `documents`: structured + PDF ingestion; owns `source_document` and ingestion jobs
- `chunking`: chunk generation; owns `document_chunk` rows (text + token estimates)
- `vector`: embedding/index operations; owns `document_chunk.embedding`
- `retrieval`: similarity search (no generation)
- `llm`: grounded answer generation over retrieved chunks
- `evaluation`: evaluation datasets/runs/results
- `extraction`: graph/category extraction for visualization
- `visualization`: pie/graph DTOs
- `web` (OPEN): UI + REST façade

### Module relationships (conceptual)

```text
web
  → documents, vector, llm, evaluation, visualization
llm → retrieval
retrieval → vector
vector → chunking
chunking ↔ documents (content + category)
evaluation → llm (+ DB)
extraction → llm (+ DB)
visualization → extraction
```

## Boundaries

- Don’t bypass Modulith boundaries by importing another module’s `internal`/`impl` packages.
- Don’t change REST DTOs/paths without updating `docu-rag/docs/openapi.yaml` (E2E depends on it).
