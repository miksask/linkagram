# Linkagram — Screenshot Tests and Showcase

Continue development of the existing **Linkagram** project.

Specs 001–005 are implemented. Before making changes, inspect the current
repository structure, documentation, architectural decisions, build
configuration, and UI code. Preserve established conventions unless there is a
clear technical reason to change them. Do not rename the project.

Implement the next work in the following order.

## 1. ADR — Screenshot Testing Choice

Add `docs/decisions/ADR-005-screenshot-testing.md` documenting the tool choice.

Decision:

* Use the official Compose Preview Screenshot Testing plugin
  (`com.android.compose.screenshot`).
* Keep preview tests in the `screenshotTest` source set.
* Commit reference images to the repository.

Document trade-offs of the official plugin (alpha API, preview-only capture,
possible macOS vs Linux render differences) and the Roborazzi alternative:

* Roborazzi strengths: screenshots after clicks and scrolls, Activity/Fragment
  capture, animation frame control, richer diff reports, preview auto-scan.
* Roborazzi risks: requires Robolectric (conflicts with the current testing
  rule), heavier dependencies and CI, AGP 9 needs Roborazzi 1.56.0+, more
  maintenance for a demo repository.

State the switch condition: adopt Roborazzi only if interaction scenarios
become necessary or the alpha plugin breaks on an AGP upgrade.

## 2. Wire the Screenshot Plugin

Enable host-side screenshot testing:

* `gradle.properties`: `android.experimental.enableScreenshotTest=true`
* root `build.gradle.kts`: declare the plugin with `apply false`
* `app/build.gradle.kts`: apply the plugin, enable the experimental property,
  set an image difference threshold, and add
  `screenshotTestImplementation` dependencies for the validation API and
  Compose UI tooling

Use a current `0.0.1-alpha15` or newer plugin version compatible with AGP 9.

## 3. Preview Tests for Spec 004 States

Add
`app/src/screenshotTest/.../AnalysisScreenPreviewTest.kt` with `@PreviewTest`
and `@Preview` composables that render the public `AnalysisScreenContent`.

Cover at least:

* idle
* analyzing
* validation error
* success with coordinates and copy action
* resolve error with a partial redirect chain
* long URL that wraps rather than truncates
* dark theme

Do not move business logic into the screenshot source set. Keep the existing
IDE `@Preview` in `main` if useful for local design work.

Record baselines with `./gradlew updateDebugScreenshotTest` and commit the
reference images under `app/src/screenshotTestDebug/reference/`. Never rewrite
baselines only to make a failing build green.

## 4. CI Gates and Artifacts

Update `.github/workflows/ci.yml`:

* run `./gradlew validateDebugScreenshotTest` after lint
* upload screenshot reports and diff images with `if: always()`
* upload the debug APK as a PR artifact after `assembleDebug`

If macOS-recorded baselines fail on the Ubuntu runner, raise the difference
threshold first. If differences remain stable and unexplained by real UI
changes, re-record baselines on CI via a controlled `workflow_dispatch` run and
document that in ADR-005. Do not silently ignore failures.

## 5. README Showcase

Update `README.md` into a portfolio-friendly front page without inventing
personal process narrative:

* screenshot gallery near the top, using stable filenames under `docs/images/`
  exported from the recorded baselines
* link to the latest release APK and `minSdk` 26
* short AI-workflow section limited to verifiable repository facts: specs,
  ADRs, skills, CI gates, screenshot diffs in PRs
* document screenshot Gradle commands under Build and test / Current status

Do not invent session counts, failure stories, or journal content. Process
history stays for a later prompts/journal phase.

## 6. Agent Docs and Rules

Update:

* `AGENTS.md` — screenshot commands and Definition of done
* `CONTRIBUTING.md` — same commands in Build and test
* `.cursor/rules/testing.mdc` — screenshot previews live in `screenshotTest`,
  baselines are not rewritten to silence failures, Robolectric remains unused
* `docs/README.md` — list ADR-005 and the prompts section

# Implementation Guidelines

* Work incrementally and keep the project buildable after each step.
* Prefer the official Compose Preview Screenshot Testing plugin over Roborazzi
  or Paparazzi for this phase.
* Do not introduce Robolectric, Hilt, multi-module layout, renaming, or new
  product features.
* Do not scrape pages, add a Maps SDK, or change networking/privacy rules.
* Update documentation when the tool choice or CI behaviour changes.
* Run available checks before considering the work complete.

At the end, provide a concise summary of:

* implemented functionality;
* important architectural decisions;
* added or changed tests;
* known limitations;
* files or documentation that were added or modified.
