## Context

`PlanetAtmosphereRenderer` is the client-side renderer that draws a volumetric atmosphere cube around planets visible from space. It uses two hardcoded constants:
- `relativeAtmosphereSize = 1.3f` — how much larger the atmosphere box is relative to the planet half-size.
- Alpha is baked entirely in the FSH with no external multiplier.

`PlanetProperties` (server-authoritative, synced to clients) already stores `thickness: double` and `density: double` per celestial. Neither field is consumed by any renderer yet.

The renderer currently has a special code path for `vantagePoint.equals(toRender)` that shrinks `halfSize` to a near-zero value to suppress rendering when the player is on the planet. This is fragile and semantically wrong.

## Goals / Non-Goals

**Goals:**
- Wire `PlanetProperties.thickness` into `relativeAtmosphereSize` with a linear mapping that preserves current visuals at `thickness = 1.0`.
- Wire `PlanetProperties.density` into a new `Density` shader uniform that scales alpha, preserving current visuals at `density = 1.0`.
- Replace the `atmosphereThickness` FSH constant with a `AtmosphereThickness` uniform so Java fully owns the value.
- Remove the degenerate `halfSize ≈ 0` path; instead return early from `invoke()` when `toRender == vantagePoint`.
- Skip rendering when no `PlanetProperties` entry exists or `density == 0`.

**Non-Goals:**
- Changing the visual algorithm (scattering model, color curves, light direction logic).
- Adding colour control from `PlanetProperties` (that is a separate concern).
- Adding atmosphere rendering for the planet the player is currently on (sky rendering).

## Decisions

### D1: Linear thickness mapping `relativeAtmosphereSize = 1.0f + 0.3f * thickness`

The existing constant is `1.3`. At `thickness = 1.0` this gives `1.0 + 0.3 = 1.3` — exact match. The `0.3` coefficient is the "atmosphere shell" fraction of the planet radius. A linear mapping is the simplest invertible relationship and keeps the parameter intuitive (vacuum = 0, thin = 0.5, Earth-like = 1.0, thick = 2.0+).

Alternative considered: `relativeAtmosphereSize = thickness * 1.3f` — rejected because `thickness = 0` would collapse the box to zero rather than allowing density-only control, and the parameter loses its "shell thickness" intuition.

### D2: `AtmosphereThickness` uniform replaces FSH constant

The FSH uses `atmosphereThickness` in ray-box intersection math. Moving it to a uniform lets Java own the single source of truth and removes the risk of the constant and Java value drifting apart. Default value in the JSON descriptor is `1.3` to match the old constant.

### D3: `Density` uniform multiplies final alpha

`frag_color.a *= Density` is appended as the last line before output. This is a simple, composable scaling with no side-effects on the light/colour computation. Default `1.0` = no change. `0.0` = fully transparent (same as skipping, but driven by shader). We still skip on the Java side when `density == 0` to avoid unnecessary draw calls.

### D4: Early return in `invoke()` when `toRender == vantagePoint`

The purpose of this renderer is explicitly atmospheres of *other* planets. The existing hack forces a tiny planet half-size which produces garbage inputs to the shader. An early return is cleaner and avoids the shader running at all.

## Risks / Trade-offs

- **Data migration**: Any existing planet with a visible atmosphere that does not have a `PlanetProperties` entry will lose its atmosphere after this change. Planets must opt-in via data files. → Mitigation: ensure Earth-equivalent test planets have entries before shipping.
- **Uniform default mismatch**: If `AtmosphereThickness` defaults to `1.3` in the JSON but Java passes a different value before first draw, there is a one-frame flicker. → Mitigation: Java always sets the uniform before `addCubeFaceAtmosphere` calls.
- **`density == 0` skip vs shader discard**: Skipping on Java side means no draw call; relying on the shader alone would still emit geometry. Java-side skip is preferred for performance but requires a non-null properties check.
