## 1. Fix space target position in PlanetToSpaceTeleporter

- [x] 1.1 In `PlanetToSpaceTeleporter.computeSpaceTarget`, change the radial offset from `actualSize * 0.7 + 20` to `actualSize + 20` so the target is guaranteed to be outside the celestial's OBB

## 2. Replace 1-tick cooldown with 20-tick per-ship cooldown in DimensionTravelTeleporter

- [x] 2.1 Replace `ConcurrentHashMap<Long, Boolean> shipsTeleportingThisTick` and `currentTickTime` with a `ConcurrentHashMap<Long, Long> shipCooldownExpiry` (ship ID → expiry game tick)
- [x] 2.2 Remove `resetIfNewTick` and the `lock` object; they are no longer needed
- [x] 2.3 Rewrite `shouldSkip(long shipId, long currentTick)` to return `currentTick < shipCooldownExpiry.getOrDefault(shipId, 0L)` (pure read, no side effect)
- [x] 2.4 Add `markShip(long shipId, long currentTick)` that sets `shipCooldownExpiry.put(shipId, currentTick + 20)`

## 3. Implement atomic two-phase group admission in DimensionTravelTeleporter

- [x] 3.1 In `teleportShip`, replace the single `shouldSkip`-and-return loop with a **check pass**: iterate `ships.keySet()` and `return` early (without marking anything) if any ship returns `true` from `shouldSkip`
- [x] 3.2 After the check pass, add a **mark pass**: iterate `ships.keySet()` and call `markShip` for each one
- [x] 3.3 Update the initial early-exit check at the top of `teleportShip` (for the root ship ID) to use the new `shouldSkip(id, gameTime)` signature

## 4. Add isStatic() guards

- [x] 4.1 In `PlanetToSpaceTeleporter.tick`, skip ships where `ship.isStatic()` is `true` (add the check alongside the `y() > atmosphereExitHeight` condition)
- [x] 4.2 In `SpaceToPlanetTeleporter.tick`, skip ships where `ship.isStatic()` is `true` (add the check alongside the `nearest == null || shipAABB == null` guard)

## 5. Extend gametests

- [x] 5.1 In `TeleportGameTests.atmosphereExit`, after the ship's `chunkClaimDimension` equals the space dimension, add a second assertion: retrieve the ship's OBB from the space-level ship world, construct its world-space OBB via `OBB.fromShip`, and assert it does NOT overlap the overworld celestial's `getOBB(ticks)`
