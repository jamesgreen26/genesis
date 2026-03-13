## Context

`PlanetDimensionEffects` is the client-side renderer for custom planet dimensions. It currently has three hardcoded behaviors that should be data-driven:

1. **Precipitation** — `hasPrecipitation()` returns `true` unconditionally; there's an explicit `//TODO` in the code.
2. **Sky color** — `getSkyColor()` always samples biome sky color. `PlanetColorPalette` defines a `genesis:rgb` variant explicitly for non-Overworld planets.
3. **Atmospheric density** — `PlanetProperties.density` (0 = vacuum, 1 = Earth-like) is loaded but never consumed by the renderer.

`PlanetProperties` is fully implemented, data-file driven, and synced to clients via `PlanetPropertiesSyncPacket`. All data is available at render time. The integration is purely a client-side read operation — no changes to data loading, networking, or server code.

## Goals / Non-Goals

**Goals:**
- Wire `PlanetProperties.precipitation` into `hasPrecipitation()`
- Branch sky color on `PlanetColorPalette.isOverworld()`: use biome sampling for `genesis:overworld`, otherwise use the planet's explicit RGB
- Use `PlanetProperties.density` to modulate atmospheric scattering (star brightness, sky intensity)
- Graceful fallback when no `PlanetProperties` entry exists for a planet (treat as Overworld-like defaults)

**Non-Goals:**
- Fog density changes driven by atmospheric thickness (scope for a future renderer pass)
- Server-side precipitation logic (Minecraft weather simulation is not changed)
- Any changes to `PlanetProperties` data model, codecs, or sync

## Decisions

### Property lookup pattern

`PlanetDimensionEffects` should call `PlanetProperties.get(celestial.getID())` and defensively null-check. If no entry is registered, fall back to `precipitation = true` and the overworld color path so unregistered planets stay visually correct.

**Alternative considered:** Require every `genesis:body` to have a `PlanetProperties` entry and log a warning if missing. Rejected — too strict for third-party addon compatibility during early development.

### Sky color branching

In `renderSky`, after resolving `PlanetProperties.color`:
- `isOverworld() == true` → existing `getSkyColor()` biome-sampling path (unchanged behavior for the Overworld).
- `isOverworld() == false` → convert `getRGB()` int array to 0–1 floats, apply the same day/night `intensity` multiplier already used in `getSkyColor()`, and apply rain/thunder desaturation using the same formulas.

Both paths produce the same `Vec3(r, g, b)` type, so the rest of the sky rendering is unaffected.

**Alternative considered:** Extract sky color determination into a separate method parameterized by `PlanetProperties`. Cleaner but adds indirection for a single call site; inline branching is sufficient here.

### Density-based atmospheric scaling

`density` is clamped to [0, 1] before use.

**Sky color:** multiply all final RGB components by `density` (after the existing day/night intensity, rain, and thunder calculations). This applies to both the biome-sampled and explicit-RGB paths — a dense atmosphere means a bright sky; a vacuum means black.

**Star brightness:** the existing formula produces `starBrightness ∈ [0, 1]` where 0 is midday and 1 is midnight. Rather than multiplying directly by `density` (which would keep stars dim at noon for thin atmospheres), use a lerp:

```
effectiveStarBrightness = lerp(density, 1.0, starBrightness)
```

- At `density = 0`: always 1.0 — no atmosphere to scatter sunlight, stars are always visible.
- At `density = 1`: equals `starBrightness` — full atmosphere, normal daytime dimming.
- Intermediate values gradually restore midday star visibility as the atmosphere thins.

**Alternative considered:** Multiply `starBrightness` directly by `(1 - density) + starBrightness * density`. Equivalent but less readable than the lerp form. The lerp also makes the intent explicit: we're blending between "always bright" (no atmosphere) and "normally bright" (full atmosphere).

### Fallback defaults

| Property | Fallback if `PlanetProperties` is null |
|---|---|
| `precipitation` | `true` |
| `color` | `genesis:overworld` (biome sampling) |
| `density` | `1.0` |

## Risks / Trade-offs

- **Thin-atmosphere daytime stars** — with `density < 1`, stars are visible during the day. This is physically correct (no atmosphere = no Rayleigh scattering) but might surprise players who expect day sky to be star-free. Mitigation: document in the data file spec; the lerp ensures the effect is gradual and predictable.
- **Rain on density=0 worlds** — `precipitation` is a separate toggle from `density`, so a world could have precipitation enabled but density = 0 (no atmosphere). Callers should treat these independently; the renderer already separates the two uses.
- **No fallback for null celestial** — if `GenesisMod.getCelestialForLevel` returns null, `renderSky` already returns `false` early (existing behavior). `hasPrecipitation()` currently can't access the level, so it can't do the same lookup. This is resolved by passing `level` into `hasPrecipitation()` or caching the celestial lookup.

## Open Questions

- Should `hasPrecipitation()` receive the `ClientLevel` parameter, or should the celestial lookup be cached on the `PlanetDimensionEffects` instance? The class is per-dimension, so caching is safe.
