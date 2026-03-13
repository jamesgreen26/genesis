## Why

Planet properties JSON previously mixed atmosphere-specific fields (`density`, `thickness`, `precipitation`, `color`) directly into the planet entry alongside `id`. Grouping them under an `atmosphere` object better reflects their semantic relationship, makes the format easier to extend with future top-level planet properties, and improves readability for datapack authors.

## What Changes

- **BREAKING** The `density`, `thickness`, `precipitation`, and `color` fields on planet entries are now nested under an `"atmosphere"` object in the JSON data format (already applied to `builtin.json`).
- A new `Atmosphere` Java record is introduced to represent the grouped atmosphere data with its own `CODEC`.
- `PlanetProperties` record loses the four individual atmosphere fields and gains a single `Atmosphere atmosphere` field.
- All Java call sites updated from `props.density()` / `props.thickness()` / `props.precipitation()` / `props.color()` to `props.atmosphere().density()` etc.

## Capabilities

### New Capabilities

- `atmosphere-data`: The structured atmosphere sub-object in planet property data — its schema, codec, and record class.

### Modified Capabilities

- (none — no existing spec files to update)

## Impact

- **Data format**: `data/genesis/system_config/planet_properties/*.json` — any third-party datapack defining planet properties must wrap atmosphere fields under `"atmosphere"`.
- **Java**:
  - `PlanetProperties.java` — record fields and CODEC change.
  - New file: `Atmosphere.java` (new record + CODEC).
  - `PlanetAtmosphereRenderer.java` — accessor call sites.
  - `PlanetDimensionEffects.java` — accessor call sites.
  - `PlanetPropertiesSyncPacket.java` — verify codec-driven or manual NBT encoding; update if needed.
