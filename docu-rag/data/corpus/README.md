# Primary corpus subset (Hugging Face)

This folder documents how to build a **repeatable subset** of the primary dataset [Sagarika-Singh-99/medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus) for local ingest, per [DocuRAG-PRD.md](../../../docs/DocuRAG-PRD.md) (Dataset Strategy) and [DocuRAG-Datasets.md](../../../docs/DocuRAG-Datasets.md).

## Files

| File | Purpose |
|------|---------|
| `subset-manifest.example.json` | Machine-readable policy: dataset id, target row band (2k–5k), suggested categories, export format, ingest hooks. Copy to `subset-manifest.json` and adjust for your run. |

## Workflow

1. Open the dataset on Hugging Face and export **JSONL** (or convert to JSONL with `text`, `title`, `category`, `source`, and a stable id field).
2. Filter rows to **3–5 categories** and roughly **2,000–5,000** documents (see manifest).
3. Save the file(s) under a directory on disk (e.g. `~/data/medical-rag-subset/`).
4. Point the app at that path:
   - **Env:** `DOCURAG_CORPUS_PATH=<directory-or-file>`
   - Or **API:** `POST /api/documents/ingest` with `{ "paths": [ "<absolute-path>" ] }`
5. Run **`POST /api/index/rebuild`** after ingest, then QA / eval as usual.

For **PDF** demos (separate from HF), use [../pdf-demo/README.md](../pdf-demo/README.md).
