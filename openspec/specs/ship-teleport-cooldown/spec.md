### Requirement: Space target is outside celestial OBB
`PlanetToSpaceTeleporter` SHALL compute a space-side target position whose distance from the celestial centre exceeds the celestial's OBB radius, so that `SpaceToPlanetTeleporter` cannot detect the ship as overlapping the celestial on the tick immediately following arrival.

#### Scenario: Ship arrives in space outside OBB
- **WHEN** a ship is teleported from a planet dimension to the space dimension
- **THEN** its world position in space SHALL not overlap the source celestial's OBB

#### Scenario: Ship is not immediately returned to planet
- **WHEN** a ship arrives in space after a planet-to-space teleport
- **THEN** `SpaceToPlanetTeleporter` SHALL NOT teleport it back to a planet on the same tick or the immediately following tick

#### Scenario: Gametest verifies no OBB overlap after atmosphere exit
- **WHEN** the `atmosphereExit` gametest runs and the ship reaches the space dimension
- **THEN** the test SHALL assert that the ship's world-space OBB does not overlap the source celestial's OBB

---

### Requirement: Per-ship teleport cooldown of 20 ticks
`DimensionTravelTeleporter` SHALL prevent a ship from being submitted for teleportation more than once within a 20-game-tick window.

#### Scenario: Ship in cooldown is not re-submitted
- **WHEN** a ship has been submitted for teleportation
- **THEN** any further teleportation requests for that ship SHALL be ignored for 20 game ticks

#### Scenario: Ship is eligible after cooldown expires
- **WHEN** 20 game ticks have elapsed since a ship's last teleportation submission
- **THEN** the ship SHALL be eligible for teleportation again

---

### Requirement: Atomic group admission
`DimensionTravelTeleporter` SHALL mark a collected ship group as in-cooldown only if every ship in the group passes the cooldown check. If any ship in the group is in cooldown, no ship in the group SHALL be marked.

#### Scenario: Partial group in cooldown blocks whole group without side effects
- **WHEN** a collected ship group contains at least one ship already in cooldown
- **THEN** no ship in the group SHALL be added to the cooldown map
- **THEN** all ships in the group SHALL remain eligible for teleportation on a future tick

#### Scenario: Clean group is fully marked and teleported
- **WHEN** every ship in a collected group passes the cooldown check
- **THEN** all ships in the group SHALL be marked in the cooldown map
- **THEN** all ships in the group SHALL be teleported

---

### Requirement: Static ships excluded from automatic teleportation
`PlanetToSpaceTeleporter` and `SpaceToPlanetTeleporter` SHALL skip any ship for which `isStatic()` returns `true` during their per-tick scans.

#### Scenario: Static ship above atmosphere is not teleported to space
- **WHEN** a static ship's world Y position exceeds `atmosphereExitHeight`
- **THEN** `PlanetToSpaceTeleporter` SHALL NOT submit it for teleportation

#### Scenario: Static ship overlapping celestial OBB is not teleported to planet
- **WHEN** a static ship's OBB overlaps a celestial's OBB in the space dimension
- **THEN** `SpaceToPlanetTeleporter` SHALL NOT submit it for teleportation
