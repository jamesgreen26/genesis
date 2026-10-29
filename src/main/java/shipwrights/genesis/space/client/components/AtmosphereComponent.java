package shipwrights.genesis.space.client.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.client.CelestialRenderContext;
import shipwrights.genesis.space.client.CelestialVisualizer;

public record AtmosphereComponent(
        double density,
        double thickness,
        int color
) implements CelestialVisualizer.Component {
    public static final ResourceLocation TYPE = GenesisMod.id("atmosphere");

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    @Override
    public void render(CelestialRenderContext context) {
        // TODO
    }

    public static final Codec<AtmosphereComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("density").forGetter(AtmosphereComponent::density),
                    Codec.DOUBLE.fieldOf("thickness").forGetter(AtmosphereComponent::thickness),
                    Codec.INT.fieldOf("color").forGetter(AtmosphereComponent::color)
            ).apply(instance, AtmosphereComponent::new)
    );
}
