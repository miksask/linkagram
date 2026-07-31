---
name: android-code-review
description: Reviews Linkagram Android and Kotlin changes for Compose state, lifecycle, coroutines, URL parsing, network safety, and test coverage. Use when reviewing pull requests, diffs, or Android/Kotlin code quality.
---

# Android Code Review

Review changes for:

- Compose state correctness
- lifecycle safety
- coroutine cancellation
- blocking calls on the main thread
- configuration changes
- malformed intent/clipboard input
- URL parsing correctness
- network timeout behavior
- redirect loops and limits
- logging of sensitive URLs
- test coverage for parsers and resolver logic

Do not suggest additional architecture layers unless they solve a demonstrated problem.

If `./gradlew` exists, prefer evidence from `test` / `lint` over speculation. If it does not, say so and review statically.
