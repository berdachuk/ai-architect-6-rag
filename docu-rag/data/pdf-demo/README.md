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

## Local layout

1. Download PDFs you are allowed to use for your demo.
2. Recommended local folder: `docu-rag/data/pdf-demo/downloaded/` (PDF binaries are gitignored).
3. Point the app at the folder:
   - Env: `DOCURAG_PDF_DEMO_PATH=<absolute-path-to-downloaded>`
   - Or local profile config: `docurag.ingestion.pdf-demo-path` in `application-local.yml` (created from `application-local.example.yml`)
4. Run ingestion per application README (FR-1), or call the API explicitly:

```bash
curl -s -X POST http://localhost:8080/api/documents/ingest \
  -H "Content-Type: application/json" \
  -d '{"paths":["/absolute/path/to/docu-rag/data/pdf-demo/downloaded"]}'
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
