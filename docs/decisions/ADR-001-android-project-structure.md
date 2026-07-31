# ADR-001: Android project structure

## Status

Accepted

## Context

Linkagram needs a runnable Android application so specs, CI, and APK releases
can be exercised against real code. The product also requires clear package
boundaries without Clean Architecture boilerplate or multi-module complexity.

## Decision

Use a single Gradle module (`:app`) with package layout:

```text
io.github.miksask.linkagram/
├── core/
│   ├── url/
│   └── clipboard/
├── data/
│   ├── resolver/
│   └── maps/
├── domain/
└── ui/
    ├── analysis/
    ├── components/
    └── theme/
```

Bootstrap only creates packages that already contain code (`ui/analysis`,
`ui/theme`). `core/`, `data/`, and `domain/` packages are added when the first
matching feature lands.

Identity and compatibility for the initial app:

- `applicationId` / namespace: `io.github.miksask.linkagram`
- `minSdk`: 26
- `compileSdk` / `targetSdk`: 37
- JDK 17, AGP 9.3, Gradle 9.5
- Jetpack Compose + Material 3
- Built-in Kotlin support from AGP (no separate `kotlin-android` plugin)
- Compose Compiler Gradle plugin for Compose compilation

## Consequences

Advantages:

- one module keeps navigation, build, and CI simple;
- package folders communicate intended boundaries without DI or repository
  ceremony;
- empty packages are avoided, so the tree reflects real code.

Trade-offs:

- later extraction of shared libraries requires a module split;
- package boundaries rely on discipline rather than compile-time module walls.

## Alternatives considered

- Multi-module `:app` / `:domain` / `:data`: rejected for MVP complexity.
- DI framework from day one: rejected until constructor wiring becomes painful.
