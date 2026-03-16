## Context

The teleportation system moves VS ships between the planet and space dimensions via two tick-based listeners (`PlanetToSpaceTeleporter`, `SpaceToPlanetTeleporter`) that both delegate to `DimensionTravelTeleporter.teleportShip`. Three correctness problems were introduced in the most recent refactor.

**Ping-pong**: `PlanetToSpaceTeleporter.computeSpaceTarget` places ships at `actualSize * 0.7 + 20` from the planet centre. `SpaceToPlanetTeleporter.shipOverlapsCelestial` uses OBB overlap, and because the celestial OBB extends to at least `actualSize`, a ship at `0.7 * actualSize + 20` (well inside the OBB for any realistically-sized celestial) is detected on the very next tick and teleported back. `SpaceToPlanetTeleporter.computePlanetTarget` already has the inverse guarantee: it places ships at `atmosphereEntryHeight`, which is guaranteed to be inside the atmosphere and outside the space-trigger zone. `computeSpaceTarget` needs the same kind of guarantee in the opposite direction.

**Cooldown too short**: The cooldown set (`shipsTeleportingThisTick`) is reset every game tick. VS teleportation takes several ticks to finalise, so the same ship can be submitted on tick N, N+1, N+2, … until VS processes the move. A 20-tick cooldown matches the typical VS processing window and prevents redundant submissions.

**Atomic group marking**: `shouldSkip` uses `ConcurrentHashMap.putIfAbsent` as a combined check-and-mark. The current loop marks ships as a side effect before confirming that the entire group is clear. If any ship in the group is already in cooldown the method returns, leaving the earlier ships marked but not teleported—they are then blocked from teleportation for the remainder of the cooldown. The fix is to check all ships first, then mark them only if the whole group passes.

## Goals / Non-Goals

**Goals:**
- Ships teleported to space do not get immediately pulled back by `SpaceToPlanetTeleporter`.
- A ship cannot be submitted for teleportation more than once per 20-tick window.
- All ships in a collected group are either all marked-and-teleported, or none are marked.
- Static ships are excluded from automatic teleportation in both directions.

**Non-Goals:**
- Changing the physics or trajectory of ships in transit.
- Modifying the VS joint constraint collection logic.
- Altering how entities are collected or teleported alongside ships.

## Decisions

### D1 — Fix ping-pong by moving the space target outside the celestial OBB

`computeSpaceTarget` currently derives the target from `actualSize * 0.7`, which is inside the celestial OBB. The target must be placed beyond `actualSize` so `shipOverlapsCelestial` cannot trigger.

**Chosen approach**: Set the radial offset to `actualSize + <margin>` (a small constant buffer, e.g. 20 blocks). This mirrors how `SpaceToPlanetTeleporter` uses `atmosphereEntryHeight` to guarantee a safe landing zone.

**Alternative considered**: Re-introduce a cross-direction cooldown map (like the old `LAUNCHING`). Rejected — it adds shared mutable state across two classes and the positional fix is simpler, more robust, and easier to reason about.

### D2 — 20-tick per-ship cooldown stored in a `ConcurrentHashMap<Long, Long>`

Replace the boolean set (`ConcurrentHashMap<Long, Boolean>`) with a map from ship ID to the game-tick at which cooldown expires (`shipId → expiryTick`). A ship is in cooldown if `currentTick < expiryTick`. On admission, set `expiryTick = currentTick + 20`. No global reset is needed; stale entries are simply ignored and can be cleaned up lazily.

**Why 20 ticks**: VS typically processes a teleport within 1–3 game ticks, but the tick-event order can delay observation. 20 ticks (1 second) provides a safe margin without noticeably delaying legitimate re-teleportation.

**Alternative considered**: Keep the per-tick reset approach but increase the reset window. Rejected — the expiry-tick approach is self-cleaning and avoids a synchronised global reset.

### D3 — Two-phase check-then-mark for group admission

Replace the single `shouldSkip` loop (which marks as a side effect) with two passes:
1. **Check pass**: iterate `ships.keySet()` and return early (without marking anything) if any ship is in cooldown.
2. **Mark pass**: iterate `ships.keySet()` and register each ship in the cooldown map.

This guarantees that if any ship in the group is in cooldown, no ship in the group is marked, and all remain eligible for teleportation on a future tick.

## Risks / Trade-offs

- **Increased spacing after launch**: Ships will appear slightly further from the planet surface in space. This is intentional and consistent with the existing margin in `SpaceToPlanetTeleporter`. → No mitigation needed.
- **20-tick window may feel slow for edge cases**: If a ship is manually teleported back into the trigger zone within 20 ticks, it will not be auto-teleported. → Acceptable; 20 ticks is 1 second and this scenario is rare in normal play.
- **Lazy cleanup of cooldown map**: Expired entries accumulate until re-accessed. → At typical ship counts this is negligible; entries are overwritten on re-admission.
