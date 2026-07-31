# Linkagram — Next Implementation Phase

Continue development of the existing **Linkagram** project.

The project bootstrap has already been completed. Before making changes, inspect the current repository structure, existing documentation, architectural decisions, build configuration, and implemented code. Preserve the established conventions unless there is a clear technical reason to change them.

Implement the next features in the following order:

## 1. Spec 001 — URL Input and Normalization

Implement URL intake, validation, and normalization.

The application must accept URLs through:

* Android Share Sheet using `ACTION_SEND`;
* URL intents using `ACTION_VIEW`;
* clipboard input;
* manual text input.

Requirements:

* extract a URL from incoming intent data or shared text;
* handle malformed, missing, and unsupported input safely;
* normalize URLs before further processing;
* keep normalization logic independent from Android UI code;
* make business logic unit-testable without an emulator;
* display clear validation errors to the user;
* avoid silently modifying input in ways that could change its meaning.

Document the normalization rules and cover important edge cases with unit tests.

## 2. Spec 002 — Redirect Resolution

After Spec 001 is complete, implement HTTP redirect resolution according to **Spec 002**.

Requirements:

* follow HTTP redirects manually;
* disable automatic redirect following in the HTTP client;
* record every redirect step;
* include the source URL, destination URL, and HTTP status code for each step whenever available;
* return the final resolved URL;
* enforce a configurable maximum redirect count;
* detect redirect loops;
* resolve relative `Location` headers correctly;
* handle missing or malformed `Location` headers;
* handle network failures, timeouts, unsupported protocols, and invalid responses safely;
* do not execute JavaScript;
* do not use a `WebView`.

Keep redirect-resolution logic isolated from the UI and cover it with unit tests using deterministic mocked HTTP responses.

## 3. Map URL Parsing

After redirect resolution is complete, implement map URL parsing using the **`map-url-parser` skill**.

Use the final resolved URL as the primary parser input.

Initially support:

* Google Maps;
* Yandex Maps;
* OpenStreetMap;
* Apple Maps links when their URL structure can be parsed;
* generic URLs containing explicit coordinates.

The parser should attempt to extract:

* map provider;
* place name;
* address;
* latitude;
* longitude.

Requirements:

* keep provider-specific parsing logic isolated;
* use deterministic URL parsing rather than page scraping;
* do not use a `WebView`;
* do not execute JavaScript;
* do not introduce a Maps SDK;
* validate coordinate ranges;
* support copying coordinates in `lat, lon` format;
* return partial results when only some fields are available;
* preserve and display the final URL even when map parsing fails.

Add focused unit tests for every supported provider and for generic coordinate extraction.

# Implementation Guidelines

* Work incrementally and keep the project buildable after each step.
* Follow the existing architecture and repository conventions.
* Prefer standard Android and Kotlin APIs.
* Keep external dependencies to a minimum.
* Do not add speculative abstractions.
* Do not introduce a backend, database, persistent history, accounts, analytics, authentication, location permissions, or Maps SDK.
* Update relevant documentation and architectural decision records when implementation choices require explanation.
* Add or update tests alongside each feature.
* Run the available formatting, linting, unit-test, and build checks before considering the work complete.

At the end, provide a concise summary of:

* implemented functionality;
* important architectural decisions;
* added or changed tests;
* known limitations;
* files or documentation that were added or modified.
