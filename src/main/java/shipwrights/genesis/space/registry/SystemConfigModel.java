package shipwrights.genesis.space.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import shipwrights.genesis.space.Celestial;

import java.util.List;

public record SystemConfigModel(List<Celestial> celestials) {
    public static final Codec<SystemConfigModel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(Celestial.CODEC).fieldOf("celestials").forGetter(SystemConfigModel::celestials)
    ).apply(instance, SystemConfigModel::new));
}
