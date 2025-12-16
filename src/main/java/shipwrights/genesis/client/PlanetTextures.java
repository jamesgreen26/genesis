package shipwrights.genesis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.GenesisMod;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages loading and caching of planet textures from resource packs.
 * Textures are loaded from assets/genesis/planets/&lt;dimension-namespace&gt;/&lt;dimension-path&gt;.png
 */
public class PlanetTextures {

    private static final ConcurrentHashMap<ResourceLocation, ResourceLocation> textureCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ResourceLocation, Boolean> existenceCache = new ConcurrentHashMap<>();

    /**
     * Gets the texture location for a planet, or null if no texture exists.
     *
     * @param dimensionId The dimension ID (e.g., minecraft:overworld)
     * @return The texture ResourceLocation, or null if no texture exists
     */
    @Nullable
    public static ResourceLocation getTexture(ResourceLocation dimensionId) {
        return textureCache.computeIfAbsent(dimensionId, PlanetTextures::loadTexture);
    }

    /**
     * Checks if a planet has a custom texture.
     *
     * @param dimensionId The dimension ID
     * @return true if a texture exists for this planet
     */
    public static boolean hasTexture(ResourceLocation dimensionId) {
        return existenceCache.computeIfAbsent(dimensionId, id -> {
            ResourceLocation textureLocation = getTextureLocation(id);
            return resourceExists(textureLocation);
        });
    }

    /**
     * Clears the texture cache. Call this when resources are reloaded.
     */
    public static void clearCache() {
        textureCache.clear();
        existenceCache.clear();
    }

    /**
     * Constructs the texture path for a given dimension ID.
     * Format: genesis:planets/<namespace>/<path>.png
     *
     * @param dimensionId The dimension ID
     * @return The ResourceLocation for the texture
     */
    public static ResourceLocation getTextureLocation(ResourceLocation dimensionId) {
        String path = "planets/" + dimensionId.getNamespace() + "/" + dimensionId.getPath() + ".png";
        return ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, path);
    }

    @Nullable
    private static ResourceLocation loadTexture(ResourceLocation dimensionId) {
        ResourceLocation textureLocation = getTextureLocation(dimensionId);

        if (resourceExists(textureLocation)) {
            existenceCache.put(dimensionId, true);
            // Convert to texture path format (without .png extension for Minecraft's texture system)
            String texturePath = "planets/" + dimensionId.getNamespace() + "/" + dimensionId.getPath();
            return ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, texturePath);
        }

        existenceCache.put(dimensionId, false);
        return null;
    }

    private static boolean resourceExists(ResourceLocation location) {
        try {
            // Convert to the full texture resource path
            ResourceLocation fullPath = ResourceLocation.fromNamespaceAndPath(
                location.getNamespace(),
                "textures/" + location.getPath()
            );
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(fullPath);
            return resource.isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}
