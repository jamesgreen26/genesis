### Requirement: Atmosphere record encapsulates atmosphere fields
The system SHALL provide an `Atmosphere` record in `shipwrights.genesis.space.planet_properties` that holds `double density`, `double thickness`, `boolean precipitation`, and `PlanetColorPalette color`, with a `CODEC` that serializes/deserializes these fields from JSON.

#### Scenario: Atmosphere codec round-trips correctly
- **WHEN** an `Atmosphere` instance is encoded to JSON via `Atmosphere.CODEC`
- **THEN** the resulting JSON contains `density`, `thickness`, `precipitation`, and `color` as direct fields

#### Scenario: Atmosphere codec parses from JSON object
- **WHEN** a JSON object with `density`, `thickness`, `precipitation`, and `color` fields is decoded via `Atmosphere.CODEC`
- **THEN** the resulting `Atmosphere` record has the correct field values

### Requirement: PlanetProperties nests atmosphere under a single field
The `PlanetProperties` record SHALL replace the four individual atmosphere fields (`density`, `thickness`, `precipitation`, `color`) with a single `Atmosphere atmosphere` field. The `CODEC` SHALL map this field to the JSON key `"atmosphere"`.

#### Scenario: Planet entry JSON with nested atmosphere is parsed
- **WHEN** a planet entry JSON object with an `"atmosphere"` sub-object is decoded via `PlanetProperties.CODEC`
- **THEN** `PlanetProperties#atmosphere()` returns an `Atmosphere` with the correct values

#### Scenario: Old flat-format planet entry fails gracefully
- **WHEN** a planet entry JSON with top-level `density`, `thickness`, `precipitation`, `color` (no `"atmosphere"` key) is decoded
- **THEN** codec parsing logs an error and the entry is skipped (not loaded)

### Requirement: Call sites access atmosphere fields via the atmosphere accessor
All Java code that reads atmosphere properties from a `PlanetProperties` instance SHALL use `props.atmosphere().<field>()` rather than `props.<field>()` directly.

#### Scenario: Atmosphere renderer reads density and thickness
- **WHEN** `PlanetAtmosphereRenderer` fetches planet properties to set shader uniforms
- **THEN** it calls `props.atmosphere().density()` and `props.atmosphere().thickness()`

#### Scenario: Dimension effects reads density, precipitation, and color
- **WHEN** `PlanetDimensionEffects` applies sky color and precipitation state
- **THEN** it calls `props.atmosphere().density()`, `props.atmosphere().precipitation()`, and `props.atmosphere().color()`
