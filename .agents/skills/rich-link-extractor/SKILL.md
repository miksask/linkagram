---
name: rich-link-extractor
description: Adds or changes allowlisted rich-link extractors that read title/og HTML meta (for example KOLEO). Use when implementing non-map share-link summaries, PageMeta capture, or documenting rich-link hosts.
---

# Rich-link extractor implementation

When adding or changing a rich-link extractor:

1. Read Spec 009 and ADR-009 first. Do not weaken Spec 003 for map parsers.
2. Add the host to the allowlist / `MetaCapturePolicy` so resolve can capture
   capped `PageMeta` on the final 2xx hop.
3. Keep extractor code under `data/extract/`, isolated per kind/host.
4. Parse only `<title>`, `og:title`, and `og:url`. Never scrape Nuxt/JSON
   payloads, never use WebView/JS, never call unofficial host APIs unless a
   new ADR explicitly allows it.
5. Return `RichLinkInfo` with a stable `RichLinkKind`, non-blank title, and
   optional canonical URL. Prefer `og:title` over `<title>`.
6. Register the extractor in `RichLinkExtractorRegistry`. Map parsing must
   still win when it returns `Parsed`.
7. Document URL patterns and title cleanup rules in Spec 009.
8. Add table-driven unit tests with HTML fixtures (no live network).
9. Update analysis UI, history mapping (`HistoryResultType.RichLink`),
   screenshot previews, and README showcase when the result is user-visible.
10. Do not offer coordinate copy or Nominatim for rich-link results.
