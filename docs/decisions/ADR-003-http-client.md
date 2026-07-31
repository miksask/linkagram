# ADR-003: HTTP client for redirect resolution

## Status

Accepted

## Context

Spec 002 requires manual redirect following, status codes, loop detection, and
deterministic unit tests with mocked HTTP responses. Android's
`HttpURLConnection` can disable redirects, but mocking and relative `Location`
resolution are awkward.

## Decision

Use OkHttp with:

- `followRedirects(false)` and `followSslRedirects(false)`
- connect / read / call timeouts
- MockWebServer for JVM unit tests

Do not add Retrofit, Ktor, or other networking stacks.

## Consequences

Advantages:

- clear control over each hop;
- `HttpUrl.resolve` for relative Location headers;
- MockWebServer keeps tests offline and deterministic.

Trade-offs:

- one extra dependency beyond AndroidX.
