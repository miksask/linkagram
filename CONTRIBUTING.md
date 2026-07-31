# Contributing to Linkagram

Linkagram is a public demo of AI-assisted Android development. Contributions
are welcome, but the point of the project is the visible engineering process:
specs, decisions, tests, and CI, not only working code.

## Prerequisites

- JDK 17
- Android SDK platform 37 and build-tools 36.0.0
- No Android Studio required; the Gradle wrapper is enough

`local.properties` must point at your SDK and stays untracked:

```properties
sdk.dir=/path/to/Android/sdk
```

## Build and test

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

All three must pass before a pull request is ready.

## Workflow

1. Read [`AGENTS.md`](AGENTS.md) for the project constraints.
2. Read the matching file in [`docs/specs/`](docs/specs/). If your change alters
   behaviour that no spec covers, add or update a spec first.
3. Keep the change as small as it can be while still coherent.
4. Add or update unit tests next to the code you touched.
5. Record substantial decisions as an ADR in [`docs/decisions/`](docs/decisions/).
6. Update the README when user-visible behaviour changes.

## Code style

- Kotlin only, Jetpack Compose with Material 3.
- Prefer readable and boring code over clever abstractions.
- Immutable UI state; sealed types instead of nullable result values.
- No new dependency without a concrete, present-day need. Say why in the PR.
- Comments explain non-obvious constraints, not what the code does.

## Testing

Business logic must be testable on the JVM without an emulator. Network tests
use `MockWebServer`; never call real external hosts, including real URL
shorteners.

## Security

URL input is untrusted. Never introduce WebView or JavaScript execution for URL
resolution, never weaken TLS validation, and never log full user URLs in release
builds. See [`.cursor/rules/network-security.mdc`](.cursor/rules/network-security.mdc)
and [ADR-004](docs/decisions/ADR-004-privacy-and-networking.md).

## AI-assisted changes

Using an AI agent is expected here. State in the pull request which parts were
AI-generated and what you verified yourself. Agent instructions live in
`AGENTS.md` and the skills in `.agents/skills/`.

## Releases

Maintainers bump `versionCode` and `versionName` in `app/build.gradle.kts`,
then push a `v*` tag. The release workflow builds the APK and attaches it to a
GitHub Release.
