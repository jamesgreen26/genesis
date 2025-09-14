package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import shipwrights.genesis.GenesisMod;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;

import static shipwrights.genesis.client.ShaderRegistry.SUN_SHADER;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlanetRenderer {


    private static LodestoneRenderType SUN_RENDER_TYPE;


    private static LodestoneRenderType getSunRenderType() {
        if (SUN_RENDER_TYPE == null) {
            SUN_RENDER_TYPE = LodestoneRenderTypeRegistry.createGenericRenderType("sun_render_type", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, LodestoneRenderTypeRegistry.builder()
                    .setShaderState(SUN_SHADER)
            );
        }
        return SUN_RENDER_TYPE;
    }

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

            poseStack.mulPose(new Quaternionf().rotationXYZ(15, 45, 5));

            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer buffer = bufferSource.getBuffer(getSunRenderType());

            float size = 512.0f;

            addCubeFace(poseStack.last().pose(), buffer, -size, -size, size, size, -size, size, size, size, size, -size, size, size);
            addCubeFace(poseStack.last().pose(), buffer, -size, -size, -size, -size, size, -size, size, size, -size, size, -size, -size);
            addCubeFace(poseStack.last().pose(), buffer, -size, -size, -size, -size, -size, size, -size, size, size, -size, size, -size);
            addCubeFace(poseStack.last().pose(), buffer, size, -size, -size, size, size, -size, size, size, size, size, -size, size);
            addCubeFace(poseStack.last().pose(), buffer, -size, -size, -size, size, -size, -size, size, -size, size, -size, -size, size);
            addCubeFace(poseStack.last().pose(), buffer, -size, size, -size, -size, size, size, size, size, size, size, size, -size);

            poseStack.popPose();
        }

        private static void addCubeFace(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2,
                                        float x3, float y3, float z3, float x4, float y4, float z4) {
            buffer.vertex(matrix, x1, y1, z1).color(0.0f, 0, 0, 0).endVertex();
            buffer.vertex(matrix, x2, y2, z2).color(0.0f, 0, 1, 0).endVertex();
            buffer.vertex(matrix, x3, y3, z3).color(0.0f, 0, 1, 1).endVertex();
            buffer.vertex(matrix, x4, y4, z4).color(0.0f, 0, 0, 1).endVertex();
        }
}
