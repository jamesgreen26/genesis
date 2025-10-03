package shipwrights.genesis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class SunRenderer {
    public static void renderSun(RenderLevelStageEvent event, VertexConsumer buffer) {
        PoseStack poseStack = event.getPoseStack();

        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        matrix.translate((float) -event.getCamera().getPosition().x,
                (float) -event.getCamera().getPosition().y,
                (float) -event.getCamera().getPosition().z);

        matrix.rotate(new Quaternionf().rotationXYZ(15, 45, 5));

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
        buffer.vertex(matrix, x1, y1, z1).uv(0, 0).endVertex();
        buffer.vertex(matrix, x2, y2, z2).uv(1, 0).endVertex();
        buffer.vertex(matrix, x3, y3, z3).uv(1, 1).endVertex();
        buffer.vertex(matrix, x4, y4, z4).uv(0, 1).endVertex();
    }
}
