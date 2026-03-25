package shipwrights.genesis.space.properties;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public interface PlanetColorPalette {

    boolean isOverworld();

    int[] getRGB();

    record RGB(
            int r,
            int g,
            int b
    ) implements PlanetColorPalette {

        static final MapCodec<RGB> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("r").forGetter(RGB::r),
                Codec.INT.fieldOf("g").forGetter(RGB::g),
                Codec.INT.fieldOf("b").forGetter(RGB::b)
        ).apply(instance, RGB::new));

        @Override
        public boolean isOverworld() {
            return false;
        }

        @Override
        public int[] getRGB() {
            return new int[]{r, g, b};
        }
    }

    class Overworld implements PlanetColorPalette {

        static final MapCodec<Overworld> MAP_CODEC = MapCodec.unit(new Overworld());

        @Override
        public boolean isOverworld() {
            return true;
        }

        @Override
        public int[] getRGB() {
            throw new UnsupportedOperationException("Overworld should be handled differently");
        }
    }

    Codec<PlanetColorPalette> CODEC = Codec.STRING.dispatch(
            "type",
            palette -> palette.isOverworld() ? "genesis:overworld" : "genesis:rgb",
            type -> switch (type) {
                case "genesis:rgb" -> RGB.MAP_CODEC.codec();
                case "genesis:overworld" -> Overworld.MAP_CODEC.codec();
                default -> throw new IllegalArgumentException("Unknown PlanetColorPalette type: " + type);
            }
    );
}
