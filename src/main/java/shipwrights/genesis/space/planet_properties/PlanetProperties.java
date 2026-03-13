package shipwrights.genesis.space.planet_properties;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record PlanetProperties(
        ResourceLocation id,
        double density,
        double thickness,
        boolean precipitation,
        PlanetColorPalette color
) {
    public static final Codec<PlanetProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(PlanetProperties::id),
            Codec.DOUBLE.fieldOf("density").forGetter(PlanetProperties::density),
            Codec.DOUBLE.fieldOf("thickness").forGetter(PlanetProperties::thickness),
            Codec.BOOL.fieldOf("precipitation").forGetter(PlanetProperties::precipitation),
            PlanetColorPalette.CODEC.fieldOf("color").forGetter(PlanetProperties::color)
    ).apply(instance, PlanetProperties::new));

    static final Map<ResourceLocation, PlanetProperties> PLANET_PROPERTIES = new HashMap<>();

    static void register(ResourceLocation id, PlanetProperties properties) {
        PLANET_PROPERTIES.put(id, properties);
    }

    static void reset() {
        PLANET_PROPERTIES.clear();
    }

    public static PlanetProperties get(ResourceLocation id) {
        return PLANET_PROPERTIES.get(id);
    }
}
