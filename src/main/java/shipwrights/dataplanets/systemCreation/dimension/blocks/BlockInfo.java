package shipwrights.dataplanets.systemCreation.dimension.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record BlockInfo (
        double mass,
        double temperature,
        double humidity,
        double minerality,
        double weirdness,
        ResourceLocation block
) {
    public static final Codec<BlockInfo> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("mass").forGetter(BlockInfo::mass),
                    Codec.DOUBLE.fieldOf("temperature").forGetter(BlockInfo::temperature),
                    Codec.DOUBLE.fieldOf("humidity").forGetter(BlockInfo::humidity),
                    Codec.DOUBLE.fieldOf("minerality").forGetter(BlockInfo::minerality),
                    Codec.DOUBLE.fieldOf("weirdness").forGetter(BlockInfo::weirdness),
                    ResourceLocation.CODEC.fieldOf("block").forGetter(BlockInfo::block)
            ).apply(instance, BlockInfo::new)
    );
}
