# Linkagram documentation

Index of product docs, specs, and architecture decisions.

## Product

| Document | Status |
|----------|--------|
| [product.md](product.md) | Current product description |

## Specs

| Document | Status |
|----------|--------|
| [specs/000-template.md](specs/000-template.md) | Template for new specs |
| [specs/002-redirect-resolution.md](specs/002-redirect-resolution.md) | Accepted draft for redirect resolution |

### Planned specs

These are expected before or during feature work; they are not written yet:

- `001` — URL input and normalization (share / VIEW / clipboard / manual)
- Map provider parsing specs (Google, Yandex, OSM, Apple, generic coordinates)
- UI results screen (redirect chain, metadata, coordinate copy)

## Decisions

| Document | Status |
|----------|--------|
| [decisions/ADR-template.md](decisions/ADR-template.md) | Template for new ADRs |
| [decisions/ADR-001-android-project-structure.md](decisions/ADR-001-android-project-structure.md) | Accepted — single-module package layout |
| [decisions/ADR-002-url-resolution.md](decisions/ADR-002-url-resolution.md) | Accepted — manual redirect following |

## Agent guidance

Canonical instructions live in [`../AGENTS.md`](../AGENTS.md).

Portable skills:

- [`.agents/skills/implement-feature/`](../.agents/skills/implement-feature/)
- [`.agents/skills/android-code-review/`](../.agents/skills/android-code-review/)
- [`.agents/skills/map-url-parser/`](../.agents/skills/map-url-parser/)
