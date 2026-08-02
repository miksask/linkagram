# Linkagram agent instructions

This file is the canonical instruction set for AI agents working in this
repository. `CLAUDE.md` is a symlink to this file, so both names resolve here.

Read this file first, then the matching files under `docs/specs/`,
`docs/decisions/`, and `.agents/skills/`.

## Project purpose

Linkagram is an Android application that accepts a URL from:

- Android Share sheet
- "Open with" / VIEW intent
- Clipboard
- Manual input

The application resolves shortened URLs, displays redirect chains, and extracts
location metadata and coordinates from map-related links.

This is a public GitHub demo project. The primary goal is to demonstrate
AI-assisted development of a Kotlin/Android application by an engineer whose
main background is Python, web development, and microservices.

## Source of truth

When instructions conflict, prefer in this order:

1. Matching file in `docs/specs/` or `docs/decisions/`
2. `docs/product.md`
3. This file (`AGENTS.md`)
4. `README.md`

Task-specific workflows live in `.agents/skills/`. Use them when relevant:

- `implement-feature` — implementing a feature from a spec
- `android-code-review` — reviewing Android/Kotlin changes
- `map-url-parser` — adding or changing a map URL parser

## Core product behavior

1. Accept a URL.
2. Validate and normalize it.
3. Resolve HTTP redirects safely.
4. Show the final URL and redirect chain.
5. Detect supported map URLs.
6. Extract available location metadata:
   - provider
   - place name
   - address
   - latitude
   - longitude
7. Allow copying coordinates in `lat, lon` format.

## Non-goals

Do not add these unless explicitly requested:

- User accounts
- Analytics
- Advertising
- Backend services
- Cloud sync / export-import of history
- Maps SDK rendering
- Route building/navigation (map routing, not in-app screen navigation)
- WebView-based URL loading
- Background tracking
- Contact/location permissions
- Over-engineered multi-module architecture
- DI frameworks (Hilt/Koin)

Optional local analysis history (Spec 006) is in scope: Room + DataStore,
off by default, never backed up to the cloud.

## Technology constraints

- Kotlin only.
- Jetpack Compose for UI.
- Material 3.
- Coroutines and Flow for asynchronous work.
- ViewModel for screen state.
- Prefer Android standard APIs and stable Jetpack libraries.
- Prefer minimal dependencies.
- Use Kotlin DSL Gradle files (`*.gradle.kts`).
- The app must work without a backend.

## Architecture

Use a simple layered architecture:

- `ui/`: Compose screens, UI state, ViewModels
- `domain/`: domain models (including history snapshots)
- `data/`: HTTP resolving, URL parsing, provider parsers, history storage
- `core/`: shared utilities

Do not introduce repositories, use cases, DI frameworks, or abstractions merely
for theoretical purity. `HistoryRepository` / `HistorySettingsRepository` exist
because Spec 006 needs a single data-access seam for multiple screens.

## Networking and security (summary)

URL processing handles untrusted user input. Never use WebView or JavaScript to
resolve URLs. Follow redirects manually with limits, loop detection, and
timeouts. Do not weaken TLS checks or log full user URLs in release builds.

Cleartext `http://` is permitted on purpose so plain http links can be analysed
instead of failing as network errors. That is the only relaxation; TLS
validation, trust anchors, and hostname verification stay at platform defaults.
See `docs/decisions/ADR-004-privacy-and-networking.md`.

Detailed networking guidance is in `.cursor/rules/network-security.mdc`.

## Implementation workflow

Before implementing a non-trivial feature:

1. Read relevant files in `docs/specs/`.
2. State a brief implementation plan.
3. List files expected to change.
4. Implement the smallest working version.
5. Run tests/lint/build where available.
6. Update relevant documentation if behavior or architecture changed.

For substantial decisions, add an ADR under `docs/decisions/`.

Prefer the `implement-feature` skill for the full feature checklist.

## Definition of done

A task is complete only when:

- The feature matches its acceptance criteria.
- Error and loading states are handled.
- Relevant unit tests are added or updated.
- UI changes update or intentionally leave screenshot baselines unchanged.
- `./gradlew test`, `./gradlew lint`, `./gradlew validateDebugScreenshotTest`,
  and `./gradlew assembleDebug` succeed.
- No secrets, API keys, or local machine paths are committed. Machine-specific
  files such as `local.properties` and `.env-vars.local` stay untracked.
- README/spec/ADR is updated when behavior changes.

## Code style

- Prefer readable and boring code over clever abstractions.
- Keep functions small and purpose-specific.
- Use immutable UI state.
- Avoid nullable values when a sealed type or explicit result can express state.
- Prefer sealed interfaces/classes for parsing and resolution results.
- Write comments only when explaining non-obvious decisions.
- Do not add TODOs without context and a linked issue/spec.

## Testing expectations

Test at minimum:

- URL normalization
- Redirect-limit behavior
- Redirect loop detection
- Unsupported URL handling
- Map provider detection
- Coordinate extraction
- Invalid/malformed coordinates
- Clipboard formatting (`lat, lon`)

Network resolver tests should use mocked HTTP responses, not real external
services.

Compose preview screenshot tests cover Spec 004 result states. Preview sources
live under `app/src/screenshotTest/`; reference images under
`app/src/screenshotTestDebug/reference/`. See ADR-005.

## Important commands

When the Gradle wrapper exists:

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
./gradlew validateDebugScreenshotTest
./gradlew updateDebugScreenshotTest
```

`updateDebugScreenshotTest` rewrites baselines. Run it only after intentional
UI changes, never to silence an unexplained failure.

Until then, do not invent a build system or claim these commands ran successfully.
