package shipwrights.genesis.space.star_properties;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record StarProperties(
        ResourceLocation id,
        int r0,
        int g0,
        int b0,
        int r1,
        int g1,
        int b1
) {
    public static final Codec<StarProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(StarProperties::id),
            Codec.INT.fieldOf("r0").forGetter(StarProperties::r0),
            Codec.INT.fieldOf("g0").forGetter(StarProperties::g0),
            Codec.INT.fieldOf("b0").forGetter(StarProperties::b0),
            Codec.INT.fieldOf("r1").forGetter(StarProperties::r1),
            Codec.INT.fieldOf("g1").forGetter(StarProperties::g1),
            Codec.INT.fieldOf("b1").forGetter(StarProperties::b1)
    ).apply(instance, StarProperties::new));

    static final Map<ResourceLocation, StarProperties> STAR_PROPERTIES = new HashMap<>();

    static void register(ResourceLocation id, StarProperties properties) {
        STAR_PROPERTIES.put(id, properties);
    }

    static void reset() {
        STAR_PROPERTIES.clear();
    }

    public static StarProperties get(ResourceLocation id) {
        return STAR_PROPERTIES.get(id);
    }
}
