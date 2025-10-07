package shipwrights.genesis.client;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import shipwrights.genesis.GenesisMod;

public class SunRenderer {
    public static void renderSun(RenderLevelStageEvent event, VertexConsumer buffer) {
        PoseStack poseStack = event.getPoseStack();

        Vector3f cameraPos = new Vector3f(
            (float) event.getCamera().getPosition().x,
            (float) event.getCamera().getPosition().y,
            (float) event.getCamera().getPosition().z
        );

        ShaderInstance shader = ShaderRegistry.SUN_SHADER.getInstance().get();
        if (shader != null) {
             Uniform uniform = shader.getUniform("CameraPosition");
             if (uniform != null) {
                 uniform.set(cameraPos.x, cameraPos.y, cameraPos.z);
             }
        }

        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        matrix.translate((float) -event.getCamera().getPosition().x,
                (float) -event.getCamera().getPosition().y,
                (float) -event.getCamera().getPosition().z);

        matrix.rotate(new Quaternionf().rotationXYZ(0, 0, 0));

        float halfSize = 720.0f;

        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize);
        addCubeFaceSun(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize);
    }

    private static void addCubeFaceSun(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2,
                                       float x3, float y3, float z3, float x4, float y4, float z4) {
        buffer.vertex(matrix, x1, y1, z1).color((int)(255 * (x1 + 720) / 1440f), (int)(255 * (y1 + 720) / 1440f), (int)(255 * (z1 + 720) / 1440f), 255).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color((int)(255 * (x2 + 720) / 1440f), (int)(255 * (y2 + 720) / 1440f), (int)(255 * (z2 + 720) / 1440f), 255).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color((int)(255 * (x3 + 720) / 1440f), (int)(255 * (y3 + 720) / 1440f), (int)(255 * (z3 + 720) / 1440f), 255).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color((int)(255 * (x4 + 720) / 1440f), (int)(255 * (y4 + 720) / 1440f), (int)(255 * (z4 + 720) / 1440f), 255).endVertex();
    }
}
