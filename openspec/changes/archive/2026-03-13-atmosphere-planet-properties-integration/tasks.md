## 1. Shader Descriptor

- [x] 1.1 Add `AtmosphereThickness` float uniform (default `1.3`) to `planet_atmosphere.json`
- [x] 1.2 Add `Density` float uniform (default `1.0`) to `planet_atmosphere.json`

## 2. Fragment Shader

- [x] 2.1 Replace `const float atmosphereThickness = 1.3;` with `uniform float AtmosphereThickness;` in `planet_atmosphere.fsh`
- [x] 2.2 Replace all usages of `atmosphereThickness` in the FSH with `AtmosphereThickness`
- [x] 2.3 Add `uniform float Density;` declaration to `planet_atmosphere.fsh`
- [x] 2.4 Append `frag_color.a *= Density;` as the final operation before output in `planet_atmosphere.fsh`

## 3. Renderer — Skip Logic

- [x] 3.1 Add early return in `PlanetAtmosphereRenderer.invoke()` when `vantagePoint != null && vantagePoint.equals(toRender)`
- [x] 3.2 Remove the `if (vantagePoint != null && vantagePoint.equals(toRender))` branch inside `renderAtmosphere()` that sets `halfSize = 0.000001f`

## 4. Renderer — Property Lookup and Uniform Wiring

- [x] 4.1 In `renderAtmosphere()`, look up `PlanetProperties props = PlanetProperties.get(toRender.getID())`
- [x] 4.2 Return early (skip render) if `props == null`
- [x] 4.3 Return early (skip render) if `props.density() == 0.0`
- [x] 4.4 Replace `float relativeAtmosphereSize = 1.3f` with `float relativeAtmosphereSize = 1.0f + 0.3f * (float) props.thickness()`
- [x] 4.5 Set the `AtmosphereThickness` shader uniform to `relativeAtmosphereSize` before the cube face calls
- [x] 4.6 Set the `Density` shader uniform to `(float) props.density()` before the cube face calls
