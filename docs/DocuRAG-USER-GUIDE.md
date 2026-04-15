# DocuRAG User Guide

This guide is for people using DocuRAG from the web interface or API to ingest medical documents and ask grounded questions.

For developer setup and build instructions, see [DocuRAG-DEVELOPER-GUIDE.md](DocuRAG-DEVELOPER-GUIDE.md).

## What DocuRAG Does

DocuRAG helps you:

- ingest medical documents (JSONL, CSV, PDF),
- build a vector index,
- ask questions and get answers grounded in retrieved chunks,
- run evaluation and inspect quality metrics.

## Access the App

Local default URLs:

- UI: `http://localhost:8084/`
- API base: `http://localhost:8084/api`
- Health: `http://localhost:8084/actuator/health`

## Quick Workflow

Use this order every time:

1. Ingest documents.
2. Rebuild index.
3. Ask questions in QA.
4. (Optional) Run evaluation and review metrics.

If you skip step 2, QA may return fallback answers because no relevant chunks are available.

## Web UI Walkthrough

### Dashboard (`/`)

Shows:

- document/chunk/index counts,
- latest ingest status,
- latest evaluation summary (if available).

### Documents (`/documents`)

Use this page to ingest corpus files or folders.

Accepted inputs:

- JSONL/JSON exports,
- CSV files,
- folders with PDF files.

Path selector:

- `Start ingest` is the single primary ingest action and uses the folder/file path you provide.
- The input is prefilled with the current sample data folder by default.
- `Choose data folder (dialog)` opens a folder picker and uploads selected files directly for ingest.

It also includes index cleanup buttons:

- `Full cleanup (remove all docs + chunks)`: deletes all ingested source documents and all chunks/embeddings linked to them, useful for a true data reset before re-ingest.
- `Realtime indexing progress`: a live progress bar (auto-refresh every 2 seconds) showing embedded chunks vs total chunks while indexing runs.

Recommended sequence after cleanup:

1. Run ingest if source documents changed.
2. Rebuild index.
3. Retry QA/evaluation.

### QA (`/qa`)

Enter a medical question and submit.

Results typically include:

- final answer,
- retrieved evidence chunks,
- scores used for ranking.

Tip: if the answer says no relevant passages were retrieved, rebuild the index and verify your data was ingested.

### Evaluation (`/evaluation`)

Use this page to run benchmark-style checks over evaluation datasets.

Main metrics:

- `Normalized accuracy`: fraction of answers matching ground truth after lowercase + whitespace normalization.
- `Mean semantic similarity`: average cosine similarity between predicted and expected answers.
- `Semantic accuracy @ 0.80`: fraction of cases with semantic similarity >= `0.80`.

## API Usage

### Ingest

```bash
curl -s -X POST http://localhost:8084/api/documents/ingest \
  -H "Content-Type: application/json" \
  -d '{"paths":["/absolute/path/to/data.jsonl"]}'
```

### Rebuild index

```bash
curl -s -X POST http://localhost:8084/api/index/rebuild
```

### Ask a question

```bash
curl -s -X POST http://localhost:8084/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"What is hypertension?","topK":5,"minScore":0.5}'
```

### Check latest evaluation

```bash
curl -s http://localhost:8084/api/evaluation/latest
```

OpenAPI contract: [docu-rag/api/openapi.yaml](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/api/openapi.yaml)

## Common Issues

### "No relevant passages were retrieved from the index"

Do this:

1. Confirm ingest succeeded.
2. Run `POST /api/index/rebuild`.
3. Check `/api/index/status` for non-zero `embeddedChunkCount`.
4. Retry question with slightly lower `minScore` (for example `0.3`).

### Evaluation metrics are all zero

Typical causes:

- index is empty or stale,
- wrong dataset selected,
- retrieval threshold too strict.

Run ingest + rebuild first, then evaluate again.

## Safety Notes

DocuRAG is a demo for medical-document retrieval and answer synthesis. It is not a medical device and should not be used as a sole source for diagnosis or treatment decisions.
