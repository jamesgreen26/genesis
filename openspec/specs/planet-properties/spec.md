## ADDED Requirements

### Requirement: Planet Properties Data Model
Each planet (celestial of type `genesis:body`) MAY have an associated `PlanetProperties` entry keyed by its `ResourceLocation` ID. The entry SHALL carry the following fields:

| Field | Type | Description |
|---|---|---|
| `density` | `double` | Atmospheric density (0 = vacuum, 1 = Earth-like) |
| `thickness` | `double` | Atmospheric thickness multiplier |
| `precipitation` | `boolean` | Whether weather/rain occurs on this planet |
| `color` | `PlanetColorPalette` | Sky/atmosphere color source |

#### Scenario: Property lookup for known planet
- **WHEN** `PlanetProperties.get("genesis:earth")` is called and properties are loaded
- **THEN** the matching `PlanetProperties` record is returned

#### Scenario: Property lookup for unknown planet
- **WHEN** `PlanetProperties.get(id)` is called for an ID with no properties defined
- **THEN** `null` is returned

---

### Requirement: Planet Color Palette
The `PlanetColorPalette` SHALL be a polymorphic type with two variants, serialized via a `"type"` dispatch field.

**`genesis:rgb`** — explicit RGB color with integer channels `r`, `g`, `b`.

**`genesis:overworld`** — a sentinel with no extra fields. Callers MUST check `isOverworld()` before calling `getRGB()`. When `isOverworld()` is `true`, the actual sky color is sourced from biome data at render time and `getRGB()` MUST NOT be called.

#### Scenario: RGB palette
- **WHEN** a planet's color is `{ "type": "genesis:rgb", "r": 180, "g": 90, "b": 50 }`
- **THEN** `isOverworld()` returns `false` and `getRGB()` returns `[180, 90, 50]`

#### Scenario: Overworld palette
- **WHEN** a planet's color is `{ "type": "genesis:overworld" }`
- **THEN** `isOverworld()` returns `true` and calling `getRGB()` throws `UnsupportedOperationException`

---

### Requirement: Planet Properties Data Files
Planet properties SHALL be loadable from JSON resource files placed under `system_config/planet_properties/` in any resource pack. Each file SHALL contain a list of entries, each with an `"id"` field mapping to a celestial `ResourceLocation` plus the property fields.

#### Scenario: Valid properties file loaded
- **WHEN** a valid JSON file exists at `<namespace>:system_config/planet_properties/<path>`
- **THEN** all entries in that file are registered into `PlanetProperties.PLANET_PROPERTIES` on resource reload

#### Scenario: Invalid properties file
- **WHEN** a JSON file fails to parse or decode
- **THEN** an error is logged and that file is skipped; other files are unaffected

#### Scenario: No conflict with celestial config loader
- **WHEN** files exist under `system_config/planet_properties/`
- **THEN** the celestial `DataLoader` does not attempt to parse them as `SystemConfigModel`

---

### Requirement: Planet Properties Registry Lifecycle
The planet properties registry SHALL be cleared and repopulated on every resource reload, in the same reload listener pass as the space registry.

#### Scenario: Resource reload triggers reload
- **WHEN** Minecraft reloads resources
- **THEN** `PlanetProperties.PLANET_PROPERTIES` is cleared, all `system_config/planet_properties/` files re-parsed, and a sync packet sent to all connected clients

---

### Requirement: Planet Properties Client Synchronisation
The server SHALL sync the full planet properties map to clients using `PlanetPropertiesSyncPacket` encoded as NBT. The packet SHALL be sent to all online players after each reload, and to any player who joins after a reload.

#### Scenario: Sync on reload
- **WHEN** planet properties finish loading on the server
- **THEN** a `PlanetPropertiesSyncPacket` is sent to every online player

#### Scenario: Sync on player join
- **WHEN** a player logs in
- **THEN** a `PlanetPropertiesSyncPacket` is sent to that player with the current properties

#### Scenario: Client receives sync
- **WHEN** a client receives a `PlanetPropertiesSyncPacket`
- **THEN** its local `PLANET_PROPERTIES` map is cleared and repopulated from the packet data
