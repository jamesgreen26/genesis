package shipwrights.genesis.util;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.util.schema.Schema;
import shipwrights.genesis.util.schema.SchemaProbe;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MoreCodecs {
    /// {@code [x, y, z]}
    public static final Codec<Vector3dc> VECTOR3DC = declaring(
            Codec.DOUBLE.listOf().comapFlatMap(
                    list -> Util.fixedSize(list, 3).map(xyz -> new Vector3d(xyz.get(0), xyz.get(1), xyz.get(2))),
                    vector -> List.of(vector.x(), vector.y(), vector.z())),
            new Schema.VectorSchema(List.of("x", "y", "z"), Schema.NumberKind.DOUBLE),
            new Vector3d());

    private static final Pattern HEX_COLOR = Pattern.compile("#?([0-9a-fA-F]{6})");

    /// A colour, packed as {@code 0xRRGGBB}. Read from that number, or from text as {@code #RRGGBB}, and written as the
    /// text, which is easier to read.
    public static final Codec<Integer> RGB = declaring(
            Codec.either(Codec.INT, Codec.STRING).comapFlatMap(
                    either -> either.map(
                            packed -> DataResult.success(packed & 0xFFFFFF),
                            text -> {
                                Matcher matcher = HEX_COLOR.matcher(text);
                                return matcher.matches()
                                        ? DataResult.success(Integer.parseInt(matcher.group(1), 16))
                                        : DataResult.error(() -> "Expected a colour as #RRGGBB, got \"" + text + "\"");
                            }),
                    color -> Either.right(hexColor(color))),
            new Schema.ColorSchema(),
            0xFFFFFF);

    /// {@code color} as {@code #RRGGBB}
    public static String hexColor(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    /// Doubles in {@code range}
    public static Codec<Double> doubleRange(Schema.Range range) {
        return ranged(Codec.DOUBLE, new Schema.NumberSchema(Schema.NumberKind.DOUBLE, range), BigDecimal::doubleValue);
    }

    /// Doubles in {@code range}, which editors start new ones at {@code seed}
    public static Codec<Double> doubleRange(Schema.Range range, double seed) {
        return ranged(Codec.DOUBLE, new Schema.NumberSchema(Schema.NumberKind.DOUBLE, range, BigDecimal.valueOf(seed)),
                BigDecimal::doubleValue);
    }

    /// Booleans, which editors start new ones as {@code seed}
    public static Codec<Boolean> bool(boolean seed) {
        return declaring(Codec.BOOL, new Schema.BoolSchema(seed), seed);
    }

    /// Ints in {@code range}
    public static Codec<Integer> intRange(Schema.Range range) {
        return ranged(Codec.INT, new Schema.NumberSchema(Schema.NumberKind.INT, range), BigDecimal::intValue);
    }

    /// Numbers {@code codec} reads that are in {@code schema}'s range, which editors built from codecs are told of
    ///
    /// @param fromDecimal the number of {@code codec}'s type a decimal is
    public static <N extends Number> Codec<N> ranged(Codec<N> codec, Schema.NumberSchema schema,
                                                     Function<BigDecimal, N> fromDecimal) {
        Schema.Range range = schema.range();
        Codec<N> checked = validate(codec, value -> range.contains(value.doubleValue())
                ? DataResult.success(value)
                : DataResult.error(() -> "Value " + value + " must be " + range));
        return declaring(checked, schema, fromDecimal.apply(schema.start()));
    }

    /// {@code codec}, but described to {@link SchemaProbe} as {@code schema} rather than by how it's read, as editors
    /// built from codecs then show what its values mean
    ///
    /// @param probed what it reads while it's being probed, without reading anything
    public static <A> Codec<A> declaring(Codec<A> codec, Schema schema, A probed) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                if (SchemaProbe.declare(ops, input, schema)) return DataResult.success(Pair.of(probed, input));
                return codec.decode(ops, input);
            }

            @Override
            public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
                return codec.encode(input, ops, prefix);
            }

            @Override
            public String toString() {
                return codec.toString();
            }
        };
    }

    /// The id of an entry in the registry {@code registry}, read and written just as {@link ResourceLocation#CODEC}
    /// does, without checking the entry exists, as it may not be loaded yet. Says which registry to
    /// {@link SchemaProbe}, so editors built from codecs can offer the entries there are.
    public static Codec<ResourceLocation> registryId(ResourceKey<? extends Registry<?>> registry) {
        return registryId(registry, null);
    }

    /// As {@link #registryId(ResourceKey)}, but editors start new ones as {@code seed}
    public static Codec<ResourceLocation> registryId(ResourceKey<? extends Registry<?>> registry,
                                                     @Nullable ResourceLocation seed) {
        return declaring(ResourceLocation.CODEC, new Schema.IdSchema(registry, seed),
                seed == null ? SchemaProbe.PROBE_ID : seed);
    }

    /**
     * Like {@link Codec#optionalFieldOf(String)}, but a field that is present and fails to parse is an error
     * instead of silently being treated as absent. Backport of {@code ExtraCodecs.strictOptionalField} from 1.20.2.
     */
    public static <A> MapCodec<Optional<A>> strictOptionalField(Codec<A> codec, String name) {
        return new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.of(ops.createString(name));
            }

            @Override
            public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
                T value = input.get(name);
                return value == null ? DataResult.success(Optional.empty()) : codec.parse(ops, value).map(Optional::of);
            }

            @Override
            public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return input.isPresent() ? prefix.add(name, codec.encodeStart(ops, input.get())) : prefix;
            }
        };
    }

    /// Tells {@link SchemaProbe} the default, so editors built from codecs can show what a field left out comes to
    public static <A> MapCodec<A> strictOptionalField(Codec<A> codec, String name, A defaultValue) {
        MapCodec<A> field = strictOptionalField(codec, name).xmap(
                value -> value.orElse(defaultValue),
                value -> Objects.equals(value, defaultValue) ? Optional.empty() : Optional.of(value)
        );
        return new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return field.keys(ops);
            }

            @Override
            public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
                T value = input.get(name);
                if (value != null) {
                    SchemaProbe.noteDefault(ops, value,
                            () -> codec.encodeStart(JsonOps.INSTANCE, defaultValue).result().orElse(null));
                }
                return field.decode(ops, input);
            }

            @Override
            public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return field.encode(input, ops, prefix);
            }

            @Override
            public String toString() {
                return field.toString();
            }
        };
    }

    /// One of {@code values}, as text
    public static Codec<String> oneOf(String... values) {
        List<String> allowed = List.of(values);
        return declaring(validate(Codec.STRING, value -> allowed.contains(value)
                        ? DataResult.success(value)
                        : DataResult.error(() -> "Expected one of " + allowed + ", got \"" + value + "\"")),
                new Schema.ChoiceSchema(allowed), allowed.get(0));
    }

    /// {@code codec}, failing for values {@code check} fails both ways, as {@code Codec#validate} does in later versions
    /// of DataFixerUpper
    private static <A> Codec<A> validate(Codec<A> codec, Function<A, DataResult<A>> check) {
        return codec.flatXmap(check, check);
    }
}
