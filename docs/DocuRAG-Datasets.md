# Datasets for DocuRAG

**Normative requirements:** [DocuRAG-PRD.md](DocuRAG-PRD.md) (Dataset Strategy, FR-1).  
**PDF demo pack (paths, manifest template):** [docu-rag/data/pdf-demo/README.md](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/data/pdf-demo/README.md).

This document expands **what to download**, **why**, and **suggested local layout**. It does not replace the PRD for acceptance criteria.

---

## Overview

DocuRAG uses a **two-source** strategy:

| Role | Source | Purpose |
|------|--------|---------|
| **Primary** | [Sagarika-Singh-99/medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus) on Hugging Face | Scalable indexing, retrieval, extraction, category charts, and evaluation (structured export / JSONL or equivalent). |
| **Supplementary** | Small set of **open English** medical or public-health **PDFs** (WHO, EU health, NHS guidance, etc.) | Demonstrate **raw PDF text extraction** (e.g. Apache PDFBox) on the same ingestion pipeline as the primary corpus. |

The Hugging Face corpus is distributed as **dataset files** (not necessarily as a folder of PDFs), so it is ideal for RAG scale and metadata but does not by itself prove **PDF parsing**. The supplementary PDF set covers that demo without replacing the primary corpus.

**Note:** Prefer **official** open PDFs over opaque generic PDF dumps (e.g. unvetted Kaggle collections) for **provenance and licensing** clarity in demos and coursework. Generic PDF datasets may still be used only to stress-test the parser if needed.

---

## Primary corpus

### `Sagarika-Singh-99/medical-rag-corpus`

- **Dataset page:** [https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus)

This is the **main** corpus for DocuRAG. It is an English medical RAG-oriented resource with document text, title, source, and category-style fields (exact schema per the dataset card), suitable for semantic retrieval, grounded QA, category aggregation, and charts.

### Why it is the main corpus

- Structured for retrieval-oriented medical QA.
- Category metadata supports pie-chart style visualization.
- Source and title fields support UI citations and filtering.
- Practical base for an English eval pipeline (with a separate small labeled eval set per PRD).

### How it should be used

Do **not** index the full corpus in the first iteration. Ingest a **curated subset** of roughly **2,000–5,000** documents from **3–5** categories so that indexing and evaluation stay practical locally. Persist a **subset manifest** in the repository (see PRD Dataset Strategy). **Repository example:** [docu-rag/data/corpus/subset-manifest.example.json](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/data/corpus/subset-manifest.example.json) and [docu-rag/data/corpus/README.md](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/data/corpus/README.md).

### Recommended subset fields (typical)

| Field (name may vary on HF) | Usage in DocuRAG |
|----------------------------|------------------|
| Document / row id | Stable **`external_id`**. |
| `text` (or equivalent) | Chunking and embeddings. |
| `title` | UI and citations. |
| `source` | Evidence metadata. |
| `category` | Visualization and filtering. |

The actual export format (JSONL, Parquet, etc.) follows the Hugging Face dataset export you choose; the implementation should document the expected layout in README.

---

## Supplementary raw PDF demo set

The primary corpus is **not** a pack of arbitrary PDF files. To satisfy **PDF ingestion** (PRD FR-1, [docu-rag/data/pdf-demo/README.md](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/data/pdf-demo/README.md)), maintain a **small** set of **open English** PDFs from trustworthy publishers.

---

## Recommended PDF documents (examples)

**Verify each URL and license/terms before redistributing files or using them in published coursework.** Links can change; treat this list as a starting point.

### WHO (IRIS)

- WHO guideline on self-care interventions for health and well-being: [9789240031326-eng.pdf](https://apps.who.int/iris/bitstream/handle/10665/342654/9789240031326-eng.pdf) (as of **2026-04-12**, this link returned HTML in our environment; confirm it still serves a PDF before using)
- Guidelines on core components of infection prevention and control programmes: [9789241549929-eng.pdf](https://iris.who.int/bitstream/handle/10665/251730/9789241549929-eng.pdf) (as of **2026-04-12**, this link returned HTML; confirm)
- Guidelines for primary health care in low-resource settings: [download](https://iris.who.int/bitstreams/8fb36d65-e274-4dfb-9c68-166c18298161/download) (confirm file type after download)
- WHO guidelines on ethical issues in public health surveillance: [9789241512657-eng.pdf](https://iris.who.int/bitstream/handle/10665/255721/9789241512657-eng.pdf) (as of **2026-04-12**, this link returned HTML; confirm)
- Consolidated guidelines on HIV, viral hepatitis and STI prevention, diagnosis, treatment and care: [9789240053465-eng.pdf](https://apps.who.int/iris/bitstream/handle/10665/360446/9789240053465-eng.pdf) (confirm)

### EU health documentation

- Guidelines on minimum / non-exhaustive patient summary dataset for electronic exchange: [guidelines_patient_summary_en_0.pdf](https://health.ec.europa.eu/system/files/2019-02/guidelines_patient_summary_en_0.pdf)
- Guideline on the packaging information of medicinal products for human use: [2018_packaging_guidelines_en_1.pdf](https://health.ec.europa.eu/system/files/2023-09/2018_packaging_guidelines_en_1.pdf)
- MDCG 2021-5 Rev. 1 — guidance on standardisation (medical devices): [EU health document download](https://health.ec.europa.eu/document/download/59ac4cb0-f187-4ca2-814d-82c42cde5408_en) (confirm PDF vs other format at download time)

### NHS (NHSBSA / related)

- Requirements and guidance for endorsement in the Electronic Prescription Service: [NHSBSA PDF](https://www.nhsbsa.nhs.uk/sites/default/files/2019-11/NHSBSAGuidanceforEndorsement_v7.2_Sept_2019_Final%20Approved%20%28PDF%20722kb%29.pdf)
- dm+d Implementation Guide (Primary Care): [PDF](https://www.nhsbsa.nhs.uk/sites/default/files/2020-11/dm%2Bd%20Implementation%20Guide%20%28Primary%20Care%29%20v2.0.pdf)
- dm+d Implementation Guide (Secondary Care): [PDF](https://www.nhsbsa.nhs.uk/sites/default/files/2017-02/Secondary_Care_Electronic_Prescribing_Implementation_Guidance_5_0.pdf)

---

## Recommended minimal PDF set (first implementation)

Five PDFs are enough to demonstrate ingestion end-to-end:

| Document | Rationale |
|----------|-----------|
| EU patient summary dataset guideline | Healthcare documentation, data-centric terms. |
| EU packaging guideline | Regulatory language, structured sections. |
| NHSBSA endorsement guidance | Prescribing workflow language. |
| NHS dm+d implementation guide (Primary Care) | Implementation / prescribing terminology. |
| NHS secondary care e-prescribing implementation guidance | Operational terminology and long-form prose. |

**Local demo pack note:** in this repository we keep PDF binaries **local-only** (gitignored). Place downloaded files under `docu-rag/data/pdf-demo/downloaded/` and set `DOCURAG_PDF_DEMO_PATH` accordingly.

---

## Recommended local folder layout

Align with the **DocuRAG application module** under `docu-rag/` (see [docu-rag/README.md](https://github.com/berdachuk/ai-architect-6-rag/blob/main/docu-rag/README.md)):

```text
docu-rag/
  data/
    pdf-demo/                    # PDFs + manifest (see README there)
      README.md
      manifest.json              # optional; gitignore if preferred
      *.pdf                      # downloaded locally; often not committed
    corpus/                      # suggested: HF export / subset
      subset-manifest.json
      medical-rag-subset.jsonl   # example processed export
  src/test/resources/fixtures/
    minimal.pdf                  # tiny PDF for unit tests only
```

If the team prefers a repository-root `data/` tree instead, document one convention in README and keep paths consistent with local configuration
(`application-local.yml` if you use one locally, or environment variables for `docurag.ingestion.*`).

**Processed** and **eval** artifacts may live under `docu-rag/src/main/resources/eval/` or `docu-rag/data/processed/` per implementation; PRD requires a versioned eval dataset and corpus subset manifest somewhere discoverable.

---

## Recommended project usage

### Primary corpus

Use the Hugging Face medical corpus for:

- chunking and embeddings,
- semantic retrieval and grounded answers,
- category-based charting,
- answer-quality evaluation (with a small labeled eval set).

### PDF demo

Use the PDF mini-set **only** to show that the ingestion layer parses **text-layer PDFs** and feeds the same pipeline. It does not need to be large. **Image-only scans** without a text layer are out of scope for baseline OCR (PRD FR-1).

---

## README wording (copy-paste friendly)

> DocuRAG uses a **two-tier** dataset strategy. The **primary** retrieval and evaluation corpus is the English **medical-rag-corpus** dataset on Hugging Face, chosen for scalable indexing and medical QA.  
> A **supplementary** set of **open English PDFs** from official health sources (e.g. WHO, EU health, NHS) demonstrates **raw PDF ingestion** alongside the primary corpus.

---

## Defense / review wording (copy-paste friendly)

> The **primary corpus** supports reliable English medical RAG development because it provides document text, titles, sources, and categories in a retrieval-friendly structured form.  
> A **supplementary PDF demo set** was added to show **parsing and analysis of real PDF files**, aligned with document-analysis expectations, without replacing the primary corpus for scale and evaluation.

---

## Final recommendation

Use **`Sagarika-Singh-99/medical-rag-corpus`** as the **primary** corpus and a **curated 5–10** open English **medical / public-health PDF** as the **supplementary** ingestion source. This balances retrieval quality, evaluation, and a credible **PDF** demo.
