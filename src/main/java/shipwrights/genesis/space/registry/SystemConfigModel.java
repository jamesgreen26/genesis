package shipwrights.genesis.space.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import shipwrights.genesis.space.OrbitingBody;
import shipwrights.genesis.space.Star;

import java.util.List;

public record SystemConfigModel(List<Star> stars, List<OrbitingBody> bodies) {
    public static final Codec<SystemConfigModel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(Star.CODEC).optionalFieldOf("stars", List.of()).forGetter(SystemConfigModel::stars),
            Codec.list(OrbitingBody.CODEC).optionalFieldOf("bodies", List.of()).forGetter(SystemConfigModel::bodies)
    ).apply(instance, SystemConfigModel::new));
}
