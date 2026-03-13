## Context

Planet properties are loaded from datapack JSON files (`system_config/planet_properties/*.json`) via a Forge reload listener. Each planet entry is parsed by `PlanetProperties.CODEC` using Mojang's codec system. The `PlanetProperties` record currently exposes `density`, `thickness`, `precipitation`, and `color` as direct fields. The JSON data file has already been updated to nest these under `"atmosphere"`, creating a mismatch that must be resolved in Java.

## Goals / Non-Goals

**Goals:**
- Introduce an `Atmosphere` record with its own `CODEC` mirroring the new JSON structure.
- Refactor `PlanetProperties` to hold an `Atmosphere` field instead of four individual atmosphere fields.
- Update all accessor call sites so the project compiles and behaves identically at runtime.
- Keep network sync (`PlanetPropertiesSyncPacket`) working correctly.

**Non-Goals:**
- Adding new atmosphere properties or changing rendering behaviour.
- Providing a migration shim for old flat-format JSON (third-party datapacks must update).
- Changing the `PlanetColorPalette` system.

## Decisions

### D1 — Separate `Atmosphere` record rather than inline codec nesting

Inline codec nesting (using `RecordCodecBuilder` directly inside `PlanetProperties.CODEC`) would work but makes the codec hard to read and prevents `Atmosphere` from being used independently in the future. A named `Atmosphere` record with its own `CODEC` keeps both codecs clean and the type explicit at call sites.

*Alternative considered*: Keep all fields flat in `PlanetProperties` and just alias the codec field to `"atmosphere.density"` — not possible with Mojang's codec API without custom map codecs.

### D2 — `Atmosphere` lives in the same package as `PlanetProperties`

All planet-property types (`PlanetColorPalette`, `PlanetPropertiesModel`, etc.) are in `shipwrights.genesis.space.planet_properties`. `Atmosphere` belongs there too, keeping the package cohesive.

### D3 — No delegation methods on `PlanetProperties`

We will not add convenience methods like `PlanetProperties#density()` that delegate to `atmosphere().density()`. Call sites are updated to use the two-level accessor directly. This keeps the API honest about the nesting and avoids confusion about which accessor to use.

## Risks / Trade-offs

- **Breaking datapack compatibility** → Any third-party JSON that uses the old flat format will silently fail to load (codec will throw / log an error and skip the entry). Acceptable since this is an early-stage mod with no published API.
- **Compile errors at call sites** — Removing the direct accessors will surface every usage as a compile error, which is the desired forcing function to find all sites. Low risk.

## Migration Plan

1. Create `Atmosphere.java`.
2. Update `PlanetProperties.java` (record + codec).
3. Fix compile errors in `PlanetAtmosphereRenderer.java`, `PlanetDimensionEffects.java`, and `PlanetPropertiesSyncPacket.java`.
4. Verify `builtin.json` already matches the new format (done).
5. Build and run in-game to confirm atmosphere renders correctly on the overworld and moon.

Rollback: revert the four changed/created Java files and revert `builtin.json`.
