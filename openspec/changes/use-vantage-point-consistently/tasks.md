## 1. Update CelestialRenderer interface

- [x] 1.1 Change `invoke` signature: replace `@Nullable Celestial vantagePoint` with `@Nullable VantagePoint vantagePoint`
- [x] 1.2 Change `setup` default method signature to `@Nullable VantagePoint vantagePoint`
- [x] 1.3 Change `teardown` default method signature to `@Nullable VantagePoint vantagePoint`

## 2. Update CelestialRenderDispatcher

- [x] 2.1 Replace `Celestial vantagePoint = GenesisMod.getCelestialForLevel(level)` with `VantagePoint vantagePoint = VantagePoint.get(level, new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z), ticks, partialTick)`
- [x] 2.2 Update the null/space guard condition to use `vantagePoint != null` (VantagePoint.get already handles both space and on-celestial cases non-null, returning null only for unrelated dimensions)

## 3. Update renderer implementations

- [x] 3.1 `BlackholeRenderer`: update `invoke` parameter type from `@Nullable Celestial` to `@Nullable VantagePoint`
- [x] 3.2 `StarRenderer`: update `invoke` parameter type from `@Nullable Celestial` to `@Nullable VantagePoint`
- [x] 3.3 `PlanetRenderer`: update `invoke` parameter type; change `vantagePoint.equals(toRender)` to `vantagePoint instanceof VantagePoint.OnCelestial oc && oc.celestial().equals(toRender)`
- [x] 3.4 `PlanetAtmosphereRenderer`: update `invoke` and `renderAtmosphere` parameter types; change equality check to instanceof pattern match (same as 3.3)

## 4. Update FogRendererMixin

- [x] 4.1 Replace `Celestial vantagePoint = GenesisMod.getCelestialForLevel(instance)` calls with `VantagePoint vantagePoint = VantagePoint.get(instance, new Vector3d(...camera pos...), ticks, partialTick)`
- [x] 4.2 Extract `Celestial` from `VantagePoint.OnCelestial` before calling `getDayTime` and `getNearestStar`: `if (vantagePoint instanceof VantagePoint.OnCelestial oc) { Celestial celestial = oc.celestial(); ... }`
- [x] 4.3 Remove any remaining direct `getCelestialForLevel` / `isSpaceDimension` calls that were used for observer position

## 5. Update PlanetDimensionEffects

- [x] 5.1 Replace `Celestial celestial = GenesisMod.getCelestialForLevel(level)` with `VantagePoint vp = VantagePoint.get(level, ...); Celestial celestial = vp instanceof VantagePoint.OnCelestial oc ? oc.celestial() : null`
- [x] 5.2 Remove any remaining direct `getCelestialForLevel` / `isSpaceDimension` calls used for sky/fog context

## 6. Update non-rendering systems

- [x] 6.1 `LevelMixin`: replace `getCelestialForLevel` with `VantagePoint.get(...)` + `instanceof OnCelestial` extraction for `getDayTime`
- [x] 6.2 `NavProjectorBlockEntityRenderer`: replace `getCelestialForLevel` with `VantagePoint.get(...)` + extract celestial for rotation/position lookups
- [x] 6.3 Teleportation classes (`SpaceToPlanetTeleporter`, `PlanetToSpaceTeleporter`): replace `getCelestialForLevel` / `isSpaceDimension` with `VantagePoint.get(...)` for source-celestial detection
- [x] 6.4 `RadarDisplay`: replace `isSpaceDimension` check with `VantagePoint.get(...)` null/type check

## 7. Verify

- [x] 7.1 Confirm no `@Nullable Celestial vantagePoint` parameters remain in any renderer class
- [x] 7.2 Confirm no class calls `getCelestialForLevel` or `isSpaceDimension` for observer-position purposes
- [x] 7.3 Build the project and resolve any compile errors
