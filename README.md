# AI Architect #6: Practical Task - RAG Creation

This repository contains the implementation for the practical task **"RAG Creation"**. The main delivered solution is **DocuRAG**, a medical document Retrieval-Augmented Generation application built with **Java 21**, **Spring Boot 4**, **Spring Modulith**, **PostgreSQL + pgvector**, **Spring AI**, and a **Thymeleaf** demo UI.

## Task summary

The task requires building a RAG solution over datasets and demonstrating at least one evaluation metric with a small evaluation dataset. The implementation in this repository is aligned to that outcome and the project guidance captured in the product documents.

Required outcome:

- Build a working RAG application over datasets
- Define at least one evaluation metric
- Evaluate the system on a small evaluation dataset
- Demonstrate the evaluation results

Optional ninja challenges:

- Corpus update handling without full Vector DB rebuild
- Access-control-aware RAG
- Multi-modal RAG for graphical content
- Additional RAG evaluation dimensions such as precision, recall, faithfulness, and groundedness

Course note:

- Upload the task deliverables to the platform block **"Answer"** and click **"Submit"**

## What is implemented

The repository currently focuses on **DocuRAG** under [`docu-rag/`](docu-rag/), supported by:

- document ingestion from structured exports and PDF files
- chunking and vector indexing with pgvector
- semantic retrieval and grounded question answering
- analysis and visualization endpoints
- evaluation flow with seeded evaluation data
- automated tests, including Testcontainers integration tests and a separate black-box E2E module

Supporting modules:

- [`docu-rag/`](docu-rag/) - main Spring Boot application
- [`docu-rag-e2e/`](docu-rag-e2e/) - Cucumber + Playwright end-to-end tests
- [`pom.xml`](pom.xml) - Maven reactor for app + E2E (repository root)
- [`docs/`](docs/) - product, architecture, delivery, and submission documents

## Acceptance criteria mapping

- **RAG over datasets**: implemented in DocuRAG with structured corpus ingestion and PDF ingestion
- **At least one evaluation metric**: implemented with normalized accuracy and semantic similarity
- **Small evaluation dataset**: seeded evaluation dataset is included for demo and verification
- **Demonstration**: available through REST endpoints, Thymeleaf pages, evaluation flows, integration tests, and E2E coverage

## Documentation

Main documents:

- [`docs/DocuRAG-PRD.md`](docs/DocuRAG-PRD.md)
- [`docs/DocuRAG-ARCHITECTURE.md`](docs/DocuRAG-ARCHITECTURE.md)
- [`docs/DocuRAG-DEVELOPER-GUIDE.md`](docs/DocuRAG-DEVELOPER-GUIDE.md)
- [`docs/DocuRAG-IMPLEMENTATION-PLAN-WBS.md`](docs/DocuRAG-IMPLEMENTATION-PLAN-WBS.md)

## Developer setup

Prerequisites:

- JDK 21
- Maven 3.9+
- Docker
- Optional for local AI runs: Ollama or another OpenAI-compatible endpoint
- Optional for UI E2E: Playwright Chromium installed in `docu-rag-e2e`
- Optional for docs: Python 3 with `pip`

Start PostgreSQL:

```bash
cd docu-rag
docker compose -f compose.yaml up -d
```

## Build

Build the main application:

```bash
cd docu-rag
mvn verify
```

This runs unit and integration tests with **Testcontainers** and mocked AI beans. A live LLM is **not** required for this build.

Build the full reactor with end-to-end tests:

```bash
mvn verify
```

Run this from the **repository root** (where the reactor `pom.xml` lives).

From the repository root, you can also run the helper scripts:

```bash
./scripts/full-build-and-e2e.sh --teardown-volumes
```

Windows PowerShell:

```powershell
.\scripts\full-build-and-e2e.ps1 -TeardownVolumes
```

## Test

Fast verification of the main application:

```bash
cd docu-rag
mvn test
```

Full verification including integration tests:

```bash
cd docu-rag
mvn verify
```

Black-box E2E verification:

```bash
mvn verify
```

From the **repository root**.

E2E notes:

- Docker must be running
- Playwright Chromium must be installed for UI scenarios
- E2E reports are generated under `docu-rag-e2e/build/cucumber-reports/`

## Run locally

Start PostgreSQL:

```bash
cd docu-rag
docker compose -f compose.yaml up -d
```

Run the application:

```bash
cd docu-rag
mvn -q spring-boot:run -Dspring-boot.run.profiles=local
```

Then open `http://localhost:8084/`.

For local AI-backed execution, configure environment variables such as:

- `CHAT_MODEL`
- `EMBEDDING_MODEL`
- `OLLAMA_BASE_URL`
- `CHAT_BASE_URL`
- `EMBEDDING_BASE_URL`
- `DOCURAG_CORPUS_PATH`
- `DOCURAG_PDF_DEMO_PATH`

## Data and evaluation

- Primary dataset: [Sagarika-Singh-99/medical-rag-corpus](https://huggingface.co/datasets/Sagarika-Singh-99/medical-rag-corpus)
- PDF demo data guidance: [`data/pdf-demo/README.md`](data/pdf-demo/README.md)
- Corpus subset guidance: [`data/corpus/README.md`](data/corpus/README.md)
- Product spec: [`docs/DocuRAG-PRD.md`](docs/DocuRAG-PRD.md)

## Repository structure

```text
.
├── README.md
├── AGENTS.md
├── mkdocs.yml
├── requirements-docs.txt
├── docs/
├── pom.xml
├── docu-rag/
├── docu-rag-e2e/
└── scripts/
```

MkDocs site:

```bash
pip install -r requirements-docs.txt
mkdocs serve
```

Build the static site:

```bash
mkdocs build
```
