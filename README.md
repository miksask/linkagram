# Linkagram

Android app that resolves shared URLs, traces redirects, and extracts location
metadata and coordinates from map links.

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
- Resolve short links
- Display redirect chain
- Detect map links
- Extract coordinates and available place/address metadata
- Copy `latitude, longitude` in one tap

## Engineering approach

- Specs: `docs/specs/`
- Architecture decisions: `docs/decisions/`
- AI agent instructions: `AGENTS.md`
- CI: GitHub Actions
- APKs: GitHub Releases

## Current limitations

- No JavaScript-based redirects
- No WebView
- Map metadata extraction depends on URL format
- No persistent link history in MVP