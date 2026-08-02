# Spec 009: Rich-link extraction

## Goal

When a resolved URL is not a recognised map link but belongs to an allowlisted
host, extract a human-readable summary from HTML meta tags (`<title>`,
`og:title`, `og:url`) and present it as a rich-link result.

## Status

Accepted

## Input

- Final URL from Spec 002 redirect resolution
- Optional `PageMeta` captured on the final 2xx hop for allowlisted hosts
- Host allowlist and extractor registry (ADR-009)

## Functional requirements

- Keep rich-link extraction separate from map URL parsing (Spec 003).
- Run rich-link extraction only when map parsing returns `Unsupported`.
- Capture page meta only for allowlisted hosts during the final successful
  resolve hop (Spec 002 / ADR-009):
  - read at most 256 KiB of response body;
  - parse only `<title>`, `og:title`, and `og:url`;
  - do not execute JavaScript, use WebView, or parse SPA/JSON payloads.
- Extractor registry is host/path based; V1 allowlist: `koleo.pl` and
  subdomains.
- Return `RichLinkInfo` with `kind`, `title`, and optional `canonicalUrl`.
- Prefer `og:title` over `<title>` when both are present and non-blank.
- Do not offer coordinate copy or Nominatim geocoding for rich links.
- When extraction fails, still show the final URL and redirect chain.

### KOLEO (V1)

- Hosts: `koleo.pl`, `*.koleo.pl`
- Paths of interest: `/p/{id}`, `/connection/{id}`, optional language prefix
  such as `/en/connection/{id}`
- Title source: `og:title` or `<title>`
- Strip a trailing `>> KOLEO` (and HTML entities such as `&gt;&gt;`) from the
  displayed title when present
- `canonicalUrl` from `og:url` when present
- Do not extract trains, platforms, prices, or station coordinates

## Non-requirements

- Nuxt / `__NUXT__` / other embedded JSON scraping
- Unofficial KOLEO or third-party timetable APIs
- Maps SDK, WebView, JavaScript
- Additional allowlisted hosts beyond KOLEO in V1 (framework must allow them)

## Result states

- `Parsed(RichLinkInfo)` — kind known; title non-blank
- `Unsupported` — host not handled, meta missing, or title empty after cleanup

## Acceptance criteria

- Given a KOLEO `/connection/{uuid}` HTML fixture with a trip `og:title`, the
  extractor returns kind `Koleo` and a cleaned title.
- Given a KOLEO `/p/{id}` page whose `og:url` points at `/connection/...`,
  `canonicalUrl` is populated from `og:url`.
- Given the same URL with map coordinates somehow also present, map parsing
  wins and no rich-link section is shown.
- Given a non-allowlisted host, no response body is retained for meta and no
  rich-link result is produced.
- Given allowlisted HTML without a usable title, analysis still shows the final
  URL and chain.
- Given history enabled, a successful KOLEO analysis is stored as
  `HistoryResultType.RichLink` with searchable title.

## Notes

Related: ADR-009, Spec 002 (`PageMeta`), Spec 003 (maps unchanged), Spec 004
(presentation), Spec 006 (history). Skill:
`.agents/skills/rich-link-extractor/`.
