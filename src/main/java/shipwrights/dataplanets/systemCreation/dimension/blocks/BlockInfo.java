package shipwrights.dataplanets.systemCreation.dimension.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import shipwrights.dataplanets.util.Color;

public record BlockInfo (
        double mass,
        double temperature,
        double humidity,
        double minerality,
        double weirdness,
        ResourceLocation block,
        String color
) {
    public static final Codec<BlockInfo> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("mass").forGetter(BlockInfo::mass),
                    Codec.DOUBLE.fieldOf("temperature").forGetter(BlockInfo::temperature),
                    Codec.DOUBLE.fieldOf("humidity").forGetter(BlockInfo::humidity),
                    Codec.DOUBLE.fieldOf("minerality").forGetter(BlockInfo::minerality),
                    Codec.DOUBLE.fieldOf("weirdness").forGetter(BlockInfo::weirdness),
                    ResourceLocation.CODEC.fieldOf("block").forGetter(BlockInfo::block),
                    Codec.STRING.fieldOf("color").forGetter(BlockInfo::color)
            ).apply(instance, BlockInfo::new)
    );

    public Color getColor() {
        String hex = color.startsWith("#") ? color.substring(1) : color;
        int rgb = Integer.parseInt(hex, 16);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return new Color(red, green, blue, 255);
    }
}
