# Linkagram

Android app that resolves shared URLs, traces redirects, and extracts location
metadata and coordinates from map links.

## Current status

The repository now includes a **minimal Android/Compose bootstrap**:

- Single-module Gradle project (`:app`)
- Application id / namespace: `io.github.miksask.linkagram`
- Jetpack Compose + Material 3 shell with a manual URL input field
- Unit test for the analysis screen ViewModel
- GitHub Actions CI for `test`, `lint`, and `assembleDebug`

Product features (share/VIEW intents, redirect resolution, map parsers) are
specified under `docs/` and are not implemented yet.

Also present:

- Product description, specs, and ADRs under `docs/`
- Agent instructions in `AGENTS.md`
- Cursor project rules in `.cursor/rules/`
- Portable Agent Skills in `.agents/skills/`

## Why this project exists

This project is an experiment in AI-assisted software development.

My primary background is Python, web development, and microservices.
Linkagram is built for Android using Kotlin and Jetpack Compose with assistance
from tools such as Claude and Cursor.

The goal is not to claim that AI writes software autonomously. The goal is to
demonstrate a workflow where AI accelerates development while project
constraints, specifications, reviews, tests, and architecture remain explicit.

## Planned features

- Receive URLs from Android Share sheet and VIEW intents
- Paste URLs from clipboard
- Resolve short links
- Display redirect chain
- Detect map links
- Extract coordinates and available place/address metadata
- Copy `latitude, longitude` in one tap

## Build and test

Requires JDK 17 and Android SDK platform 37.

### macOS / Homebrew JDK

Homebrew installs `openjdk@17` as keg-only, so macOS `/usr/bin/java` stays as
a stub and prints `Unable to locate a Java Runtime` until `JAVA_HOME` points at
the real JDK.

Local machine exports live in `.env-vars.local` (not committed). Before Gradle:

```bash
source .env-vars.local
```

Example contents:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

Optional: register the JDK for `/usr/libexec/java_home`:

```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```


### Gradle commands

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Engineering approach

- Specs: `docs/specs/`
- Architecture decisions: `docs/decisions/`
- Documentation index: `docs/README.md`
- AI agent instructions: `AGENTS.md`
- Cursor rules: `.cursor/rules/`
- Agent skills: `.agents/skills/`
- CI: GitHub Actions
- APKs: GitHub Releases (after release workflow is added)

## Cursor / AI workflow

1. Read `AGENTS.md` for global constraints and definition of done.
2. Read the matching file in `docs/specs/` before implementing behavior.
3. Use skills when relevant:
   - `/implement-feature`
   - `/android-code-review`
   - `/map-url-parser`
4. Keep substantial decisions in `docs/decisions/`.
5. Run `./gradlew test`, `./gradlew lint`, and `./gradlew assembleDebug` when
   changing application code.

## Current limitations

- No URL resolution or map parsing yet
- No share / VIEW / clipboard entry points yet
- No JavaScript-based redirects planned
- No WebView planned
- Map metadata extraction will depend on URL format
- No persistent link history in MVP
