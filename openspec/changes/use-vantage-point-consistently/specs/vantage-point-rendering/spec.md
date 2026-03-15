## ADDED Requirements

### Requirement: VantagePoint threads through the celestial renderer pipeline
The `CelestialRenderer` interface SHALL accept `@Nullable VantagePoint vantagePoint` (not `@Nullable Celestial`) in its `invoke`, `setup`, and `teardown` methods. All implementations SHALL update their signatures accordingly.

#### Scenario: Renderer receives VantagePoint in space
- **WHEN** the player is in the space dimension
- **THEN** renderers receive a `VantagePoint.InSpace` instance (non-null)

#### Scenario: Renderer receives VantagePoint on a celestial
- **WHEN** the player is on a planet
- **THEN** renderers receive a `VantagePoint.OnCelestial` instance (non-null) containing the celestial and camera rotation

#### Scenario: Renderer receives null VantagePoint
- **WHEN** the current dimension has no associated celestial and is not the space dimension
- **THEN** renderers receive `null` and SHALL skip space-aware rendering logic

### Requirement: CelestialRenderDispatcher constructs VantagePoint via factory
`CelestialRenderDispatcher` SHALL call `VantagePoint.get(level, cameraPos, ticks, partialTick)` to obtain the observer's vantage point, rather than calling `getCelestialForLevel` or `isSpaceDimension` directly.

#### Scenario: Dispatcher in space dimension
- **WHEN** the current level is the space dimension
- **THEN** `VantagePoint.get(...)` returns a non-null `VantagePoint.InSpace` and celestials are rendered

#### Scenario: Dispatcher on a planet
- **WHEN** the current level has an associated celestial
- **THEN** `VantagePoint.get(...)` returns a non-null `VantagePoint.OnCelestial` and celestials are rendered

#### Scenario: Dispatcher in unrelated dimension
- **WHEN** the current level is neither the space dimension nor associated with any celestial
- **THEN** `VantagePoint.get(...)` returns `null` and no celestials are rendered

### Requirement: Non-rendering systems use VantagePoint for observer context
Gameplay systems that make decisions based on which planet the player is on (`LevelMixin`, `NavProjectorBlockEntityRenderer`, teleporters, radar) SHALL use `VantagePoint.get(...)` instead of calling `getCelestialForLevel` / `isSpaceDimension` directly. They SHALL extract `Celestial` from `VantagePoint.OnCelestial` only when celestial-specific APIs (e.g. `getDayTime`, `getRotation`) are needed.

#### Scenario: LevelMixin getDayTime on a planet
- **WHEN** `VantagePoint.get(...)` returns `VantagePoint.OnCelestial`
- **THEN** `LevelMixin` extracts the celestial and calls `getDayTime` on it

#### Scenario: LevelMixin getDayTime in space or unrelated dimension
- **WHEN** `VantagePoint.get(...)` returns `VantagePoint.InSpace` or `null`
- **THEN** `LevelMixin` falls back to vanilla day-time behavior

#### Scenario: NavProjector in space
- **WHEN** `VantagePoint.get(...)` returns `VantagePoint.InSpace`
- **THEN** the nav projector renders without a planet-derived rotation reference

#### Scenario: Teleporter observes player on a planet
- **WHEN** `VantagePoint.get(...)` returns `VantagePoint.OnCelestial`
- **THEN** the teleporter correctly identifies the source celestial for the transition

### Requirement: "Rendering self" detection uses VantagePoint.OnCelestial pattern match
When a renderer needs to determine whether the camera is on the celestial currently being rendered (e.g. to skip close-up effects), it SHALL use `vantagePoint instanceof VantagePoint.OnCelestial oc && oc.celestial().equals(toRender)` rather than a direct `Celestial` equality check.

#### Scenario: Camera is on the planet being rendered
- **WHEN** `vantagePoint` is `OnCelestial` and its celestial equals `toRender`
- **THEN** the renderer treats this as "rendering from surface" and applies appropriate near-surface adjustments

#### Scenario: Camera is on a different planet
- **WHEN** `vantagePoint` is `OnCelestial` but its celestial does not equal `toRender`
- **THEN** the renderer treats this as a distant body and renders normally

#### Scenario: Camera is in space
- **WHEN** `vantagePoint` is `VantagePoint.InSpace`
- **THEN** the instanceof check fails and the renderer treats this as a distant body
