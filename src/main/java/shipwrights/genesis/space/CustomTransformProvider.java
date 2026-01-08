package shipwrights.genesis.space;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows custom position and rotation providers for celestial bodies.
 * <br>
 * Implementations must be registered via {@link #register(ResourceLocation, Codec)} before use.
 */
public interface CustomTransformProvider {
    Quaterniondc getRotation(long ticks, float subticks);
    Vector3d getCurrentPos(long ticks, float subticks);

    /**
     * Returns the type identifier for this provider.
     * This is used during serialization to determine which codec to use.
     */
    ResourceLocation getType();

    // Registry for CustomTransformProvider codecs
    Map<ResourceLocation, Codec<? extends CustomTransformProvider>> REGISTRY = new HashMap<>();

    /**
     * Register a CustomTransformProvider type with its codec.
     *
     * @param type The unique identifier for this provider type
     * @param codec The codec to serialize/deserialize this provider type
     */
    static void register(ResourceLocation type, Codec<? extends CustomTransformProvider> codec) {
        REGISTRY.put(type, codec);
    }

    /**
     * Dispatch codec that handles serialization of any registered CustomTransformProvider type.
     */
    Codec<CustomTransformProvider> DISPATCH_CODEC = ResourceLocation.CODEC.dispatchStable(
        CustomTransformProvider::getType,
        type -> {
            Codec<? extends CustomTransformProvider> codec = REGISTRY.get(type);
            if (codec == null) {
                throw new IllegalArgumentException("Unknown CustomTransformProvider type: " + type);
            }
            return codec;
        }
    );
}
