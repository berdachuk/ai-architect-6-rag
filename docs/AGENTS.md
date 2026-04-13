# Docs — Agent Guide

Purpose: maintain the canonical **product + architecture documentation** for DocuRAG and keep it consistent with the code and runtime profiles.

## What This Folder Owns

- PRD, use cases, user stories, architecture notes
- Developer guide (local run, build, manual verification)
- MkDocs site config (`mkdocs.yml`) and theme extras

## Commands

```bash
# Validate doc build (strict)
mkdocs build -s
```

## Doc Consistency Rules

- If `/api/**` changes in code, ensure `docu-rag/docs/openapi.yaml` stays aligned (E2E depends on it).
- If profiles/ports change:
  - `local` defaults to `8080`
  - `e2e` defaults to `18080` (stub AI)
  Update the Developer Guide + any diagrams/flows.
- Prefer linking to files in-repo over duplicating long content.

## Skills

- `../.claude/skills/core-architecture/SKILL.md`
- `../.claude/skills/api-design/SKILL.md`

