package shipwrights.genesis.client;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.*;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.lang.Math;

public class SunRenderer {
    public static void renderSun(Camera camera, PoseStack poseStack, VertexConsumer buffer, double size, Vector3dc center, Quaterniondc localRotation) {

        Vector3f cameraPos = new Vector3f(
            (float) camera.getPosition().x,
            (float) camera.getPosition().y,
            (float) camera.getPosition().z
        ).sub((float) center.x(), (float) center.y(), (float) center.z());

        float halfSize = (float) size / 2;

        ShaderInstance shader = ShaderRegistry.SUN_SHADER.getInstance().get();
        if (shader != null) {
             Uniform uniform = shader.getUniform("CameraPosition");
             if (uniform != null) {
                 uniform.set(cameraPos.x, cameraPos.y, cameraPos.z);
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

        matrix.translate(cameraPos.negate(new Vector3f()));

        matrix.rotate(new Quaternionf().rotationXYZ(0, 0, 0));


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
