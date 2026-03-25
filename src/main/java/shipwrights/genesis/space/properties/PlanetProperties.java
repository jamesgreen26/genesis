package shipwrights.genesis.space.properties;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PlanetProperties(
        Atmosphere atmosphere
) implements CelestialProperties {
    public static final Codec<PlanetProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Atmosphere.CODEC.fieldOf("atmosphere").forGetter(PlanetProperties::atmosphere)
    ).apply(instance, PlanetProperties::new));
}
