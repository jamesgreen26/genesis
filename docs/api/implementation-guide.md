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
      "size": 1400.0,
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
        1400.0,  // Size in blocks
        1.0      // Standard gravity
    );

    // Register it
    event.accept(sun);
}
```

### Explanation

- **ID**: Unique identifier for this celestial (`mymod:sun`)
- **Type**: Using the built-in `genesis:star` type (emits light, not visitable)
- **Size**: 1,400 blocks diameter
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
      "size": 1400.0,
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
      "size": 100.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "mymod:sun",
        "seed": 12345,
        "orbitDistance": 15000.0,
        "orbitTime": 4608000.0
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
        1400.0, 1.0
    );
    event.accept(sun);

    // Create an orbiting planet
    OrbitingTransformProvider planetTransform = new OrbitingTransformProvider(
        ResourceLocation.parse("mymod:sun"),  // parentID
        12345,        // seed
        15000.0,      // orbitDistance in blocks
        4608000.0,    // orbitTime in ticks (≈ 64 hours)
        24000.0       // dayLength in ticks (20 minutes)
    );

    Celestial planet = new Celestial(
        planetTransform,
        ResourceLocation.parse("mymod:earth"),
        CelestialType.get(ResourceLocation.parse("genesis:body")),
        100.0,  // Size in blocks
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
- **orbitDistance**: 15,000 blocks from parent
- **orbitTime**: 4,608,000 ticks per orbit (≈ 64 hours)

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
      "size": 1400.0,
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
      "size": 100.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "mymod:sun",
        "seed": 12345,
        "orbitDistance": 15000.0,
        "orbitTime": 4608000.0
      }
    },
    {
      "ID": "mymod:moon",
      "type": "genesis:body",
      "size": 27.0,
      "gravity": 0.1622,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "mymod:earth",
        "seed": 67890,
        "orbitDistance": 750.0,
        "orbitTime": 23040.0,
        "dayLength": 18000.0
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
        750.0,      // 750 blocks from planet
        23040.0,    // ≈ 19.2 minutes per orbit
        18000.0     // 15-minute day
    );

    Celestial moon = new Celestial(
        moonTransform,
        ResourceLocation.parse("mymod:moon"),
        CelestialType.get(ResourceLocation.parse("genesis:body")),
        27.0,   // Size in blocks
        0.1622  // Moon gravity (about 1/6 Earth)
    );
    event.accept(moon);
}
```

### Explanation

- **Parent Reference**: Moon orbits the planet (`parentID: "mymod:earth"`) not the sun
- **Size**: 27 blocks diameter
- **Gravity**: Lower at 0.1622 (approximately real moon gravity)
- **Orbit Distance**: 750 blocks from planet
- **Orbit Time**: 23,040 ticks (≈ 19.2 minutes per orbit)
- **Day Length**: 18,000 ticks (≈ 15 minutes per rotation)

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
  "orbitDistance": 15000.0,
  "orbitTime": 4608000.0,
  "dayLength": 24000.0
}
```

#### In Code

```java
import shipwrights.genesis.space.transformProvider.OrbitingTransformProvider;

OrbitingTransformProvider transform = new OrbitingTransformProvider(
    parentID,          // ResourceLocation - ID of parent celestial
    seed,              // int - deterministic random seed
    orbitDistance,     // double - orbit radius in blocks
    orbitTime,         // double - orbital period in ticks
    dayLength          // double - axial rotation period in ticks (0 = tidally locked)
);
```

#### Property Reference

| Field/Parameter | Type | Required | Default | Description |
|-----------------|------|----------|---------|-------------|
| `type` | String | Yes | - | Must be `"genesis:orbiting"` |
| `parentID` / first param | String/ResourceLocation | Yes | - | ID of the celestial to orbit around |
| `seed` / second param | Integer/int | Yes | - | Random seed for deterministic orbital angles |
| `orbitDistance` / third param | Number/double | Yes | - | Orbit radius in blocks |
| `orbitTime` / fourth param | Number/double | Yes | - | Orbital period in ticks |
| `dayLength` / fifth param | Number/double | No | 1.0 | Day length in ticks (0 = tidally locked) |

#### Understanding the Parameters

`orbitDistance`, `orbitTime`, and `dayLength` are **absolute values**, not multipliers:

- **orbitDistance** — radius of the orbit in blocks (e.g., `15000.0` = 15,000 blocks from parent)
- **orbitTime** — time for one full orbit in ticks (e.g., `4608000` ≈ 64 hours)
- **dayLength** — time for one full axial rotation in ticks (e.g., `24000` = 20 minutes); set to `0` for tidal locking (one face always points toward parent)

---

## Celestial Types

Celestial types define the behavioral properties and rendering of celestials. They determine whether a celestial emits light, casts shadows, and whether players can visit it. Addon mods can register additional types with custom rendering and behavior.

### Built-in Types

Genesis provides three built-in celestial types:

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

#### `genesis:blackhole`
- **Emits Light**: No
- **Casts Shadow**: No
- **Visitable**: No
- **Rendering**: Blackhole shader effect
- **Use For**: Black holes

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
- `size: 100.0` = 100 blocks (Earth-sized planet)
- `size: 1400.0` = 1,400 blocks (typical sun)
- `size: 27.0` = 27 blocks (typical moon)

**Usage:**
- Datapacks: `"size": 100.0`
- Code: `100.0` (as parameter)

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

**Description:** Color components ranging from 0.0 to 1.0. Not currently used by the renderer, but reserved for future features such as map tinting.

**Examples:**
- Sun: `(1.0, 0.95, 0.8)` - warm yellow
- Earth: `(0.3, 0.5, 0.8)` - blue
- Mars: `(0.9, 0.4, 0.3)` - red
- Moon: `(0.7, 0.7, 0.7)` - gray

**Usage:**
- Datapacks: Optional fields `"r"`, `"g"`, `"b"` (default: 0.5 each)
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

### Typical Reference Values

These are the values used by the default Genesis solar system as a reference point:

| Parameter | Default system value | Description |
|-----------|---------------------|-------------|
| `orbitDistance` | 15,000 blocks | Earth-sun distance |
| `orbitTime` | 4,608,000 ticks | Earth year (≈ 64 hours) |
| `dayLength` | 24,000 ticks | Earth day (20 minutes) |

---

## Textures

**Note:** This section applies to resource packs or mods (client-side assets). Textures work with celestials defined in either datapacks or code.

Custom textures can be added for celestial bodies using the `genesis:body` type (planets and moons). The planet renderer automatically looks for textures based on the celestial's ID.

### Texture Location

**Important:** Textures are client-side assets and must be added via a **resource pack** or **mod** (not a datapack). Your datapack provides the celestial configuration (JSON), while textures are provided separately through client-side resources.

Textures must be placed under the **`genesis` namespace**, regardless of which mod or datapack defines the celestial:

```
assets/genesis/textures/planets/<planet_namespace>/<planet_name>.png
```

> **Important:** The root namespace is always `genesis`. Using `assets/mymod/...` will not work — the renderer only looks in `assets/genesis/textures/planets/`.

**Examples:**
- `assets/genesis/textures/planets/minecraft/overworld.png` — for celestial ID `minecraft:overworld`
- `assets/genesis/textures/planets/genesis/moon.png` — for celestial ID `genesis:moon`
- `assets/genesis/textures/planets/mymod/custom_planet.png` — for celestial ID `mymod:custom_planet`

The texture path is automatically derived from the celestial's ID. For a celestial with ID `mymod:custom_planet`, the renderer looks for:

```
assets/genesis/textures/planets/mymod/custom_planet.png
```

**Workflow:** Define your celestial in a datapack JSON (server-side), then add matching textures in a resource pack (client-side).

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
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import net.minecraft.resources.ResourceLocation;

// Get a specific celestial by ID
Celestial sun = GenesisMod.SPACE_REGISTRY.get(ResourceLocation.parse("genesis:sun"));
```

### Getting All Celestials

```java
import java.util.List;

// Get all registered celestials
List<Celestial> allCelestials = GenesisMod.SPACE_REGISTRY.getAll();
```

### Filtering Celestials by Type

```java
// Get all stars
Collection<Celestial> stars = GenesisMod.SPACE_REGISTRY.getWhere(type ->
    type.castsLight()
);

// Get all visitable bodies (planets and moons)
Collection<Celestial> planets = GenesisMod.SPACE_REGISTRY.getWhere(type ->
    type.isVisitable()
);

// Get celestials of a specific type
Collection<Celestial> bodies = GenesisMod.SPACE_REGISTRY.getWhere(type ->
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
      "size": 1440,
      "gravity": 2.0,
      "transformProvider": {
        "type": "genesis:static",
        "x": 0.0,
        "y": 0.0,
        "z": 0.0,
        "xRot": 15,
        "yRot": 45,
        "zRot": 5
      }
    },
    {
      "ID": "minecraft:overworld",
      "type": "genesis:body",
      "size": 96,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "genesis:sun",
        "seed": 12345,
        "orbitDistance": 15000,
        "orbitTime": 4600000,
        "dayLength" : 24000
      }
    },
    {
      "ID": "genesis:moon",
      "type": "genesis:body",
      "size": 29,
      "gravity": 0.1622,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "minecraft:overworld",
        "seed": 67890,
        "orbitDistance": 750,
        "orbitTime": 184329,
        "dayLength": 0
      }
    },
    {
      "ID": "genesis:testbh",
      "type": "genesis:blackhole",
      "size": 2048,
      "gravity": 8,
      "transformProvider": {
        "type": "genesis:static",
        "x": 0.0,
        "y": -200000.0,
        "z": -200000.0
      }
    }
  ]
}
```

This creates:
- A large sun at the origin
- The Minecraft Overworld orbiting the sun at standard distance and period
- A small moon orbiting the Overworld with lower gravity
- A large black hole very far away

### In Code (Equivalent)

```java
private static void registerCelestials(SpaceRegistry.RegisterCelestialsEvent event) {
    // Sun
    StaticTransformProvider sunTransform = new StaticTransformProvider(0, 0, 0);
    Celestial sun = new Celestial(
        sunTransform,
        ResourceLocation.parse("genesis:sun"),
        CelestialType.get(ResourceLocation.parse("genesis:star")),
        1400.0, 1.0,
        1.0f, 1.0f, 1.0f
    );
    event.accept(sun);

    // Overworld
    OrbitingTransformProvider overworldTransform = new OrbitingTransformProvider(
        ResourceLocation.parse("genesis:sun"),
        12345, 15000.0, 4608000.0, 24000.0
    );
    Celestial overworld = new Celestial(
        overworldTransform,
        ResourceLocation.parse("minecraft:overworld"),
        CelestialType.get(ResourceLocation.parse("genesis:body")),
        100.0, 1.0,
        0.8f, 0.8f, 0.8f
    );
    event.accept(overworld);

    // Moon
    OrbitingTransformProvider moonTransform = new OrbitingTransformProvider(
        ResourceLocation.parse("minecraft:overworld"),
        67890, 750.0, 23040.0, 18000.0
    );
    Celestial moon = new Celestial(
        moonTransform,
        ResourceLocation.parse("genesis:moon"),
        CelestialType.get(ResourceLocation.parse("genesis:body")),
        27.0, 0.1622,
        0.7f, 0.7f, 0.7f
    );
    event.accept(moon);

    // Black hole
    StaticTransformProvider blackholeTransform = new StaticTransformProvider(0, -200000, -200000);
    Celestial blackhole = new Celestial(
        blackholeTransform,
        ResourceLocation.parse("genesis:testbh"),
        CelestialType.get(ResourceLocation.parse("genesis:blackhole")),
        2048.0, 8.0
    );
    event.accept(blackhole);
}
```

---

## Next Steps

- **Want to create custom celestial types or transform providers?** See [Advanced Topics](advanced.md)
- **Need detailed API documentation?** Check the [API Reference](api-reference.md)
- **New to celestials?** Review the [Getting Started Guide](getting-started.md)
