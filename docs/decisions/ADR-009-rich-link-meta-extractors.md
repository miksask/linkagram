# ADR-009: Allowlisted rich-link meta extractors

## Status

Accepted

Partially supersedes the ADR-004 statement that network traffic is only redirect
resolution of the user-supplied URL (and ADR-008 Nominatim). For hosts on the
rich-link allowlist, the final successful response body may be read during that
same resolve request, up to a fixed size cap, solely to extract HTML meta tags.
Cleartext, TLS, logging, clipboard, and permission rules in ADR-004 remain in
force. Spec 003’s ban on HTML scraping for **map** parsers is unchanged.

## Context

Some useful share links (for example KOLEO train connections) encode only an
opaque id in the path. The human-readable summary lives in `<title>` /
`og:title` on the final HTML page. Map URL parsing cannot surface that without
reading page meta. Full HTML/DOM scraping, Nuxt payload parsing, WebView, and
JavaScript execution would expand attack surface and couple the app to fragile
page internals.

The product still needs a clear boundary: maps stay URL-structure-only; a
separate extractor category may use a narrow, allowlisted meta exception.

## Decision

1. Introduce a parallel **rich-link** result category (`RichLinkInfo`), not
   `LocationInfo` / `MapProvider`.
2. On the final 2xx hop of redirect resolution, if the host matches a
   `MetaCapturePolicy` allowlist, read at most 256 KiB of the response body and
   parse only `<title>`, `og:title`, and `og:url` into `PageMeta`.
3. After map parsing, if the map result is `Unsupported` and `PageMeta` is
   present, run a `RichLinkExtractor` registry. Map parse always wins when it
   returns `Parsed`.
4. First allowlisted host / extractor: KOLEO (`koleo.pl` and subdomains),
   title/og only — no Nuxt JSON, no unofficial API.
5. Rich links do not offer coordinate copy or Nominatim geocoding.
6. Opt-in history stores rich links as `HistoryResultType.RichLink` using
   existing Room columns (kind in `provider`, title in `place_name`); no schema
   migration for V1.

## Consequences

Advantages:

- opaque share links can show a readable summary without becoming a travel app;
- map parsers stay free of HTML;
- allowlist + tag whitelist + body cap limit how much HTML is trusted;
- framework is ready for additional hosts without new Room migrations.

Trade-offs:

- title/og formats can change and break extractors;
- allowlisted final hops download more data than header-only resolve;
- history UI must distinguish map provider from rich-link kind.

## Alternatives considered

- Stuff KOLEO into `LocationInfo`: rejected — wrong model, would enable geocode
  on trip titles.
- Full Nuxt payload scrape: rejected — fragile and out of product scope.
- Unofficial KOLEO API: rejected — extra client, unclear ToS, worse privacy
  story than reading the same page the user shared.
- Second HTTP GET after map miss: rejected — prefer one body read on the final
  resolve hop for allowlisted hosts only.
