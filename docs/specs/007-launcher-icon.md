# Spec 007: Launcher icon

## Goal

Show the Linkagram brand mark as the Android launcher icon using the
designer-supplied finished artwork.

## Status

Accepted

## Input

- Designer-supplied master artwork at `docs/images/linkagram-icon.png`
  (finished PNG with baked-in squircle)
- Android launcher mask (circle, squircle, or other OEM shape)

## Functional requirements

- The app launcher entry uses the brand mark instead of the temporary blue
  placeholder vector.
- Adaptive icons use the finished PNG as the **foreground** layer and a solid
  black **background** so transparent corners of the master do not show
  through.
- Legacy density mipmaps remain available for surfaces that do not use adaptive
  icons; they are simple resizes of the same master.
- Manifest `android:icon` and `android:roundIcon` point at
  `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`.

## Non-requirements

- In-app Compose chrome or splash-screen branding
- Play Store listing assets beyond the APK launcher resources
- Separate designer layer exports (background / mark / monochrome)
- Perfect adaptive masking without a squircle-inside-mask on circular
  launchers — the master includes a baked-in squircle (see ADR-007)
- Android 13+ themed / monochrome icon layer
- Animated or adaptive-icon parallax artwork beyond static layers

## Result states

Not applicable. Launcher presentation is a static resource; there is no UI
loading or error state for the icon itself.

## Acceptance criteria

- Given the app is installed on Android 8.0+, the launcher shows the brand
  artwork from the designer master (not the old blue placeholder).
- Given a circular launcher mask, the baked-in squircle may remain visible
  inside the circle (accepted trade-off).
- Given surfaces that resolve legacy mipmaps, density-specific
  `ic_launcher` / `ic_launcher_round` bitmaps are present.
- Given the previous placeholder, `@drawable/ic_launcher` is gone.

## Notes

- Related decision: [ADR-007](../decisions/ADR-007-adaptive-launcher-icon.md).
- Master artwork is kept in `docs/images/`; generated Android resources live
  under `app/src/main/res/`.
