# ADR-008: Opt-in address geocoding via Nominatim

## Status

Accepted

Partially supersedes the ADR-004 statement that "the only network traffic is
the resolution of the URL the user supplied". Cleartext, TLS, logging,
clipboard, and permission rules in ADR-004 remain in force.

## Context

Many Google Maps short links (`maps.app.goo.gl`) resolve to place URLs that
encode a place name and postal address but no `@lat,lon` or `!3d/!4d`
coordinates. Spec 003 forbids HTML scraping and WebView/JS; live checks showed
that Google Maps place pages return empty server-rendered metadata, so scraping
would not work without executing JavaScript.

The postal address in the URL path can be geocoded by a third-party service.
That introduces network traffic beyond resolving the user-supplied URL and must
be an explicit, privacy-conscious choice.

## Decision

Add an opt-in "Find coordinates" action that queries OpenStreetMap Nominatim
(`search?format=jsonv2&limit=1`) with a progressive query trim (full
`place, address`, then drop leading comma-separated components). Results are
labelled approximate. The lookup runs only after a user tap, uses its own
OkHttp client with a descriptive User-Agent, and never writes geocoded
coordinates into local history.

## Consequences

Advantages:

- place-only Google Maps short links can still yield copyable coordinates;
- no Google API key, no Maps SDK, no HTML/JS scraping;
- privacy cost is visible and user-initiated.

Trade-offs:

- each lookup sends place/address text to Nominatim;
- results are approximate and may miss or mismatch the intended pin;
- Nominatim rate limits and availability become a soft dependency for this
  optional path.

## Alternatives considered

- HTML scraping of the Google Maps page: rejected — empty server-rendered
  content; would require JavaScript execution, forbidden by Spec 003.
- Google Places / Geocoding API: rejected — needs an API key and a backend or
  embedded secret, outside project non-goals.
- Automatic geocoding during analyze: rejected — silent third-party disclosure
  of every place URL. Explicit tap keeps the cost visible.
- Offline geocoder: rejected — too large a dependency for an optional path.
