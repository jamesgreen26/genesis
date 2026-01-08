# CustomTransformProvider API

The `CustomTransformProvider` API allows you to create celestial bodies with custom position and rotation logic that goes beyond standard orbital mechanics.

## Overview

By default, celestial bodies in Genesis follow orbital mechanics based on their `orbitDistance`, `orbitTime`, and other orbital parameters. The `CustomTransformProvider` allows you to override this behavior with custom logic.

## Use Cases

- **Fixed position bodies**: Planets that don't orbit
- **Non-standard orbits**: Figure-8 orbits, precessing orbits, etc.
- **Dynamic positions**: Bodies that respond to game events
- **Testing**: Controlled positions for unit tests

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
    public Quaterniondc getRotation() {
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

### Dynamic Rotation

```java
private final Quaterniond rotation = new Quaterniond();

@Override
public Quaterniondc getRotation() {
    // Update rotation each time it's called
    rotation.rotateY(0.01);
    return rotation;
}
```

## Important Notes

1. **Thread Safety**: Your provider may be called from multiple threads. Make sure your implementation is thread-safe.

2. **Performance**: The `getCurrentPos()` and `getRotation()` methods are called frequently. Keep them efficient.

3. **Serialization**: All fields must be serializable via the Codec. Don't store references to game objects.

4. **Backward Compatibility**: The `customTransform` field is optional. Bodies without it will use standard orbital mechanics.

5. **Client/Server Sync**: Your custom provider will be automatically synchronized to clients via the space registry sync packet.

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
     */
    ResourceLocation getType();

    /**
     * Returns the rotation quaternion for the celestial body.
     */
    Quaterniondc getRotation();

    /**
     * Returns the current position of the celestial body.
     *
     * @param ticks The current game time in ticks
     * @param subticks Partial tick for smooth interpolation
     */
    Vector3d getCurrentPos(long ticks, float subticks);

    /**
     * Registers a custom transform provider type.
     * Call this during mod initialization.
     */
    static void register(ResourceLocation type, Codec<? extends CustomTransformProvider> codec);
}
```

## Questions?

For more information or questions about the CustomTransformProvider API, please refer to the Genesis mod documentation or source code.
