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

- If `/api/**` changes in code, ensure `docu-rag/api/openapi.yaml` stays aligned (E2E depends on it).
- If profiles/ports change:
  - `local` defaults to `8084`
  - `e2e` defaults to `18080` (stub AI)
  Update the Developer Guide + any diagrams/flows.
- When creating product docs, feature specs, UX flows, or screen-level requirements, include text-based wireframes by default unless the user explicitly asks for prose only. Use simple Markdown hierarchy with screen names, layout regions, controls, labels, tables/lists, and button text. Example:

```markdown
## 1. Home / Mode selection
**Screen: Home (desktop app)**

- Top bar
    - [Logo] Product Name
    - [Nav] Section A | Section B | Settings
    - [User avatar]

- Main content (two-column layout)
    - Left: Quick actions
        - Card 1:
            - Title: "Primary action"
            - Text: "Short description."
            - [Button] Start
    - Right: Recent items
        - Section: Recent sessions
            - Table: [Session name] | [Type] | [Last updated] | [Status]
```

- Prefer linking to files in-repo over duplicating long content.

## Skills

- `../.claude/skills/core-architecture/SKILL.md`
- `../.claude/skills/api-design/SKILL.md`
