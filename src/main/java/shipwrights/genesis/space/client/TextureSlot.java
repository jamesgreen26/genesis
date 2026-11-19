package shipwrights.genesis.space.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/// A texture a {@link CelestialVisualizer.Component} draws a celestial with, found by its name rather than given in the
/// celestial's JSON. Declared so that editors know to ask for it.
///
/// @param location the texture's full location, as in {@code genesis:textures/celestials/minecraft/overworld.png}
/// @param name     what to call it, in a word or two
/// @param required whether the component needs it, or does without
/// @param hint     what it should look like, such as its layout
/// @param generatable whether editors can make it with the planet surface generator, being a cube's surface laid out as
///                    {@link CubeFace} has it
public record TextureSlot(ResourceLocation location, Component name, boolean required, Component hint,
                          boolean generatable) {

    public TextureSlot(ResourceLocation location, Component name, boolean required, Component hint) {
        this(location, name, required, hint, false);
    }

    /// Whether a texture is at {@code location}: in a resource pack, or put there directly, as editors do to preview
    /// textures that aren't in one yet
    public static boolean exists(ResourceLocation location) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getTextureManager().getTexture(location, null) != null
                || minecraft.getResourceManager().getResource(location).isPresent();
    }
}
