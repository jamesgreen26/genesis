package shipwrights.genesis.space.client.texture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import shipwrights.genesis.util.MoreCodecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// What {@link PlanetTextureGenerator} makes a planet's surface from: how big, its seed, how its two main palettes are
/// blended, and its colours.
///
/// The surface has a height everywhere. The lowest of it is drawn in the {@linkplain Group#FIRST first} palette and
/// the rest in the {@linkplain Group#SECOND second}, each palette's colours spread over its share from low to high.
///
/// @param faceSize pixels across each face, so the texture's three times that wide and twice that high
/// @param seed     which surface it is: the same settings always make the same texture
/// @param blend    how much of the surface is drawn in the first palette rather than the second, from 0 to 1
/// @param noisiness how much fine detail there is, from 0 to 1: at 0 broad, smooth shapes; at 1 busy, mottled ones
/// @param blockiness how big the blocks shapes are snapped to are, from 0 to 1: at 0 there are none, and shapes are
///                   smooth; at 1 they're twice as big as halfway
/// @param ditherSpread   how wide the dithered transition between colours is, from 0 to 1: at 0 there's none
/// @param ditherCoverage how much of the edges between colours are dithered, from 0 to 1, in stretches picked by noise
/// @param palette  the colours of each {@link Group}, as {@code 0xRRGGBB}, in order
public record PlanetTextureSettings(int faceSize, long seed, double blend, double noisiness, double blockiness,
                                    double ditherSpread, double ditherCoverage,
                                    Map<Group, List<Integer>> palette) {
    public static final List<Integer> FACE_SIZES = List.of(32, 64, 128, 256);
    public static final int DEFAULT_FACE_SIZE = 128;
    /// The most colours a group can have
    public static final int MAX_COLORS = 12;

    /// Grey and cratered, as the moon: its darker greys in the first palette and its lighter in the second
    public static final PlanetTextureSettings DEFAULT = new PlanetTextureSettings(DEFAULT_FACE_SIZE, 0, 0.45, 0.5, 0.5, 0.5, 0.75,
            Map.of(Group.FIRST, List.of(0x27272E, 0x303038, 0x464852),
                    Group.SECOND, List.of(0x5E606F, 0x6E7081, 0x797C8F, 0x9698A7),
                    Group.SPECKS, List.of(0x66746B)));

    /// A set of colours with one part to play
    public enum Group implements StringRepresentable {
        /// The lowest of the surface, from low to high
        FIRST(1),
        /// The rest of the surface, from low to high
        SECOND(1),
        /// Drawn in places instead of the second palette, from low to high; none for none
        PATCHES(0),
        /// Around the poles, from their edge to their middle; none for none
        POLES(0),
        /// Specks scattered about; none for none
        SPECKS(0);

        public static final Codec<Group> CODEC = StringRepresentable.fromEnum(Group::values);
        private final int minColors;

        Group(int minColors) {
            this.minColors = minColors;
        }

        /// The fewest colours it can have
        public int minColors() {
            return minColors;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<PlanetTextureSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(8, 1024).optionalFieldOf("face_size", DEFAULT_FACE_SIZE).forGetter(PlanetTextureSettings::faceSize),
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(PlanetTextureSettings::seed),
            Codec.doubleRange(0, 1).optionalFieldOf("blend", DEFAULT.blend).forGetter(PlanetTextureSettings::blend),
            Codec.doubleRange(0, 1).optionalFieldOf("noisiness", DEFAULT.noisiness).forGetter(PlanetTextureSettings::noisiness),
            Codec.doubleRange(0, 1).optionalFieldOf("blockiness", DEFAULT.blockiness).forGetter(PlanetTextureSettings::blockiness),
            Codec.doubleRange(0, 1).optionalFieldOf("dither_spread", DEFAULT.ditherSpread).forGetter(PlanetTextureSettings::ditherSpread),
            Codec.doubleRange(0, 1).optionalFieldOf("dither_coverage", DEFAULT.ditherCoverage).forGetter(PlanetTextureSettings::ditherCoverage),
            Codec.unboundedMap(Group.CODEC, MoreCodecs.RGB.listOf()).fieldOf("palette").forGetter(PlanetTextureSettings::palette)
    ).apply(instance, PlanetTextureSettings::new));

    public PlanetTextureSettings {
        EnumMap<Group, List<Integer>> copy = new EnumMap<>(Group.class);
        for (Group group : Group.values()) copy.put(group, List.copyOf(palette.getOrDefault(group, List.of())));
        // In the groups' order, so they're saved in it
        palette = Collections.unmodifiableMap(copy);
    }

    /// The colours of {@code group}, in order
    public List<Integer> colors(Group group) {
        return palette.get(group);
    }

    public PlanetTextureSettings withColors(Group group, List<Integer> colors) {
        Map<Group, List<Integer>> changed = new EnumMap<>(palette);
        changed.put(group, colors);
        return new PlanetTextureSettings(faceSize, seed, blend, noisiness, blockiness, ditherSpread, ditherCoverage, changed);
    }

    public PlanetTextureSettings withColor(Group group, int index, int color) {
        List<Integer> colors = new ArrayList<>(colors(group));
        colors.set(index, color & 0xFFFFFF);
        return withColors(group, colors);
    }

    public PlanetTextureSettings withFaceSize(int faceSize) {
        return new PlanetTextureSettings(faceSize, seed, blend, noisiness, blockiness, ditherSpread, ditherCoverage, palette);
    }

    public PlanetTextureSettings withSeed(long seed) {
        return new PlanetTextureSettings(faceSize, seed, blend, noisiness, blockiness, ditherSpread, ditherCoverage, palette);
    }

    public PlanetTextureSettings withBlend(double blend) {
        return new PlanetTextureSettings(faceSize, seed, blend, noisiness, blockiness, ditherSpread, ditherCoverage, palette);
    }

    public PlanetTextureSettings withNoisiness(double noisiness) {
        return new PlanetTextureSettings(faceSize, seed, blend, noisiness, blockiness, ditherSpread, ditherCoverage, palette);
    }

    public PlanetTextureSettings withBlockiness(double blockiness) {
        return new PlanetTextureSettings(faceSize, seed, blend, noisiness, blockiness, ditherSpread, ditherCoverage, palette);
    }

    public PlanetTextureSettings withDitherSpread(double ditherSpread) {
        return new PlanetTextureSettings(faceSize, seed, blend, noisiness, blockiness, ditherSpread, ditherCoverage, palette);
    }

    public PlanetTextureSettings withDitherCoverage(double ditherCoverage) {
        return new PlanetTextureSettings(faceSize, seed, blend, noisiness, blockiness, ditherSpread, ditherCoverage, palette);
    }

    /// Pixels across the whole texture
    public int width() {
        return faceSize * 3;
    }

    public int height() {
        return faceSize * 2;
    }

    /// Whether {@code other} is laid out the same, only in other colours, so can be drawn from the same
    /// {@link PlanetTextureGenerator.Layout}
    public boolean sameLayout(PlanetTextureSettings other) {
        if (faceSize != other.faceSize || seed != other.seed || blend != other.blend || noisiness != other.noisiness
                || blockiness != other.blockiness || ditherSpread != other.ditherSpread
                || ditherCoverage != other.ditherCoverage) {
            return false;
        }
        for (Group group : Group.values()) {
            if (colors(group).size() != other.colors(group).size()) return false;
        }
        return true;
    }
}
