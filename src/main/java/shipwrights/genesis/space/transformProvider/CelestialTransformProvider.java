package shipwrights.genesis.space.transformProvider;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import shipwrights.genesis.space.Celestial;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows custom position and rotation for celestial bodies.
 * <br>
 * Implementations must be registered via {@link #register(ResourceLocation, Codec)} before use.
 */
public interface CelestialTransformProvider {

    Vector3d getPosition(long ticks, float subticks, Registry<Celestial> registry);

    Quaterniondc getRotation(long ticks, float subticks, Registry<Celestial> registry);

    ResourceLocation getType();

    // Registry for CelestialTransformProvider codecs
    Map<ResourceLocation, Codec<? extends CelestialTransformProvider>> REGISTRY = new HashMap<>();

    /**
     * Register a CelestialTransformProvider type with its codec.
     *
     * @param type The unique identifier for this provider type
     * @param codec The codec to serialize/deserialize this provider type
     */
    static void register(ResourceLocation type, Codec<? extends CelestialTransformProvider> codec) {
        REGISTRY.put(type, codec);
    }

    /**
     * Dispatch codec that handles serialization of any registered CelestialTransformProvider type.
     */
    Codec<CelestialTransformProvider> DISPATCH_CODEC = ResourceLocation.CODEC.dispatchStable(
        CelestialTransformProvider::getType,
        type -> {
            Codec<? extends CelestialTransformProvider> codec = REGISTRY.get(type);
            if (codec == null) {
                throw new IllegalArgumentException("Unknown CelestialTransformProvider type: " + type);
            }
            return codec;
        }
    );
}
