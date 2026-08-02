# ADR-007: Adaptive launcher icon

## Status

Accepted

## Context

Designers supplied a finished 1270×1270 PNG with a baked-in squircle mask, a
diagonal purple-to-orange gradient, and a ribbon mark. The app previously used a
temporary blue vector at `@drawable/ic_launcher`. Android 8+ (our `minSdk`)
expects adaptive icons with separate background and foreground layers; Android
13+ can also request a monochrome layer for themed icons. Using the finished PNG
as a single foreground would leave a squircle-inside-mask artifact on circular
launchers.

No separate designer layer files were available. Reconstructing background,
foreground, and monochrome from the flat master is therefore part of the
integration, not a deferred polish step.

## Decision

1. **Adaptive icon** (`mipmap-anydpi-v26`) with:
   - reconstructed full-bleed gradient **background** (no outer squircle);
   - reconstructed RGBA **foreground** mark (ribbon + center dot + soft shadow),
     padded into the adaptive safe zone.
2. **Monochrome** silhouette of the mark in `mipmap-anydpi-v33` for themed icons.
3. **Legacy mipmaps** (`mdpi`…`xxxhdpi`) for `ic_launcher` and
   `ic_launcher_round` as pre-composited fallbacks.
4. Raster layers live in `drawable-nodpi/`; XML adaptive definitions reference
   them. Manifest points at `@mipmap/ic_launcher` /
   `@mipmap/ic_launcher_round`.
5. Keep the designer master at `docs/images/linkagram-icon.png`. Do not add an
   ImageMagick/Python runtime or Gradle plugin dependency for icon generation;
   assets are committed after a one-off local rebuild.
6. Accept that reconstructed layers may differ slightly from original design
   source files (edge anti-alias, shadow softness) while matching the supplied
   master composition.

## Consequences

Advantages:

- System masks apply correctly without a double frame.
- Themed icons work on Android 13+.
- Legacy fallbacks cover non-adaptive consumers.
- Process stays explicit: Spec 007 + this ADR + committed master.

Trade-offs:

- Layer split is approximate because the source PNG is flat.
- Regenerating assets requires a local image toolchain; the APK build itself
  does not.

## Alternatives considered

- **Ship the finished PNG as-is** — rejected; baked squircle conflicts with
  adaptive masks.
- **Wait for designer layer exports** — rejected for this iteration; the flat
  master is enough for a usable adaptive set.
- **Vectorize the mark** — rejected; the 3D ribbon gradients are not a good fit
  for a hand-authored vector in this demo.
