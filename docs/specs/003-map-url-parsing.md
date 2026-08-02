# Spec 003: Map URL Parsing

## Goal

Extract map provider, place metadata, and coordinates from a final resolved URL
when the URL structure makes that information available.

## Status

Accepted

## Input

The final URL from redirect resolution (or a normalized direct URL).

## Functional requirements

- Detect provider from host/path patterns:
  - Google Maps
  - Yandex Maps
  - OpenStreetMap
  - Apple Maps (when parseable)
  - Organic Maps (`omaps.app`)
  - Generic URLs with explicit coordinates
- Extract when available: provider, place name, address, latitude, longitude.
- Validate latitude ∈ [-90, 90] and longitude ∈ [-180, 180].
- Return partial results when only some fields are available.
- Support copying coordinates as `lat, lon`.
- Never scrape HTML, never use WebView/JS/Maps SDK.
- If parsing fails, the UI still shows the final URL and redirect chain.

## Supported patterns (initial)

### Google Maps

- `https://www.google.com/maps/@lat,lon,zoom`
- `https://www.google.com/maps/place/.../@lat,lon,...`
- `https://maps.google.com/?q=lat,lon`
- `https://www.google.com/maps/search/?api=1&query=lat,lon`
- Query params `q` / `query` containing `lat,lon`
- Adjacent `!3d<lat>!4d<lon>` pair inside a `data=` path segment (common after
  `maps.app.goo.gl` short-link resolution). Precedence: `@` → `!3d/!4d` → `q`/`query`.
- `/place/<name>,<address>` segments split on the first comma into `placeName`
  and `address` when both parts are non-empty.

### Yandex Maps

- `https://yandex.ru/maps/?ll=lon,lat`
- `https://yandex.com/maps/?ll=lon,lat`
- `pt=lon,lat` style point params when present
- `text=` as place name when present

### OpenStreetMap

- `https://www.openstreetmap.org/#map=zoom/lat/lon`
- `https://www.openstreetmap.org/?mlat=lat&mlon=lon`
- `geo=` query when present

### Apple Maps

- `https://maps.apple.com/?ll=lat,lon`
- `q=` / `address=` as place/address when present

### Organic Maps

- ge0 short links: `https://omaps.app/<ge0>[/<name>]` — coordinates are decoded
  from the first path segment (same algorithm as Organic Maps url-processor);
  optional name segment with `_` / `+` as spaces
- Clear coordinates: `https://omaps.app/lat,lon[/<name>]`
- Map API: `https://omaps.app/map?v=1&ll=lat,lon&n=Name` (first `ll` / `n`)
- Host: `omaps.app` and subdomains. Zoom is ignored. Route/search/crosshair and
  `om://` are out of scope.

### Generic

- Query params commonly named `lat`/`lon`, `latitude`/`longitude`, `mlat`/`mlon`
- Path or query pair `lat,lon` when unambiguous
- `geo:lat,lon` URI scheme (if encountered after redirects as https deep-link
  equivalents only; plain `geo:` is out of HTTP resolve scope)

## Result states

- `Parsed(LocationInfo)` — provider known; fields optional
- `Unsupported` — not recognized as a map/coordinate URL

## Acceptance criteria

- Given a Google Maps `@lat,lon` URL, coordinates are extracted.
- Given a Google Maps place URL with `!3d<lat>!4d<lon>` and no `@`, coordinates
  are extracted.
- Given a Google Maps place URL with both `@` and `!3d/!4d`, the `@` values win.
- Given a Google Maps `/place/Name,+Street,+City` URL, `placeName` and `address`
  are split on the first comma.
- Given a Yandex `ll=lon,lat` URL, coordinates are extracted in correct order.
- Given an OSM hash map URL, coordinates are extracted.
- Given an Apple Maps `ll=lat,lon` URL, coordinates are extracted.
- Given an Organic Maps ge0 short link, coordinates and place name are extracted.
- Given an Organic Maps clear `lat,lon` path, coordinates are extracted.
- Given an Organic Maps `/map?ll=lat,lon` URL, coordinates are extracted.
- Given invalid coordinates (lat=999), parsing does not return those values.
- Given a non-map URL, analysis still shows the final URL.

## Notes

Opt-in address geocoding for place URLs that carry no coordinates is specified
in [008-address-geocoding.md](008-address-geocoding.md).
