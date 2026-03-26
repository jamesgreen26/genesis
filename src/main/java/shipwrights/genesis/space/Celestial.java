package shipwrights.genesis.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import kotlin.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;
import shipwrights.genesis.math.OBB;
import shipwrights.genesis.space.properties.CelestialProperties;
import shipwrights.genesis.space.transformProvider.CelestialTransformProvider;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;
import shipwrights.genesis.space.type.CelestialType;

import java.util.Optional;
import java.util.function.Predicate;

public record Celestial(
        CelestialTransformProvider transformProvider,
        CelestialType type,
        double size,
        double gravity,
        float r,
        float g,
        float b,
        @NotNull CelestialProperties properties
) {
    public double getActualSize() {
        return this.size();
    }

    public Vector3dc getPosition(long ticks, float partialTick, Registry<Celestial> registry) {
        return transformProvider.getPosition(ticks, partialTick, registry);
    }

    public Vector3dc getPosition(long ticks, Registry<Celestial> registry) {
        return getPosition(ticks, 0f, registry);
    }

    public Quaterniondc getRotation(long ticks, float partialTick, Registry<Celestial> registry) {
        return transformProvider.getRotation(ticks, partialTick, registry);
    }

    public OBB getOBB(long ticks, Registry<Celestial> registry) {
        return getOBB(ticks, 0, registry);
    }

    public OBB getOBB(long ticks, float subticks, Registry<Celestial> registry) {
        return OBB.createCube(getActualSize(), getRotation(ticks, subticks, registry), getPosition(ticks, subticks, registry));
    }

    public Celestial getNearestStar(long gameTime, float partialTick, Registry<Celestial> registry) {
        if (BuiltinCelestialTypes.STAR.equals(type())) {
            return this;
        } else {
            Pair<Celestial, Double> result = SpaceLevel.nearestCelestialWhere(registry, getPosition(gameTime, partialTick, registry), gameTime, partialTick, Predicate.isEqual(BuiltinCelestialTypes.STAR));
            if (result != null) {
                return result.getFirst();
            } else {
                throw new IllegalStateException("Why are there no stars??");
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Codec<Celestial> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<com.mojang.datafixers.util.Pair<Celestial, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                DataResult<CelestialType> typeResult = field(ops, map, "type", Codec.STRING).flatMap(s -> {
                    CelestialType t = CelestialType.get(ResourceLocation.parse(s));
                    return t != null ? DataResult.success(t) : DataResult.error(() -> "Unknown celestial type: " + s);
                });
                DataResult<Double> sizeResult = field(ops, map, "size", Codec.DOUBLE);
                DataResult<Double> gravityResult = field(ops, map, "gravity", Codec.DOUBLE);
                float r = optional(ops, map, "r", Codec.FLOAT, 0.5f);
                float g = optional(ops, map, "g", Codec.FLOAT, 0.5f);
                float b = optional(ops, map, "b", Codec.FLOAT, 0.5f);
                DataResult<CelestialTransformProvider> tpResult = field(ops, map, "transformProvider", CelestialTransformProvider.DISPATCH_CODEC);

                return typeResult.flatMap(type -> {
                    T propsRaw = Optional.ofNullable(map.get("properties")).orElseGet(ops::emptyMap);
                    DataResult<? extends CelestialProperties> propsResult = type.propertiesCodec().parse(ops, propsRaw);

                    return sizeResult.flatMap(size ->
                        gravityResult.flatMap(gravity ->
                            tpResult.flatMap(tp ->
                                propsResult.map(props ->
                                    com.mojang.datafixers.util.Pair.of(
                                        new Celestial(tp, type, size, gravity, r, g, b, props),
                                        input
                                    )
                                )
                            )
                        )
                    );
                });
            });
        }

        @Override
        public <T> DataResult<T> encode(Celestial input, DynamicOps<T> ops, T prefix) {
            Codec propsCodec = input.type().propertiesCodec();
            return ops.mapBuilder()
                    .add("type", ops.createString(input.type().getID().toString()))
                    .add("size", ops.createDouble(input.size()))
                    .add("gravity", ops.createDouble(input.gravity()))
                    .add("r", ops.createFloat(input.r()))
                    .add("g", ops.createFloat(input.g()))
                    .add("b", ops.createFloat(input.b()))
                    .add("transformProvider", CelestialTransformProvider.DISPATCH_CODEC.encodeStart(ops, input.transformProvider()))
                    .add("properties", propsCodec.encodeStart(ops, input.properties()))
                    .build(prefix);
        }

        private <T, V> DataResult<V> field(DynamicOps<T> ops, MapLike<T> map, String key, Codec<V> codec) {
            T val = map.get(key);
            if (val == null) return DataResult.error(() -> "Missing required field: " + key);
            return codec.parse(ops, val);
        }

        private <T, V> V optional(DynamicOps<T> ops, MapLike<T> map, String key, Codec<V> codec, V defaultValue) {
            T val = map.get(key);
            if (val == null) return defaultValue;
            return codec.parse(ops, val).result().orElse(defaultValue);
        }
    };
}
