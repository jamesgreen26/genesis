# API Reference

Complete API reference for the Genesis celestial system.

## Table of Contents

- [Celestial](#celestial)
- [CelestialType](#celestialtype)
- [CelestialTransformProvider](#celestialtransformprovider)
- [StaticTransformProvider](#statictransformprovider)
- [OrbitingTransformProvider](#orbitingtransformprovider)
- [VantagePoint](#vantagepoint)
- [SpaceRegistry](#spaceregistry)
- [SpaceLevel](#spacelevel)
- [PlanetProperties](#planetproperties)
- [StarProperties](#starproperties)
- [GenesisMod](#genesismod)
- [JSON Schema](#json-schema)

---

## Celestial

**Package:** `shipwrights.genesis.space`
**File:** `src/main/java/shipwrights/genesis/space/Celestial.java`

`Celestial` is a **record** representing an astronomical body in the Genesis system.

### Record Components

```java
public record Celestial(
    CelestialTransformProvider transformProvider,
    ResourceLocation ID,
    CelestialType type,
    double size,
    double gravity,
    float r,
    float g,
    float b
)
```

**Parameters:**
- `transformProvider` - Provider for position and rotation calculations
- `ID` - Unique identifier (e.g., `minecraft:overworld`)
- `type` - The celestial type (e.g., star, body)
- `size` - Size of the celestial body
- `gravity` - Gravity strength (1.0 = Earth gravity)
- `r` - Red color component (0.0 to 1.0) — reserved for future use (e.g., map tinting)
- `g` - Green color component (0.0 to 1.0) — reserved for future use
- `b` - Blue color component (0.0 to 1.0) — reserved for future use

Record accessor names match the component names: `ID()`, `type()`, `size()`, `gravity()`, `r()`, `g()`, `b()`, `transformProvider()`.

### Methods

#### getPosition()
```java
public Vector3dc getPosition(long ticks, float partialTick)
public Vector3dc getPosition(long ticks)
```
Calculates the position of this celestial at a specific time.

**Returns:** `Vector3dc` - Position vector in world space

---

#### getRotation()
```java
public Quaterniondc getRotation(long ticks, float partialTick)
public Quaterniondc getRotation(long ticks)
```
Calculates the rotation of this celestial at a specific time.

**Returns:** `Quaterniondc` - Rotation quaternion

---

#### getOBB()
```java
public OBB getOBB(long ticks)
public OBB getOBB(long ticks, float subticks)
```
Returns the oriented bounding box for this celestial at the given time.

**Returns:** `OBB` - Oriented bounding box

---

#### getActualSize()
```java
public double getActualSize()
```
Returns the size of this celestial (equivalent to `size()`).

---

#### getNearestStar()
```java
public Celestial getNearestStar(long gameTime, float partialTick)
```
Finds the nearest star-type celestial to this one. If this celestial is itself a star, returns `this`.

**Parameters:**
- `gameTime` - Current game time in ticks
- `partialTick` - Fractional tick

**Returns:** `Celestial` - The nearest star

**Throws:** `IllegalStateException` if no star exists in the registry

---

### Static Fields

```java
public static final Codec<Celestial> CODEC
```

Mojang codec for JSON serialization/deserialization.

---

## CelestialType

**Package:** `shipwrights.genesis.space.type`
**File:** `src/main/java/shipwrights/genesis/space/type/CelestialType.java`

Interface defining the behavioral properties and rendering of celestial types.

### Methods

#### castsLight()
```java
boolean castsLight()
```
Whether this type emits light (affects day/night cycles on other celestials).

---

#### castsShadow()
```java
boolean castsShadow()
```
Whether this type casts shadows (blocks light from other sources).

---

#### isVisitable()
```java
boolean isVisitable()
```
Whether players can land on this type of celestial.

---

#### getRenderer()
```java
@NotNull CelestialRenderer getRenderer()
```
Returns the renderer used to draw this type of celestial.

---

#### getID()
```java
@NotNull ResourceLocation getID()
```
Returns the unique identifier for this type.

---

### Static Methods

#### get()
```java
static CelestialType get(ResourceLocation ID)
```
Retrieves a registered celestial type by its ID.

---

#### register()
```java
static void register(CelestialType type)
```
Registers a new celestial type.

---

### Built-in Types

#### genesis:star
- `castsLight()` → `true`
- `castsShadow()` → `false`
- `isVisitable()` → `false`
- Rendered with glowing star effects

#### genesis:body
- `castsLight()` → `false`
- `castsShadow()` → `true`
- `isVisitable()` → `true`
- Rendered with surface textures

#### genesis:blackhole
- `castsLight()` → `false`
- `castsShadow()` → `false`
- `isVisitable()` → `false`
- Rendered with a blackhole shader effect

---

## CelestialTransformProvider

**Package:** `shipwrights.genesis.space.transformProvider`
**File:** `src/main/java/shipwrights/genesis/space/transformProvider/CelestialTransformProvider.java`

Interface for providing position and rotation transformations over time.

### Methods

#### getPosition()
```java
Vector3d getPosition(long ticks, float subticks)
```
Calculates position at a specific time.

**Returns:** `Vector3d` - Position vector

---

#### getRotation()
```java
Quaterniondc getRotation(long ticks, float subticks)
```
Calculates rotation at a specific time.

**Returns:** `Quaterniondc` - Rotation quaternion

---

#### getType()
```java
ResourceLocation getType()
```
Returns the type identifier for this transform provider.

---

### Static Methods

#### register()
```java
static void register(ResourceLocation type, Codec<? extends CelestialTransformProvider> codec)
```
Registers a transform provider type with its codec for JSON deserialization.

---

## StaticTransformProvider

**Package:** `shipwrights.genesis.space.transformProvider`
**File:** `src/main/java/shipwrights/genesis/space/transformProvider/StaticTransformProvider.java`

Transform provider for celestials with fixed positions and rotations.

**Type ID:** `genesis:static`

### Constructors

```java
public StaticTransformProvider(double x, double y, double z)
public StaticTransformProvider(double x, double y, double z, double xRot, double yRot, double zRot)
```

**Parameters:**
- `x`, `y`, `z` - Position coordinates
- `xRot`, `yRot`, `zRot` - Rotation in radians (optional, default 0.0)

### JSON Fields

```json
{
  "type": "genesis:static",
  "x": 0.0,
  "y": 0.0,
  "z": 0.0,
  "xRot": 0.0,
  "yRot": 0.0,
  "zRot": 0.0
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `type` | String | Yes | - | Must be `"genesis:static"` |
| `x` | Number | Yes | - | X position |
| `y` | Number | Yes | - | Y position |
| `z` | Number | Yes | - | Z position |
| `xRot` | Number | No | 0.0 | X rotation (radians) |
| `yRot` | Number | No | 0.0 | Y rotation (radians) |
| `zRot` | Number | No | 0.0 | Z rotation (radians) |

---

## OrbitingTransformProvider

**Package:** `shipwrights.genesis.space.transformProvider`
**File:** `src/main/java/shipwrights/genesis/space/transformProvider/OrbitingTransformProvider.java`

Transform provider for celestials that orbit around a parent body.

**Type ID:** `genesis:orbiting`

### Constructor

```java
public OrbitingTransformProvider(
    ResourceLocation parentID,
    int seed,
    double orbitDistance,
    double orbitTime,
    double dayLength
)
```

**Parameters:**
- `parentID` - ID of the parent celestial to orbit
- `seed` - Random seed for deterministic orbital angles (longitude and latitude)
- `orbitDistance` - Orbit radius **in blocks**
- `orbitTime` - Orbital period **in ticks**
- `dayLength` - Axial rotation period **in ticks** (0 = tidally locked: one face always points toward parent)

### JSON Fields

```json
{
  "type": "genesis:orbiting",
  "parentID": "namespace:parent",
  "seed": 12345,
  "orbitDistance": 15000.0,
  "orbitTime": 4608000.0,
  "dayLength": 24000.0
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `type` | String | Yes | - | Must be `"genesis:orbiting"` |
| `parentID` | String | Yes | - | Parent celestial ID |
| `seed` | Integer | Yes | - | Random seed for orbital angles |
| `orbitDistance` | Number | Yes | - | Orbit radius in blocks |
| `orbitTime` | Number | Yes | - | Orbital period in ticks |
| `dayLength` | Number | No | 1.0 | Axial rotation period in ticks (0 = tidally locked) |

### Behavior

- Uses `seed` to generate deterministic random orbital angles (spherical coordinates)
- Orbits in a circle around the parent's position
- Rotates on its own axis at the rate given by `dayLength`
- Setting `dayLength` to `0` enables tidal locking: the body's -Z face always points toward the parent

---

## VantagePoint

**Package:** `shipwrights.genesis.space`
**File:** `src/main/java/shipwrights/genesis/space/VantagePoint.java`

Interface describing the observer's position and orientation when rendering the sky. Passed to `CelestialRenderer.invoke()` so renderers can compute correct relative positions.

### Methods

#### getPosition()
```java
Vector3dc getPosition()
```
The observer's world-space position.

---

#### getRotation()
```java
Quaterniondc getRotation()
```
The observer's orientation (combined celestial and camera rotation when on a surface).

---

### Static Factory

#### get()
```java
@Nullable static VantagePoint get(Level level, Vector3dc posInLevel, long ticks, float partialTick)
```
Creates the appropriate `VantagePoint` for the current observer position.

**Returns:** `VantagePoint` — or `null` if the level has no space-level association

---

### Implementations

#### VantagePoint.InSpace
Observer is viewing from space (not standing on any celestial).
- `getPosition()` — returns zero vector
- `getRotation()` — returns identity quaternion

#### VantagePoint.OnCelestial
Observer is standing on a celestial body's surface.

**Fields:**
- `Celestial celestial` — the planet the observer is on
- `Quaterniondc cameraRotationFromNorthPole` — camera orientation as lat/lon offset
- `long ticks`, `float partialTick` — current time

**Methods:**
- `getPosition()` — returns the celestial's position
- `getRotation()` — combined celestial rotation and camera rotation
- `getCelestialRotation()` — the celestial's own rotation

---

## SpaceRegistry

**Package:** `shipwrights.genesis.space.registry`
**File:** `src/main/java/shipwrights/genesis/space/registry/SpaceRegistry.java`

Central registry for all celestials. Access the live instance via `GenesisMod.SPACE_REGISTRY`.

### Instance Methods

#### get()
```java
public @Nullable Celestial get(ResourceLocation id)
```
Retrieves a celestial by its ID.

**Returns:** `Celestial` — or `null` if not found

**Example:**
```java
Celestial sun = GenesisMod.SPACE_REGISTRY.get(ResourceLocation.parse("genesis:sun"));
```

---

#### getAll()
```java
public List<Celestial> getAll()
```
Returns all registered celestials.

---

#### getWhere()
```java
public Collection<Celestial> getWhere(Predicate<CelestialType> predicate)
```
Filters celestials by type predicate.

**Example:**
```java
// Get all stars
Collection<Celestial> stars = GenesisMod.SPACE_REGISTRY.getWhere(type -> type.castsLight());

// Get all visitable bodies
Collection<Celestial> planets = GenesisMod.SPACE_REGISTRY.getWhere(type -> type.isVisitable());
```

---

### RegisterCelestialsEvent

**Inner Class:** `SpaceRegistry.RegisterCelestialsEvent`

Event class used during celestial registration (see [GenesisMod](#genesismod)).

#### accept()
```java
public void accept(Celestial celestial)
```
Registers a celestial to the registry.

---

## SpaceLevel

**Package:** `shipwrights.genesis.space`
**File:** `src/main/java/shipwrights/genesis/space/SpaceLevel.java`

Static utility class for spatial queries against registered celestials.

### Static Methods

#### nearestCelestialWhere()
```java
@Nullable static Pair<Celestial, Double> nearestCelestialWhere(
    Vector3dc position,
    long ticks,
    float partialTick,
    Predicate<CelestialType> predicate
)
```
Finds the nearest celestial matching the given type predicate, measured from `position`.

**Returns:** `Pair<Celestial, Double>` — the nearest matching celestial and its distance, or `null` if none match

---

#### celestialRaycast()
```java
@Nullable static Pair<Celestial, Double> celestialRaycast(
    long ticks,
    float partialTick,
    Vector3d origin,
    Vector3d direction,
    Predicate<CelestialType> predicate
)
```
Casts a ray and returns the first celestial (matching the predicate) that it intersects.

**Returns:** `Pair<Celestial, Double>` — the hit celestial and distance along the ray, or `null` if no hit

---

## PlanetProperties

**Package:** `shipwrights.genesis.space.planet_properties`
**File:** `src/main/java/shipwrights/genesis/space/planet_properties/PlanetProperties.java`

Per-planet configuration loaded from JSON datapacks. Includes atmosphere data. Loaded from `data/<namespace>/system_config/planet_properties/*.json`.

### Record Components

```java
public record PlanetProperties(ResourceLocation id, Atmosphere atmosphere)
```

### Static Methods

#### get()
```java
static PlanetProperties get(ResourceLocation id)
```
Retrieves properties for a planet by ID. Returns `null` if not registered.

---

#### register()
```java
static void register(ResourceLocation id, PlanetProperties properties)
```
Registers properties for a planet.

---

### Atmosphere

```java
public record Atmosphere(
    double density,
    double thickness,
    boolean precipitation,
    boolean isBreathable,
    PlanetColorPalette color
)
```

| Field | Type | Description |
|-------|------|-------------|
| `density` | double | Atmosphere density (affects fade when leaving) |
| `thickness` | double | Visual thickness of the atmosphere shell |
| `precipitation` | boolean | Whether precipitation occurs |
| `isBreathable` | boolean | Whether the atmosphere supports breathing |
| `color` | PlanetColorPalette | Atmosphere color |

### PlanetColorPalette

Interface for atmosphere color. Two implementations:

- **`RGB`** — custom RGB color: `{ "type": "genesis:rgb", "r": 0, "g": 100, "b": 255 }`
- **`Overworld`** — use Minecraft's built-in sky color: `{ "type": "genesis:overworld" }`

### JSON File Format

```
data/<namespace>/system_config/planet_properties/<filename>.json
```

```json
{
  "planets": [
    {
      "id": "minecraft:overworld",
      "atmosphere": {
        "density": 1.0,
        "thickness": 0.1,
        "precipitation": true,
        "isBreathable": true,
        "color": { "type": "genesis:overworld" }
      }
    }
  ]
}
```

---

## StarProperties

**Package:** `shipwrights.genesis.space.star_properties`
**File:** `src/main/java/shipwrights/genesis/space/star_properties/StarProperties.java`

Per-star color configuration loaded from JSON datapacks. Supports a primary and secondary color for gradient effects. Loaded from `data/<namespace>/system_config/star_properties/*.json`.

### Record Components

```java
public record StarProperties(
    ResourceLocation id,
    int r0, int g0, int b0,
    int r1, int g1, int b1
)
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | ResourceLocation | Star celestial ID |
| `r0`, `g0`, `b0` | int | Primary color (RGB 0–255) |
| `r1`, `g1`, `b1` | int | Secondary color (RGB 0–255) |

### Static Methods

#### get()
```java
static StarProperties get(ResourceLocation id)
```
Retrieves star color properties by celestial ID. Returns `null` if not registered.

---

#### register()
```java
static void register(ResourceLocation id, StarProperties properties)
```
Registers color properties for a star.

---

### JSON File Format

```
data/<namespace>/system_config/star_properties/<filename>.json
```

```json
{
  "stars": [
    {
      "id": "genesis:sun",
      "r0": 255, "g0": 220, "b0": 150,
      "r1": 255, "g1": 180, "b1": 80
    }
  ]
}
```

---

## GenesisMod

**Package:** `shipwrights.genesis`
**File:** `src/main/java/shipwrights/genesis/GenesisMod.java`

Main mod class. Holds the live `SPACE_REGISTRY` instance and the registration hook.

### Static Fields

```java
public static SpaceRegistry SPACE_REGISTRY
```

The active celestial registry. Use this to query celestials at runtime.

### Static Methods

#### onRegisterCelestialsEvent()
```java
public static void onRegisterCelestialsEvent(
    Consumer<SpaceRegistry.RegisterCelestialsEvent> callback
)
```

Registers a callback to be invoked during celestial registration.

**Example:**
```java
GenesisMod.onRegisterCelestialsEvent(event -> {
    Celestial sun = new Celestial(
        new StaticTransformProvider(0, 0, 0),
        ResourceLocation.parse("mymod:sun"),
        CelestialType.get(ResourceLocation.parse("genesis:star")),
        1400.0, 1.0,
        1.0f, 0.95f, 0.8f
    );
    event.accept(sun);
});
```

---

## JSON Schema

### System Config File Format

**Location:** `data/<namespace>/system_config/<filename>.json`

All JSON files in this directory are loaded and merged. Files inside `planet_properties/` and `star_properties/` subdirectories are handled by their respective loaders.

```json
{
  "celestials": [
    {
      "ID": "string",
      "type": "string",
      "size": 100.0,
      "gravity": 1.0,
      "r": 0.5,
      "g": 0.5,
      "b": 0.5,
      "transformProvider": {
        "type": "string"
      }
    }
  ]
}
```

### Celestial Object Schema

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `ID` | String | Yes | - | Unique identifier (`namespace:name`) |
| `type` | String | Yes | - | Celestial type ID |
| `size` | Number | Yes | - | Size of the celestial body |
| `gravity` | Number | Yes | - | Gravity multiplier (1.0 = Earth) |
| `r` | Number | No | 0.5 | Red component (0.0–1.0) |
| `g` | Number | No | 0.5 | Green component (0.0–1.0) |
| `b` | Number | No | 0.5 | Blue component (0.0–1.0) |
| `transformProvider` | Object | Yes | - | Transform configuration |

### Transform Provider Types

See [StaticTransformProvider](#statictransformprovider) and [OrbitingTransformProvider](#orbitingtransformprovider) for complete field specifications.

---

## See Also

- [Getting Started Guide](getting-started.md) - Introduction and quick start
- [Implementation Guide](implementation-guide.md) - Step-by-step guide for datapacks and code
- [Advanced Topics](advanced.md) - Custom types and transforms
