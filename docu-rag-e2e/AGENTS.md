# DocuRAG E2E — Agent Guide

Purpose: **black-box verification** of DocuRAG via **Cucumber** (API + CLI) and **Playwright** (UI). REST calls must use the **OpenAPI-generated client**.

## What This Module Owns

- E2E infrastructure lifecycle (Compose + fat JAR + health gating)
- Feature files (`src/test/resources/features/*.feature`)
- Step definitions + Playwright UI checks
- OpenAPI client generation (test sources)

## Dependencies / Contracts

- REST contract source of truth: `../docu-rag/api/openapi.yaml`
  - If controllers/DTOs change, update the YAML and regenerate by running `mvn generate-test-sources` (or `mvn verify`).
- App under test: `../docu-rag/target/docu-rag-0.1.0-SNAPSHOT.jar`
- Runtime profile: app is started with `--spring.profiles.active=e2e` (stub AI via `TestAIConfig`).

## Commands

```bash
# Recommended (reactor: builds jar then runs E2E)
cd .. && mvn verify

# Module only (requires jar already built)
mvn verify

# Install Playwright Chromium once per machine
mvn exec:java -e -Dexec.classpathScope=test -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

## Constraints

- Do not hand-roll HTTP for endpoints covered by OpenAPI; use the generated client.
- Do not edit generated sources under `target/generated-sources/openapi` by hand.
- Keep ports aligned with `application-e2e.yml` (default app port is **18080**, Postgres host port **5433**).

## Skills

- `../.claude/skills/api-design/SKILL.md` (OpenAPI, REST surface alignment)
- `../.claude/skills/testing/SKILL.md` (E2E patterns, fixtures, reports)

