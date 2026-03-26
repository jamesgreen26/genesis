# Advanced Topics: Custom Types and Transform Providers

## Custom Celestial Types

Implement `CelestialType` and register it before any celestials of that type are loaded.

```java
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.space.properties.CelestialProperties;
import shipwrights.genesis.space.properties.EmptyProperties;
import shipwrights.genesis.space.renderer.CelestialRenderer;
import shipwrights.genesis.space.type.CelestialType;

public class GasGiantType implements CelestialType {

    public static final GasGiantType INSTANCE = new GasGiantType();
    private static final ResourceLocation ID = ResourceLocation.parse("yourmod:gas_giant");

    @Override public boolean castsLight()  { return false; }
    @Override public boolean castsShadow() { return true; }
    @Override public boolean isVisitable() { return false; }

    @Override
    public @NotNull CelestialRenderer getRenderer() {
        return new GasGiantRenderer();
    }

    @Override
    public @NotNull ResourceLocation getID() { return ID; }

    @Override
    public @NotNull Codec<? extends CelestialProperties> propertiesCodec() {
        // Use EmptyProperties if you have no type-specific config
        return EmptyProperties.CODEC;
    }

    public static void register() {
        CelestialType.register(INSTANCE);
    }
}
```

Call `GasGiantType.register()` during mod initialization (before datapack loading). Then use `"type": "yourmod:gas_giant"` in your JSON files.

If your type needs custom per-celestial configuration, define your own `CelestialProperties` record with a `CODEC` and return it from `propertiesCodec()` instead.

---

## Custom Celestial Renderers

**Custom renderers require familiarity with Minecraft's rendering pipeline.**

Implement `CelestialRenderer`:

```java
public interface CelestialRenderer {
    void invoke(@NotNull RenderLevelStageEvent event,
                @NotNull Celestial toRender,
                @NotNull VantagePoint vantagePoint);

    default void setup(@NotNull RenderLevelStageEvent event,
                       @NotNull VantagePoint vantagePoint) {}

    default void teardown(@NotNull RenderLevelStageEvent event,
                          @NotNull VantagePoint vantagePoint) {}
}
```

- `invoke()` — called once per celestial per frame
- `setup()` / `teardown()` — called once per frame for all celestials of this type; use for shared render state

In `invoke()`:
1. Get position/rotation with `toRender.getPosition(ticks, partialTick, registry)` and `toRender.getRotation(...)`
2. Subtract `vantagePoint.getPosition()` to get relative position
3. Transform the pose stack and render your geometry
4. Use `vantagePoint instanceof VantagePoint.OnCelestial` to distinguish surface vs. space view

Reference implementations in the Genesis source:
- `StarRenderer.java` — glowing cube with custom shader
- `PlanetRenderer.java` — textured cube with lighting and atmosphere
- `BlackholeRenderer.java` — blackhole shader effect

---

## Custom Transform Providers

Implement `CelestialTransformProvider` and register a codec for datapack support.

```java
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.space.Celestial;
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
    public Vector3d getPosition(long ticks, float subticks, Registry<Celestial> registry) {
        double t = (ticks + subticks) / period * 2 * Math.PI;
        double scale = size * 1000;
        return new Vector3d(scale * Math.sin(t), 0, scale * Math.sin(t) * Math.cos(t));
    }

    @Override
    public Quaterniondc getRotation(long ticks, float subticks, Registry<Celestial> registry) {
        double angle = (ticks + subticks) / (period * 0.1);
        return new Quaterniond().rotateY(angle);
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

Call `FigureEightTransformProvider.register()` during mod initialization. Then use it in JSON:

```json
{
  "type": "genesis:body",
  "size": 100,
  "gravity": 1.0,
  "transformProvider": {
    "type": "yourmod:figure_eight",
    "size": 10.0,
    "period": 100000.0
  },
  "properties": { ... }
}
```

The `registry` parameter in `getPosition`/`getRotation` is available if you need to look up a parent celestial's position (as `genesis:orbiting` does internally).
