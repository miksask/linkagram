# Spec 004: Location Details and Result Presentation

## Goal

Present the outcome of an analysis: the final URL, the redirect chain, and any
location metadata that could be extracted.

## Status

Accepted

## Input

- `ResolveResult` from Spec 002
- `MapParseResult` from Spec 003
- `RichLinkParseResult` from Spec 009 (when map parsing is unsupported)

## Functional requirements

- Always show the final URL when one is known, including when map parsing fails
  and when the final response is an HTTP error.
- Show the redirect chain as an ordered list; each entry shows the source URL,
  the destination URL, and the status code when available.
- Show location details only when a map provider was recognised: provider name,
  place name, address, and coordinates, each rendered only when present.
- Show coordinates as `lat, lon` and offer a one-tap copy action.
- When map parsing is unsupported and a rich link was extracted (Spec 009), show
  a rich-link section with kind and title (and canonical URL when present). Do
  not show location details, coordinate copy, or Find coordinates for that
  result. Map location and rich link are mutually exclusive; map wins.
- Show exactly one of: idle, analyzing, validation error, or a result.
- Disable the input field and both action buttons while an analysis runs.
- Replace previous results as soon as the input changes, so stale output is
  never shown next to a new URL.
- Keep the whole result area scrollable; chains can be long.

## Non-requirements

- Interactive map rendering, Maps SDK, route building
- Persisting or exporting results
- Sharing the result back out to other apps

## Result states

| State | Shown |
|-------|-------|
| Idle | Tagline, empty input |
| Analyzing | Progress indicator, disabled controls |
| Validation error | Inline error under the input, no network call made |
| Success | Final URL, chain, location details or rich-link details when available |
| Resolve error | Error message plus any partial chain collected |

## Edge cases

- Redirect chain present but the final hop failed: show both the error and the
  partial chain.
- Provider recognised but no coordinates: show provider, place, address; hide
  the copy action.
- Coordinates present but no place or address: show coordinates and copy.
- Very long URLs must wrap rather than truncate silently.

## Acceptance criteria

- Given a non-map URL, the final URL and chain are still displayed.
- Given a KOLEO rich-link success, kind and title appear and copy/geocode do not.
- Given a map URL with coordinates, a copy action appears and copies `lat, lon`.
- Given `TooManyRedirects`, the collected chain is shown alongside the error.
- Given a new input while a result is on screen, the old result disappears.
- Given an in-flight analysis, the analyze and paste buttons are disabled.

## Test expectations

- ViewModel test: success populates final URL, chain, and coordinates.
- ViewModel test: changing the draft clears previous results.
- ViewModel test: each `ResolveResult` maps to the expected `ResolveError`.

## Notes

Presentation lives in `ui/analysis/AnalysisScreen.kt`; state shape is
`AnalysisUiState`. Related: Spec 003 (parsing), Spec 005 (clipboard copy).
