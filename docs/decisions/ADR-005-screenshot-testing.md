# ADR-005: Screenshot testing

## Status

Accepted

## Context

Linkagram already has JVM unit tests for URL normalization, redirect
resolution, and map parsing. Those tests cannot catch Compose layout
regressions. For a Python/backend author who cannot comfortably review
Kotlin UI code, screenshot diffs in pull requests are a practical review
surface.

Constraints that matter here:

- Business logic stays emulator-free and Robolectric-free
  (`.cursor/rules/testing.mdc`).
- The project already uses AGP 9.3, Kotlin 2.3, and Jetpack Compose.
- `AnalysisScreenContent` is a public composable driven by immutable
  `AnalysisUiState`, so preview-based capture is a natural fit.
- The repository is a small public demo; tooling cost must stay low.

## Decision

Use the official Compose Preview Screenshot Testing plugin
(`com.android.compose.screenshot`, currently `0.0.1-alpha15`):

- Enable `android.experimental.enableScreenshotTest`.
- Place `@PreviewTest` previews in `app/src/screenshotTest`.
- Record and commit reference images under
  `app/src/screenshotTestDebug/reference/`.
- Validate with `./gradlew validateDebugScreenshotTest` locally and in CI.
- Export a stable subset of screenshots to `docs/images/` for the README.

If macOS-recorded baselines fail on the Ubuntu CI runner, raise the image
difference threshold first. If differences remain stable and are not caused
by intentional UI changes, re-record baselines on CI through a controlled
`workflow_dispatch` run and commit those images. Do not rewrite baselines
only to silence a failing build.

## Consequences

Advantages:

- No Robolectric, so the existing testing rule stays intact.
- Preview tests reuse the same composable surface already designed for
  Spec 004 result states.
- Diff images in CI give a visual review path without reading Compose code.
- First-party AGP 9 support.

Trade-offs:

- The plugin is still alpha; APIs and Gradle tasks may change.
- Capture is preview-only: no clicks, scrolls, or Activity lifecycle.
- LayoutLib rendering can differ between macOS and Linux hosts.
- Reference PNGs add binary weight to the repository.

## Alternatives considered

### Roborazzi

Stronger feature set:

- Screenshots after clicks and scrolls
- Activity / Fragment capture
- Animation frame control
- Richer side-by-side diff reports
- Optional auto-generation of tests from Compose previews

Rejected for now because:

- Requires Robolectric, which conflicts with the current testing rule and
  needs `testOptions.unitTests.isIncludeAndroidResources = true`
- Adds heavier dependencies and CI surface
- AGP 9 support starts at Roborazzi 1.56.0+
- More maintenance than this demo needs while preview-only coverage is enough

Switch to Roborazzi only if interaction scenarios become necessary or the
official alpha plugin breaks on an AGP upgrade. Document that change in a
new ADR that supersedes this one.

### Paparazzi

Mature LayoutLib-based JVM screenshots without Robolectric, but AGP 9
compatibility is less clear for this project’s current toolchain, and the
official Compose Preview plugin already matches the preview-driven UI we
have.
