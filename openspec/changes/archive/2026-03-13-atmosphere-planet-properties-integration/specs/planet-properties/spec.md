## MODIFIED Requirements

### Requirement: Planet Properties Data Model
Each planet (celestial of type `genesis:body`) MAY have an associated `PlanetProperties` entry keyed by its `ResourceLocation` ID. The entry SHALL carry the following fields:

| Field | Type | Description |
|---|---|---|
| `density` | `double` | Atmospheric density (0 = vacuum / no atmosphere rendered, 1 = Earth-like baseline) |
| `thickness` | `double` | Atmospheric thickness multiplier (1.0 = Earth-like baseline, maps to `relativeAtmosphereSize = 1.0 + 0.3 * thickness`) |
| `precipitation` | `boolean` | Whether weather/rain occurs on this planet |
| `color` | `PlanetColorPalette` | Sky/atmosphere color source |

#### Scenario: Property lookup for known planet
- **WHEN** `PlanetProperties.get("genesis:earth")` is called and properties are loaded
- **THEN** the matching `PlanetProperties` record is returned

#### Scenario: Property lookup for unknown planet
- **WHEN** `PlanetProperties.get(id)` is called for an ID with no properties defined
- **THEN** `null` is returned

## ADDED Requirements

### Requirement: Atmosphere Renderer Driven by Planet Properties
`PlanetAtmosphereRenderer` SHALL look up `PlanetProperties` for the planet being rendered and use `thickness` and `density` to configure the atmosphere shader. When `toRender` equals `vantagePoint`, rendering SHALL be skipped entirely.

#### Scenario: No properties — atmosphere suppressed
- **WHEN** `PlanetAtmosphereRenderer.invoke()` is called for a planet with no `PlanetProperties` entry
- **THEN** no atmosphere geometry is submitted and the shader is not invoked for that planet

#### Scenario: Density zero — atmosphere suppressed
- **WHEN** a planet's `PlanetProperties.density` is `0.0`
- **THEN** no atmosphere geometry is submitted for that planet

#### Scenario: Thickness 1.0 and density 1.0 reproduce baseline appearance
- **WHEN** a planet has `thickness = 1.0` and `density = 1.0`
- **THEN** the rendered atmosphere is visually identical to the pre-integration hardcoded appearance (`relativeAtmosphereSize = 1.3`, full alpha)

#### Scenario: Thickness scales atmosphere shell size
- **WHEN** a planet has `thickness = 2.0`
- **THEN** the atmosphere outer box extends to `1.0 + 0.3 * 2.0 = 1.6` times the planet half-size

#### Scenario: Density scales atmosphere opacity
- **WHEN** a planet has `density = 0.5`
- **THEN** the fragment shader's final alpha is multiplied by `0.5`, halving the atmosphere opacity

#### Scenario: Vantage-point planet skipped
- **WHEN** `toRender` is the same celestial as `vantagePoint`
- **THEN** `PlanetAtmosphereRenderer.invoke()` returns immediately without rendering

### Requirement: Atmosphere Shader Uniforms for Thickness and Density
The `planet_atmosphere` shader SHALL expose `AtmosphereThickness` (float) and `Density` (float) uniforms. `AtmosphereThickness` SHALL replace the previously hard-coded `1.3` constant used in ray-box intersection math. `Density` SHALL be multiplied into the final fragment alpha.

#### Scenario: AtmosphereThickness uniform consumed by FSH
- **WHEN** `AtmosphereThickness` is set to `1.3` (default)
- **THEN** ray-box intersection uses `v_half_size * 1.3` as the outer atmosphere radius, matching pre-integration behaviour

#### Scenario: Density uniform attenuates alpha
- **WHEN** `Density` is set to `0.5`
- **THEN** `frag_color.a` is multiplied by `0.5` before output

#### Scenario: Density uniform at 1.0 is a no-op
- **WHEN** `Density` is set to `1.0`
- **THEN** `frag_color.a` is unchanged compared to pre-integration output
