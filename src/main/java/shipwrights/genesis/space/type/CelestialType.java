package shipwrights.genesis.space.type;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import shipwrights.genesis.space.properties.CelestialProperties;
import shipwrights.genesis.space.renderer.CelestialRenderer;

import java.util.HashMap;
import java.util.Map;

public interface CelestialType {
    boolean castsLight();
    boolean castsShadow();
    boolean isVisitable();
    @NotNull CelestialRenderer getRenderer();
    @NotNull ResourceLocation getID();
    @NotNull Codec<? extends CelestialProperties> propertiesCodec();

    static CelestialType get(ResourceLocation ID) {
        return REGISTRY.get(ID);
    }

    static void register(CelestialType type) {
        REGISTRY.putIfAbsent(type.getID(), type);
    }

    Map<ResourceLocation, CelestialType> REGISTRY = new HashMap<>();
}
