package shipwrights.genesis.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * A render target that stores planet depth for masking in post-processing.
 * Planets render their depth to this target, which is then sampled by the
 * post-processing shader to exclude planets from effects.
 */
public class PlanetMaskTarget {
    private static RenderTarget target;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    /**
     * Gets or creates the planet mask render target, resizing if the window size changed.
     */
    public static RenderTarget getTarget() {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        if (target == null || width != lastWidth || height != lastHeight) {
            if (target != null) {
                target.destroyBuffers();
            }
            // Create a render target with depth buffer
            target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            lastWidth = width;
            lastHeight = height;
        }

        return target;
    }

    /**
     * Clears the planet mask target to prepare for a new frame.
     * Should be called before rendering planets.
     */
    public static void clear() {
        RenderTarget t = getTarget();
        t.bindWrite(true);
        // Clear to depth = 1.0 (far plane) so only planet pixels have closer depth
        RenderSystem.clearDepth(1.0);
        RenderSystem.clearColor(0, 0, 0, 0);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    /**
     * Binds the planet mask target for writing.
     */
    public static void bindWrite() {
        getTarget().bindWrite(true);
    }

    /**
     * Unbinds the planet mask target and restores the main framebuffer.
     */
    public static void unbindWrite() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    /**
     * Gets the depth texture ID for sampling in shaders.
     */
    public static int getDepthTextureId() {
        return getTarget().getDepthTextureId();
    }

    /**
     * Gets the color texture ID for sampling in shaders.
     */
    public static int getColorTextureId() {
        return getTarget().getColorTextureId();
    }

    /**
     * Binds the depth texture to a texture unit for sampling.
     */
    public static void bindDepthTexture(int textureUnit) {
        RenderSystem.activeTexture(GL30.GL_TEXTURE0 + textureUnit);
        RenderSystem.bindTexture(getDepthTextureId());
    }

    /**
     * Binds the color texture to a texture unit for sampling.
     */
    public static void bindColorTexture(int textureUnit) {
        RenderSystem.activeTexture(GL30.GL_TEXTURE0 + textureUnit);
        RenderSystem.bindTexture(getColorTextureId());
    }

    /**
     * Destroys the render target. Call on mod unload or when no longer needed.
     */
    public static void destroy() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }
}
