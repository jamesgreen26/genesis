package shipwrights.genesis.space.planet_properties;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Atmosphere(
        double density,
        double thickness,
        boolean precipitation,
        PlanetColorPalette color
) {
    public static final Codec<Atmosphere> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("density").forGetter(Atmosphere::density),
            Codec.DOUBLE.fieldOf("thickness").forGetter(Atmosphere::thickness),
            Codec.BOOL.fieldOf("precipitation").forGetter(Atmosphere::precipitation),
            PlanetColorPalette.CODEC.fieldOf("color").forGetter(Atmosphere::color)
    ).apply(instance, Atmosphere::new));
}
