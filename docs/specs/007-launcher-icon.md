# Spec 007: Launcher icon

## Goal

Show the Linkagram brand mark as the Android launcher icon, with correct
adaptive masking and a themed (monochrome) variant on Android 13+.

## Status

Accepted

## Input

- Designer-supplied master artwork at `docs/images/linkagram-icon.png`
- Android launcher mask (circle, squircle, or other OEM shape)
- System themed-icon request on Android 13+

## Functional requirements

- The app launcher entry uses the brand mark instead of the temporary blue
  placeholder vector.
- Adaptive icons use separate background and foreground layers so the system
  mask is applied once, without a baked-in squircle frame.
- The foreground mark stays inside the adaptive safe zone so circular and
  squircle launchers do not clip the ribbon or center dot.
- Android 13+ themed icons use a monochrome silhouette of the mark.
- Legacy density mipmaps remain available for surfaces that do not use adaptive
  icons.
- Manifest `android:icon` and `android:roundIcon` point at
  `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`.

## Non-requirements

- In-app Compose chrome or splash-screen branding
- Play Store listing assets beyond the APK launcher resources
- Perfect pixel match to designer layer files (source was a flat PNG; layers are
  reconstructed locally — see ADR-007)
- Animated or adaptive-icon parallax artwork beyond static layers

## Result states

Not applicable. Launcher presentation is a static resource; there is no UI
loading or error state for the icon itself.

## Acceptance criteria

- Given the app is installed on Android 8.0+, the launcher shows the brand mark
  with a system-applied mask and no double rounded frame.
- Given a circular launcher mask, the ribbon and center dot remain fully visible
  inside the safe zone.
- Given Android 13+ with themed icons enabled, the monochrome silhouette is
  used when the launcher requests it.
- Given surfaces that resolve legacy mipmaps, density-specific
  `ic_launcher` / `ic_launcher_round` bitmaps are present.
- Given the previous placeholder, `@drawable/ic_launcher` and the unused
  `ic_launcher_background` color are gone.

## Notes

- Related decision: [ADR-007](../decisions/ADR-007-adaptive-launcher-icon.md).
- Master artwork is kept in `docs/images/` for the demo narrative; generated
  Android resources live under `app/src/main/res/`.
