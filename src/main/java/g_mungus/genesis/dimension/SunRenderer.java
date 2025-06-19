package g_mungus.genesis.dimension;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import g_mungus.genesis.GenesisMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(value = Dist.CLIENT)
public class SunRenderer {

    private static final ResourceLocation SUN_RENDER_TYPE = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "sun");

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        RenderType renderType = VeilRenderType.get(SUN_RENDER_TYPE);
        if (renderType == null) {
            System.out.println("Could not load render type");
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(GenesisMod.SPACE_DIM)) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();


        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        double cubeX = 0;
        double cubeY = 0;
        double cubeZ = 0;


        double dx = cubeX - camX;
        double dy = cubeY - camY;
        double dz = cubeZ - camZ;

        double distanceSq = dx * dx + dy * dy + dz * dz;
        double maxRealRenderDist = 100.0;
        float baseSize = 64.0f;

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        poseStack.pushPose();
        Matrix4f matrix;
        if (distanceSq <= maxRealRenderDist * maxRealRenderDist) {
            poseStack.translate(-camX, -camY, -camZ);
            matrix = poseStack.last().pose();
            drawCube(buffer, matrix, (float) cubeX, (float) cubeY, (float) cubeZ, baseSize);
        } else {
            double distance = Math.sqrt(distanceSq);
            float scale = (float) (baseSize * (maxRealRenderDist / distance));

            double dirX = dx / distance;
            double dirY = dy / distance;
            double dirZ = dz / distance;

            float fakeX = (float) (camX + dirX * maxRealRenderDist);
            float fakeY = (float) (camY + dirY * maxRealRenderDist);
            float fakeZ = (float) (camZ + dirZ * maxRealRenderDist);

            poseStack.translate(-camX, -camY, -camZ);
            Matrix4f matrix4f = poseStack.last().pose();
            drawCube(buffer, matrix4f, fakeX, fakeY, fakeZ, scale);
        }
        poseStack.popPose();
    }


    private static void drawCube(VertexConsumer buffer, Matrix4f matrix, float centerX, float centerY, float centerZ, float size) {
        float half = size / 2f;

        float x0 = centerX - half;
        float x1 = centerX + half;
        float y0 = centerY - half;
        float y1 = centerY + half;
        float z0 = centerZ - half;
        float z1 = centerZ + half;

        // Front face (toward positive Z)
        buffer.addVertex(matrix, x0, y0, z1).setUv(0f, 0f);
        buffer.addVertex(matrix, x1, y0, z1).setUv(1f, 0f);
        buffer.addVertex(matrix, x1, y1, z1).setUv(1f, 1f);
        buffer.addVertex(matrix, x0, y1, z1).setUv(0f, 1f);

        // Back face (toward negative Z)
        buffer.addVertex(matrix, x1, y0, z0).setUv(0f, 0f);
        buffer.addVertex(matrix, x0, y0, z0).setUv(1f, 0f);
        buffer.addVertex(matrix, x0, y1, z0).setUv(1f, 1f);
        buffer.addVertex(matrix, x1, y1, z0).setUv(0f, 1f);

        // Left face
        buffer.addVertex(matrix, x0, y0, z0).setUv(0f, 0f);
        buffer.addVertex(matrix, x0, y0, z1).setUv(1f, 0f);
        buffer.addVertex(matrix, x0, y1, z1).setUv(1f, 1f);
        buffer.addVertex(matrix, x0, y1, z0).setUv(0f, 1f);

        // Right face
        buffer.addVertex(matrix, x1, y0, z1).setUv(0f, 0f);
        buffer.addVertex(matrix, x1, y0, z0).setUv(1f, 0f);
        buffer.addVertex(matrix, x1, y1, z0).setUv(1f, 1f);
        buffer.addVertex(matrix, x1, y1, z1).setUv(0f, 1f);

        // Top face
        buffer.addVertex(matrix, x0, y1, z1).setUv(0f, 0f);
        buffer.addVertex(matrix, x1, y1, z1).setUv(1f, 0f);
        buffer.addVertex(matrix, x1, y1, z0).setUv(1f, 1f);
        buffer.addVertex(matrix, x0, y1, z0).setUv(0f, 1f);

        // Bottom face
        buffer.addVertex(matrix, x0, y0, z0).setUv(0f, 0f);
        buffer.addVertex(matrix, x1, y0, z0).setUv(1f, 0f);
        buffer.addVertex(matrix, x1, y0, z1).setUv(1f, 1f);
        buffer.addVertex(matrix, x0, y0, z1).setUv(0f, 1f);
    }


}
