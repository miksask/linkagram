# Linkagram documentation

Index of product docs, specs, and architecture decisions.

## Product and architecture

| Document | Status |
|----------|--------|
| [product.md](product.md) | Current product description |
| [architecture.md](architecture.md) | Package layout, data flow, seams |

## Specs

| Document | Status |
|----------|--------|
| [specs/000-template.md](specs/000-template.md) | Template for new specs |
| [specs/001-url-input-normalization.md](specs/001-url-input-normalization.md) | Accepted — URL intake and normalization |
| [specs/002-redirect-resolution.md](specs/002-redirect-resolution.md) | Accepted — manual redirect following |
| [specs/003-map-url-parsing.md](specs/003-map-url-parsing.md) | Accepted — map URL parsing |
| [specs/004-location-details.md](specs/004-location-details.md) | Accepted — result presentation |
| [specs/005-clipboard.md](specs/005-clipboard.md) | Accepted — clipboard read and copy |
| [specs/006-local-analysis-history.md](specs/006-local-analysis-history.md) | Accepted — opt-in local analysis history |

## Decisions

| Document | Status |
|----------|--------|
| [decisions/ADR-template.md](decisions/ADR-template.md) | Template for new ADRs |
| [decisions/ADR-001-android-project-structure.md](decisions/ADR-001-android-project-structure.md) | Accepted — single-module package layout |
| [decisions/ADR-002-url-resolution.md](decisions/ADR-002-url-resolution.md) | Accepted — manual redirect following |
| [decisions/ADR-003-http-client.md](decisions/ADR-003-http-client.md) | Accepted — OkHttp + MockWebServer |
| [decisions/ADR-004-privacy-and-networking.md](decisions/ADR-004-privacy-and-networking.md) | Accepted — cleartext allowed, TLS untouched; persistence carve-out in ADR-006 |
| [decisions/ADR-005-screenshot-testing.md](decisions/ADR-005-screenshot-testing.md) | Accepted — Compose Preview Screenshot Testing |
| [decisions/ADR-006-local-analysis-history-storage.md](decisions/ADR-006-local-analysis-history-storage.md) | Accepted — Room + DataStore for local history |

## Prompts

Phase prompts used to drive AI-assisted implementation. They are part of the
demo narrative, not runtime documentation.

| Document | Status |
|----------|--------|
| [prompts/01-start.md](prompts/01-start.md) | Bootstrap and product framing |
| [prompts/02-next-steps.md](prompts/02-next-steps.md) | Specs 001–003 implementation |
| [prompts/03-screenshot-tests-and-showcase.md](prompts/03-screenshot-tests-and-showcase.md) | Screenshot tests, CI artifacts, README showcase |
| [prompts/04-local-analysis-history.md](prompts/04-local-analysis-history.md) | Spec 006 local history implementation |

## Agent guidance

Canonical instructions live in [`../AGENTS.md`](../AGENTS.md).

Cursor rules in [`../.cursor/rules/`](../.cursor/rules/):

- `project-context.mdc` — scope and source of truth (always applied)
- `git-commits.mdc` — commit message style (always applied)
- `android-kotlin.mdc` — Kotlin and Compose conventions
- `network-security.mdc` — untrusted URL and networking rules
- `testing.mdc` — unit testing conventions

Portable skills:

- [`.agents/skills/implement-feature/`](../.agents/skills/implement-feature/)
- [`.agents/skills/android-code-review/`](../.agents/skills/android-code-review/)
- [`.agents/skills/map-url-parser/`](../.agents/skills/map-url-parser/)

## Contributing

Setup, checks, and workflow: [`../CONTRIBUTING.md`](../CONTRIBUTING.md).
