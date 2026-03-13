## Why

`PlanetDimensionEffects` drives sky rendering and weather for planet dimensions but currently ignores all data in `PlanetProperties`, hardcoding precipitation to `true` and sky color to biome-based sampling. Now that `PlanetProperties` is fully loaded and synced to clients, the renderer should use it.

## What Changes

- `hasPrecipitation()` reads `PlanetProperties.precipitation` instead of returning `true` unconditionally.
- Sky color rendering branches on `PlanetColorPalette`: when `isOverworld()` is `true` (existing biome sampling path); when `false`, uses the planet's explicit RGB color with day/night intensity.
- Atmospheric density from `PlanetProperties.density` scales star brightness and sky intensity — a vacuum world (density = 0) has no atmospheric scattering effects.
- `PlanetDimensionEffects` gains a helper to look up properties for the current level's celestial.

## Capabilities

### New Capabilities

- `planet-sky-rendering`: Rendering of sky color, star brightness, and precipitation in `PlanetDimensionEffects` is driven by `PlanetProperties` data.

### Modified Capabilities

- `planet-properties`: Adding rendering-side usage requirements for `precipitation` and `color` fields; no data-model or sync behavior changes.

## Impact

- `PlanetDimensionEffects.java` — primary change site
- `PlanetProperties.java` — read-only usage; no structural changes
- `PlanetColorPalette.java` — read-only usage; `isOverworld()` / `getRGB()` contract already defined
- No changes to networking, data files, or server-side code
