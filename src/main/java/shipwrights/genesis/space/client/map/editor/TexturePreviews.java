package shipwrights.genesis.space.client.map.editor;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import shipwrights.genesis.space.client.CelestialVisualizer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

/// Puts the project's textures where the game looks for them, so celestials being made are drawn with them before
/// they're in any pack. Taken away again by {@link #releaseAll}, leaving whatever packs have there.
final class TexturePreviews {
    private static final Logger LOGGER = LogUtils.getLogger();

    /// The PNGs put in place, so unchanged ones aren't put in place again
    private final Map<ResourceLocation, byte[]> registered = new HashMap<>();
    private final Map<ResourceLocation, Size> sizes = new HashMap<>();

    record Size(int width, int height) {}

    /// Puts {@code textures} in place, and takes away those put in place before that are no longer among them
    void sync(Map<ResourceLocation, byte[]> textures) {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        boolean changed = false;
        for (ResourceLocation location : new HashSet<>(registered.keySet())) {
            if (!textures.containsKey(location)) {
                manager.release(location);
                registered.remove(location);
                sizes.remove(location);
                changed = true;
            }
        }
        for (Map.Entry<ResourceLocation, byte[]> texture : textures.entrySet()) {
            if (registered.get(texture.getKey()) == texture.getValue()) continue;
            try {
                NativeImage image = readPng(texture.getValue());
                sizes.put(texture.getKey(), new Size(image.getWidth(), image.getHeight()));
                manager.register(texture.getKey(), new DynamicTexture(image));
            } catch (IOException e) {
                LOGGER.warn("Couldn't read texture {} as a PNG", texture.getKey(), e);
                manager.release(texture.getKey());
                sizes.remove(texture.getKey());
            }
            registered.put(texture.getKey(), texture.getValue());
            changed = true;
        }
        if (changed) CelestialVisualizer.texturesChanged();
    }

    /// Reads a PNG of any size. Not {@link NativeImage#read(byte[])}, which copies it onto LWJGL's small per-thread
    /// stack, so fails with "Out of stack space" for any texture much over 64 KiB.
    static NativeImage readPng(byte[] png) throws IOException {
        return NativeImage.read(new ByteArrayInputStream(png));
    }

    /// The size of the texture put in place at {@code location}, if it could be read
    Optional<Size> size(ResourceLocation location) {
        return Optional.ofNullable(sizes.get(location));
    }

    void releaseAll() {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        registered.keySet().forEach(manager::release);
        registered.clear();
        sizes.clear();
        CelestialVisualizer.texturesChanged();
    }
}
