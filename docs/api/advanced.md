# Advanced Topics: Custom Types and Transform Providers

This guide covers advanced customization of the Genesis celestial system, including creating custom celestial types with unique rendering and behavior, and custom transform providers for unique orbital mechanics.

## Custom Celestial Types

Celestial types define the rendering and behavioral properties of celestials. Genesis provides two built-in types (`genesis:star` and `genesis:body`), but you can create your own.

### Implementing CelestialType

To create a custom type, implement the `CelestialType` interface:

```java
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import shipwrights.genesis.space.type.CelestialType;
import shipwrights.genesis.client.renderer.CelestialRenderer;

public class CustomCelestialType implements CelestialType {

    private final ResourceLocation id;
    private final CelestialRenderer renderer;

    public CustomCelestialType(ResourceLocation id, CelestialRenderer renderer) {
        this.id = id;
        this.renderer = renderer;
    }

    @Override
    public boolean castsLight() {
        // Return true if this type should emit light to other celestials
        return false;
    }

    @Override
    public boolean castsShadow() {
        // Return true if this type should cast shadows on other celestials
        return true;
    }

    @Override
    public boolean isVisitable() {
        // Return true if players can land on this type
        return true;
    }

    @Override
    @NotNull
    public CelestialRenderer getRenderer() {
        // Return the renderer instance for this type
        return renderer;
    }

    @Override
    @NotNull
    public ResourceLocation getID() {
        // Return the unique identifier for this type
        return id;
    }
}
```

### Type Properties Explained

#### castsLight()
- Returns `true` for light-emitting celestials (stars, suns)
- Affects day/night cycles on orbiting bodies
- Stars typically return `true`, planets/moons return `false`

#### castsShadow()
- Returns `true` for opaque celestials that block light
- Affects lighting calculations for nearby celestials
- Planets and moons typically return `true`, stars return `false`

#### isVisitable()
- Returns `true` if players can physically land on this celestial
- Stars are typically not visitable (`false`)
- Planets and moons are visitable (`true`)

### Registering Custom Types

Register your custom type during mod initialization:

```java
import shipwrights.genesis.space.type.CelestialType;
import net.minecraft.resources.ResourceLocation;

public class YourMod {
    public static void init() {
        // Create your custom type
        CustomCelestialType gasGiant = new CustomCelestialType(
            ResourceLocation.parse("yourmod:gas_giant"),
            new GasGiantRenderer()
        );

        // Register it
        CelestialType.register(gasGiant);
    }
}
```

Once registered, you can use it in celestial creation:

```java
Celestial jupiter = new Celestial(
    transform,
    ResourceLocation.parse("yourmod:jupiter"),
    CelestialType.get(ResourceLocation.parse("yourmod:gas_giant")),
    11.0,  // Large size
    2.5,   // High gravity
    0.9f, 0.8f, 0.7f
);
```

## Custom Celestial Renderers

**Warning: Creating custom renderers is a complex task. Existing rendering experience with Minecraft's rendering pipeline is strongly recommended.**

Custom celestial renderers give you complete control over how celestials appear in the sky. Similar to custom `BlockEntityRenderer` implementations, you can define rendering behavior however you want - from simple textured cubes to complex animated effects with shaders.

### The CelestialRenderer Interface

**Package:** `shipwrights.genesis.space.renderer`
**File:** `src/main/java/shipwrights/genesis/space/renderer/CelestialRenderer.java`

```java
public interface CelestialRenderer {
    void invoke(@NotNull RenderLevelStageEvent event,
                @NotNull Celestial toRender,
                @Nullable Celestial vantagePoint);

    default void setup(@NotNull RenderLevelStageEvent event,
                      @Nullable Celestial vantagePoint) {}

    default void teardown(@NotNull RenderLevelStageEvent event,
                         @Nullable Celestial vantagePoint) {}
}
```

### Methods

#### invoke()
The main rendering method called for each celestial.

**Parameters:**
- `event` - Forge's `RenderLevelStageEvent` containing the pose stack, projection matrix, and rendering context
- `toRender` - The celestial being rendered
- `vantagePoint` - The celestial from which the rendering is viewed (null if viewing from space)

**Typical implementation:**
1. Get the celestial's position and rotation using `toRender.getPosition()` and `toRender.getRotation()`
2. Calculate relative position if a vantage point exists
3. Transform the pose stack to the celestial's position
4. Render geometry (cubes, custom models, billboards, etc.)
5. Apply textures, colors, and shaders

#### setup() (optional)
Called once before rendering all celestials of this type in a frame. Use for shared state preparation like enabling blending, binding textures, or configuring shaders.

#### teardown() (optional)
Called once after rendering all celestials of this type in a frame. Use for cleanup like disabling blending or resetting render state.

### Implementation Stub

```java
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.renderer.CelestialRenderer;
import org.joml.Vector3dc;
import org.joml.Quaterniondc;

public class CustomCelestialRenderer implements CelestialRenderer {

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event,
                      @NotNull Celestial toRender,
                      @Nullable Celestial vantagePoint) {
        // Get current time for animation
        long ticks = level.getGameTime();
        float partialTick = event.getPartialTick();

        // Get celestial's world position and rotation
        Vector3dc position = toRender.getPosition(ticks, partialTick);
        Quaterniondc rotation = toRender.getRotation(ticks, partialTick);

        // Calculate relative position if viewing from another celestial
        if (vantagePoint != null) {
            Vector3dc vantagePos = vantagePoint.getPosition(ticks, partialTick);
            // Subtract vantage point position and apply inverse rotation
            // (see StarRenderer.java for full implementation)
        }

        // Transform pose stack to celestial position
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // Apply position and rotation transforms

        // Render your celestial geometry here
        // - Use VertexConsumer to build meshes
        // - Apply shaders and textures
        // - Use toRender.r(), toRender.g(), toRender.b() for color tinting
        // - Scale by toRender.size() * Celestial.BASE_SIZE

        poseStack.popPose();
    }

    @Override
    public void setup(@NotNull RenderLevelStageEvent event,
                     @Nullable Celestial vantagePoint) {
        // Optional: Enable render states like blending
        // RenderSystem.enableBlend();
        // RenderSystem.blendFunc(...);
    }

    @Override
    public void teardown(@NotNull RenderLevelStageEvent event,
                        @Nullable Celestial vantagePoint) {
        // Optional: Clean up render states
        // RenderSystem.disableBlend();
    }
}
```

### Example Implementations

Genesis includes two complete renderer implementations you can reference:

- **[StarRenderer.java](../../src/main/java/shipwrights/genesis/space/renderer/StarRenderer.java)** - Renders glowing stars with custom shaders
  - Uses custom shader for glowing effects
  - Implements billboarding for constant screen size
  - Handles bloom and atmospheric effects

- **[PlanetRenderer.java](../../src/main/java/shipwrights/genesis/space/renderer/PlanetRenderer.java)** - Renders solid planets with lighting
  - Renders textured cube geometry
  - Calculates lighting from nearest star
  - Supports shadows and day/night transitions

These implementations demonstrate:
- Position and rotation calculations relative to vantage points
- Shader usage and uniform binding
- Vertex buffer building and mesh rendering
- Integration with Minecraft's rendering pipeline

### Using Custom Renderers

Create your renderer and pass it to your custom celestial type:

```java
public class YourMod {
    public static void init() {
        CelestialRenderer customRenderer = new CustomCelestialRenderer();

        CustomCelestialType myType = new CustomCelestialType(
            ResourceLocation.parse("yourmod:custom_type"),
            customRenderer  // Your renderer instance
        );

        CelestialType.register(myType);
    }
}
```

## Custom Transform Providers

Transform providers control the position and rotation of celestials over time. Genesis provides static and orbiting transforms, but you can create custom orbital mechanics.

### Implementing CelestialTransformProvider

To create a custom transform provider, implement the `CelestialTransformProvider` interface:

```java
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.space.transformProvider.CelestialTransformProvider;

public class CustomTransformProvider implements CelestialTransformProvider {

    @Override
    public Quaterniondc getRotation(long ticks, float subticks) {
        // Return the rotation quaternion at this time
        // ticks: game time in ticks
        // subticks: fractional tick for smooth interpolation (0.0 to 1.0)
        return new Quaternion(); // Your rotation logic here
    }

    @Override
    public Vector3d getPosition(long ticks, float subticks) {
        // Return the position vector at this time
        return new Vector3d(x, y, z); // Your position logic here
    }

    @Override
    public ResourceLocation getType() {
        // Return the unique type identifier
        return ResourceLocation.parse("yourmod:custom");
    }
}
```

### Codec Registration for Datapack Support

To support JSON configuration in datapacks, you need to register a codec for your transform provider:

```java
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import shipwrights.genesis.space.transformProvider.CelestialTransformProvider;
import net.minecraft.resources.ResourceLocation;

public class CustomTransformProvider implements CelestialTransformProvider {

    // Define fields that can be configured
    private final double speed;
    private final double radius;

    // Create a codec for serialization
    public static final Codec<CustomTransformProvider> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("speed").forGetter(p -> p.speed),
            Codec.DOUBLE.fieldOf("radius").forGetter(p -> p.radius)
        ).apply(instance, CustomTransformProvider::new)
    );

    public CustomTransformProvider(double speed, double radius) {
        this.speed = speed;
        this.radius = radius;
    }

    // Implement interface methods...
    @Override
    public Quaterniondc getRotation(long ticks, float subticks) {
        // Your rotation logic
        return new Quaternion();
    }

    @Override
    public Vector3d getPosition(long ticks, float subticks) {
        // Your position logic using speed and radius
        return new Vector3d();
    }

    @Override
    public ResourceLocation getType() {
        return ResourceLocation.parse("yourmod:custom");
    }

    // Register the codec
    public static void register() {
        CelestialTransformProvider.register(
            ResourceLocation.parse("yourmod:custom"),
            CODEC
        );
    }
}
```

Then users can use it in JSON:

```json
{
  "transformProvider": {
    "type": "yourmod:custom",
    "speed": 1.5,
    "radius": 2000.0
  }
}
```

### Example: Figure-8 Orbit

Here's a transform provider that creates a figure-8 orbital pattern:

```java
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternion;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import shipwrights.genesis.space.transformProvider.CelestialTransformProvider;

public class FigureEightTransformProvider implements CelestialTransformProvider {

    public static final Codec<FigureEightTransformProvider> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("size").forGetter(p -> p.size),
            Codec.DOUBLE.fieldOf("period").forGetter(p -> p.period)
        ).apply(instance, FigureEightTransformProvider::new)
    );

    private final double size;
    private final double period;

    public FigureEightTransformProvider(double size, double period) {
        this.size = size;
        this.period = period;
    }

    @Override
    public Vector3d getPosition(long ticks, float subticks) {
        double time = (ticks + subticks) / period;
        double angle = time * 2 * Math.PI;

        // Parametric figure-8 (lemniscate)
        double scale = size * 1000;
        double x = scale * Math.sin(angle);
        double z = scale * Math.sin(angle) * Math.cos(angle);
        double y = 0;

        return new Vector3d(x, y, z);
    }

    @Override
    public Quaterniondc getRotation(long ticks, float subticks) {
        // Simple rotation around Y-axis
        double angle = (ticks + subticks) / (period * 0.1);
        return new Quaternion().rotateY((float) angle);
    }

    @Override
    public ResourceLocation getType() {
        return ResourceLocation.parse("yourmod:figure_eight");
    }

    public static void register() {
        CelestialTransformProvider.register(
            ResourceLocation.parse("yourmod:figure_eight"),
            CODEC
        );
    }
}
```

Usage in code:

```java
Celestial weirdPlanet = new Celestial(
    new FigureEightTransformProvider(10.0, 100000.0),
    ResourceLocation.parse("yourmod:weird_planet"),
    CelestialType.get(ResourceLocation.parse("genesis:body")),
    1.0, 1.0,
    0.8f, 0.3f, 0.8f
);
event.accept(weirdPlanet);
```

Or in JSON:

```json
{
  "ID": "yourmod:weird_planet",
  "type": "genesis:body",
  "size": 1.0,
  "gravity": 1.0,
  "transformProvider": {
    "type": "yourmod:figure_eight",
    "size": 10.0,
    "period": 100000.0
  }
}
```

## Understanding Configuration Constants

Genesis defines several base constants that are used as multipliers:

```java
// From Celestial class
public static double BASE_SIZE = 96;              // blocks (diameter)
public static double BASE_ORBIT_DISTANCE = 15_000;  // blocks
public static double BASE_ORBIT_TIME = 4_608_000;   // ticks (64 hours)
public static double BASE_DAY_LENGTH = 24_000;      // ticks (20 minutes)
```

These can be referenced in your custom transform providers to maintain consistency:

```java
import shipwrights.genesis.space.Celestial;

// Use base constants in your calculations
double actualDistance = distanceMultiplier * Celestial.BASE_ORBIT_DISTANCE;
double actualPeriod = periodMultiplier * Celestial.BASE_ORBIT_TIME;
```

## Next Steps

- **Need API details?** See the complete [API Reference](api-reference.md)
- **Want simpler examples?** Review the [Implementation Guide](implementation-guide.md)
- **Getting started?** Check the [Getting Started Guide](getting-started.md)
