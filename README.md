# Linkagram

Android app that resolves shared URLs, traces redirects, and extracts location
metadata and coordinates from map links.

## Current status

Runnable Android/Compose app with:

- URL intake from Share (`ACTION_SEND`), VIEW intents, clipboard, and manual input
- URL validation and normalization (Spec 001)
- Manual HTTP redirect resolution with chain display (Spec 002)
- Map URL parsing for Google, Yandex, OSM, Apple, and generic coordinates (Spec 003)
- One-tap copy of coordinates as `lat, lon`
- Unit tests and GitHub Actions CI (`test`, `lint`, `assembleDebug`)

Application id / namespace: `io.github.miksask.linkagram`

## Why this project exists

This project is an experiment in AI-assisted software development.

My primary background is Python, web development, and microservices.
Linkagram is built for Android using Kotlin and Jetpack Compose with assistance
from tools such as Claude and Cursor.

The goal is not to claim that AI writes software autonomously. The goal is to
demonstrate a workflow where AI accelerates development while project
constraints, specifications, reviews, tests, and architecture remain explicit.

## Features

- Receive URLs from Android Share sheet and VIEW intents
- Paste URLs from clipboard
- Resolve short links and show redirect hops with status codes
- Detect map links and extract coordinates / available place metadata
- Copy `latitude, longitude` in one tap

## Build and test

Requires JDK 17 and Android SDK platform 37.

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
- APKs: GitHub Releases (release workflow still to add)

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

- JavaScript-only redirects are not followed (no WebView)
- Map metadata depends on stable URL structure, not page scraping
- Not every provider URL variant is covered yet
- No persistent link history in MVP
