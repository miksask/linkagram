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

## Result states

- Success
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