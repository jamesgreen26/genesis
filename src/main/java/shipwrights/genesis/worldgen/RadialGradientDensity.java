package shipwrights.genesis.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

public class RadialGradientDensity implements DensityFunction {

    public static final MapCodec<RadialGradientDensity> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("radius").forGetter(r -> r.radius),
                    Codec.DOUBLE.fieldOf("max_value").forGetter(r -> r.maxValue),
                    Codec.DOUBLE.fieldOf("min_value").forGetter(r -> r.minValue),
                    Codec.DOUBLE.optionalFieldOf("center_x", 0.0).forGetter(r -> r.centerX),
                    Codec.DOUBLE.optionalFieldOf("center_z", 0.0).forGetter(r -> r.centerZ)
            ).apply(instance, RadialGradientDensity::new)
    );

    public static final KeyDispatchDataCodec<RadialGradientDensity> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final double radius;
    private final double maxValue;
    private final double minValue;
    private final double centerX;
    private final double centerZ;

    public RadialGradientDensity(double radius, double maxValue, double minValue, double centerX, double centerZ) {
        this.radius = radius;
        this.maxValue = maxValue;
        this.minValue = minValue;
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    @Override
    public double compute(@NotNull FunctionContext context) {
        if (radius <= 0.0) {
            return maxValue;
        }

        double dx = context.blockX() - centerX;
        double dz = context.blockZ() - centerZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        double t = dist / radius;
        double value = maxValue - t * (maxValue - minValue);

        if (value < minValue) return minValue;
        if (value > maxValue) return maxValue;
        return value;
    }

    @Override
    public void fillArray(double[] arr, @NotNull ContextProvider provider) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = compute(provider.forIndex(i));
        }
    }

    @Override
    public @NotNull DensityFunction mapAll(@NotNull Visitor visitor) {
        return this;
    }

    @Override public double minValue() { return minValue; }
    @Override public double maxValue() { return maxValue; }
    @Override public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
