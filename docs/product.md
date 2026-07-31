
# Linkagram Product Description

## Problem

Shortened URLs hide their actual destination. Map URLs often contain useful
location information, but coordinates are not always easy to find and copy.

## Product promise

Paste or share a link into Linkagram to discover where it leads and, when
possible, extract place details and coordinates.

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

## Supported providers: initial target

- Google Maps
- Yandex Maps
- OpenStreetMap
- Apple Maps links, when parseable
- Generic URLs containing explicit coordinates

Support should be incremental. Unsupported map URLs must still display the final URL
and redirect chain.

## Privacy principles

- No accounts.
- No analytics by default.
- No backend.
- No persistent URL history in MVP.
- No location permission required.
- The app only accesses URLs explicitly shared, pasted, or entered by the user.