# ADR-007: Adaptive launcher icon

## Status

Accepted

## Context

Designers supplied a finished 1270×1270 PNG with a baked-in squircle mask, a
diagonal purple-to-orange gradient, and a ribbon mark. The app previously used a
temporary blue vector at `@drawable/ic_launcher`. Android 8+ (our `minSdk`)
expects adaptive icons with background and foreground layers.

No separate designer layer files were available. An earlier attempt to
reconstruct background, foreground, and monochrome from the flat master with
local image processing produced a visibly poor launcher icon. Shipping the
finished artwork as-is is preferable to a bad layer split.

## Decision

1. **Adaptive icon** (`mipmap-anydpi-v26`) with:
   - solid black **background** (`@color/ic_launcher_background`);
   - the finished master PNG as **foreground** (resized to 108 dp / 432 px
     nodpi), including its baked-in squircle.
2. **No monochrome / themed** layer — without a clean silhouette export, a
   reconstructed mono mark is not worth shipping.
3. **Legacy mipmaps** (`mdpi`…`xxxhdpi`) for `ic_launcher` and
   `ic_launcher_round` as simple resizes of the same master.
4. Manifest points at `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`.
5. Keep the designer master at `docs/images/linkagram-icon.png`. Do not add a
   runtime or Gradle plugin dependency for icon generation; only resize/copy.
6. Accept **squircle-inside-mask** on circular (and some OEM) launchers as the
   trade-off for pixel-faithful artwork.

## Consequences

Advantages:

- Launcher artwork matches the designer deliverable.
- No lossy layer reconstruction.
- Legacy fallbacks stay trivial to regenerate.

Trade-offs:

- Circular launchers show the baked squircle inside the system circle.
- No Android 13+ themed icon until designers supply a monochrome asset.

## Alternatives considered

- **Reconstruct background / foreground / monochrome from the flat PNG** —
  rejected after on-device review: edges, holes, and gradient ghosts looked
  worse than squircle-inside-mask.
- **Wait for designer layer exports** — deferred; as-is unblocks branding now.
- **Vectorize the mark** — rejected; the 3D ribbon gradients are not a good fit
  for a hand-authored vector in this demo.
