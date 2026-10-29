package shipwrights.genesis.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.Util;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class MoreCodecs {
    public static final Codec<Vector3dc> VECTOR3DC
            = Codec.DOUBLE.listOf().comapFlatMap(p_253502_ -> Util.fixedSize(p_253502_, 3).map(p_253489_ -> new Vector3d(p_253489_.get(0), p_253489_.get(1), p_253489_.get(2))), p_269787_ -> List.of(p_269787_.x(), p_269787_.y(), p_269787_.z()));

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

    public static <A> MapCodec<A> strictOptionalField(Codec<A> codec, String name, A defaultValue) {
        return strictOptionalField(codec, name).xmap(
                value -> value.orElse(defaultValue),
                value -> Objects.equals(value, defaultValue) ? Optional.empty() : Optional.of(value)
        );
    }
}
