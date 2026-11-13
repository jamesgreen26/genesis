package shipwrights.genesis.space.client.components;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderStateShard;

/// Blending for the celestial shaders, which output premultiplied alpha: colour already multiplied by alpha. They
/// dither their output to hide colour banding (see `shaders/include/dither.glsl`), and the dithering must reach the
/// screen at full strength, where vanilla's blending would multiply it by alpha along with the colour.
final class PremultipliedTransparency {
    private PremultipliedTransparency() {
    }

    /// As vanilla's translucent transparency: blended over what's behind it
    static final RenderStateShard.TransparencyStateShard TRANSLUCENT = new RenderStateShard.TransparencyStateShard(
            "genesis_premultiplied_translucent_transparency", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
    );

    /// As vanilla's lightning transparency: added to what's behind it
    static final RenderStateShard.TransparencyStateShard ADDITIVE = new RenderStateShard.TransparencyStateShard(
            "genesis_premultiplied_additive_transparency", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
    );
}
