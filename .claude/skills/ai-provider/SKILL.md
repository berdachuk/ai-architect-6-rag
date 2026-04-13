# AI Provider (DocuRAG)

## Description

How DocuRAG wires chat + embedding providers through Spring AI using OpenAI-compatible HTTP endpoints, and how profiles keep tests deterministic.

## When to use

- Changing Spring AI configuration or provider URLs/models
- Modifying `DocuRagAiConfiguration` or `Custom*Properties`
- Debugging why AI beans are/aren’t present under a profile

## Instructions

- Prefer `spring.ai.custom.*` settings (see `docu-rag/src/main/resources/application.yml`).
- Default local dev assumes an OpenAI-compatible Ollama endpoint:
  - `OLLAMA_BASE_URL` (shared), or `CHAT_BASE_URL` / `EMBEDDING_BASE_URL`
  - `CHAT_MODEL`, `EMBEDDING_MODEL`
- Profiles:
  - `test` and `e2e` must use **stub** `ChatModel`/`EmbeddingModel` (`TestAIConfig`)
  - `local` is for live providers (Ollama/OpenAI-compatible)

## Boundaries

- Never make automated tests require a live AI provider.
- Don’t log secrets (API keys) in startup logs or health details.

