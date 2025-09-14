package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import shipwrights.genesis.GenesisMod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlanetRenderer {

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || !minecraft.level.dimension().location().equals(GenesisMod.SPACE_DIM)) {
                return;
            }

            renderSun(event);
        }

        private static void renderSun(RenderLevelStageEvent event) {
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();

            poseStack.translate(-event.getCamera().getPosition().x,
                    -event.getCamera().getPosition().y,
                    -event.getCamera().getPosition().z);

            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            float size = 5.0f;
            float r = 1.0f, g = 1.0f, b = 0.0f, a = 1.0f;

            addCubeFace(poseStack.last().pose(), bufferBuilder, -size, -size, size, size, -size, size, size, size, size, -size, size, size, r, g, b, a);
            addCubeFace(poseStack.last().pose(), bufferBuilder, -size, -size, -size, -size, size, -size, size, size, -size, size, -size, -size, r, g, b, a);
            addCubeFace(poseStack.last().pose(), bufferBuilder, -size, -size, -size, -size, -size, size, -size, size, size, -size, size, -size, r, g, b, a);
            addCubeFace(poseStack.last().pose(), bufferBuilder, size, -size, -size, size, size, -size, size, size, size, size, -size, size, r, g, b, a);
            addCubeFace(poseStack.last().pose(), bufferBuilder, -size, -size, -size, size, -size, -size, size, -size, size, -size, -size, size, r, g, b, a);
            addCubeFace(poseStack.last().pose(), bufferBuilder, -size, size, -size, -size, size, size, size, size, size, size, size, -size, r, g, b, a);

            poseStack.popPose();

            BufferBuilder.RenderedBuffer renderedBuffer = bufferBuilder.end();
            BufferUploader.drawWithShader(renderedBuffer);
        }

        private static void addCubeFace(Matrix4f matrix, BufferBuilder buffer, float x1, float y1, float z1, float x2, float y2, float z2,
                                        float x3, float y3, float z3, float x4, float y4, float z4,
                                        float r, float g, float b, float a) {
            buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x4, y4, z4).color(r, g, b, a).endVertex();
        }
}
