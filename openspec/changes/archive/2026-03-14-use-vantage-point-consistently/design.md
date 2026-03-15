## Context

The rendering pipeline currently passes a `@Nullable Celestial` called `vantagePoint` through `CelestialRenderer.invoke/setup/teardown`, `PlanetRenderer`, `PlanetAtmosphereRenderer`, etc. This value is obtained at the call site via `GenesisMod.getCelestialForLevel(level)`, which returns `null` when the player is in the space dimension. The pattern works but is semantically wrong: `Celestial` is a game-world body, not a camera position. It also cannot natively represent the "in space" case without null checks scattered throughout renderers.

`VantagePoint` (already written) solves this with two sealed implementations:
- `VantagePoint.InSpace` — player is in the space dimension (no parent celestial)
- `VantagePoint.OnCelestial(celestial, cameraRotationFromNorthPole, ticks, partialTick)` — player is on a planet

Both expose `getPosition()` and `getRotation()`, the only two values renderers actually need for camera math.

## Goals / Non-Goals

**Goals:**
- Thread `VantagePoint` through the entire celestial rendering pipeline instead of `@Nullable Celestial`
- Remove all renderer-internal calls to `getCelestialForLevel` / `isSpaceDimension`
- Update `FogRendererMixin` to obtain its celestial-specific data (`getDayTime`, `getNearestStar`) by extracting `Celestial` from `VantagePoint.OnCelestial`
- Update `PlanetDimensionEffects` sky rendering to obtain its celestial via `VantagePoint` rather than calling `getCelestialForLevel` directly
- Update non-rendering systems (`LevelMixin`, `NavProjectorBlockEntityRenderer`, teleporters, radar) to use `VantagePoint.get(...)` wherever they currently call `getCelestialForLevel` / `isSpaceDimension` for observer-position context

**Non-Goals:**
- Changing the `VantagePoint` interface itself

## Decisions

### Keep `@Nullable VantagePoint` (allow null) in renderer signatures
The null case means "not in a space-aware dimension at all" (e.g. a completely unrelated mod dimension). Renderers already guard on this; keeping nullable preserves that contract without introducing a `VantagePoint.None` type.

**Alternative considered:** Non-nullable with a `VantagePoint.None` singleton. Rejected because it would require all renderers to handle a third case with no practical difference from null-guarding.

### Construct `VantagePoint` once at the dispatcher, not in each renderer
`CelestialRenderDispatcher.onRenderLevel` is the single entry point for all celestial rendering. Building the `VantagePoint` there and passing it down avoids redundant construction and keeps renderers stateless.

### Extract `Celestial` from `VantagePoint.OnCelestial` in systems that need it
`FogRendererMixin`, `PlanetDimensionEffects`, `LevelMixin`, and `NavProjectorBlockEntityRenderer` call methods like `getDayTime()`, `getNearestStar()`, `getPosition()`, `getRotation()` that are `Celestial`-specific. Rather than adding these to `VantagePoint`, callers pattern-match `instanceof VantagePoint.OnCelestial` and extract the celestial field. This keeps `VantagePoint` minimal.

### Non-rendering systems migrate too
Teleporters, radar, the nav projector, and `LevelMixin` all call `getCelestialForLevel` / `isSpaceDimension` for observer-context decisions (e.g. which planet to teleport from/to, what day-time to report). These should be replaced with `VantagePoint.get(...)` + extraction, for the same reason as renderers: `VantagePoint` is the single source of truth for observer state.

### "Am I rendering myself?" check uses `instanceof OnCelestial`
`PlanetRenderer` and `PlanetAtmosphereRenderer` skip certain effects when the camera is on the planet being rendered. Currently: `vantagePoint.equals(toRender)`. New pattern:
```java
vantagePoint instanceof VantagePoint.OnCelestial oc && oc.celestial().equals(toRender)
```

## Risks / Trade-offs

- **BREAKING change to `CelestialRenderer`**: Any external mod implementing this interface will fail to compile. → Accepted; this is an internal mod interface not currently published as an API.
- **`FogRendererMixin` complexity**: The mixin now needs an instanceof check before accessing `Celestial`. → Low risk; pattern is straightforward Java 16+ pattern matching.

## Migration Plan

1. Update `CelestialRenderer` interface signatures (breaking)
2. Update `CelestialRenderDispatcher` to call `VantagePoint.get()`
3. Update all `CelestialRenderer` implementors (BlackholeRenderer, StarRenderer, PlanetRenderer, PlanetAtmosphereRenderer) — type swap + instanceof checks where needed
4. Update `FogRendererMixin` — build VantagePoint, extract Celestial for getDayTime/getNearestStar
5. Update `PlanetDimensionEffects` — obtain celestial via VantagePoint for sky/fog color calculations
6. Update non-rendering systems (LevelMixin, NavProjectorBlockEntityRenderer, teleporters, radar) — replace getCelestialForLevel/isSpaceDimension with VantagePoint.get() + extraction
7. Compile and verify no remaining ad-hoc getCelestialForLevel/isSpaceDimension calls for observer-position purposes
