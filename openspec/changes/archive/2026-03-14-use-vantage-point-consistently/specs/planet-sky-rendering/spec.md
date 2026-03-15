## MODIFIED Requirements

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
