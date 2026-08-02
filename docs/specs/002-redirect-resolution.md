# Spec 002: Redirect Resolution

## Goal

Resolve an input URL to its final destination and show the redirect chain.

## Input

A URL received from:
- share intent
- view intent
- clipboard
- manual input

## Functional requirements

- Normalize URLs without a scheme by assuming `https://`.
- Follow HTTP redirects.
- Support common redirect status codes:
  - 301
  - 302
  - 303
  - 307
  - 308
- Display every redirect step.
- Display final URL.
- Display status code for each request when available.
- Limit redirects to 10.
- Detect loops.
- Apply connect/read/call timeouts.
- Do not execute JavaScript.
- Do not use WebView.
- On a final 2xx response, if a `MetaCapturePolicy` allowlists the host, read at
  most 256 KiB of the body and attach optional `PageMeta` (`title`, `og:title`,
  `og:url`) to `Success`. Non-allowlisted hosts discard the body as before.
  See Spec 009 / ADR-009.

## Result states

- Success (optional `pageMeta` when the final host is allowlisted)
- InvalidInput
- NetworkError
- Timeout
- TooManyRedirects
- RedirectLoop
- UnsupportedProtocol
- HttpError
- UnknownError

## Acceptance criteria

- Given a direct URL, the app displays it as the final URL with zero redirects.
- Given a short URL returning 301/302 redirects, the app shows every hop.
- Given more than 10 redirects, the app shows a clear limit error.
- Given a redirect loop, the app stops and reports it.
- Given an invalid URL, no network request is made.
- Given a timeout, the UI remains usable and shows an error state.
- Given a final 2xx on an allowlisted host, `Success.pageMeta` may contain
  title/og tags parsed from a capped body.
- Given a final 2xx on a non-allowlisted host, no page meta is attached.