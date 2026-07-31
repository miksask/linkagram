# ADR-002: URL resolution strategy

## Status

Accepted

## Context

Linkagram must resolve shortened URLs and show the redirect chain. URLs are
untrusted external input.

## Decision

Use an HTTP client with automatic redirects disabled.

Follow redirects manually to:
- collect every redirect step;
- enforce a maximum redirect count;
- detect loops;
- expose status codes and locations to the UI.

Do not use WebView for URL resolution.

## Consequences

Advantages:
- full redirect-chain visibility;
- predictable limits and error handling;
- no JavaScript execution;
- easier unit testing.

Trade-offs:
- some JavaScript-only redirect pages will not resolve;
- some websites may behave differently from a browser;
- provider-specific parsing may be required for certain map links.