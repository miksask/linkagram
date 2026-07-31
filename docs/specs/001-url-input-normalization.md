# Spec 001: URL Input and Normalization

## Goal

Accept a URL from share, VIEW intent, clipboard, or manual input; validate and
normalize it before further processing.

## Status

Accepted

## Input

- `ACTION_SEND` shared text (`text/plain`)
- `ACTION_VIEW` http(s) URL
- Clipboard text
- Manual text field input

## Functional requirements

- Extract a URL candidate from shared or pasted text when the payload is not a
  bare URL.
- Trim leading and trailing whitespace.
- If the candidate has no scheme, prepend `https://`.
- Accept only `http` and `https`.
- Reject empty, blank, and non-URL input with a clear error.
- Do not rewrite host, path, query, or fragment for cosmetic reasons.
- Do not lower-case path or query (host may be lower-cased by the URL parser).
- Keep normalization logic free of Android UI types so it is unit-testable on
  the JVM.

## Normalization rules

1. Trim whitespace from the raw input.
2. If the trimmed text is empty → `InvalidUrl(Empty)`.
3. Prefer an explicit `http://` or `https://` URL found in the text (first match).
4. Otherwise treat the whole trimmed string as a candidate.
5. If the candidate has no scheme → prepend `https://`.
6. Parse with `java.net.URI` / OkHttp `HttpUrl` style rules; require a non-blank
   host.
7. If scheme is present but not `http`/`https` → `InvalidUrl(UnsupportedScheme)`.
8. If parsing fails → `InvalidUrl(Malformed)`.

## Result states

- `NormalizedUrl(url: String)`
- `InvalidUrl(reason)` where reason is Empty | Malformed | UnsupportedScheme | NoUrlFound

## Non-requirements

- HTTP redirect following (Spec 002)
- Map metadata extraction (Spec 003)
- Persistent history

## Acceptance criteria

- Given `example.com/path`, the app normalizes to `https://example.com/path`.
- Given `  https://example.com  `, the app normalizes to `https://example.com`.
- Given `Check this https://maps.example/x please`, the app extracts
  `https://maps.example/x`.
- Given `ftp://example.com`, the app shows an unsupported-scheme error.
- Given empty input, the app shows a validation error and makes no network call.
- Given a share/VIEW intent with a valid URL, the draft field is prefilled.
