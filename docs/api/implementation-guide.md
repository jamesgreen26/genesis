# Implementation Guide: Creating Celestials
---
Genesis allows you to add custom stars and planets (celestials) to the solar system through JSON configuration files or programmatically through code.

## Setup

### Datapack Setup

Celestial configurations are stored in JSON files within your datapack's `system_config` directory:

```
data/
└── <namespace>/
    └── system_config/
        └── <filename>.json
```

**Examples:**
- `data/genesis/system_config/builtin.json` (Genesis default system)
- `data/mymod/system_config/solar_system.json` (Your custom system)

You can have multiple JSON files - all celestials from all files will be loaded and registered.

**Basic JSON Structure:**

```json
{
  "celestials": [
    {
      "ID": "namespace:celestial_id",
      "type": "namespace:type_id",
      "size": 1.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "provider_type",
        ...
      }
    }
  ]
}
```

### Code Setup

Genesis provides an event-based system for registering celestials. Subscribe to the event during your mod's initialization:

```java
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.registry.SpaceRegistry;

public class YourMod {
    public static void init() {
        GenesisMod.onRegisterCelestialsEvent(YourMod::registerCelestials);
    }

    private static void registerCelestials(SpaceRegistry.RegisterCelestialsEvent event) {
        // Register your celestials here
    }
}
```

**Location in Genesis source:** `src/main/java/shipwrights/genesis/GenesisMod.java:88-90`

---

## Creating Your First Celestial: A Simple Star

Let's create a star at the center of your solar system. Stars are celestial bodies that emit light and affect day/night cycles on orbiting bodies.

### In Datapacks

```json
{
  "celestials": [
    {
      "ID": "mymod:sun",
      "type": "genesis:star",
      "size": 15.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:static",
        "x": 0.0,
        "y": 0.0,
        "z": 0.0
      }
    }
  ]
}
```

### In Code

```java
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.type.CelestialType;
import shipwrights.genesis.space.transformProvider.StaticTransformProvider;

private static void registerCelestials(SpaceRegistry.RegisterCelestialsEvent event) {
    // Create a static transform at the origin
    StaticTransformProvider sunTransform = new StaticTransformProvider(0, 0, 0);

    // Create the star celestial
    Celestial sun = new Celestial(
        sunTransform,
        ResourceLocation.parse("mymod:sun"),
        CelestialType.get(ResourceLocation.parse("genesis:star")),
        15.0,   // Large size
        1.0    // Standard gravity
    );

    // Register it
    event.accept(sun);
}
```

### Explanation

- **ID**: Unique identifier for this celestial (`mymod:sun`)
- **Type**: Using the built-in `genesis:star` type (emits light, not visitable)
- **Size**: 15x the base size (96 blocks), making it 1,440 blocks diameter
- **Gravity**: Standard gravity value (1.0)
- **Transform Provider**: Static position at coordinates (0, 0, 0) - the celestial never moves

---

## Creating an Orbiting Planet

Now let's add a planet that orbits around your sun. Planets use the orbiting transform provider to move in a circular orbit around a parent celestial.

### In Datapacks

```json
{
  "celestials": [
    {
      "ID": "mymod:sun",
      "type": "genesis:star",
      "size": 15.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:static",
        "x": 0.0,
        "y": 0.0,
        "z": 0.0
      }
    },
    {
      "ID": "mymod:earth",
      "type": "genesis:body",
      "size": 1.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "mymod:sun",
        "seed": 12345,
        "orbitDistance": 1.0,
        "orbitTime": 1.0
      }
    }
  ]
}
```

### In Code

```java
private static void registerCelestials(SpaceRegistry.RegisterCelestialsEvent event) {
    // Register the sun first (same as previous example)
    StaticTransformProvider sunTransform = new StaticTransformProvider(0, 0, 0);
    Celestial sun = new Celestial(
        sunTransform,
        ResourceLocation.parse("mymod:sun"),
        CelestialType.get(ResourceLocation.parse("genesis:star")),
        15.0, 1.0
    );
    event.accept(sun);

    // Create an orbiting planet
    OrbitingTransformProvider planetTransform = new OrbitingTransformProvider(
        ResourceLocation.parse("mymod:sun"),  // parentID
        12345,      // seed
        1.0,        // orbitDistance multiplier
        1.0,        // orbitTime multiplier
        1.0         // dayLength multiplier
    );

    Celestial planet = new Celestial(
        planetTransform,
        ResourceLocation.parse("mymod:earth"),
        CelestialType.get(ResourceLocation.parse("genesis:body")),
        1.0,    // Earth-sized
        1.0     // Earth gravity
    );
    event.accept(planet);
}
```

### Explanation

- **Type**: The planet uses `genesis:body` (solid, visitable) instead of `genesis:star`
- **Transform Provider**: Now uses `orbiting` type instead of `static`
- **parentID**: References the sun - the planet will orbit around it
- **seed**: Determines the random orbital angles (same seed = same orbit)
- **orbitDistance**: 1.0x the base distance (15,000 blocks from parent)
- **orbitTime**: 1.0x the base orbital period (4,608,000 ticks ≈ 64 hours)

---

## Adding a Moon

Moons orbit planets, not the sun. This creates a hierarchical orbital system where the moon follows the planet as it orbits.

### In Datapacks

```json
{
  "celestials": [
    {
      "ID": "mymod:sun",
      "type": "genesis:star",
      "size": 15.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:static",
        "x": 0.0,
        "y": 0.0,
        "z": 0.0
      }
    },
    {
      "ID": "mymod:earth",
      "type": "genesis:body",
      "size": 1.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "mymod:sun",
        "seed": 12345,
        "orbitDistance": 1.0,
        "orbitTime": 1.0
      }
    },
    {
      "ID": "mymod:moon",
      "type": "genesis:body",
      "size": 0.3,
      "gravity": 0.1622,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "mymod:earth",
        "seed": 67890,
        "orbitDistance": 0.05,
        "orbitTime": 0.005,
        "dayLength": 0.75
      }
    }
  ]
}
```

### In Code

```java
private static void registerCelestials(SpaceRegistry.RegisterCelestialsEvent event) {
    // Register sun (as above)
    // ...

    // Register planet (as above)
    // ...

    // Create a moon orbiting the planet
    OrbitingTransformProvider moonTransform = new OrbitingTransformProvider(
        ResourceLocation.parse("mymod:earth"),  // Orbit the planet, not the sun!
        67890,      // Different seed for different orbit
        0.05,       // Much closer orbit
        0.005,      // Much faster orbit
        0.75        // Slower day length
    );

    Celestial moon = new Celestial(
        moonTransform,
        ResourceLocation.parse("mymod:moon"),
        CelestialType.get(ResourceLocation.parse("genesis:body")),
        0.3,        // Smaller size
        0.1622      // Moon gravity (about 1/6 Earth)
    );
    event.accept(moon);
}
```

### Explanation

- **Parent Reference**: Moon orbits the planet (`parentID: "mymod:earth"`) not the sun
- **Size**: Smaller at 0.3 (28.8 blocks diameter)
- **Gravity**: Lower at 0.1622 (approximately real moon gravity)
- **Orbit Distance**: Much closer at 0.05x base distance (750 blocks from planet)
- **Orbit Time**: Much faster at 0.005x base time (23,040 ticks ≈ 19.2 minutes)
- **Day Length**: Custom rotation speed at 0.75x (18,000 ticks ≈ 15 minutes)

---

## Transform Providers

Transform providers control how a celestial moves and rotates over time. Genesis includes two built-in providers: static and orbiting.

### Static Transform Provider

**Use Case:** For celestials that stay in one place, like a sun at the center of a solar system.

#### In Datapacks

**Position Only:**
```json
"transformProvider": {
  "type": "genesis:static",
  "x": 0.0,
  "y": 0.0,
  "z": 0.0
}
```

**Position with Rotation:**
```json
"transformProvider": {
  "type": "genesis:static",
  "x": 0.0,
  "y": 0.0,
  "z": 0.0,
  "xRot": 0.0,
  "yRot": 0.0,
  "zRot": 0.0
}
```

#### In Code

```java
import shipwrights.genesis.space.transformProvider.StaticTransformProvider;

// Position only
StaticTransformProvider transform1 = new StaticTransformProvider(x, y, z);

// Position with rotation (radians)
StaticTransformProvider transform2 = new StaticTransformProvider(
    x, y, z,
    xRot, yRot, zRot
);
```

#### Property Reference

| Field/Parameter | Type | Required | Default | Description |
|-----------------|------|----------|---------|-------------|
| `type` | String | Yes | - | Must be `"genesis:static"` |
| `x` / first param | Number/double | Yes | - | X coordinate |
| `y` / second param | Number/double | Yes | - | Y coordinate |
| `z` / third param | Number/double | Yes | - | Z coordinate |
| `xRot` / fourth param | Number/double | No | 0.0 | Rotation around X-axis (radians) |
| `yRot` / fifth param | Number/double | No | 0.0 | Rotation around Y-axis (radians) |
| `zRot` / sixth param | Number/double | No | 0.0 | Rotation around Z-axis (radians) |

### Orbiting Transform Provider

**Use Case:** For celestials that orbit around a parent body, like planets orbiting a sun or moons orbiting a planet.

#### In Datapacks

```json
"transformProvider": {
  "type": "genesis:orbiting",
  "parentID": "namespace:parent_celestial",
  "seed": 12345,
  "orbitDistance": 1.0,
  "orbitTime": 1.0,
  "dayLength": 1.0
}
```

#### In Code

```java
import shipwrights.genesis.space.transformProvider.OrbitingTransformProvider;

OrbitingTransformProvider transform = new OrbitingTransformProvider(
    parentID,          // ResourceLocation - ID of parent celestial
    seed,              // int - deterministic random seed
    orbitDistance,     // double - multiplier (base: 15,000 blocks)
    orbitTime,         // double - multiplier (base: 4,608,000 ticks)
    dayLength          // double - multiplier (base: 24,000 ticks)
);
```

#### Property Reference

| Field/Parameter | Type | Required | Default | Description |
|-----------------|------|----------|---------|-------------|
| `type` | String | Yes | - | Must be `"genesis:orbiting"` |
| `parentID` / first param | String/ResourceLocation | Yes | - | ID of the celestial to orbit around |
| `seed` / second param | Integer/int | Yes | - | Random seed for orbital angles and rotation |
| `orbitDistance` / third param | Number/double | Yes | - | Orbit distance multiplier (base: 15,000 blocks) |
| `orbitTime` / fourth param | Number/double | Yes | - | Orbital period multiplier (base: 4,608,000 ticks) |
| `dayLength` / fifth param | Number/double | No | 1.0 | Day length multiplier (base: 24,000 ticks) |

#### Understanding the Multipliers

The orbiting transform provider uses base constants that are multiplied by your values:

- **orbitDistance**
  - Base: 15,000 blocks
  - `1.0` = 15,000 blocks from parent
  - `0.5` = 7,500 blocks from parent
  - `2.0` = 30,000 blocks from parent

- **orbitTime**
  - Base: 4,608,000 ticks (64 hours)
  - `1.0` = 4,608,000 ticks per orbit
  - `0.5` = 2,304,000 ticks (32 hours) per orbit
  - `2.0` = 9,216,000 ticks (128 hours) per orbit

- **dayLength**
  - Base: 24,000 ticks (20 minutes)
  - `1.0` = 24,000 ticks per rotation
  - `0.5` = 12,000 ticks (10 minutes) per rotation
  - `2.0` = 48,000 ticks (40 minutes) per rotation

These base values are defined in the `Celestial` class as `BASE_ORBIT_DISTANCE`, `BASE_ORBIT_TIME`, and `BASE_DAY_LENGTH`.

---

## Celestial Types

Celestial types define the behavioral properties and rendering of celestials. They determine whether a celestial emits light, casts shadows, and whether players can visit it.

### Built-in Types

Genesis provides two built-in celestial types:

#### `genesis:star`
- **Emits Light**: Yes (casts light on other celestials)
- **Casts Shadow**: No
- **Visitable**: No (players can't land on stars)
- **Rendering**: Glowing effects
- **Use For**: Suns and other light sources

#### `genesis:body`
- **Emits Light**: No
- **Casts Shadow**: Yes (blocks light from stars)
- **Visitable**: Yes (players can land on these)
- **Rendering**: Surface textures
- **Use For**: Planets, moons, large asteroids

### In Datapacks

Specify the type using its resource location ID:

```json
{
  "ID": "mymod:sun",
  "type": "genesis:star",
  ...
}
```

or

```json
{
  "ID": "mymod:earth",
  "type": "genesis:body",
  ...
}
```

### In Code

Retrieve types using `CelestialType.get()`:

```java
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.space.type.CelestialType;

// Star type - emits light, not visitable
CelestialType star = CelestialType.get(ResourceLocation.parse("genesis:star"));

// Body type - solid, visitable, casts shadows
CelestialType body = CelestialType.get(ResourceLocation.parse("genesis:body"));
```

### Custom Types

For creating your own celestial types with custom rendering and behavior, see the [Advanced Topics](advanced.md) guide.

---

## Understanding Properties

Every celestial has physical properties that define its size, gravity, and appearance. These properties work the same way in both datapacks and code.

### Size

**Description:** Size multiplier on the base diameter of 96 blocks.

**Examples:**
- `size: 1.0` = 96 blocks diameter (Earth-sized)
- `size: 15.0` = 1,440 blocks diameter (typical sun)
- `size: 0.3` = 28.8 blocks diameter (typical moon)

**Usage:**
- Datapacks: `"size": 1.0`
- Code: `1.0` (as parameter)

### Gravity

**Description:** Gravity strength relative to Earth (1.0 = Earth gravity).

**Examples:**
- `gravity: 1.0` = Earth gravity
- `gravity: 0.38` = Mars/Mercury gravity
- `gravity: 0.1622` = Moon gravity (about 1/6 Earth)
- `gravity: 2.5` = High gravity gas giant

**Usage:**
- Datapacks: `"gravity": 1.0`
- Code: `1.0` (as parameter)

### Color (RGB)

**Description:** Color components ranging from 0.0 to 1.0. Currently has limited use but will have future applications for rendering and effects.

**Examples:**
- Sun: `(1.0, 0.95, 0.8)` - warm yellow
- Earth: `(0.3, 0.5, 0.8)` - blue
- Mars: `(0.9, 0.4, 0.3)` - red
- Moon: `(0.7, 0.7, 0.7)` - gray

**Usage:**
- Datapacks: Optional fields `"r"`, `"g"`, `"b"` (default: 0.8 each)
  ```json
  {
    ...
    "r": 0.3,
    "g": 0.5,
    "b": 0.8
  }
  ```
- Code: Required float parameters in constructor
  ```java
  new Celestial(
      transform, id, type, size, gravity,
      0.3f,  // red
      0.5f,  // green
      0.8f   // blue
  );
  ```

### Base Constants

Genesis defines several base constants used as multipliers throughout the system:

| Constant | Value | Description |
|----------|-------|-------------|
| `BASE_SIZE` | 96 blocks | Base celestial diameter |
| `BASE_ORBIT_DISTANCE` | 15,000 blocks | Base orbital distance |
| `BASE_ORBIT_TIME` | 4,608,000 ticks | Base orbital period (≈ 64 hours) |
| `BASE_DAY_LENGTH` | 24,000 ticks | Base rotation period (20 minutes) |

**In Code:** These are accessible as static fields on the `Celestial` class:
```java
import shipwrights.genesis.space.Celestial;

double actualSize = sizeMultiplier * Celestial.BASE_SIZE;
double actualDistance = distanceMultiplier * Celestial.BASE_ORBIT_DISTANCE;
```

---

## Textures

**Note:** This section applies to resource packs or mods (client-side assets). Textures work with celestials defined in either datapacks or code.

Custom textures can be added for celestial bodies using the `genesis:body` type (planets and moons). The planet renderer automatically looks for textures based on the celestial's ID.

### Texture Location

**Important:** Textures are client-side assets and must be added via a **resource pack** or **mod** (not a datapack). Your datapack provides the celestial configuration (JSON), while textures are provided separately through client-side resources.

Place PNG texture files in a resource pack or mod:

```
assets/<namespace>/textures/planets/<namespace>/<body_name>.png
```

**Examples (in a resource pack or mod):**
- `assets/genesis/textures/planets/minecraft/overworld.png`
- `assets/genesis/textures/planets/genesis/moon.png`
- `assets/mymod/textures/planets/mymod/custom_planet.png`

The texture path is automatically derived from the body's ID. For a celestial with ID `mymod:custom_planet`, the renderer looks for:

```
assets/genesis/textures/planets/mymod/custom_planet.png
```

**Workflow:** Define your celestial in a datapack JSON (server-side), then add matching textures in a resource pack (client-side). If no texture is found, the body will be rendered using the RGB color values specified in the configuration.

### Texture Layout

Planet and moon textures use a **cube map layout** arranged in a **3×2 grid**. The texture should be divided into 6 equal sections representing the faces of a cube:

**Top Row (upper half of texture):**
- Left third: **North face**
- Middle third: **West face**
- Right third: **South face**

**Bottom Row (lower half of texture):**
- Left third: **East face**
- Middle third: **Down face** (bottom of the planet)
- Right third: **Up face** (top of the planet)

```
┌─────────┬─────────┬─────────┐
│  North  │  West   │  South  │  ← Top row
├─────────┼─────────┼─────────┤
│  East   │  Down   │   Up    │  ← Bottom row
└─────────┴─────────┴─────────┘
```

### Texture Guidelines

When creating your texture:

- **Dimensions**: Should be divisible by 3 horizontally and by 2 vertically for clean mapping
- **Edge matching**: Adjacent faces should have matching edges where they meet on the cube
- **Orientation**: The Up and Down faces represent the top and bottom of the planet; the four cardinal faces (North, East, South, West) wrap around the sides
- **Recommended sizes**: Use powers of 2 for best results (e.g., 192×128, 384×256, 768×512) to ensure each face section divides evenly

---

## Querying Celestials

**Note:** This section is specific to code/mod development.

Once celestials are registered, you can retrieve and query them using the `SpaceRegistry` class.

### Getting a Specific Celestial

```java
import shipwrights.genesis.space.registry.SpaceRegistry;
import shipwrights.genesis.space.Celestial;
import net.minecraft.resources.ResourceLocation;

// Get a specific celestial by ID
Celestial sun = SpaceRegistry.get(ResourceLocation.parse("genesis:sun"));
```

### Getting All Celestials

```java
import java.util.List;

// Get all registered celestials
List<Celestial> allCelestials = SpaceRegistry.getAll();
```

### Filtering Celestials by Type

```java
// Get all stars
List<Celestial> stars = SpaceRegistry.getWhere(type ->
    type.castsLight()
);

// Get all visitable bodies (planets and moons)
List<Celestial> planets = SpaceRegistry.getWhere(type ->
    type.isVisitable()
);

// Get celestials of a specific type
List<Celestial> bodies = SpaceRegistry.getWhere(type ->
    type.getID().equals(ResourceLocation.parse("genesis:body"))
);
```

---

## Complete Example: Genesis Solar System

Here's the complete default solar system that comes with Genesis, showing how everything comes together.

### In Datapacks

From `builtin.json`:

```json
{
  "celestials": [
    {
      "ID": "genesis:sun",
      "type": "genesis:star",
      "size": 15.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:static",
        "x": 0.0,
        "y": 0.0,
        "z": 0.0
      }
    },
    {
      "ID": "minecraft:overworld",
      "type": "genesis:body",
      "size": 1.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "genesis:sun",
        "seed": 12345,
        "orbitDistance": 1.0,
        "orbitTime": 1.0
      }
    },
    {
      "ID": "genesis:moon",
      "type": "genesis:body",
      "size": 0.3,
      "gravity": 0.1622,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "minecraft:overworld",
        "seed": 67890,
        "orbitDistance": 0.05,
        "orbitTime": 0.005,
        "dayLength": 0.75
      }
    }
  ]
}
```

This creates:
- A large sun at the origin
- The Minecraft Overworld orbiting the sun at standard distance and period
- A small moon orbiting the Overworld with lower gravity

### In Code (Equivalent)

```java
private static void registerCelestials(SpaceRegistry.RegisterCelestialsEvent event) {
    // Sun
    StaticTransformProvider sunTransform = new StaticTransformProvider(0, 0, 0);
    Celestial sun = new Celestial(
        sunTransform,
        ResourceLocation.parse("genesis:sun"),
        CelestialType.get(ResourceLocation.parse("genesis:star")),
        15.0, 1.0,
        1.0f, 1.0f, 1.0f
    );
    event.accept(sun);

    // Overworld
    OrbitingTransformProvider overworldTransform = new OrbitingTransformProvider(
        ResourceLocation.parse("genesis:sun"),
        12345, 1.0, 1.0, 1.0
    );
    Celestial overworld = new Celestial(
        overworldTransform,
        ResourceLocation.parse("minecraft:overworld"),
        CelestialType.get(ResourceLocation.parse("genesis:body")),
        1.0, 1.0,
        0.8f, 0.8f, 0.8f
    );
    event.accept(overworld);

    // Moon
    OrbitingTransformProvider moonTransform = new OrbitingTransformProvider(
        ResourceLocation.parse("minecraft:overworld"),
        67890, 0.05, 0.005, 0.75
    );
    Celestial moon = new Celestial(
        moonTransform,
        ResourceLocation.parse("genesis:moon"),
        CelestialType.get(ResourceLocation.parse("genesis:body")),
        0.3, 0.1622,
        0.7f, 0.7f, 0.7f
    );
    event.accept(moon);
}
```

---

## Next Steps

- **Want to create custom celestial types or transform providers?** See [Advanced Topics](advanced.md)
- **Need detailed API documentation?** Check the [API Reference](api-reference.md)
- **New to celestials?** Review the [Getting Started Guide](getting-started.md)
