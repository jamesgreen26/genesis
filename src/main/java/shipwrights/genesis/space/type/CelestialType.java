package shipwrights.genesis.space.type;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.space.renderer.EffectFactory;

import java.util.HashMap;
import java.util.Map;

public interface CelestialType {
    boolean castsLight();
    boolean castsShadow();
    boolean isVisitable();
    @Nullable EffectFactory getEffectFactory();
    @NotNull ResourceLocation getID();

    static CelestialType get(ResourceLocation ID) {
        return REGISTRY.get(ID);
    }

    static void register(CelestialType type) {
        REGISTRY.putIfAbsent(type.getID(), type);
    }

    Map<ResourceLocation, CelestialType> REGISTRY = new HashMap<>();
}
