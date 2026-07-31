# ADR-004: Privacy and networking posture

## Status

Accepted

## Context

Linkagram resolves URLs that come from share sheets, VIEW intents, the
clipboard, and manual input. Those URLs are untrusted and may contain tokens,
identifiers, or location data. The app has no backend and no accounts, so the
only network traffic is the resolution of the URL the user supplied.

Two questions needed an explicit answer:

1. Android blocks cleartext traffic by default from `targetSdk` 28 onward.
   Specs 001 and 002 accept `http://` URLs, so every plain http link — and
   every redirect that lands on http — would fail as a generic network error.
2. Which parts of a resolved URL may be logged or persisted.

## Decision

Permit cleartext traffic through `res/xml/network_security_config.xml` with
system trust anchors only. A URL analyser that cannot show where an http link
leads is misleading: reporting "network error" hides a real, resolvable
destination and pushes the user back to a browser.

Nothing else about TLS is relaxed:

- system trust anchors only, no user-installed CAs;
- no debug overrides;
- no custom `TrustManager` or `HostnameVerifier`;
- automatic redirect following stays disabled (ADR-002).

Privacy rules that follow from having no backend:

- do not log full user URLs in release builds;
- do not persist URLs, redirect chains, or coordinates;
- read the clipboard only in response to an explicit user action;
- request no location, contacts, or storage permissions.

## Consequences

Advantages:

- http links resolve and display their redirect chain as specified;
- downgrade redirects (`https` to `http`) are visible to the user instead of
  silently failing, which is exactly the information a link inspector exists to
  surface;
- TLS posture stays at platform defaults for https traffic.

Trade-offs:

- the app can issue cleartext requests, so a network attacker can observe or
  tamper with http hops;
- the displayed chain for http hops is only as trustworthy as the network.

## Alternatives considered

- Https-only: simpler security story, but it contradicts Specs 001 and 002 and
  removes a legitimate use case (inspecting old or plain-http short links).
- Per-domain cleartext allowlist: impossible, since the target hosts are
  arbitrary user input.
