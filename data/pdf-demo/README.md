# Supplementary PDF demo pack (DocuRAG)

This folder supports the **supplementary** ingestion path described in [DocuRAG-PRD.md](../../../docs/DocuRAG-PRD.md) (Dataset Strategy — supplementary PDF demo corpus). For a **curated URL list** (WHO / EU / NHS examples) and folder layout notes, see [DocuRAG-Datasets.md](../../../docs/DocuRAG-Datasets.md).

## Role vs primary corpus

| Role | Source | Purpose |
|------|--------|---------|
| **Primary RAG corpus** | [medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus) on Hugging Face | Scale, categories, retrieval quality, evaluation |
| **This PDF pack** | **5–15** open **English** PDFs from **official or clearly public** health / guidance sites | Prove **end-to-end PDF text extraction → chunk → embed → QA** |

Do **not** replace the Hugging Face corpus with random PDF-only dumps for the main medical RAG story. Use PDFs here as a **format and credibility** demo.

## Why prefer official medical PDFs

- **Real PDFs** with verifiable provenance (guidelines, patient-information standards, public prescribing documentation).
- **English** content aligned with DocuRAG scope.
- Stronger **defense / coursework narrative** than opaque generic PDF collections, where licensing and source transparency are often unclear.

Generic “dataset of PDF files” repositories may still be useful **only** to prove parser robustness; they are a **weak** substitute for medical RAG demos compared to the Hugging Face corpus plus a small official PDF set.

## Example source (illustrative)

You may include PDFs such as (verify current URL and terms on the publisher site before redistributing binaries):

- **EU / public health — patient summary guidelines (English PDF):**  
  [https://health.ec.europa.eu/system/files/2019-02/guidelines_patient_summary_en_0.pdf](https://health.ec.europa.eu/system/files/2019-02/guidelines_patient_summary_en_0.pdf)

Expand the list to **5–10** similar documents from:

- EU health commission pages,
- NHS / UK government public documentation,
- Other **openly published** clinical or public-health guidance in English.

Record each addition in the manifest section below.

## Manual PDF Selection and Indexing

### Step 1: Select PDFs

**Option A — Web UI (recommended)**
1. Start the app: `cd docu-rag && mvn spring-boot:run -Dspring-boot.run.profiles=local`
2. Open http://localhost:8084 in your browser
3. Go to **Documents** page
4. Click **"Choose data folder"** button — this opens a native folder picker
5. Select your PDF folder (e.g., `data/pdf-demo/downloaded/`)
6. The selected path appears on screen (no ingest starts yet)
7. Click **"Start ingest"** to begin ingestion + indexing

**Option B — Upload specific files**
1. On the Documents page, click **"Choose data folder"**
2. In the file picker dialog, select individual PDF files instead of a folder
3. Click **"Start ingest"**

**Option C — API**
```bash
# Single folder
curl -s -X POST http://localhost:8084/api/documents/ingest \
  -H "Content-Type: application/json" \
  -d '{"paths":["/absolute/path/to/pdfs"]}'

# Multiple specific paths
curl -s -X POST http://localhost:8084/api/documents/ingest \
  -H "Content-Type: application/json" \
  -d '{"paths":["/path/to/pdf1.pdf", "/path/to/pdf2.pdf"]}'
```

### Step 2: Monitor Progress

After starting ingest:
- The Documents page auto-refreshes progress every 2 seconds
- Watch the progress bar and "Last ingest status" field
- When status shows "COMPLETED", ingestion is done

### Step 3: Verify Indexing

```bash
# Check document count
curl http://localhost:8084/api/documents | python3 -c "import sys,json; print(f'Documents: {len(json.load(sys.stdin))}')"

# Check index/chunk status
curl http://localhost:8084/api/index/status

# Rebuild index (if needed)
curl -X POST http://localhost:8084/api/index/rebuild
```

### Step 4: Query (RAG)

```bash
curl -X POST http://localhost:8084/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What does the FDA guidance say about drug safety?", "topK": 3}'
```

## Local folder layout

- PDF binaries: `data/pdf-demo/downloaded/` (gitignored)
- Manifest: `data/pdf-demo/manifest.json` (gitignored)
- Config: Set in `application-local.yml`:
  ```yaml
  docurag:
    ingestion:
      pdf-demo-path: /absolute/path/to/data/pdf-demo/downloaded
  ```

**Git:** Large binaries are often **not** committed. This repository may keep **only** this README and, if needed, **tiny** PDF fixtures under `src/test/resources` for automated tests.

## Suggested manifest (`manifest.example.json`)

```json
{
  "version": "1",
  "documents": [
    {
      "title": "Guidelines on the readability of the package leaflet and labelling for human use (example)",
      "url": "https://health.ec.europa.eu/system/files/2019-02/guidelines_patient_summary_en_0.pdf",
      "category": "regulatory_guidance",
      "notes": "EU Commission; confirm license/terms before redistribution of the file."
    }
  ]
}
```

Copy to `manifest.json` locally (optionally gitignored) and extend with your chosen PDFs.

## Implementation note

DocuRAG extracts **text layers** with **Apache PDFBox** (v1). **Image-only** scans without a text layer are **out of scope** for baseline OCR; they may be skipped with a log line (see PRD FR-1).
