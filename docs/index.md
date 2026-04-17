# DocuRAG Documentation

Welcome to the DocuRAG documentation hub.

## About DocuRAG

**DocuRAG** is a medical document RAG system built as a Spring Boot modular monolith. It ingests structured Hugging Face corpus exports and supplementary medical PDFs, chunks and embeds them, stores vectors in PostgreSQL with pgvector, and serves grounded question answering plus evaluation workflows.

The project emphasizes:

- **Spring Modulith boundaries** for a clean modular monolith
- **PostgreSQL + pgvector** for retrieval infrastructure
- **PDF ingestion** alongside structured corpus ingestion
- **Spring AI with OpenAI-compatible providers** for chat and embeddings
- **Evaluation support** for retrieval and answer quality

## Quick Links

- [Product Requirements](DocuRAG-PRD.md)
- [Architecture Overview](DocuRAG-ARCHITECTURE.md)
- [Developer Guide](DocuRAG-DEVELOPER-GUIDE.md)
- [Implementation Plan](DocuRAG-IMPLEMENTATION-PLAN-WBS.md)
- [Datasets](DocuRAG-Datasets.md)
- [Use Cases](DocuRAG-USE-CASES.md)
- [User Stories](DocuRAG-USER-STORIES.md)
- [Forms and Flows](DocuRAG-FORMS-AND-FLOWS.md)
- [Text Wireframes](DocuRAG-WIREFRAMES.md)

## Documentation Structure

- **Product**: scope, requirements, use cases, user stories, and text wireframes
- **Architecture**: system design, flows, and UI structure
- **Delivery**: implementation planning, data sources, and local development workflow
- **Submission**: course submission support materials

## Core Stack

- **Java 21**
- **Spring Boot**
- **Spring Modulith**
- **Spring AI**
- **PostgreSQL + pgvector**
- **Flyway**
- **Thymeleaf**
- **Docker Compose**

## Local Docs Build

Install dependencies:

```bash
pip install -r requirements-docs.txt
```

Run locally from the repository root:

```bash
mkdocs serve
```

Build the static site:

```bash
mkdocs build
```
