package shipwrights.genesis.space.star_properties;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record StarPropertiesModel(List<StarProperties> stars) {
    public static final Codec<StarPropertiesModel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(StarProperties.CODEC).fieldOf("stars").forGetter(StarPropertiesModel::stars)
    ).apply(instance, StarPropertiesModel::new));
}
