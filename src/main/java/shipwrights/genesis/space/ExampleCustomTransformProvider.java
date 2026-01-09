package shipwrights.genesis.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

/**
 * Example implementation of CustomTransformProvider that demonstrates the API.
 * <br><br>
 * This provider keeps a celestial body in a fixed position, useful for testing
 * or for bodies that don't follow normal orbital mechanics.
 * <br><br>
 * <b>Usage Example:</b>
 * <pre>{@code
 * // 1. Register the codec during mod initialization
 * CustomTransformProvider.register(
 *     ResourceLocation.parse("yourmod:fixed_position"),
 *     FixedPositionTransformProvider.CODEC
 * );
 *
 * // 2. Use in JSON configuration
 * {
 *   "ID": "yourmod:special_body",
 *   "parentID": "genesis:sun",
 *   "size": 1.0,
 *   "orbitDistance": 1.0,
 *   "orbitTime": 1.0,
 *   "gravity": 1.0,
 *   "customTransform": {
 *     "type": "yourmod:fixed_position",
 *     "x": 1000.0,
 *     "y": 5000.0,
 *     "z": 2000.0
 *   }
 * }
 * }</pre>
 */
public class ExampleCustomTransformProvider implements CustomTransformProvider {

    public static final ResourceLocation TYPE = ResourceLocation.parse("genesis:example_fixed_position");

    private final double x;
    private final double y;
    private final double z;
    private final Quaterniondc rotation;

    public ExampleCustomTransformProvider(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = new Quaterniond(); // Identity rotation
    }

    @Override
    public Quaterniondc getRotation(long ticks, float subticks, Orbitable parent) {
        return rotation;
    }

    @Override
    public Vector3d getCurrentPos(long ticks, float subticks, Orbitable parent) {
        return new Vector3d(x, y, z);
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    // Codec for serialization/deserialization
    public static final Codec<ExampleCustomTransformProvider> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(p -> p.x),
            Codec.DOUBLE.fieldOf("y").forGetter(p -> p.y),
            Codec.DOUBLE.fieldOf("z").forGetter(p -> p.z)
        ).apply(instance, ExampleCustomTransformProvider::new)
    );

    // Example registration method (call this during mod initialization)
    public static void register() {
        CustomTransformProvider.register(TYPE, CODEC);
    }
}
