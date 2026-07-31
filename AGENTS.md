# Linkagram — AI Agent Guide

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
- Database persistence
- Maps SDK rendering
- Route building/navigation
- WebView-based URL loading
- Background tracking
- Contact/location permissions
- Over-engineered multi-module architecture

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
- `domain/`: use cases and domain models
- `data/`: HTTP resolving, URL parsing, provider parsers
- `core/`: shared utilities

Do not introduce repositories, use cases, DI frameworks, or abstractions merely
for theoretical purity. Add an abstraction only when it has a concrete use.

## Networking and security requirements

URL processing handles untrusted user input.

- Never open URLs in a WebView.
- Do not execute JavaScript.
- Do not follow redirects indefinitely.
- Limit redirect count.
- Use request timeouts.
- Handle malformed URLs safely.
- Do not log full URLs in release builds if they may contain tokens or personal data.
- Avoid storing submitted URLs unless explicit persistence is added.
- Treat redirect chains as potentially sensitive data.
- Do not bypass TLS certificate validation.
- Do not disable hostname verification.

## Implementation workflow

Before implementing a non-trivial feature:

1. Read relevant files in `docs/specs/`.
2. State a brief implementation plan.
3. List files expected to change.
4. Implement the smallest working version.
5. Run tests/lint/build where available.
6. Update relevant documentation if behavior or architecture changed.

For substantial decisions, add an ADR under `docs/decisions/`.

## Definition of done

A task is complete only when:

- The feature matches its acceptance criteria.
- Error and loading states are handled.
- Relevant unit tests are added or updated.
- `./gradlew test` passes where applicable.
- `./gradlew lint` passes where applicable.
- `./gradlew assembleDebug` succeeds.
- No secrets, API keys, or local machine paths are committed.
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

Network resolver tests should use mocked HTTP responses, not real external services.

## Important commands

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint