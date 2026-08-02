
# Linkagram Product Description

## Problem

Shortened URLs hide their actual destination. Map URLs often contain useful
location information, but coordinates are not always easy to find and copy.

## Product promise

Paste or share a link into Linkagram to discover where it leads and, when
possible, extract place details and coordinates from map links, or a readable
title summary from allowlisted rich-link hosts (Spec 009).

## Primary user flows

### 1. Share a link

A user shares a URL from a browser, messenger, or map application to Linkagram.

Expected result:
- Linkagram opens.
- URL is processed.
- Result is shown.

### 2. Paste from clipboard

A user opens Linkagram with a URL already copied.

Expected result:
- The app detects a valid URL in the clipboard.
- The user can process it with one action.
- Clipboard is never sent anywhere except the target URL host during resolution.

### 3. Inspect a short link

Expected result:
- Final resolved URL.
- Redirect chain.
- HTTP status codes, if available.
- Redirect count.
- Clear errors for timeout, loop, inaccessible host, malformed URL, etc.

### 4. Inspect a map link

Expected result:
- Map provider.
- Place title, if extractable.
- Address, if extractable.
- Latitude and longitude, if extractable.
- One-tap coordinate copy in `lat, lon` format.

### 5. Inspect an allowlisted rich link

Expected result (when the host is allowlisted and not a map URL):
- Rich-link kind (for example KOLEO).
- Title from `og:title` or `<title>`, cleaned for display.
- Canonical URL from `og:url` when present.
- No coordinate copy and no automatic geocoding.

### 6. Browse local analysis history

A user who opts in can reopen previous successful analyses on the same device.

Expected result:
- Setting **Save analysis history** is off by default.
- When on, each successful analysis is stored locally with no confirmation.
- History list supports search, date filters, reopen without network, delete,
  undo for single delete, and clear / delete-matching with confirmation.
- Failed or incomplete analyses are never stored.
- History is never synced to a backend or cloud backup.

## Supported providers: initial target

Map URL parsers:

- Google Maps
- Yandex Maps
- OpenStreetMap
- Apple Maps links, when parseable
- Organic Maps (`omaps.app` share and map links)
- Generic URLs containing explicit coordinates

Rich-link extractors (title/og meta, Spec 009 / ADR-009):

- KOLEO (`koleo.pl` share and connection links)

Support should be incremental. Unsupported map or rich-link URLs must still
display the final URL and redirect chain.

When a recognized map URL has place/address metadata but no coordinates, the
user may tap Find coordinates to look up an approximate pin via OpenStreetMap
Nominatim (Spec 008 / ADR-008). That lookup is never automatic. If history was
saved for that analysis, a successful lookup updates the same row’s coordinates;
it does not add a second entry.

## Privacy principles

- No accounts.
- No analytics by default.
- No backend.
- Optional local analysis history only (off by default); excluded from cloud
  backup and device transfer. See Spec 006 and ADR-006.
- No location permission required.
- The app only accesses URLs explicitly shared, pasted, or entered by the user.
- For allowlisted rich-link hosts, a capped slice of the final HTML response may
  be read during resolve to extract title/og tags (Spec 009 / ADR-009).
- Opt-in Nominatim geocoding may send place/address text to OpenStreetMap after
  an explicit tap (Spec 008 / ADR-008). Results are labelled approximate in the
  UI. Geocode never creates a new history row; if that analysis was already
  saved, the existing row’s latitude/longitude are updated in place. The
  approximate label itself is not persisted on history rows.