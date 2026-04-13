# AI Context Strategy (DocuRAG)

This repository uses a layered, tool-agnostic context architecture so multiple coding agents (Codex, Cursor, Claude Code, Copilot Agents) can operate with consistent boundaries and workflows.

## Layer Model

```text
AGENTS.md (root)
  → major-module AGENTS.md (2–5 files)
    → skills in .claude/skills/**/SKILL.md (single source of truth)
      → optional adapters (Cursor rules, MCP, IDE configs)
```

### 1) Root `AGENTS.md`

Compact index: what the repo is, the module map, global rules/boundaries, and pointers to:

- module-level `AGENTS.md`
- the skill index
- canonical product docs (`docs/DocuRAG-PRD.md`, etc.)

### 2) Module-level `AGENTS.md`

Created only for major boundaries with distinct workflows or constraints:

- `docu-rag/` (application runtime, Modulith, DB, profiles)
- `docu-rag-e2e/` (black-box E2E, OpenAPI client generation, Playwright)
- `docs/` (MkDocs, documentation consistency)

These files must stay short and avoid duplicating the root guide.

### 3) Skills (`.claude/skills/**/SKILL.md`)

Skills are the single source of truth for “how to work” in the repo. Each skill should:

- state when to load it,
- provide concrete instructions and examples,
- define boundaries (what it must not change/decide).

Add a new skill when:

- a workflow repeats often (e.g., OpenAPI updates, Flyway migrations),
- a boundary is frequently violated (e.g., Modulith imports),
- a module has non-obvious conventions (e.g., pgvector SQL casts).

Update existing skills when:

- the PRD or architecture changes,
- conventions change (e.g., JDBC named parameters rule),
- new tooling is adopted (e.g., new test profile or E2E harness).

## Keeping Context In Sync

- If the REST API changes: update `docu-rag/docs/openapi.yaml`, and ensure E2E regeneration still works (`docu-rag-e2e`).
- If DB schema changes: add a Flyway migration and update any docs that describe schema/flows.
- If module boundaries change: update `package-info.java` and the `core-architecture` skill.

## Optional Adapters (Future)

Adapters (Cursor rules, IDE templates, MCP servers) should **read** the canonical skills and guides, or be generated from them. They must not become a second source of truth.

