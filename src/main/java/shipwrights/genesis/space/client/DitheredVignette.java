package shipwrights.genesis.space.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import shipwrights.genesis.GenesisMod;

import java.io.IOException;

/// Vanilla's vignette, dithered. It darkens the screen so gradually that each 8-bit step of brightness spans many
/// pixels, which shows as bands, most of all over the dark of space. Drawn in place of vanilla's by
/// {@link shipwrights.genesis.mixin.GuiMixin}.
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public final class DitheredVignette {
    private DitheredVignette() {
    }

    private static ShaderInstance shader;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), GenesisMod.id("vignette"),
                DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded);
    }

    /// Whether the vignette as vanilla has set it up can be drawn dithered. The world border's warning can't: it
    /// tints the screen red, darkening each channel differently, and the shader darkens them all alike.
    public static boolean canDraw() {
        float[] color = RenderSystem.getShaderColor();
        return shader != null && color[0] == color[1] && color[1] == color[2];
    }

    /// Draws the vignette as vanilla would with `GuiGraphics.blit`, taking its strength from the shader colour. Leaves
    /// the blending changed, as vanilla resets it straight after drawing.
    ///
    /// @param u0 the texture coordinates of the corner at `x0, y0`, with `v0`, from 0 to 1
    public static void draw(Matrix4f pose, ResourceLocation texture, float x0, float y0, float x1, float y1, float z,
                            float u0, float v0, float u1, float v1) {
        // The shader's colour is only its dithering, added to the screen darkened by its alpha. The screen's own alpha
        // is left alone.
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(() -> shader);

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(pose, x0, y0, z).setUv(u0, v0);
        builder.addVertex(pose, x0, y1, z).setUv(u0, v1);
        builder.addVertex(pose, x1, y1, z).setUv(u1, v1);
        builder.addVertex(pose, x1, y0, z).setUv(u1, v0);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }
}
