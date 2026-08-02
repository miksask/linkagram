# Spec 008: Address Geocoding

## Goal

When a recognized map URL has a place name or address but no coordinates in the
URL, let the user look up approximate coordinates with an explicit tap.

## Status

Accepted

## Input

A parsed `LocationInfo` with at least one of `placeName` or `address` and with
`latitude`/`longitude` both null.

## Functional requirements

- Show a "Find coordinates" action only when the location has place/address
  metadata and no coordinates.
- Geocode only after the user taps that action. Never during `analyze()`.
- Use a single provider: OpenStreetMap Nominatim search API.
- Label successful results as approximate.
- Allow copying geocoded coordinates as `lat, lon` (same format as Spec 005).
- When history was saved for this analysis (`HistorySaveResult.Saved`), update
  that row’s `latitude` / `longitude` after a successful geocode. Do not create
  a new history row. If history was disabled or save failed, geocode still
  updates the live analysis UI only.
- Never scrape HTML, never use WebView/JS, never call a Maps SDK or a paid
  Places API.
- Surface not-found and network/HTTP failures without clearing the location
  section; allow retry.

## Non-requirements

- Automatic geocoding on every analysis.
- Reverse geocoding.
- Multiple geocoding providers or user-configurable endpoints.
- Persisting an “approximate” flag on history rows.
- Offline geocoding.

## Result states

- `Available` — place/address present, no coordinates; button shown.
- `Loading` — request in flight.
- `Found` (UI: coordinates + approximate caption) — lookup succeeded.
- `NotFound` — provider returned no match.
- `Failed` — network, timeout, HTTP, or parse error.
- `Unavailable` — coordinates already present, or no place/address to query.

## Acceptance criteria

- Given a Google Maps place URL with address and no coordinates, the Find
  coordinates button is shown after analysis.
- Given a successful Nominatim response, coordinates appear with an approximate
  caption and the copy action is available.
- Given an empty Nominatim response, a not-found message is shown and the button
  remains for retry.
- Given a network or HTTP failure, a failure message is shown and the button
  remains for retry.
- Given exact coordinates already extracted from the URL, the Find coordinates
  button is not shown.
- Geocoding is never started by `analyze()` alone.
- Given history enabled and a successful geocode, the saved history row gains
  the geocoded coordinates and history details can copy them without Analyze
  again.

## Notes

Decision record: [ADR-008](../decisions/ADR-008-address-geocoding.md).
Related: Spec 003 (URL parsing), Spec 004 (location UI), Spec 005 (clipboard),
ADR-004 (privacy and networking).
