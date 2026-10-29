package shipwrights.genesis.space.client.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.client.CelestialRenderContext;
import shipwrights.genesis.space.client.CelestialVisualizer;

/// A star's two shader colours, {@code Color0} and {@code Color1}, as packed {@code 0xRRGGBB} values.
public record StarComponent(
        int color0,
        int color1
) implements CelestialVisualizer.Component {
    public static final ResourceLocation TYPE = GenesisMod.id("star");

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    @Override
    public void render(CelestialRenderContext context) {
        // TODO
    }

    public static final Codec<StarComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("color0").forGetter(StarComponent::color0),
                    Codec.INT.fieldOf("color1").forGetter(StarComponent::color1)
            ).apply(instance, StarComponent::new)
    );
}
