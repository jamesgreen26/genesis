package shipwrights.genesis.util.schema;

import com.google.gson.JsonElement;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;

/// The shape of the data a codec reads, as worked out by {@link SchemaProbe}: enough to build an editor for it and to
/// fill in a starting value, without knowing the codec.
public sealed interface Schema {

    enum NumberKind {
        INT, LONG, FLOAT, DOUBLE;

        public boolean isWhole() {
            return this == INT || this == LONG;
        }
    }

    /// @param range the numbers it may be, as {@link shipwrights.genesis.util.MoreCodecs#ranged} checks
    /// @param seed  what a new one starts as, if the codec says; otherwise {@linkplain Range#seed what's nearest 0}
    record NumberSchema(NumberKind kind, Range range, @Nullable BigDecimal seed) implements Schema {
        /// Any number of its kind
        public NumberSchema(NumberKind kind) {
            this(kind, Range.ALL);
        }

        public NumberSchema(NumberKind kind, Range range) {
            this(kind, range, null);
        }

        /// What a new one starts as
        public BigDecimal start() {
            return seed != null && range.contains(seed) ? seed : range.seed();
        }
    }

    /// The numbers between two bounds, either of which may be left open, but for any {@code excluded}
    ///
    /// @param min      the least it may be, or null for no least
    /// @param max      the most it may be, or null for no most
    /// @param excluded numbers within the bounds it may not be, such as 0 for a period
    record Range(@Nullable BigDecimal min, boolean minInclusive, @Nullable BigDecimal max, boolean maxInclusive,
                 List<BigDecimal> excluded) {
        public static final Range ALL = new Range(null, false, null, false);

        public Range(@Nullable BigDecimal min, boolean minInclusive, @Nullable BigDecimal max, boolean maxInclusive) {
            this(min, minInclusive, max, maxInclusive, List.of());
        }

        /// Anything but 0
        public static Range nonZero() {
            return ALL.excluding(0);
        }

        /// At least {@code min}
        public static Range atLeast(double min) {
            return new Range(BigDecimal.valueOf(min), true, null, false);
        }

        /// More than {@code min}
        public static Range above(double min) {
            return new Range(BigDecimal.valueOf(min), false, null, false);
        }

        /// From {@code min} to {@code max}, both included
        public static Range between(double min, double max) {
            return new Range(BigDecimal.valueOf(min), true, BigDecimal.valueOf(max), true);
        }

        /// From {@code min}, included, up to but not including {@code max}
        public static Range from(double min, double below) {
            return new Range(BigDecimal.valueOf(min), true, BigDecimal.valueOf(below), false);
        }

        /// This, but not {@code value}
        public Range excluding(double value) {
            List<BigDecimal> more = new ArrayList<>(excluded);
            more.add(BigDecimal.valueOf(value));
            return new Range(min, minInclusive, max, maxInclusive, List.copyOf(more));
        }

        public boolean isAll() {
            return min == null && max == null && excluded.isEmpty();
        }

        public boolean contains(BigDecimal value) {
            return withinBounds(value) && excluded.stream().noneMatch(out -> out.compareTo(value) == 0);
        }

        private boolean withinBounds(BigDecimal value) {
            if (min != null) {
                int compared = value.compareTo(min);
                if (compared < 0 || compared == 0 && !minInclusive) return false;
            }
            if (max != null) {
                int compared = value.compareTo(max);
                if (compared > 0 || compared == 0 && !maxInclusive) return false;
            }
            return true;
        }

        /// Whether {@code value} is in range; never infinities or NaN, which have no bounds to be within
        public boolean contains(double value) {
            return Double.isFinite(value) && contains(BigDecimal.valueOf(value));
        }

        /// A number in range to start from: what's nearest 0 within the bounds, or, if that's excluded, the nearest
        /// whole step from it that isn't
        public BigDecimal seed() {
            BigDecimal base = nearestZero();
            for (int step = 1; !contains(base) && step <= excluded.size() + 1; step++) {
                BigDecimal up = base.add(BigDecimal.valueOf(step));
                if (contains(up)) return up;
                BigDecimal down = base.subtract(BigDecimal.valueOf(step));
                if (contains(down)) return down;
            }
            return base;
        }

        /// 0 if the bounds allow it, else the bound nearest 0, or a step inside it if that bound's left out
        private BigDecimal nearestZero() {
            if (withinBounds(BigDecimal.ZERO)) return BigDecimal.ZERO;
            boolean aboveZero = min != null && min.signum() >= 0;
            BigDecimal bound = aboveZero ? min : max;
            if (aboveZero ? minInclusive : maxInclusive) return bound;
            BigDecimal stepped = aboveZero ? bound.add(BigDecimal.ONE) : bound.subtract(BigDecimal.ONE);
            if (withinBounds(stepped)) return stepped;
            // Narrower than a step
            return min.add(max).divide(BigDecimal.valueOf(2));
        }

        /// As {@code ≥ 0}, {@code > 0 and ≤ 1}, {@code 0 to 1}, or {@code ≠ 0}
        @Override
        public String toString() {
            List<String> parts = new ArrayList<>();
            if (min != null && max != null && minInclusive && maxInclusive) {
                parts.add(plain(min) + " to " + plain(max));
            } else {
                if (min != null) parts.add((minInclusive ? "≥ " : "> ") + plain(min));
                if (max != null) parts.add((maxInclusive ? "≤ " : "< ") + plain(max));
            }
            excluded.forEach(out -> parts.add("≠ " + plain(out)));
            return parts.isEmpty() ? "any" : String.join(" and ", parts);
        }

        private static String plain(BigDecimal value) {
            return value.stripTrailingZeros().toPlainString();
        }
    }

    /// @param seed what editors start new ones as
    record BoolSchema(boolean seed) implements Schema {
        public BoolSchema() {
            this(false);
        }
    }

    record StringSchema() implements Schema {}

    /// One of a few fixed words, as {@link shipwrights.genesis.util.MoreCodecs#oneOf} reads
    record ChoiceSchema(List<String> values) implements Schema {}

    /// A colour, as packed {@code 0xRRGGBB} or text such as {@code #RRGGBB}, as {@link shipwrights.genesis.util.MoreCodecs#RGB}
    /// reads
    record ColorSchema() implements Schema {}

    /// A fixed number of numbers, one along each of {@code axes}, as a list: {@code [x, y, z]}
    record VectorSchema(List<String> axes, NumberKind kind) implements Schema {}

    /// The id of an entry in {@code registry}, as text
    ///
    /// @param seed what a new one starts as, if the codec says; otherwise it starts empty
    record IdSchema(ResourceKey<? extends Registry<?>> registry, @Nullable ResourceLocation seed) implements Schema {
        public IdSchema(ResourceKey<? extends Registry<?>> registry) {
            this(registry, null);
        }
    }

    /// A list, of any length the codec allows, of elements all of one schema
    record ListSchema(Schema element) implements Schema {}

    /// An object with named fields, in the order the codec reads them
    record MapSchema(List<Field> fields) implements Schema {}

    /// @param required     whether the codec fails without it, rather than using a default
    /// @param defaultValue what the codec uses without it, as {@link shipwrights.genesis.util.MoreCodecs#strictOptionalField}
    ///                     says, if it's said
    record Field(String name, Schema schema, boolean required, @Nullable JsonElement defaultValue) {
        public Field(String name, Schema schema, boolean required) {
            this(name, schema, required, null);
        }
    }

    /// An object whose {@code typeKey} field picks which of {@code options} it is. Each option's fields sit alongside
    /// {@code typeKey}, in the same object.
    record DispatchSchema(String typeKey, Map<ResourceLocation, Schema> options) implements Schema {}

    /// Any one of several schemas, as with {@link com.mojang.serialization.Codec#either}, tried in order
    record UnionSchema(List<Schema> alternatives) implements Schema {}

    /// Something the probe couldn't make out, such as a map with arbitrary keys. Edited as plain JSON.
    record UnknownSchema() implements Schema {}
}
