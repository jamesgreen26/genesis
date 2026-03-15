## Why

The codebase inconsistently represents the observer's position in space: some systems pass a `@Nullable Celestial` called `vantagePoint` (which conflates "which planet are we on?" with "where is the camera?"), while others call `GenesisMod.getCelestialForLevel` / `isSpaceDimension` ad-hoc. The new `VantagePoint` interface cleanly models all observer states (on a celestial, or in open space) and exposes position/rotation directly, but is not yet used anywhere.

## What Changes

- Replace all `@Nullable Celestial vantagePoint` parameters in the renderer pipeline (`CelestialRenderer`, `CelestialRenderDispatcher`, `BlackholeRenderer`, `StarRenderer`, `PlanetRenderer`, `PlanetAtmosphereRenderer`) with `@Nullable VantagePoint`
- Update `CelestialRenderDispatcher` to construct a `VantagePoint` via `VantagePoint.get(...)` instead of calling `getCelestialForLevel` directly
- Update `FogRendererMixin` to build a `VantagePoint` and extract the underlying `Celestial` only when needed (e.g. for `getDayTime`, `getNearestStar`)
- Update `PlanetDimensionEffects` sky rendering to obtain its celestial context via `VantagePoint` rather than calling `getCelestialForLevel` directly
- Update non-rendering systems (`LevelMixin`, nav projector, teleporters, radar) to use `VantagePoint.get(...)` where they currently call `getCelestialForLevel` / `isSpaceDimension` for observer-position purposes
- **BREAKING**: `CelestialRenderer.invoke`, `setup`, and `teardown` signatures change from `@Nullable Celestial vantagePoint` to `@Nullable VantagePoint vantagePoint`

## Capabilities

### New Capabilities
- `vantage-point-rendering`: Defines how the observer's position in space (`VantagePoint`) is threaded through the celestial rendering pipeline, replacing the previous ad-hoc `Celestial` vantage point pattern.

### Modified Capabilities
- `planet-sky-rendering`: The sky/fog rendering path now receives a `VantagePoint` instead of a raw `Celestial`, changing how atmosphere and fog determine whether the camera is on the same body being rendered.

## Impact

- `CelestialRenderer` interface (breaking signature change — all implementors must update)
- `CelestialRenderDispatcher` (construction site for `VantagePoint`)
- `BlackholeRenderer`, `StarRenderer`, `PlanetRenderer`, `PlanetAtmosphereRenderer` (parameter type swap)
- `FogRendererMixin` (extract `Celestial` from `VantagePoint.OnCelestial` for `getDayTime`/`getNearestStar`)
- `PlanetDimensionEffects` (obtain celestial via `VantagePoint` for sky/fog color calculations)
- `LevelMixin`, `NavProjectorBlockEntityRenderer`, teleportation classes, radar (replace ad-hoc `getCelestialForLevel`/`isSpaceDimension` calls with `VantagePoint.get(...)`)
