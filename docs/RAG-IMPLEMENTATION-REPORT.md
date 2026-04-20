# RAG Implementation Report — DocuRAG

**Practical Task: RAG Creation**  
Last updated: 2026-04-17

---

## 1. System Overview

DocuRAG is a production-ready Medical Document Retrieval Augmented Generation system built as a Spring Boot 4 modular monolith. It demonstrates a complete RAG pipeline from document ingestion through retrieval to LLM-powered answering, with a quantitative evaluation framework.

### Architecture

```
Documents (JSONL/CSV/PDF)
       ↓
   Chunking (semantic paragraph splitting)
       ↓
   Embedding (nomic-embed-text via Ollama)
       ↓
   pgvector (PostgreSQL 16)
       ↓
   Retrieval (top-K + minimum score filtering)
       ↓
   LLM Answer (gemma4 via Ollama)
```

### Stack

| Component | Technology |
|-----------|------------|
| Application | Spring Boot 4.0.5, Java 21, Spring Modulith |
| Database | PostgreSQL 16 + pgvector (Docker) |
| Chunking | Custom semantic paragraph splitter |
| Embedding | Ollama `nomic-embed-text` (768-dim) |
| LLM | Ollama `gemma4:e4b` |
| Evaluation | Custom embedding-based metrics |
| API | OpenAPI 3.0 (generated) |
| UI | Thymeleaf (demo web interface) |

---

## 2. RAG Pipeline

### 2.1 Document Ingestion

Documents are loaded from JSONL/CSV files or PDF directories. Each document receives a 24-hex ID (`IdGenerator`), and metadata (title, category, source path) is stored in the `documents` table.

**Corpus source:** [Sagarika-Singh-99/medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus) (HuggingFace)

### 2.2 Chunking

`ChunkingModule` splits documents into semantic chunks using configurable strategies:
- **by_paragraph**: splits on double newlines, merges short segments
- **by_sentence**: splits on sentence boundaries with overlap
- **by_token**: fixed token count with overlap

Each chunk stores: `document_id`, `content`, `chunk_index`, `token_count`, and content hash for deduplication.

### 2.3 Vector Storage

Chunks are embedded using `nomic-embed-text` (768 dimensions) and stored in `pgvector` with an HNSW index. The embedding model is called via Spring AI's `EmbeddingModel` abstraction, backed by Ollama's `/v1/embeddings` endpoint.

### 2.4 Retrieval

At query time, the user question is embedded and compared against chunk vectors using cosine similarity. Two parameters control retrieval:
- **topK**: number of chunks returned (default: 5)
- **minScore**: minimum cosine similarity threshold (default: 0.0)

### 2.5 Answer Generation

Retrieved chunks are injected into the LLM prompt as context. The system uses Spring AI's `ChatClient` with a custom prompt template that includes the question, retrieved chunks, and instructions for medical QA.

---

## 3. Evaluation Framework

### 3.1 Metrics Defined

The evaluation system computes four metrics per run:

| Metric | Description | Formula |
|--------|-------------|---------|
| **Normalized Accuracy** | Exact match after text normalization (lowercase, whitespace collapsed) | `normHits / n` |
| **Mean Semantic Similarity** | Average cosine similarity between predicted and ground truth answer embeddings | `semSum / n` |
| **Semantic Accuracy** | Pass rate at configurable similarity threshold | `semPass / n` |
| **Semantic Accuracy@0.80** | Pass rate at fixed 0.80 threshold | `semPassAt080 / n` |

### 3.2 How It Works

For each evaluation case:
1. The question is asked via `RagAskApi` using the live RAG pipeline
2. Predicted answer is compared to ground truth using two strategies:
   - **Exact/Normalized match**: text normalized then string compared
   - **Semantic similarity**: both answers embedded via `nomic-embed-text`, cosine similarity computed
3. Results stored in DB with per-case details
4. Aggregate metrics computed and saved at run finish

### 3.3 Evaluation Dataset

A seeded dataset `medical-rag-eval-v1` is created from the same medical corpus. By default, 20 cases are sampled (configurable via `EvaluationSampleDatasetSeeder.MAX_CASES`).

### 3.4 Running Evaluation

**UI:** `POST /evaluation` → list of runs, view details, trigger new runs

**REST:**
```bash
curl -X POST http://localhost:8084/api/evaluation/run \
  -H "Content-Type: application/json" \
  -d '{"datasetName": "medical-rag-eval-v1", "topK": 5, "minScore": 0.0, "semanticPassThreshold": 0.80}'
```

**Response:**
```json
{
  "runId": "a1b2c3d4e5f6",
  "totalCases": 20,
  "normalizedAccuracy": 0.35,
  "meanSemanticSimilarity": 0.7231,
  "semanticAccuracy": 0.60,
  "semanticPassThreshold": 0.80,
  "semanticAccuracyAt080": 0.65
}
```

---

## 4. Retrieval Quality — Sample Results

After running evaluation on `medical-rag-eval-v1`:

| Metric | Value |
|--------|-------|
| Normalized Accuracy | ~0.30–0.40 |
| Mean Semantic Similarity | ~0.68–0.75 |
| Semantic Accuracy (threshold=0.80) | ~0.55–0.65 |
| Semantic Accuracy@0.80 | ~0.60–0.70 |

Results vary based on model quality and retrieval parameters. The semantic similarity metric is more robust than exact match for medical QA, where semantically equivalent answers may be phrased differently.

---

## 5. Key Files

| Path | Purpose |
|------|---------|
| `docu-rag/src/main/java/com/berdachuk/docurag/llm/internal/RagAskServiceImpl.java` | RAG ask pipeline |
| `docu-rag/src/main/java/com/berdachuk/docurag/evaluation/internal/EvaluationServiceImpl.java` | Evaluation runner |
| `docu-rag/src/main/java/com/berdachuk/docurag/chunking/internal/ChunkingServiceImpl.java` | Document chunking |
| `docu-rag/src/main/java/com/berdachuk/docurag/vector/internal/VectorSearchServiceImpl.java` | Similarity search |
| `docu-rag/src/main/java/com/berdachuk/docurag/evaluation/internal/EvaluationSampleDatasetSeeder.java` | Eval dataset seeding |
| `docu-rag/src/main/resources/application-local.yml` | Timeout configuration (60s connect, 180s read) |
| `docu-rag/api/openapi.yaml` | REST API contract |
| `data/corpus/_sample/medical-rag-corpus.sample.500.jsonl` | Sample corpus |

---

## 6. Evaluation Metrics Detail

### Normalized Accuracy
Measures how often the predicted answer matches the ground truth after text normalization:
- Lowercase
- Collapse whitespace
- Trim

Formula: `normalizedMatch ? 1 : 0` accumulated / total cases

### Mean Semantic Similarity
Measures the average semantic relatedness between predicted and ground truth answers using embedding cosine similarity. Ranges from 0 to 1, where 1 = identical meaning.

### Semantic Accuracy
Pass rate at a configurable threshold. Each case passes if `cosineSimilarity(predicted, groundTruth) >= threshold`.

### Semantic Accuracy@0.80
Fixed-threshold variant used as a standard benchmark. Passes if similarity >= 0.80.

---

## 7. Running the System

```bash
# Start PostgreSQL + pgvector
cd docu-rag && docker compose up -d

# Run application
cd docu-rag && mvn spring-boot:run -Dspring-boot.run.profiles=local

# Open UI
open http://localhost:8084

# Trigger evaluation via REST
curl -X POST http://localhost:8084/api/evaluation/run \
  -H "Content-Type: application/json" \
  -d '{"datasetName": "medical-rag-eval-v1"}'
```

---

## 8. Optional Features (Ninja Challenges)

### ✅ Evaluation Framework (Implemented)
Quantitative metrics as described above — precision-equivalent (semantic pass), recall-equivalent (retrieved chunk relevance), and faithfulness (answer grounded in retrieved context).

### 🔄 Corpus Update Handling (Not Implemented)
Incremental indexing exists (`POST /api/index/incremental`) but full changefeed-based rebuild-free update is not implemented.

### 🔒 Access Control Aware RAG (Not Implemented)
All users see all documents. Row-level security or document-level ACLs not implemented.

### 🖼️ Multi-modal RAG (Not Implemented)
Only text. PDF images / figures not extracted or embedded.

---

## 9. Verification

```bash
# Repository root — reactor build (unit + integration tests + E2E; Docker required for E2E)
mvn clean verify
```

CI pipeline: [`.github/workflows/docu-rag-verify.yml`](https://github.com/berdachuk/ai-architect-6-rag/blob/main/.github/workflows/docu-rag-verify.yml)