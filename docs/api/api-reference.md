# API Reference

Complete API reference for the Genesis celestial system.

## Table of Contents

- [Celestial](#celestial)
- [CelestialType](#celestialtype)
- [CelestialTransformProvider](#celestialtransformprovider)
- [StaticTransformProvider](#statictransformprovider)
- [OrbitingTransformProvider](#orbitingtransformprovider)
- [SpaceRegistry](#spaceregistry)
- [GenesisMod](#genesismod)
- [Constants](#constants)
- [JSON Schema](#json-schema)

---

## Celestial

**Package:** `shipwrights.genesis.space`
**File:** `src/main/java/shipwrights/genesis/space/Celestial.java`

The main class representing a celestial body in the Genesis system.

### Constructor

```java
public Celestial(
    CelestialTransformProvider transformProvider,
    ResourceLocation id,
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
- `id` - Unique identifier (e.g., `minecraft:overworld`)
- `type` - The celestial type (e.g., star, body)
- `size` - Size multiplier (1.0 = 96 blocks diameter)
- `gravity` - Gravity strength (1.0 = Earth gravity)
- `r` - Red color component (0.0 to 1.0)
- `g` - Green color component (0.0 to 1.0)
- `b` - Blue color component (0.0 to 1.0)

### Methods

#### getID()
```java
public ResourceLocation getID()
```
Returns the unique identifier for this celestial.

**Returns:** `ResourceLocation` - The celestial's ID

---

#### getType()
```java
public CelestialType getType()
```
Returns the type of this celestial.

**Returns:** `CelestialType` - The celestial's type

---

#### size()
```java
public double size()
```
Returns the size multiplier of this celestial.

**Returns:** `double` - Size multiplier (actual diameter = size × 96 blocks)

---

#### gravity()
```java
public double gravity()
```
Returns the gravity strength of this celestial.

**Returns:** `double` - Gravity multiplier (1.0 = Earth gravity)

---

#### r(), g(), b()
```java
public float r()
public float g()
public float b()
```
Returns the color components of this celestial.

**Returns:** `float` - Color value from 0.0 to 1.0

---

#### getPosition()
```java
public Vector3d getPosition(long ticks, float partialTick)
```
Calculates the position of this celestial at a specific time.

**Parameters:**
- `ticks` - Game time in ticks
- `partialTick` - Fractional tick for interpolation (0.0 to 1.0)

**Returns:** `Vector3d` - Position vector in world space

---

#### getRotation()
```java
public Quaterniondc getRotation(long ticks, float partialTick)
```
Calculates the rotation of this celestial at a specific time.

**Parameters:**
- `ticks` - Game time in ticks
- `partialTick` - Fractional tick for interpolation (0.0 to 1.0)

**Returns:** `Quaterniondc` - Rotation quaternion

---

#### getTransformProvider()
```java
public CelestialTransformProvider getTransformProvider()
```
Returns the transform provider for this celestial.

**Returns:** `CelestialTransformProvider` - The transform provider

---

#### getNearestStar()
```java
public Celestial getNearestStar(long gameTime, float partialTick)
```
Finds the nearest star-type celestial to this one.

**Parameters:**
- `gameTime` - Current game time in ticks
- `partialTick` - Fractional tick

**Returns:** `Celestial` - The nearest star, or null if none exists

---

#### getDayTime()
```java
public long getDayTime(long gameTime)
```
Calculates the day time (0-24000) for this celestial based on the nearest star's position.

**Parameters:**
- `gameTime` - Current game time in ticks

**Returns:** `long` - Day time value (0 = noon, 12000 = midnight)

---

### Static Fields

```java
public static double BASE_SIZE = 96;
public static double BASE_ORBIT_DISTANCE = 15_000;
public static double BASE_ORBIT_TIME = 4_608_000;
public static double BASE_DAY_LENGTH = 24_000;
```

**Descriptions:**
- `BASE_SIZE` - Base diameter in blocks (96)
- `BASE_ORBIT_DISTANCE` - Base orbital distance in blocks (15,000)
- `BASE_ORBIT_TIME` - Base orbital period in ticks (4,608,000 ≈ 64 hours)
- `BASE_DAY_LENGTH` - Base day length in ticks (24,000 = 20 minutes)

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

**Returns:** `boolean` - `true` for stars, `false` for planets/moons

---

#### castsShadow()
```java
boolean castsShadow()
```
Whether this type casts shadows (blocks light from other sources).

**Returns:** `boolean` - `true` for opaque bodies, `false` for stars

---

#### isVisitable()
```java
boolean isVisitable()
```
Whether players can land on this type of celestial.

**Returns:** `boolean` - `true` for planets/moons, `false` for stars

---

#### getRenderer()
```java
@NotNull CelestialRenderer getRenderer()
```
Returns the renderer used to draw this type of celestial.

**Returns:** `CelestialRenderer` - The renderer instance

---

#### getID()
```java
@NotNull ResourceLocation getID()
```
Returns the unique identifier for this type.

**Returns:** `ResourceLocation` - The type's ID

---

### Static Methods

#### get()
```java
static CelestialType get(ResourceLocation ID)
```
Retrieves a registered celestial type by its ID.

**Parameters:**
- `ID` - The type identifier

**Returns:** `CelestialType` - The type, or throws if not found

---

#### register()
```java
static void register(CelestialType type)
```
Registers a new celestial type.

**Parameters:**
- `type` - The type to register

---

### Built-in Types

#### genesis:star
- `castsLight()` → `true`
- `castsShadow()` → `false`
- `isVisitable()` → `false`
- Rendered with glowing effects

#### genesis:body
- `castsLight()` → `false`
- `castsShadow()` → `true`
- `isVisitable()` → `true`
- Rendered with surface textures

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

**Parameters:**
- `ticks` - Game time in ticks
- `subticks` - Fractional tick (0.0 to 1.0)

**Returns:** `Vector3d` - Position vector

---

#### getRotation()
```java
Quaterniondc getRotation(long ticks, float subticks)
```
Calculates rotation at a specific time.

**Parameters:**
- `ticks` - Game time in ticks
- `subticks` - Fractional tick (0.0 to 1.0)

**Returns:** `Quaterniondc` - Rotation quaternion

---

#### getType()
```java
ResourceLocation getType()
```
Returns the type identifier for this transform provider.

**Returns:** `ResourceLocation` - The provider type ID

---

### Static Methods

#### register()
```java
static void register(ResourceLocation type, Codec<? extends CelestialTransformProvider> codec)
```
Registers a transform provider type with its codec for JSON deserialization.

**Parameters:**
- `type` - Type identifier
- `codec` - Mojang codec for JSON parsing

---

## StaticTransformProvider

**Package:** `shipwrights.genesis.space.transformProvider`
**File:** `src/main/java/shipwrights/genesis/space/transformProvider/StaticTransformProvider.java`

Transform provider for celestials with fixed positions and rotations.

**Type ID:** `genesis:static`

### Constructors

#### Position Only
```java
public StaticTransformProvider(double x, double y, double z)
```

**Parameters:**
- `x` - X coordinate
- `y` - Y coordinate
- `z` - Z coordinate

---

#### Position and Rotation
```java
public StaticTransformProvider(
    double x, double y, double z,
    double xRot, double yRot, double zRot
)
```

**Parameters:**
- `x`, `y`, `z` - Position coordinates
- `xRot`, `yRot`, `zRot` - Rotation in radians

---

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
- `seed` - Random seed for orbital angles
- `orbitDistance` - Distance multiplier (base: 15,000 blocks)
- `orbitTime` - Period multiplier (base: 4,608,000 ticks)
- `dayLength` - Rotation period multiplier (base: 24,000 ticks)

---

### JSON Fields

```json
{
  "type": "genesis:orbiting",
  "parentID": "namespace:parent",
  "seed": 12345,
  "orbitDistance": 1.0,
  "orbitTime": 1.0,
  "dayLength": 1.0
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `type` | String | Yes | - | Must be `"genesis:orbiting"` |
| `parentID` | String | Yes | - | Parent celestial ID |
| `seed` | Integer | Yes | - | Random seed |
| `orbitDistance` | Number | Yes | - | Distance multiplier |
| `orbitTime` | Number | Yes | - | Period multiplier |
| `dayLength` | Number | No | 1.0 | Day length multiplier |

---

### Behavior

- Uses `seed` to generate deterministic random orbital angles in spherical coordinates
- Orbits in a circle around the parent's position
- Rotates on its axis based on `dayLength`
- Actual distance = `orbitDistance` × `Celestial.BASE_ORBIT_DISTANCE`
- Actual period = `orbitTime` × `Celestial.BASE_ORBIT_TIME`
- Actual day = `dayLength` × `Celestial.BASE_DAY_LENGTH`

---

## SpaceRegistry

**Package:** `shipwrights.genesis.space.registry`
**File:** `src/main/java/shipwrights/genesis/space/registry/SpaceRegistry.java`

Central registry for all celestials in the game.

### Static Methods

#### get()
```java
public static Celestial get(ResourceLocation id)
```
Retrieves a celestial by its ID.

**Parameters:**
- `id` - The celestial's identifier

**Returns:** `Celestial` - The celestial, or null if not found

---

#### getAll()
```java
public static List<Celestial> getAll()
```
Returns all registered celestials.

**Returns:** `List<Celestial>` - List of all celestials

---

#### getWhere()
```java
public static List<Celestial> getWhere(Predicate<CelestialType> predicate)
```
Filters celestials by type predicate.

**Parameters:**
- `predicate` - Filter condition on celestial type

**Returns:** `List<Celestial>` - Filtered list of celestials

**Example:**
```java
// Get all stars
List<Celestial> stars = SpaceRegistry.getWhere(type -> type.castsLight());

// Get all visitable bodies
List<Celestial> planets = SpaceRegistry.getWhere(type -> type.isVisitable());
```

---

### RegisterCelestialsEvent

**Inner Class:** `SpaceRegistry.RegisterCelestialsEvent`

Event class used during celestial registration.

#### accept()
```java
public void accept(Celestial celestial)
```
Registers a celestial to the registry.

**Parameters:**
- `celestial` - The celestial to register

---

## GenesisMod

**Package:** `shipwrights.genesis`
**File:** `src/main/java/shipwrights/genesis/GenesisMod.java`

Main mod class with registration hooks.

### Static Methods

#### onRegisterCelestialsEvent()
```java
public static void onRegisterCelestialsEvent(
    Consumer<SpaceRegistry.RegisterCelestialsEvent> callback
)
```

**Location:** `src/main/java/shipwrights/genesis/GenesisMod.java:88-90`

Registers a callback to be invoked during celestial registration.

**Parameters:**
- `callback` - Consumer that receives the registration event

**Example:**
```java
GenesisMod.onRegisterCelestialsEvent(event -> {
    // Register celestials here
    Celestial sun = new Celestial(...);
    event.accept(sun);
});
```

---

## Constants

### Size Constants

```java
Celestial.BASE_SIZE = 96  // blocks (diameter)
```

**Usage:**
- `size: 1.0` → 96 blocks diameter
- `size: 15.0` → 1,440 blocks diameter

---

### Distance Constants

```java
Celestial.BASE_ORBIT_DISTANCE = 15_000  // blocks
```

**Usage:**
- `orbitDistance: 1.0` → 15,000 blocks from parent
- `orbitDistance: 0.5` → 7,500 blocks from parent

---

### Time Constants

```java
Celestial.BASE_ORBIT_TIME = 4_608_000    // ticks (≈ 64 hours)
Celestial.BASE_DAY_LENGTH = 24_000       // ticks (20 minutes)
```

**Usage:**
- `orbitTime: 1.0` → 4,608,000 ticks per orbit
- `orbitTime: 0.5` → 2,304,000 ticks per orbit
- `dayLength: 1.0` → 24,000 ticks per day
- `dayLength: 2.0` → 48,000 ticks per day

---

## JSON Schema

### System Config File Format

**Location:** `data/<namespace>/system_config/<filename>.json`

```json
{
  "celestials": [
    {
      "ID": "string",
      "type": "string",
      "size": number,
      "gravity": number,
      "r": number,
      "g": number,
      "b": number,
      "transformProvider": {
        "type": "string",
        ...
      }
    }
  ]
}
```

### Celestial Object Schema

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `ID` | String | Yes | -       | Unique identifier (format: `namespace:name`) |
| `type` | String | Yes | -       | Celestial type ID |
| `size` | Number | Yes | -       | Size multiplier |
| `gravity` | Number | Yes | -       | Gravity multiplier |
| `r` | Number | No | 0.5     | Red component (0.0-1.0) |
| `g` | Number | No | 0.5     | Green component (0.0-1.0) |
| `b` | Number | No | 0.5     | Blue component (0.0-1.0) |
| `transformProvider` | Object | Yes | -       | Transform configuration |

### Transform Provider Types

See [StaticTransformProvider](#statictransformprovider) and [OrbitingTransformProvider](#orbitingtransformprovider) sections above for complete field specifications.

---

## See Also

- [Getting Started Guide](getting-started.md) - Introduction and quick start
- [Implementation Guide](implementation-guide.md) - Step-by-step guide for datapacks and code
- [Advanced Topics](advanced.md) - Custom types and transforms
