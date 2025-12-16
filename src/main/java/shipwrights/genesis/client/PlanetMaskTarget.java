package shipwrights.genesis.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * A render target that stores planet depth for masking in post-processing.
 * Uses a 32-bit floating point color texture to preserve depth precision.
 */
public class PlanetMaskTarget {
    private static int framebufferId = -1;
    private static int colorTextureId = -1;
    private static int depthTextureId = -1;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    /**
     * Ensures the render target exists and is the correct size.
     */
    private static void ensureTarget() {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        if (framebufferId == -1 || width != lastWidth || height != lastHeight) {
            destroy();

            // Create framebuffer
            framebufferId = GlStateManager.glGenFramebuffers();
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);

            // Create 32-bit float color texture for high-precision depth storage
            colorTextureId = GlStateManager._genTexture();
            GlStateManager._bindTexture(colorTextureId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, width, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (java.nio.FloatBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTextureId, 0);

            // Create depth texture
            depthTextureId = GlStateManager._genTexture();
            GlStateManager._bindTexture(depthTextureId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT24, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (java.nio.FloatBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTextureId, 0);

            // Verify framebuffer is complete
            int status = GlStateManager.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Planet mask framebuffer incomplete: " + status);
            }

            // Unbind
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GlStateManager._bindTexture(0);

            lastWidth = width;
            lastHeight = height;
        }
    }

    /**
     * Clears the planet mask target to prepare for a new frame.
     * Should be called before rendering planets.
     */
    public static void clear() {
        ensureTarget();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
        RenderSystem.viewport(0, 0, lastWidth, lastHeight);
        RenderSystem.clearDepth(1.0);
        RenderSystem.clearColor(0, 0, 0, 0);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    /**
     * Binds the planet mask target for writing.
     */
    public static void bindWrite() {
        ensureTarget();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
        RenderSystem.viewport(0, 0, lastWidth, lastHeight);
    }

    /**
     * Unbinds the planet mask target and restores the main framebuffer.
     */
    public static void unbindWrite() {
        Minecraft mc = Minecraft.getInstance();
        mc.getMainRenderTarget().bindWrite(true);
    }

    /**
     * Gets the depth texture ID for sampling in shaders.
     */
    public static int getDepthTextureId() {
        ensureTarget();
        return depthTextureId;
    }

    /**
     * Gets the color texture ID for sampling in shaders.
     */
    public static int getColorTextureId() {
        ensureTarget();
        return colorTextureId;
    }

    /**
     * Destroys the render target. Call on mod unload or when no longer needed.
     */
    public static void destroy() {
        if (colorTextureId != -1) {
            GlStateManager._deleteTexture(colorTextureId);
            colorTextureId = -1;
        }
        if (depthTextureId != -1) {
            GlStateManager._deleteTexture(depthTextureId);
            depthTextureId = -1;
        }
        if (framebufferId != -1) {
            GlStateManager._glDeleteFramebuffers(framebufferId);
            framebufferId = -1;
        }
        lastWidth = -1;
        lastHeight = -1;
    }
}
