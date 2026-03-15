### Requirement: Precipitation Driven by Planet Properties
`PlanetDimensionEffects` SHALL read `PlanetProperties.precipitation` for the current planet to decide whether rain/snow renders and ticks. When no `PlanetProperties` entry exists for the current celestial, precipitation SHALL default to `true`.

#### Scenario: Precipitation enabled via planet properties
- **WHEN** the current planet has `"precipitation": true` in its `PlanetProperties`
- **THEN** `renderSnowAndRain` and `tickRain` return `false` (Minecraft handles precipitation normally)

#### Scenario: Precipitation disabled via planet properties
- **WHEN** the current planet has `"precipitation": false` in its `PlanetProperties`
- **THEN** `renderSnowAndRain` and `tickRain` return `true` (suppressing Minecraft precipitation)

#### Scenario: No planet properties registered
- **WHEN** `PlanetProperties.get(celestial.getID())` returns `null`
- **THEN** precipitation defaults to `true` (Overworld-like behaviour)

---

### Requirement: Sky Color Driven by Planet Color Palette
`PlanetDimensionEffects` SHALL branch sky color rendering on the `PlanetColorPalette` variant for the current planet.

- When `isOverworld()` is `true`, the sky color SHALL be determined by biome sampling (existing `getSkyColor` path), unmodified.
- When `isOverworld()` is `false`, the sky color SHALL be calculated from the planet's explicit `getRGB()` values, with the same day/night `intensity` multiplier and rain/thunder desaturation applied as in the `getSkyColor` biome path.

When no `PlanetProperties` entry exists, the renderer SHALL fall back to the `genesis:overworld` (biome sampling) path.

#### Scenario: Overworld palette uses biome sampling
- **WHEN** the current planet's `PlanetColorPalette` has `isOverworld() == true`
- **THEN** sky color is sourced from `CubicSampler.gaussianSampleVec3` over biome sky colors, as before

#### Scenario: RGB palette uses explicit color
- **WHEN** the current planet's `PlanetColorPalette` has `isOverworld() == false`
- **THEN** sky color is derived from `getRGB()` with day/night intensity and rain/thunder adjustments applied

#### Scenario: Rain desaturates RGB sky
- **WHEN** `isOverworld() == false` and `level.getRainLevel(partialTick) > 0`
- **THEN** the RGB sky color is desaturated by the same formula used in `getSkyColor`

#### Scenario: Thunder darkens RGB sky
- **WHEN** `isOverworld() == false` and `level.getThunderLevel(partialTick) > 0`
- **THEN** the RGB sky color is darkened by the same formula used in `getSkyColor`

#### Scenario: No planet properties — fallback to biome sampling
- **WHEN** `PlanetProperties.get(celestial.getID())` returns `null`
- **THEN** sky color is sourced from biome sampling (Overworld-like fallback)

---

### Requirement: Atmospheric Density Scales Visual Effects
`PlanetDimensionEffects` SHALL use `PlanetProperties.density`, clamped to [0, 1], to scale atmospheric visual effects:

- **Sky color**: `density` SHALL be multiplied directly into all RGB components of the final sky color (both biome-sampled and explicit-RGB paths). At `density = 0`, sky color is black; at `density = 1`, sky color is unmodified.
- **Star brightness**: `density` SHALL control how much the atmosphere suppresses stars during the day. The effective star brightness SHALL be `lerp(density, 1.0, starBrightness)`, where `starBrightness` is the existing sun-angle-derived value. At `density = 0` (no atmosphere), stars are always at full brightness. At `density = 1` (full atmosphere), star brightness follows the normal daytime-dimming formula. Intermediate values produce partial suppression.
- **Sunrise/sunset and fog color**: `density` SHALL scale the sunrise/sunset RGBA and fog color so vacuum worlds show no atmospheric scattering effects.

When no `PlanetProperties` entry exists, `density` SHALL default to `1.0`.

#### Scenario: Vacuum planet — sky is black
- **WHEN** `density = 0`
- **THEN** sky color RGB components are all 0 (black sky, regardless of palette or time of day)

#### Scenario: Earth-like density — sky color unaffected
- **WHEN** `density = 1.0`
- **THEN** sky color is identical to the pre-density result (multiplying by 1.0 changes nothing)

#### Scenario: Partial density — sky is darkened proportionally
- **WHEN** `0 < density < 1`
- **THEN** sky color components are scaled by `density` (e.g., density=0.5 produces half the normal sky brightness)

#### Scenario: Vacuum planet — stars visible at midday
- **WHEN** `density = 0` and the sun is directly overhead
- **THEN** star brightness is 1.0 (no atmospheric scattering to wash out stars)

#### Scenario: Earth-like density — normal star dimming
- **WHEN** `density = 1.0` and the sun is above the horizon
- **THEN** star brightness behaves exactly as before (lerp with density=1 returns `starBrightness` unchanged)

#### Scenario: Thin atmosphere — partial star suppression at noon
- **WHEN** `density = 0.3` and it is midday (starBrightness ≈ 0)
- **THEN** effective star brightness ≈ `lerp(0.3, 1.0, 0.0)` = 0.7 (stars dimmed but still visible)

#### Scenario: No planet properties — density defaults to 1.0
- **WHEN** `PlanetProperties.get(celestial.getID())` returns `null`
- **THEN** visual effects behave as if `density = 1.0`

#### Scenario: Vacuum planet — no sunrise or sunset
- **WHEN** `density = 0`
- **THEN** sunrise/sunset color RGBA components are all 0 (no atmospheric scattering glow)

---

### Requirement: PlanetDimensionEffects obtains celestial context via VantagePoint
`PlanetDimensionEffects` SHALL construct a `VantagePoint` via `VantagePoint.get(...)` to obtain the current planet context for sky and fog color calculations. It SHALL NOT call `getCelestialForLevel` or `isSpaceDimension` directly. It SHALL extract the underlying `Celestial` from `VantagePoint.OnCelestial` when celestial-specific data (e.g. color palette, density, nearest star) is needed.

#### Scenario: Sky rendering on a planet
- **WHEN** `VantagePoint.get(...)` returns `VantagePoint.OnCelestial`
- **THEN** `PlanetDimensionEffects` extracts the `Celestial` and proceeds with planet-specific sky color, density, and star brightness calculations

#### Scenario: Sky rendering in unrelated dimension
- **WHEN** `VantagePoint.get(...)` returns `null`
- **THEN** `PlanetDimensionEffects` falls back to vanilla sky rendering behavior

### Requirement: Fog and atmosphere rendering uses VantagePoint for observer position
`FogRendererMixin` SHALL obtain the observer's position and rotation for fog/atmosphere calculations by constructing a `VantagePoint` via `VantagePoint.get(...)` and extracting the underlying `Celestial` (for `getDayTime`, `getNearestStar`, etc.) only when the result is `instanceof VantagePoint.OnCelestial`. It SHALL NOT call `getCelestialForLevel` or `isSpaceDimension` directly for this purpose.

#### Scenario: Fog mixin on a planet
- **WHEN** the current level has an associated celestial
- **THEN** `VantagePoint.get(...)` returns `VantagePoint.OnCelestial`, the mixin extracts the `Celestial` via `oc.celestial()`, and uses it for `getDayTime` and `getNearestStar` lookups

#### Scenario: Fog mixin in space
- **WHEN** the current level is the space dimension
- **THEN** `VantagePoint.get(...)` returns `VantagePoint.InSpace`, the instanceof check for `OnCelestial` fails, and the mixin skips celestial-specific fog logic

#### Scenario: Fog mixin in unrelated dimension
- **WHEN** `VantagePoint.get(...)` returns `null`
- **THEN** the mixin skips all space-aware fog logic, producing vanilla fog behavior
