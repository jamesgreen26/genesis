package shipwrights.genesis.space.planet_properties;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record PlanetPropertiesModel(List<PlanetProperties> planets) {
    public static final Codec<PlanetPropertiesModel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(PlanetProperties.CODEC).fieldOf("planets").forGetter(PlanetPropertiesModel::planets)
    ).apply(instance, PlanetPropertiesModel::new));
}
