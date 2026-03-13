# Space Registry

The space registry is the authoritative in-memory store for all celestial bodies in a Genesis world. It manages loading, baking, syncing, and querying of `Celestial` objects across the server and client.

---

## Celestial Bodies

### Requirement: Celestial Identity
Every celestial body SHALL have a unique `ResourceLocation` ID used as its registry key. Duplicate IDs are silently ignored — the first registration wins.

#### Scenario: Unique ID registration
- **WHEN** two celestials are registered with the same ID
- **THEN** only the first is stored; the second is discarded

---

### Requirement: Celestial Properties
Every celestial body SHALL carry the following properties:

| Property | Type | Description |
|---|---|---|
| `ID` | `ResourceLocation` | Unique identifier |
| `type` | `CelestialType` | Governs light, shadow, visitability, and renderer |
| `size` | `double` | Size multiplier; actual size = `size × BASE_SIZE` (96 blocks) |
| `gravity` | `double` | Gravity multiplier |
| `r`, `g`, `b` | `float` | RGB color (each defaults to `0.5` if omitted) |
| `transformProvider` | `CelestialTransformProvider` | Computes position and rotation each tick |

#### Scenario: Default color
- **WHEN** a celestial is defined without `r`, `g`, or `b` fields
- **THEN** the omitted channels each default to `0.5`

#### Scenario: Actual size calculation
- **WHEN** a celestial has `size = 2.0`
- **THEN** its actual world-space size is `192` blocks (`2.0 × 96`)

---

### Requirement: Celestial Spatial Queries
The registry SHALL support querying celestials by position and type predicate, returning the nearest match and its distance.

#### Scenario: Nearest star lookup
- **WHEN** a celestial calls `getNearestStar(gameTime, partialTick)`
- **THEN** the registry returns the closest celestial whose type is `genesis:star`

#### Scenario: No stars present
- **WHEN** there are no stars in the registry
- **THEN** `getNearestStar` throws `IllegalStateException`

---

### Requirement: Day/Sun Calculations
Each celestial body SHALL be able to compute a local day time and sun elevation based on the direction to the nearest star and its own rotation.

#### Scenario: Day time mapping
- **WHEN** the nearest star is directly "above" a celestial (sun dot = 1.0)
- **THEN** `getDayTime` returns approximately `6000` (midday in Minecraft ticks)

#### Scenario: Sun dot
- **WHEN** `getSunDot` is called
- **THEN** it returns the dot product of the celestial's up vector with the direction to the nearest star, ranging from `-1.0` to `1.0`

---

## Celestial Types

### Requirement: Celestial Type Registry
Celestial types SHALL be registered by `ResourceLocation` before they are referenced by any celestial definition. Duplicate type IDs are silently ignored.

#### Scenario: Type lookup
- **WHEN** a celestial is deserialized with `type = "genesis:star"`
- **THEN** `CelestialType.get(...)` returns the `STAR` type instance

#### Scenario: Unknown type
- **WHEN** a celestial definition references an unregistered type ID
- **THEN** `CelestialType.get(...)` returns `null`, which will cause downstream errors

---

### Requirement: Builtin Celestial Types
The mod SHALL provide three built-in celestial types:

| ID | Casts Light | Casts Shadow | Visitable | Renderer |
|---|---|---|---|---|
| `genesis:star` | yes | no | no | `StarRenderer` |
| `genesis:body` | no | yes | yes | `PlanetRenderer` |
| `genesis:blackhole` | no | no | no | `BlackholeRenderer` |

#### Scenario: Star properties
- **WHEN** a celestial's type is `genesis:star`
- **THEN** `castsLight()` is `true`, `castsShadow()` is `false`, `isVisitable()` is `false`

#### Scenario: Body (planet) properties
- **WHEN** a celestial's type is `genesis:body`
- **THEN** `castsShadow()` is `true` and `isVisitable()` is `true`

#### Scenario: Blackhole properties
- **WHEN** a celestial's type is `genesis:blackhole`
- **THEN** `castsLight()`, `castsShadow()`, and `isVisitable()` are all `false`

---

## Transform Providers

### Requirement: Transform Provider Interface
Every celestial body SHALL have a `CelestialTransformProvider` that computes its position (`Vector3d`) and rotation (`Quaterniondc`) as a function of `ticks` and `partialTick`. Providers MUST be registered with a `ResourceLocation` type key and a Mojang codec before being used in data files.

#### Scenario: Position at tick
- **WHEN** `getPosition(ticks, partialTick)` is called on any provider
- **THEN** a deterministic `Vector3d` is returned for that tick value

#### Scenario: Unregistered provider type
- **WHEN** a data file references a transform provider type not in `CelestialTransformProvider.REGISTRY`
- **THEN** deserialization throws `IllegalArgumentException`

---

### Requirement: Static Transform Provider (`genesis:static`)
A static provider SHALL hold a fixed position (`x`, `y`, `z`) and rotation expressed as Euler angles (`xRot`, `yRot`, `zRot` in radians, all defaulting to `0`). Position and rotation do not change over time.

#### Scenario: Fixed position
- **WHEN** `getPosition` is called at any tick
- **THEN** it always returns `(x, y, z)` unchanged

#### Scenario: Default rotation
- **WHEN** `xRot`, `yRot`, and `zRot` are omitted
- **THEN** the rotation is the identity quaternion

---

### Requirement: Orbiting Transform Provider (`genesis:orbiting`)
An orbiting provider SHALL compute a time-varying position around a parent celestial using Keplerian-style spherical coordinates. It SHALL also apply a time-varying rotation (daily spin around the Y axis).

Parameters:

| Field | Type | Default | Description |
|---|---|---|---|
| `parentID` | `ResourceLocation` | required | ID of the parent celestial to orbit |
| `seed` | `int` | required | Seed for deterministic random orbital angles |
| `orbitDistance` | `double` | required | Orbit radius multiplier × `BASE_ORBIT_DISTANCE` (15,000 blocks) |
| `orbitTime` | `double` | required | Orbital period multiplier × `BASE_ORBIT_TIME` (4,608,000 ticks) |
| `dayLength` | `double` | `1.0` | Day length multiplier × `BASE_DAY_LENGTH` (24,000 ticks) |

#### Scenario: Orbital position advances over time
- **WHEN** `getPosition` is called at increasing tick values
- **THEN** the returned position traces a circle around the parent's position at the configured radius

#### Scenario: Day length floored
- **WHEN** `dayLength` is set to `0` or a negative value
- **THEN** the effective day length is clamped to `0.001` to prevent division by zero

#### Scenario: Deterministic from seed
- **WHEN** two providers are created with the same `seed`
- **THEN** their orbital angles and initial state are identical

#### Scenario: Parent position included
- **WHEN** the parent celestial moves (e.g., is itself orbiting)
- **THEN** the child's position is computed relative to the parent's current position

---

## Data Loading

### Requirement: System Config Data Files
Celestials SHALL be loadable from JSON resource files placed under any resource pack in the `system_config/` directory. Each file SHALL contain a `SystemConfigModel` (a `celestials` array). Multiple files from multiple resource packs are all loaded and merged.

#### Scenario: Valid config file loaded
- **WHEN** a valid JSON file exists at `<namespace>:system_config/<path>`
- **THEN** all celestials in that file are registered into the space registry on resource reload

#### Scenario: Invalid config file
- **WHEN** a JSON file fails to parse or decode
- **THEN** an error is logged and that file is skipped; other files are unaffected

#### Scenario: Multiple config files
- **WHEN** multiple `system_config` files exist across resource packs
- **THEN** all are merged into the registry (first-registration-wins for ID conflicts)

---

### Requirement: Registry Lifecycle
The registry SHALL be cleared and re-baked on every resource reload, and cleared again when the server stops.

#### Scenario: Resource reload triggers re-bake
- **WHEN** Minecraft reloads resources (e.g., `/reload`)
- **THEN** the registry is cleared, all data files re-parsed, all registration callbacks re-fired, and the updated registry synced to all connected clients

#### Scenario: Server stop clears registry
- **WHEN** the server fires `ServerStoppedEvent`
- **THEN** the registry is cleared

---

### Requirement: Programmatic Registration
Mods and addon code SHALL be able to register celestials programmatically by subscribing a `Consumer<RegisterCelestialsEvent>` callback. Callbacks are fired during each bake.

#### Scenario: Callback fires on bake
- **WHEN** a registration callback is registered before the first bake
- **THEN** it is called every time the registry bakes, and any celestials it registers are included

---

## Client Synchronization

### Requirement: Server-to-Client Registry Sync
The server SHALL sync the full registry to clients using `SpaceRegistrySyncPacket` encoded as NBT. The packet is sent to all online players on each bake, and to any player who joins after a bake.

#### Scenario: Sync on bake
- **WHEN** the registry finishes baking on the server
- **THEN** a `SpaceRegistrySyncPacket` is sent to every online player

#### Scenario: Sync on player join
- **WHEN** a player logs in
- **THEN** a `SpaceRegistrySyncPacket` is sent to that player with the current registry state

#### Scenario: Client receives sync
- **WHEN** a client receives a `SpaceRegistrySyncPacket`
- **THEN** its local registry is cleared and repopulated from the packet data

---

## Registry Queries

### Requirement: Registry Lookup API
The registry SHALL expose three query methods:

- `get(ResourceLocation)` — returns a single celestial by ID, or `null`
- `getAll()` — returns all registered celestials as a list
- `getWhere(Predicate<CelestialType>)` — returns all celestials whose type matches the predicate

#### Scenario: Get by ID
- **WHEN** `get("genesis:earth")` is called and that celestial is registered
- **THEN** the matching `Celestial` is returned

#### Scenario: Get by ID missing
- **WHEN** `get("genesis:unknown")` is called
- **THEN** `null` is returned

#### Scenario: Filter by type
- **WHEN** `getWhere(type -> type.castsLight())` is called
- **THEN** only celestials whose type returns `true` for `castsLight()` are returned
