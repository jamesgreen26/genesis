## Why

`PlanetAtmosphereRenderer` renders atmospheres with hardcoded size and opacity constants, making it impossible for data-driven planet configs to control how thick or dense a planet's atmosphere appears from space. `PlanetProperties` already models `thickness` and `density` per planet — the renderer should consume them.

## What Changes

- `PlanetAtmosphereRenderer` looks up `PlanetProperties` for the planet being rendered and uses `thickness` and `density` to drive the shader.
- Rendering is skipped entirely when the planet has no `PlanetProperties`, when `density == 0`, or when `toRender == vantagePoint` (removes the degenerate tiny-halfSize fallback path).
- `relativeAtmosphereSize` (currently `1.3f`) becomes `1.0f + 0.3f * (float) props.thickness()`, so `thickness = 1.0` reproduces the existing look.
- A new `Density` shader uniform is introduced; the fragment shader multiplies final alpha by it, so `density = 1.0` reproduces the existing look.
- The hard-coded `const float atmosphereThickness = 1.3` in the FSH is replaced by a `uniform float AtmosphereThickness` set from Java.

## Capabilities

### New Capabilities

_(none — this is a renderer integration with an existing data model)_

### Modified Capabilities

- `planet-properties`: Two new rendering requirements are added — the `thickness` and `density` fields now have a specified effect on the `PlanetAtmosphereRenderer` (atmosphere scale and opacity respectively), with `1.0` defined as the baseline matching the pre-change appearance.

## Impact

- **Renderer**: `PlanetAtmosphereRenderer.java` — logic changes, two new uniform set calls.
- **Shader source**: `planet_atmosphere.fsh` — two new uniforms, FSH const replaced.
- **Shader descriptor**: `planet_atmosphere.json` — two new uniform declarations.
- **Data files**: Any planet that should have a visible atmosphere from space now requires a `PlanetProperties` entry with `density > 0` and a `thickness` value.
