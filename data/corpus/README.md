# Primary corpus subset (Hugging Face)

This folder documents how to build a **repeatable subset** of the primary dataset [Sagarika-Singh-99/medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus) for local ingest, per [DocuRAG-PRD.md](../../../docs/DocuRAG-PRD.md) (Dataset Strategy) and [DocuRAG-Datasets.md](../../../docs/DocuRAG-Datasets.md).

## Files

| File                           | Purpose                                                                                                                                                                  |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
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

---

## Converting `final_corpus.pkl` to JSONL

The repo ships a pre-downloaded `final_corpus.pkl` (288 MB, 216,102 rows) under `../_hf/`. It is a **pandas DataFrame** with columns:

| Column      | Type   | Description                            |
|-------------|--------|----------------------------------------|
| `doc_id`    | string | Row identifier                         |
| `text`      | string | Full content (Q&A, summary, etc.)      |
| `title`     | string | Entity / document title                |
| `source`    | string | Origin dataset (`medquad`, `medqa`, …) |
| `category`  | string | Row type (see below)                   |
| `meta_json` | string | JSON string with additional fields     |

**Available categories and row counts:**

```
mcqa:              193,155  (multi-choice Q&A — largest, exclude for small subset)
faq:                16,359  (FQA / question-answer pairs — good for RAG)
summary:            5,049  (long-form summaries)
Symptoms_definition: 1,194  (definition-style)
Symptoms:              304  (symptom entries)
Precaution:             41  (precaution entries)
```

**Recommended subset composition (≈ 2,000–5,000 rows):** `faq` + `summary` + `Symptoms_definition` + `Symptoms` + `Precaution`, omitting `mcqa` which is very large.

### Quick one-liner (full export)

```bash
python3 -c "
import pandas as pd, json

df = pd.read_pickle('../_hf/final_corpus.pkl')
with open('my-corpus.jsonl', 'w', encoding='utf-8') as f:
    for _, row in df.iterrows():
        f.write(json.dumps({
            'id': str(row['doc_id']),
            'title': row['title'],
            'content': row['text'],
            'category': row['category'],
            'source': row['source'],
            'metadata': row['meta_json']
        }, ensure_ascii=False) + '\n')
print(f'Exported {len(df)} rows')
"
```

### Filtered export (recommended subset)

```bash
python3 -c "
import pandas as pd, json

df = pd.read_pickle('../_hf/final_corpus.pkl')

# Keep everything except 'mcqa', cap at 5000 rows total
df_sub = df[df['category'] != 'mcqa'].head(5000)

# Or pick specific categories explicitly
# df_sub = df[df['category'].isin(['faq', 'summary', 'Symptoms_definition'])].head(2000)

with open('_sample/medical-rag-corpus.sample.jsonl', 'w', encoding='utf-8') as f:
    for _, row in df_sub.iterrows():
        f.write(json.dumps({
            'id': str(row['doc_id']),
            'title': row['title'],
            'content': row['text'],
            'category': row['category'],
            'source': row['source'],
            'metadata': row['meta_json']
        }, ensure_ascii=False) + '\n')
print(f'Exported {len(df_sub)} rows to _sample/medical-rag-corpus.sample.jsonl')
"
```

After export, ingest via:
```bash
curl -s -X POST http://localhost:8084/api/documents/ingest \
  -H "Content-Type: application/json" \
  -d '{"paths":["/absolute/path/to/data/corpus/_sample/medical-rag-corpus.sample.jsonl"]}'
```
