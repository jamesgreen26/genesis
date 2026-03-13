## 1. Create Atmosphere record

- [x] 1.1 Create `src/main/java/shipwrights/genesis/space/planet_properties/Atmosphere.java` as a Java record with fields `double density`, `double thickness`, `boolean precipitation`, `PlanetColorPalette color`
- [x] 1.2 Add `public static final Codec<Atmosphere> CODEC` to `Atmosphere.java` using `RecordCodecBuilder` mapping each field by its JSON name

## 2. Refactor PlanetProperties

- [x] 2.1 Replace the four individual atmosphere fields in the `PlanetProperties` record with a single `Atmosphere atmosphere` field
- [x] 2.2 Update `PlanetProperties.CODEC` to use `Atmosphere.CODEC.fieldOf("atmosphere")` in place of the four separate field codecs

## 3. Fix call sites

- [x] 3.1 Update `PlanetAtmosphereRenderer.java` — change `props.density()` → `props.atmosphere().density()` and `props.thickness()` → `props.atmosphere().thickness()`
- [x] 3.2 Update `PlanetDimensionEffects.java` — change `props.density()` → `props.atmosphere().density()`, `props.precipitation()` → `props.atmosphere().precipitation()`, `props.color()` → `props.atmosphere().color()`
- [x] 3.3 Inspect `PlanetPropertiesSyncPacket.java` and update any direct references to the removed accessors (`density`, `thickness`, `precipitation`, `color`) to go through `atmosphere()` — no changes needed, fully codec-driven

## 4. Verify

- [x] 4.1 Confirm `builtin.json` already uses the nested `atmosphere` format (already done)
- [x] 4.2 Build the project (`./gradlew build`) and confirm zero compile errors
- [x] 4.3 Run the game and verify the atmosphere renders correctly on the overworld and moon
