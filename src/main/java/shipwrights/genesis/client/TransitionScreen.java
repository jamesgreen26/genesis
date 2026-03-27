package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.renderer.GameRenderer;
import org.jetbrains.annotations.NotNull;

public class TransitionScreen extends ReceivingLevelScreen {
    public TransitionScreen() {
        super();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (TransitionFrame.capturedTarget == null || minecraft == null) return;

        int w = this.width;
        int h = this.height;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TransitionFrame.capturedTarget.getColorTextureId());

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        // Framebuffer textures are flipped vertically.
        buffer.vertex(0, h, 0).uv(0, 0).endVertex();
        buffer.vertex(w, h, 0).uv(1, 0).endVertex();
        buffer.vertex(w, 0, 0).uv(1, 1).endVertex();
        buffer.vertex(0, 0, 0).uv(0, 1).endVertex();

        tesselator.end();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    @Override
    public void onClose() {
        super.onClose();
        TransitionState.CURRENT = TransitionState.NONE;
    }
}
