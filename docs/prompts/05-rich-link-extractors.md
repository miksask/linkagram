# Linkagram — Rich-link extractors

Continue development of the existing **Linkagram** project.

Before making changes, read Spec 009, ADR-009, Specs 002/003/004/006, and the
`rich-link-extractor` skill. Preserve established conventions.

Implement allowlisted rich-link extraction in this order:

## 1. Specs and ADR

Confirm these documents exist and match the implementation:

* `docs/decisions/ADR-009-rich-link-meta-extractors.md`
* `docs/specs/009-rich-link-extraction.md`
* updates to Specs 002, 003, 004, 006 and ADR-004 / `docs/product.md`

## 2. Domain and resolve

* Add `PageMeta`, `RichLinkKind`, `RichLinkInfo`, `RichLinkParseResult`.
* Extend `ResolveResult.Success` with optional `pageMeta`.
* On the final 2xx hop, capture a capped body for allowlisted hosts only via
  `MetaCapturePolicy` (do not hardcode KOLEO inside the resolver).

## 3. Extractors

* Add `data/extract/` with HTML meta parser, allowlist, registry, and
  `KoleoRichLinkExtractor`.
* Parse only `<title>`, `og:title`, `og:url`.
* Map parsing wins; rich links run only on `MapParseResult.Unsupported`.
* Unit-test with HTML fixtures; no live KOLEO network calls.

## 4. UI and history

* Show a rich-link section on the analysis screen (kind + title + optional
  canonical URL). No coordinate copy, no Nominatim.
* Persist `HistoryResultType.RichLink` using existing Room columns (kind in
  `provider`, title in `place_name`).
* Update history list/details display for rich links.
* Add screenshot preview(s) for rich-link success; update README gallery under
  `docs/images/` after intentional UI change.

## Implementation guidelines

* Prefer the `rich-link-extractor` skill for new hosts.
* Do not scrape Nuxt payloads or call unofficial timetable APIs.
* Do not use WebView or JavaScript.
* Keep Spec 003 map parsers free of HTML.
* Run `./gradlew test`, `lint`, `validateDebugScreenshotTest`, and
  `assembleDebug` before considering the work complete.
