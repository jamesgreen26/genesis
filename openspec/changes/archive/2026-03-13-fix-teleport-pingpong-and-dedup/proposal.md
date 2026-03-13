## Why

A recent refactor of the teleportation system introduced three correctness bugs: ships ping-pong between planet and space dimensions every two ticks, connected ship groups can be silently dropped during teleportation, and static ships are no longer filtered from automatic teleportation. These bugs cause the gametest suite to fail.

## What Changes

- **PlanetToSpaceTeleporter**: `computeSpaceTarget` must place ships at a position guaranteed to be outside the celestial's OBB, so `SpaceToPlanetTeleporter` does not immediately pull them back.
- **DimensionTravelTeleporter**: Replace the 1-tick ship cooldown with a 20-tick per-ship cooldown, preventing repeated teleport submissions while VS processes the move.
- **DimensionTravelTeleporter**: Separate the cooldown check from the cooldown write in `teleportShip`, so no ship is marked as in-cooldown unless the entire collected group is confirmed clear and the teleport will actually execute.
- **PlanetToSpaceTeleporter**: Skip ships where `isStatic()` is true.
- **SpaceToPlanetTeleporter**: Skip ships where `isStatic()` is true.

## Capabilities

### New Capabilities
- `ship-teleport-cooldown`: Per-ship teleportation cooldown enforced across multiple ticks, with atomic check-and-mark semantics for ship groups.

### Modified Capabilities

## Impact

- `DimensionTravelTeleporter` — cooldown map and reset logic rewritten
- `PlanetToSpaceTeleporter` — `computeSpaceTarget` and ship-skip predicate updated
- `SpaceToPlanetTeleporter` — ship-skip predicate updated
- Gametests: `atmosphereExit`, `entityTeleportsWithShip`, `connectedShipsTeleportTogether` expected to pass after these fixes
