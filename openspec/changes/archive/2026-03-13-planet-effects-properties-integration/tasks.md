## 1. Precipitation Integration

- [x] 1.1 Add a `getPlanetProperties(ClientLevel)` helper to `PlanetDimensionEffects` that calls `GenesisMod.getCelestialForLevel(level)` then `PlanetProperties.get(celestial.getID())`, returning `null` if either is absent
- [x] 1.2 Update `hasPrecipitation()` to accept a `ClientLevel` parameter (or use a cached celestial) and delegate to `getPlanetProperties`, returning `precipitation` from the result or `true` as fallback
- [x] 1.3 Pass the `level` argument through to `hasPrecipitation()` in `renderSnowAndRain` and `tickRain`

## 2. Sky Color — RGB Palette Path

- [x] 2.1 Extract a new `getSkyColorForPlanet(Vec3 position, float partialTick, long time, ClientLevel level, PlanetColorPalette palette, double density)` static method (or inline branch) that handles both the overworld and RGB code paths
- [x] 2.2 In the RGB path, read `getRGB()`, convert channels to 0–1 floats, apply the day/night `intensity` multiplier (same formula as `getSkyColor`)
- [x] 2.3 In the RGB path, apply rain desaturation using the same formulas already present in `getSkyColor`
- [x] 2.4 In the RGB path, apply thunder darkening using the same formulas already present in `getSkyColor`
- [x] 2.5 In the RGB path, apply the lightning flash effect using the same formulas already present in `getSkyColor`
- [x] 2.6 Wire the palette branch into `renderSky`: call `getPlanetProperties` to get the palette; fall back to overworld sampling if null

## 3. Atmospheric Density Scaling

- [x] 3.1 In `renderSky`, read `density` from `PlanetProperties` (default `1.0` if null) and clamp to [0, 1]
- [x] 3.2 Replace raw `starBrightness` usage with `Mth.lerp(density, 1.0f, starBrightness)` so thin atmospheres maintain visible stars during the day
- [x] 3.3 After computing the final sky RGB (both biome-sampled and explicit-RGB paths), multiply all three components by `density` so lower density produces a darker sky
- [x] 3.4 Multiply the sunrise/sunset fan alpha by `density` so vacuum worlds have no visible sunrise or sunset
