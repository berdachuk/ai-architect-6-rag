# DocuRAG — Wireframes

Low-fidelity **layout wireframes** for the Thymeleaf demo UI. Aligned with [DocuRAG-PRD.md](DocuRAG-PRD.md) (UI Requirements), [DocuRAG-FORMS-AND-FLOWS.md](DocuRAG-FORMS-AND-FLOWS.md), and [DocuRAG-USE-CASES.md](DocuRAG-USE-CASES.md).

**Assumptions:** Desktop-first, simple server-rendered HTML, **lightweight JS** only for pie chart and entity graph. No SPA. **Medical disclaimer** on every interactive page (PRD NFR-5).

---

## Global chrome (all pages)

Every screen includes:

1. **Header** — product name, optional env badge (e.g. `local`).
2. **Primary nav** — links: Dashboard | Q&A | Analysis | Documents | Evaluation.
3. **Main content** — page-specific (wireframes below).
4. **Footer** — **disclaimer** + optional dataset link / model names when relevant.

### W-00 — Page shell (template)

```
+------------------------------------------------------------------+
|  DocuRAG                                    [local]              |
+------------------------------------------------------------------+
|  Home | Q&A | Analysis | Documents | Evaluation                  |
+------------------------------------------------------------------+
|                                                                  |
|                      << MAIN CONTENT AREA >>                     |
|                                                                  |
+------------------------------------------------------------------+
|  Disclaimer: Educational / demo only. Not medical advice.        |
|  Primary: HF medical-rag-corpus  |  PDF demo: optional  |  Chat / Emb: <models>   |
+------------------------------------------------------------------+
```

*(Footer line: show **primary Hugging Face corpus** name, optional note that **supplementary PDFs** may be ingested for format demo, and model names when known; omit unknowns.)*

---

## W-01 — Dashboard (`GET /`)

**Purpose:** Counts, ingestion/index status, latest evaluation summary, deep links.

```
+------------------------------------------------------------------+
|  DocuRAG ...                        (same header + nav as W-00)  |
+------------------------------------------------------------------+
|  Dashboard                                                       |
+------------------------------------------------------------------+
|  Summary cards (row)                                             |
|  +----------------+  +----------------+  +----------------+      |
|  | Documents      |  | Chunks         |  | Index status   |      |
|  |    3,042       |  |    18,200      |  |  Ready         |      |
|  +----------------+  +----------------+  +----------------+      |
|                                                                  |
|  Ingestion / indexing                                            |
|  +------------------------------------------------------------+  |
|  | Last job: completed  |  Docs loaded: ...  |  Errors: 0    |  |
|  | [ Optional: Trigger ingest ] [ Optional: Rebuild index ]    |  |
|  +------------------------------------------------------------+  |
|                                                                  |
|  Category distribution (mini table or inline bar)                |
|  +------------------------------------------------------------+  |
|  | cardiology  40%  |  neurology 25%  |  ...                  |  |
|  +------------------------------------------------------------+  |
|                                                                  |
|  Latest evaluation                                               |
|  +------------------------------------------------------------+  |
|  | Run: ...  |  Norm. acc. 0.61  |  Mean sim. 0.84  | [Eval ->]|  |
|  +------------------------------------------------------------+  |
|                                                                  |
|  Quick links                                                     |
|  [ Ask a question ]  [ View analysis ]  [ Browse documents ]      |
+------------------------------------------------------------------+
|  Disclaimer + corpus / models (footer)                           |
+------------------------------------------------------------------+
```

---

## W-02 — Q&A (`GET/POST /qa`)

**Purpose:** Ask English question; show grounded answer + retrieved chunks.

```
+------------------------------------------------------------------+
|  ... header + nav ...                                            |
+------------------------------------------------------------------+
|  Question answering                                              |
+------------------------------------------------------------------+
|  +------------------------------------------------------------+  |
|  | Your question (required)                                    |  |
|  | +--------------------------------------------------------+ |  |
|  | |                                                        | |  |
|  | |  <textarea: question>                                  | |  |
|  | |                                                        | |  |
|  | +--------------------------------------------------------+ |  |
|  | Top-K [ 5 ]   Min similarity [ 0.70 ]                      |  |
|  |                        [ Ask ]                             |  |
|  +------------------------------------------------------------+  |
|                                                                  |
|  (After POST — same page)                                        |
|  +------------------------------------------------------------+  |
|  | Answer (model: gemma4:31b-cloud)                           |  |
|  | +--------------------------------------------------------+ |  |
|  | | <assistant text>                                       | |  |
|  | +--------------------------------------------------------+ |  |
|  +------------------------------------------------------------+  |
|                                                                  |
|  Retrieved sources                                               |
|  +------------------------------------------------------------+  |
|  | # | Score | Category   | Title / snippet                   |  |
|  |---|-------|------------|-------------------------------------|  |
|  | 1 | 0.87  | cardiology | ... snippet ...                   |  |
|  | 2 | 0.82  | ...        | ...                               |  |
|  +------------------------------------------------------------+  |
+------------------------------------------------------------------+
|  Disclaimer ...                                                  |
+------------------------------------------------------------------+
```

**Empty state:** Only the form block; no answer table until first successful POST.

**Error state:** Banner above form: retrieval/LLM error message.

---

## W-03 — Analysis (`GET/POST /analysis`)

**Purpose:** Category **pie chart** + entity **graph** (or relationship list fallback).

```
+------------------------------------------------------------------+
|  ... header + nav ...                                            |
+------------------------------------------------------------------+
|  Analysis & visualization                                        |
+------------------------------------------------------------------+
|  Optional scope form                                             |
|  +------------------------------------------------------------+  |
|  | Scope [ All v ]  Category [________]  Max docs [ 500 ]     |  |
|  |                              [ Refresh analysis ]          |  |
|  +------------------------------------------------------------+  |
|                                                                  |
|  Category distribution                                           |
|  +------------------------------------------------------------+  |
|  |                                                            |  |
|  |              <canvas / SVG: PIE CHART>                     |  |
|  |                                                            |  |
|  |         (legend: category + count + %)                     |  |
|  +------------------------------------------------------------+  |
|                                                                  |
|  Entity / relation graph                                         |
|  +------------------------------------------------------------+  |
|  |                                                            |  |
|  |    <JS graph: nodes + edges>   OR   numbered edge list     |  |
|  |                                                            |  |
|  +------------------------------------------------------------+  |
+------------------------------------------------------------------+
|  Disclaimer ...                                                  |
+------------------------------------------------------------------+
```

**Fallback wireframe (no graph lib):** Replace graph panel with a **scrollable list**: `Node A --relates--> Node B`.

---

## W-04 — Documents (`GET /documents`)

**Purpose:** Paginated list of ingested source documents.

```
+------------------------------------------------------------------+
|  ... header + nav ...                                            |
+------------------------------------------------------------------+
|  Documents                                                       |
+------------------------------------------------------------------+
|  Filters                                                         |
|  +------------------------------------------------------------+  |
|  | Category [ All v ]   Title contains [__________]  [ Apply ]|  |
|  +------------------------------------------------------------+  |
|                                                                  |
|  +------------------------------------------------------------+  |
|  | Title              | Category    | Source  | Actions       |  |
|  |--------------------|-------------|---------|---------------|  |
|  | Hypertension ...   | cardiology  | MedQuAD | [ View ]      |  |
|  | ...                | ...         | ...     |               |  |
|  +------------------------------------------------------------+  |
|                                                                  |
|  [ << Prev ]   Page 1 of 12   [ Next >> ]    Show [20] per page  |
+------------------------------------------------------------------+
|  Disclaimer ...                                                  |
+------------------------------------------------------------------+
```

**Detail view** (optional separate route or modal — PRD lists `GET /api/documents/{id}`; UI may be `GET /documents/{id}`):

```
+------------------------------------------------------------------+
|  Document detail                                                 |
+------------------------------------------------------------------+
|  Title: ...                                                      |
|  Category: ...  |  External ID: ...  |  Internal ID: (24 hex)    |
|  Source: ...                                                     |
|  +------------------------------------------------------------+  |
|  | <scrollable content text>                                  |  |
|  +------------------------------------------------------------+  |
|  [ Back to list ]                                                |
+------------------------------------------------------------------+
```

---

## W-05 — Evaluation (`GET/POST /evaluation`)

**Purpose:** Start eval run; show aggregate metrics and optional per-case table.

```
+------------------------------------------------------------------+
|  ... header + nav ...                                            |
+------------------------------------------------------------------+
|  Evaluation                                                      |
+------------------------------------------------------------------+
|  Run evaluation                                                  |
|  +------------------------------------------------------------+  |
|  | Dataset name     [ medical-rag-eval-v1        ▼ ]            |  |
|  | Top-K            [ 5 ]                                      |  |
|  | Min score        [ 0.70 ]                                   |  |
|  | Semantic pass @  [ 0.80 ]                                   |  |
|  |                        [ Run evaluation ]                   |  |
|  +------------------------------------------------------------+  |
|                                                                  |
|  Latest / selected run summary                                   |
|  +------------------------------------------------------------+  |
|  | Run ID: 674a1b2c3d4e5f6789012346                           |  |
|  | Cases: 100  |  Norm. accuracy: 0.61  |  Mean sim.: 0.84      |  |
|  | Semantic accuracy @0.80: 0.76                                |  |
|  +------------------------------------------------------------+  |
|                                                                  |
|  Per-case results (optional expand / paginate)                   |
|  +------------------------------------------------------------+  |
|  | # | Question (truncated) | Exact | Norm | Sim | Pass       |  |
|  |---|----------------------|-------|------|-----|------------|  |
|  | 1 | What is ...          |  no   | yes  | 0.88| yes        |  |
|  +------------------------------------------------------------+  |
+------------------------------------------------------------------+
|  Disclaimer ...                                                  |
+------------------------------------------------------------------+
```

**Long-running run:** Show “Running…” state on submit and/or poll status (implementation choice; PRD allows simple re-render after completion).

---

## W-06 — Minimal header-only (error / login not in v1)

PRD: **no user accounts** in v1. If a global error page is added:

```
+------------------------------------------------------------------+
|  DocuRAG                                                         |
+------------------------------------------------------------------+
|  Something went wrong                                            |
|  +------------------------------------------------------------+  |
|  | <message>  |  [ Back to dashboard ]                        |  |
|  +------------------------------------------------------------+  |
+------------------------------------------------------------------+
```

---

## Responsive note (non-blocking)

PRD targets a **simple demo**. Minimum: main column stacks vertically on narrow viewports; charts may shrink height; tables scroll horizontally with `overflow-x: auto`.

---

## Traceability

| Wireframe | Route | Forms doc | PRD |
|-----------|-------|-----------|-----|
| W-01 | `/` | F-01 optional | Required pages |
| W-02 | `/qa` | F-02 | `/qa` |
| W-03 | `/analysis` | F-03 | `/analysis` |
| W-04 | `/documents` | F-04 | `/documents` |
| W-05 | `/evaluation` | F-05 | `/evaluation` |

---

**Document version:** 1.0. Update when templates or OpenAPI add fields.
