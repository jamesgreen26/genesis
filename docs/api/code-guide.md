# Code Guide: Registering Celestials in Java

This guide shows mod developers how to register celestials programmatically using Java code. This approach gives you full control and allows you to create dynamic celestials based on runtime conditions.

## Prerequisites

- Java development environment
- Genesis mod added as a dependency in your mod
- Basic understanding of Minecraft mod development

## Registration Entry Point

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

## Creating Celestials

### Basic Constructor

The `Celestial` constructor requires these parameters:

```java
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.type.CelestialType;
import shipwrights.genesis.space.transformProvider.CelestialTransformProvider;

Celestial celestial = new Celestial(
    transformProvider,      // CelestialTransformProvider
    ResourceLocation.parse("yourmod:celestial_id"),  // ResourceLocation
    CelestialType.get(ResourceLocation.parse("genesis:star")),  // CelestialType
    15.0,                   // size (double)
    1.0,                    // gravity (double)
    1.0f,                   // r (red, float 0-1)
    0.9f,                   // g (green, float 0-1)
    0.7f                    // b (blue, float 0-1)
);
```

## Example 1: Registering a Simple Star

Here's how to create a star at the center of your solar system:

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
        ResourceLocation.parse("yourmod:sun"),
        CelestialType.get(ResourceLocation.parse("genesis:star")),
        15.0,   // Large size
        1.0,    // Standard gravity
        1.0f,   // Red
        0.95f,  // Green
        0.8f    // Blue (warm yellow color)
    );

    // Register it
    event.accept(sun);
}
```

## Example 2: Creating an Orbiting Planet

Now let's add a planet that orbits the sun:

```java
import shipwrights.genesis.space.transformProvider.OrbitingTransformProvider;

private static void registerCelestials(SpaceRegistry.RegisterCelestialsEvent event) {
    // Register the sun first (same as Example 1)
    StaticTransformProvider sunTransform = new StaticTransformProvider(0, 0, 0);
    Celestial sun = new Celestial(
        sunTransform,
        ResourceLocation.parse("yourmod:sun"),
        CelestialType.get(ResourceLocation.parse("genesis:star")),
        15.0, 1.0,
        1.0f, 0.95f, 0.8f
    );
    event.accept(sun);

    // Create an orbiting planet
    OrbitingTransformProvider planetTransform = new OrbitingTransformProvider(
        ResourceLocation.parse("yourmod:sun"),  // parentID
        12345,      // seed
        1.0,        // orbitDistance multiplier
        1.0,        // orbitTime multiplier
        1.0         // dayLength multiplier
    );

    Celestial planet = new Celestial(
        planetTransform,
        ResourceLocation.parse("yourmod:earth"),
        CelestialType.get(ResourceLocation.parse("genesis:body")),
        1.0,    // Earth-sized
        1.0,    // Earth gravity
        0.3f,   // Red
        0.5f,   // Green
        0.8f    // Blue (blue-ish planet)
    );
    event.accept(planet);
}
```

## Example 3: Adding a Moon

Moons orbit planets, not the sun:

```java
private static void registerCelestials(SpaceRegistry.RegisterCelestialsEvent event) {
    // Register sun (as above)
    // ...

    // Register planet (as above)
    // ...

    // Create a moon orbiting the planet
    OrbitingTransformProvider moonTransform = new OrbitingTransformProvider(
        ResourceLocation.parse("yourmod:earth"),  // Orbit the planet, not the sun!
        67890,      // Different seed for different orbit
        0.05,       // Much closer orbit
        0.005,      // Much faster orbit
        0.75        // Slower day length
    );

    Celestial moon = new Celestial(
        moonTransform,
        ResourceLocation.parse("yourmod:moon"),
        CelestialType.get(ResourceLocation.parse("genesis:body")),
        0.3,        // Smaller size
        0.1622,     // Moon gravity (about 1/6 Earth)
        0.7f,       // Red
        0.7f,       // Green
        0.7f        // Blue (grayish)
    );
    event.accept(moon);
}
```

## Working with Transform Providers

### Static Transform Provider

For celestials that don't move:

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

### Orbiting Transform Provider

For celestials that orbit around a parent:

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

**Understanding the multipliers:**
- `orbitDistance`: 1.0 = 15,000 blocks from parent
- `orbitTime`: 1.0 = 4,608,000 ticks (64 hours) per orbit
- `dayLength`: 1.0 = 24,000 ticks (20 minutes) per rotation

These values are defined in `Celestial.BASE_ORBIT_DISTANCE`, `Celestial.BASE_ORBIT_TIME`, and `Celestial.BASE_DAY_LENGTH`.

## Working with Celestial Types

Genesis provides two built-in types:

```java
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.space.type.CelestialType;

// Star type - emits light, not visitable
CelestialType star = CelestialType.get(ResourceLocation.parse("genesis:star"));

// Body type - solid, visitable, casts shadows
CelestialType body = CelestialType.get(ResourceLocation.parse("genesis:body"));
```

For custom types, see the [Advanced Topics](advanced.md) guide.

## Understanding Properties

### Size
- Size is a multiplier on the base size (96 blocks diameter)
- `size: 1.0` = 96 blocks diameter
- `size: 15.0` = 1,440 blocks diameter (typical sun)
- `size: 0.3` = 28.8 blocks diameter (typical moon)

### Gravity
- Gravity strength relative to Earth
- `gravity: 1.0` = Earth gravity
- `gravity: 0.38` = Mars/Mercury gravity
- `gravity: 0.1622` = Moon gravity

### Color (RGB)
- Each component ranges from 0.0 to 1.0
- Doesn't do much right now, but will have uses in the future
- Examples:
  - Sun: `(1.0, 0.95, 0.8)` - warm yellow
  - Earth: `(0.3, 0.5, 0.8)` - blue
  - Mars: `(0.9, 0.4, 0.3)` - red
  - Moon: `(0.7, 0.7, 0.7)` - gray

## Querying Registered Celestials

You can retrieve and query celestials after registration:

```java
import shipwrights.genesis.space.registry.SpaceRegistry;
import shipwrights.genesis.space.Celestial;
import net.minecraft.resources.ResourceLocation;

// Get a specific celestial by ID
Celestial sun = SpaceRegistry.get(ResourceLocation.parse("genesis:sun"));

// Get all celestials
List<Celestial> allCelestials = SpaceRegistry.getAll();

// Filter celestials by type
List<Celestial> stars = SpaceRegistry.getWhere(type ->
    type.getID().equals(ResourceLocation.parse("genesis:star"))
);
```

## Next Steps

- **Want to create custom celestial types?** See [Advanced Topics](advanced.md)
- **Need detailed API documentation?** Check the [API Reference](api-reference.md)
- **Prefer using datapacks?** Review the [Datapack Guide](datapack-guide.md)
