# CustomTransformProvider API

The `CustomTransformProvider` API allows you to create celestial bodies with custom position and rotation logic that goes beyond standard orbital mechanics.

## Overview

By default, celestial bodies in Genesis follow orbital mechanics based on their `orbitDistance`, `orbitTime`, and other orbital parameters. The `CustomTransformProvider` allows you to override this behavior with custom logic for both position and rotation.

Both `getCurrentPos()` and `getRotation()` receive the current game time, allowing you to create dynamic, time-dependent transformations.

## Use Cases

- **Fixed position/rotation**: Planets that don't orbit or rotate
- **Non-standard orbits**: Figure-8 orbits, precessing orbits, etc.
- **Custom rotation**: Tidally locked bodies, tumbling asteroids, precessing axes
- **Dynamic behavior**: Bodies that respond to game events or time
- **Testing**: Controlled positions and rotations for unit tests

## How to Use

### Step 1: Implement CustomTransformProvider

Create a class that implements `CustomTransformProvider`:

```java
package com.yourmod.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import shipwrights.genesis.space.CustomTransformProvider;

public class MyCustomProvider implements CustomTransformProvider {

    public static final ResourceLocation TYPE = ResourceLocation.parse("yourmod:my_provider");

    private final double x;
    private final double y;
    private final double z;

    public MyCustomProvider(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    @Override
    public Quaterniondc getRotation(long ticks, float subticks) {
        return new Quaterniond(); // No rotation
    }

    @Override
    public Vector3d getCurrentPos(long ticks, float subticks) {
        // Return your custom position
        return new Vector3d(x, y, z);
    }

    // Define a codec for serialization
    public static final Codec<MyCustomProvider> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(p -> p.x),
            Codec.DOUBLE.fieldOf("y").forGetter(p -> p.y),
            Codec.DOUBLE.fieldOf("z").forGetter(p -> p.z)
        ).apply(instance, MyCustomProvider::new)
    );
}
```

### Step 2: Register Your Provider

Register your provider during mod initialization:

```java
import shipwrights.genesis.space.CustomTransformProvider;

public class YourMod {
    public YourMod() {
        // Register your custom transform provider
        CustomTransformProvider.register(
            MyCustomProvider.TYPE,
            MyCustomProvider.CODEC
        );
    }
}
```

### Step 3: Use in JSON Configuration

Now you can use your provider in celestial body JSON configurations:

```json
{
  "ID": "yourmod:special_planet",
  "parentID": "genesis:sun",
  "size": 1.0,
  "orbitDistance": 100,
  "orbitTime": 1000,
  "gravity": 1.0,
  "r": 0.5,
  "g": 0.7,
  "b": 0.9,

  "customTransform": {
    "type": "yourmod:my_provider",
    "x": 500.0,
    "y": 100.0,
    "z": 200.0
  }
}
```

## Built-in Example

Genesis includes an example implementation at:
- **Class**: `shipwrights.genesis.space.ExampleCustomTransformProvider`
- **Type ID**: `genesis:example_fixed_position`
- **See**: [custom_transform_example.json](custom_transform_example.json)

## Advanced Examples

### Time-Dependent Position

```java
@Override
public Vector3d getCurrentPos(long ticks, float subticks) {
    // Circular orbit with custom radius
    double angle = (ticks + subticks) * Math.PI * 2.0 / 20000.0;
    double radius = 500.0;
    return new Vector3d(
        Math.cos(angle) * radius,
        0,
        Math.sin(angle) * radius
    );
}
```

### Figure-8 Orbit

```java
@Override
public Vector3d getCurrentPos(long ticks, float subticks) {
    double t = (ticks + subticks) * Math.PI * 2.0 / 20000.0;
    return new Vector3d(
        400 * Math.sin(t),
        200 * Math.sin(2 * t),
        0
    );
}
```

### Time-Dependent Rotation

```java
@Override
public Quaterniondc getRotation(long ticks, float subticks) {
    // Rotate based on time - completes one rotation every 20000 ticks (1000 seconds)
    double angle = (ticks + subticks) * Math.PI * 2.0 / 20000.0;
    return new Quaterniond().rotateY(angle);
}
```

### Tidally Locked Rotation (always facing parent)

```java
@Override
public Quaterniondc getRotation(long ticks, float subticks) {
    // Calculate rotation to always face the parent body
    Vector3d pos = getCurrentPos(ticks, subticks);
    Vector3d toParent = new Vector3d(0, 0, 0).sub(pos).normalize();

    // Create rotation that points toward parent
    Quaterniond rotation = new Quaterniond();
    rotation.lookAlong(toParent, new Vector3d(0, 1, 0));
    return rotation;
}
```

### Tumbling Rotation (multiple axes)

```java
@Override
public Quaterniondc getRotation(long ticks, float subticks) {
    // Tumble on multiple axes like an asteroid
    double t = (ticks + subticks) * Math.PI * 2.0 / 20000.0;
    return new Quaterniond()
        .rotateX(t * 1.3)      // Rotate on X axis
        .rotateY(t * 0.7)      // Rotate on Y axis at different speed
        .rotateZ(t * 0.5);     // Rotate on Z axis
}
```

## Important Notes

1. **Stateless & Deterministic**: Both `getCurrentPos()` and `getRotation()` should be pure functions that only depend on the `ticks` and `subticks` parameters. Given the same time, they should always return the same result. **Do not mutate internal state** - create and return new objects each time.

2. **Thread Safety**: Your provider may be called from multiple threads. Because methods should be stateless (see above), thread safety is automatic if you follow that guideline.

3. **Performance**: The methods are called frequently (every frame for rendering). Keep them efficient - avoid expensive calculations when possible.

4. **Serialization**: All fields must be serializable via the Codec. Don't store references to game objects like `Level`, `Entity`, etc. Store only primitive data and configuration values.

5. **Backward Compatibility**: The `customTransform` field is optional. Bodies without it will use standard orbital mechanics.

6. **Client/Server Sync**: Your custom provider will be automatically synchronized to clients via the space registry sync packet.

7. **Return New Objects**: Always return new `Vector3d` and `Quaterniond` instances. Never return a mutable field that could be modified by the caller.

## Testing

You can use CustomTransformProvider in unit tests to create predictable test scenarios:

```java
@Test
public void testRaycast() {
    OrbitingBody body = new OrbitingBody(
        "test:body",
        "test:parent",
        0.5, 50, 1000, 1.0, 1f, 1f, 1f,
        new MyCustomProvider(100, 0, 0) // Fixed position for testing
    );

    // Now you can test with a known position
    // ...
}
```

## API Reference

### CustomTransformProvider Interface

```java
public interface CustomTransformProvider {
    /**
     * Returns the type identifier for this provider.
     * This is used during serialization to determine which codec to use.
     *
     * @return A unique ResourceLocation identifying this provider type
     */
    ResourceLocation getType();

    /**
     * Returns the rotation quaternion for the celestial body at the given time.
     * Should be a pure function - same inputs always produce same output.
     *
     * @param ticks The current game time in ticks
     * @param subticks Partial tick for smooth interpolation (0.0 to 1.0)
     * @return A quaternion representing the celestial body's rotation
     */
    Quaterniondc getRotation(long ticks, float subticks);

    /**
     * Returns the current position of the celestial body at the given time.
     * Should be a pure function - same inputs always produce same output.
     *
     * @param ticks The current game time in ticks
     * @param subticks Partial tick for smooth interpolation (0.0 to 1.0)
     * @return A Vector3d representing the celestial body's position in space
     */
    Vector3d getCurrentPos(long ticks, float subticks);

    /**
     * Registers a custom transform provider type.
     * Call this during mod initialization before any celestial bodies are loaded.
     *
     * @param type The unique identifier for this provider type
     * @param codec The codec used to serialize/deserialize this provider
     */
    static void register(ResourceLocation type, Codec<? extends CustomTransformProvider> codec);
}
```

### Key Points

- **`ticks`**: The game time in ticks (20 ticks = 1 second)
- **`subticks`**: A value between 0.0 and 1.0 representing partial progress through the current tick, used for smooth interpolation between frames
- **Return values**: Always create and return new objects. Don't return mutable fields that could be modified by callers.

## Questions?

For more information or questions about the CustomTransformProvider API, please refer to the Genesis mod documentation or source code.
