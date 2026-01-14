package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.ShaderRegistry;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;

import java.lang.Math;

import static shipwrights.genesis.client.ShaderRegistry.getSunRenderType;

public class StarRenderer implements CelestialRenderer {

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @Nullable Celestial vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor)event.getLevelRenderer()).getLevel();
        long ticks = GenesisMod.getTicks(level);
        Vector3dc position = toRender.getPosition(ticks, event.getPartialTick());
        Quaterniondc rotation = toRender.getRotation(ticks, event.getPartialTick());

        // TODO transform according to vantage point

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer sunBuffer = bufferSource.getBuffer(getSunRenderType());
        renderSun(event.getCamera().getPosition(), event.getPoseStack(), sunBuffer, toRender.getActualSize(),position, rotation);
        bufferSource.endBatch(getSunRenderType());
    }

    private void renderSun(Vec3 cameraPos, PoseStack poseStack, VertexConsumer buffer, double size, Vector3dc center, Quaterniondc localRotation) {

        Vector3f cameraPos0 = new Vector3f(
            (float) cameraPos.x,
            (float) cameraPos.y,
            (float) cameraPos.z
        ).sub((float) center.x(), (float) center.y(), (float) center.z());

        // Transform camera position into sun's local rotated space
        Vector3f rotatedCameraPos = new Vector3f(cameraPos0);
        new Quaternionf(localRotation).conjugate().transform(rotatedCameraPos);

        float halfSize = (float) size / 2;

        ShaderInstance shader = ShaderRegistry.SUN_SHADER.getInstance().get();
        if (shader != null) {
             Uniform uniform = shader.getUniform("CameraPosition");
             if (uniform != null) {
                 uniform.set(rotatedCameraPos.x, rotatedCameraPos.y, rotatedCameraPos.z);
             }

            Uniform uniform1 = shader.getUniform("HalfSize");
             if (uniform1 != null) {
                 uniform1.set(halfSize);
             }
        }

        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        matrix.translate(cameraPos0.negate(new Vector3f()));

        matrix.rotate(new Quaternionf(localRotation));


        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize);
        addCubeFaceSun(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize);
    }

    private static void addCubeFaceSun(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2,
                                       float x3, float y3, float z3, float x4, float y4, float z4) {
        
        float halfSize = Math.abs(x1);
        float size = 2 * halfSize;
        buffer.vertex(matrix, x1, y1, z1).color((int)(255 * (x1 + halfSize) / size), (int)(255 * (y1 + halfSize) / size), (int)(255 * (z1 + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color((int)(255 * (x2 + halfSize) / size), (int)(255 * (y2 + halfSize) / size), (int)(255 * (z2 + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color((int)(255 * (x3 + halfSize) / size), (int)(255 * (y3 + halfSize) / size), (int)(255 * (z3 + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color((int)(255 * (x4 + halfSize) / size), (int)(255 * (y4 + halfSize) / size), (int)(255 * (z4 + halfSize) / size), 255).endVertex();
    }
}
