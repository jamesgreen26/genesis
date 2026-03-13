# Design: Planet Properties

## Parallel Registry Pattern

Planet properties follow the exact same lifecycle as the space registry:

```
Resource reload
  → DataLoader clears loaded configs
  → Scans system_config/planet_properties/**
  → Parses each JSON into PlanetPropertiesModel (list of keyed entries)
  → Populates PlanetProperties.PLANET_PROPERTIES map
  → Sends PlanetPropertiesSyncPacket to all online players

Player join
  → PlanetPropertiesSyncPacket sent to joining player
```

This keeps planet data in sync with celestial data automatically, since both are driven by the same `AddReloadListenerEvent`.

## Data File Format

Each file under `system_config/planet_properties/` is a JSON object mapping celestial IDs to property objects:

```json
{
  "planets": [
    {
      "id": "genesis:earth",
      "density": 1.0,
      "thickness": 1.0,
      "precipitation": true,
      "color": { "type": "genesis:overworld" }
    },
    {
      "id": "genesis:mars",
      "density": 0.006,
      "thickness": 0.3,
      "precipitation": false,
      "color": { "type": "genesis:rgb", "r": 180, "g": 90, "b": 50 }
    }
  ]
}
```

## PlanetColorPalette as a Dispatch Codec

`PlanetColorPalette` is polymorphic, mirroring `CelestialTransformProvider`. The dispatch field is `"type"`:

- `genesis:rgb` → `PlanetColorPalette.RGB(int r, int g, int b)`
- `genesis:overworld` → `PlanetColorPalette.Overworld` (singleton, no extra fields)

`Overworld` is a sentinel: callers MUST check `isOverworld()` before calling `getRGB()`, which throws. The actual sky color for the Overworld is sourced from biome data at render time.

## Sync Packet

`PlanetPropertiesSyncPacket` encodes the full map as NBT (same as `SpaceRegistrySyncPacket`). On the client, receipt replaces the entire `PLANET_PROPERTIES` map.

## Key Decision: Separate Registry vs. Extending Celestial

Extending `Celestial` with optional planet fields was considered and rejected:
- It forces every celestial type (stars, black holes) to carry null fields
- It couples the celestial codec to planet-specific concerns
- It makes it harder for future celestial types to add their own extra data

The separate registry is more composable and follows the pattern already established by the codebase.
