package g_mungus.genesis.space;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import g_mungus.genesis.GenesisMod;
import g_mungus.genesis.PlanetRegistry;
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
import org.joml.*;

import java.lang.Math;

@EventBusSubscriber(value = Dist.CLIENT)
public class SunRenderer {

    private static final ResourceLocation SUN_RENDER_TYPE = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "sun");

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;

        RenderType sunRenderType = VeilRenderType.get(SUN_RENDER_TYPE);
        if (sunRenderType == null) {
            System.out.println("Could not load render type");
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(GenesisMod.SPACE_DIM)) return;

        Camera camera = event.getCamera();

        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        double fov = 2 * Math.atan(1.0 / RenderSystem.getProjectionMatrix().get(1,1));

        Matrix4f newProj = createFarProjectionMatrix(0.001f, 1000000.0f, fov);

        RenderSystem.setProjectionMatrix(newProj, VertexSorting.byDistance(((float) camX), (float) camY, (float) camZ));

        renderBody(event, sunRenderType, 0, 0, 0, 2048f, new Vector3f(15, 45, 5), 0);

        PlanetRegistry.planets.forEach((key, value) -> {
            renderBody(event, sunRenderType, value.location().x(), value.location().y(), value.location().z(), value.size(), value.eulerAngles(), 1);
        });

    }

    private static void renderBody(RenderLevelStageEvent event, RenderType renderType, int cubeX, int cubeY, int cubeZ, float baseSize, Vector3fc rot, float type) {
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();


        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        poseStack.pushPose();
        Matrix4f matrix;

        poseStack.translate(-camX, -camY, -camZ);
        poseStack.rotateAround(eulerToQuaternion(rot), cubeX, cubeY, cubeZ);
        matrix = poseStack.last().pose();
        drawCube(buffer, matrix, (float) cubeX, (float) cubeY, (float) cubeZ, baseSize, type);

        poseStack.popPose();
    }


    private static void drawCube(VertexConsumer buffer, Matrix4f matrix, float centerX, float centerY, float centerZ, float size, float type) {
        float half = size / 2f;

        float x0 = centerX - half;
        float x1 = centerX + half;
        float y0 = centerY - half;
        float y1 = centerY + half;
        float z0 = centerZ - half;
        float z1 = centerZ + half;

        // Front face (toward positive Z)
        buffer.addVertex(matrix, x0, y0, z1).setColor(type, 0f, 0f, 0f);
        buffer.addVertex(matrix, x1, y0, z1).setColor(type, 0f, 1f, 0f);
        buffer.addVertex(matrix, x1, y1, z1).setColor(type, 0f, 1f, 1f);
        buffer.addVertex(matrix, x0, y1, z1).setColor(type, 0f, 0f, 1f);

        // Back face (toward negative Z)
        buffer.addVertex(matrix, x1, y0, z0).setColor(type, 0f, 0f, 0f);
        buffer.addVertex(matrix, x0, y0, z0).setColor(type, 0f, 1f, 0f);
        buffer.addVertex(matrix, x0, y1, z0).setColor(type, 0f, 1f, 1f);
        buffer.addVertex(matrix, x1, y1, z0).setColor(type, 0f, 0f, 1f);

        // Left face
        buffer.addVertex(matrix, x0, y0, z0).setColor(type, 0f, 0f, 0f);
        buffer.addVertex(matrix, x0, y0, z1).setColor(type, 0f, 1f, 0f);
        buffer.addVertex(matrix, x0, y1, z1).setColor(type, 0f, 1f, 1f);
        buffer.addVertex(matrix, x0, y1, z0).setColor(type, 0f, 0f, 1f);

        // Right face
        buffer.addVertex(matrix, x1, y0, z1).setColor(type, 0f, 0f, 0f);
        buffer.addVertex(matrix, x1, y0, z0).setColor(type, 0f, 1f, 0f);
        buffer.addVertex(matrix, x1, y1, z0).setColor(type, 0f, 1f, 1f);
        buffer.addVertex(matrix, x1, y1, z1).setColor(type, 0f, 0f, 1f);

        // Top face
        buffer.addVertex(matrix, x0, y1, z1).setColor(type, 0f, 0f, 0f);
        buffer.addVertex(matrix, x1, y1, z1).setColor(type, 0f, 1f, 0f);
        buffer.addVertex(matrix, x1, y1, z0).setColor(type, 0f, 1f, 1f);
        buffer.addVertex(matrix, x0, y1, z0).setColor(type, 0f, 0f, 1f);

        // Bottom face
        buffer.addVertex(matrix, x0, y0, z0).setColor(type, 0f, 0f, 0f);
        buffer.addVertex(matrix, x1, y0, z0).setColor(type, 0f, 1f, 0f);
        buffer.addVertex(matrix, x1, y0, z1).setColor(type, 0f, 1f, 1f);
        buffer.addVertex(matrix, x0, y0, z1).setColor(type, 0f, 0f, 1f);
    }


    private static Quaternionf eulerToQuaternion(Vector3fc eulerAnglesRad) {
        return new Quaternionf()
                .rotationXYZ(eulerAnglesRad.x(), eulerAnglesRad.y(), eulerAnglesRad.z());
    }

    public static Matrix4f createFarProjectionMatrix(float near, float far, double fov) {
        float aspect = (float) Minecraft.getInstance().getWindow().getWidth() /
                (float) Minecraft.getInstance().getWindow().getHeight();

        float yScale = (float)(1.0 / Math.tan(fov / 2.0));
        float xScale = yScale / aspect;
        float frustumLength = far - near;

        Matrix4f matrix = new Matrix4f();

        matrix.m00(xScale);
        matrix.m11(yScale);
        matrix.m22(-(far + near) / frustumLength);
        matrix.m23(-1.0f);
        matrix.m32(-(2 * near * far) / frustumLength);
        matrix.m33(0.0f);

        return matrix;
    }

}
