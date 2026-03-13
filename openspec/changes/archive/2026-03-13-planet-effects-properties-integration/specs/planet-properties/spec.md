## MODIFIED Requirements

### Requirement: Planet Color Palette
The `PlanetColorPalette` SHALL be a polymorphic type with two variants, serialized via a `"type"` dispatch field.

**`genesis:rgb`** — explicit RGB color with integer channels `r`, `g`, `b`.

**`genesis:overworld`** — a sentinel with no extra fields. Callers MUST check `isOverworld()` before calling `getRGB()`. When `isOverworld()` is `true`, the actual sky color is sourced from biome data at render time and `getRGB()` MUST NOT be called.

The `genesis:overworld` variant SHALL be used by `PlanetDimensionEffects` to select the biome-sampling sky colour path. The `genesis:rgb` variant SHALL cause `PlanetDimensionEffects` to use the planet's explicit RGB values for sky colour.

#### Scenario: RGB palette
- **WHEN** a planet's color is `{ "type": "genesis:rgb", "r": 180, "g": 90, "b": 50 }`
- **THEN** `isOverworld()` returns `false` and `getRGB()` returns `[180, 90, 50]`

#### Scenario: Overworld palette
- **WHEN** a planet's color is `{ "type": "genesis:overworld" }`
- **THEN** `isOverworld()` returns `true` and calling `getRGB()` throws `UnsupportedOperationException`

#### Scenario: RGB palette drives sky renderer
- **WHEN** a planet's color has `isOverworld() == false`
- **THEN** `PlanetDimensionEffects` uses `getRGB()` for sky color rather than biome sampling

#### Scenario: Overworld palette drives sky renderer
- **WHEN** a planet's color has `isOverworld() == true`
- **THEN** `PlanetDimensionEffects` uses biome-sampled sky color
