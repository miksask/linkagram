---
name: map-url-parser
description: Adds or changes map URL parsers for Google Maps, Yandex Maps, OpenStreetMap, Apple Maps, or generic coordinate URLs. Use when implementing provider parsing, extracting coordinates, or documenting supported URL patterns.
---

# URL Parser Implementation

When adding a map URL parser:

1. Collect representative URL examples.
2. Document supported patterns in the provider spec.
3. Parse only stable and observable URL structures.
4. Never claim metadata that cannot be extracted reliably.
5. Return partial results when only coordinates are available.
6. Keep provider-specific code isolated.
7. Add table-driven tests for every URL pattern.
8. Treat malformed URLs as normal input, not exceptional crashes.
