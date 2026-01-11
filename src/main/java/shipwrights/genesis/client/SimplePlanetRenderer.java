package shipwrights.genesis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.*;

import java.lang.Math;

import static shipwrights.genesis.client.ShaderRegistry.getTexturedPlanetRenderType;

public class SimplePlanetRenderer {

    public static void RenderPlanetAt(ResourceLocation planetID, PoseStack poseStack, double x, double y, double z, double halfExtent, Quaterniondc localRotation, Vector3dc lightOffset) {
        // Get the texture for this planet
        ResourceLocation textureLocation = PlanetTextures.getTexture(planetID);
        if (textureLocation == null) {
            return; // No texture available
        }

        // Set up buffer source
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        var renderType = getTexturedPlanetRenderType(textureLocation);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        // Clone the matrix
        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        // Apply translation
        matrix.translate((float) x, (float) y, (float) z);

        // Apply rotation
        Quaternionf rotation = new Quaternionf(localRotation);
        matrix.rotate(rotation);

        float halfSize = (float) halfExtent;

        Vector3d lightDir = new Vector3d(lightOffset).mul(-1);

        // UV layout (3x2 grid):
        // | north (0,0)     | west (1/3,0)   | south (2/3,0)  |
        // | east (0,0.5)    | down (1/3,0.5) | up (2/3,0.5)   |
        float third = 1.0f / 3.0f;
        float twoThirds = 2.0f / 3.0f;

        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, twoThirds, 0.0f, 1.0f, 0.5f);        // South face (+Z)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, halfSize, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, 0.0f, 0.0f, third, 0.5f);        // North face (-Z)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, third, 0.0f, twoThirds, 0.5f);       // West face (-X)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, halfSize, -halfSize, halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, 0.0f, 0.5f, third, 1.0f);            // East face (+X)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, third, 0.5f, twoThirds, 1.0f);       // Down face (-Y)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, twoThirds, 0.5f, 1.0f, 1.0f);        // Up face (+Y)


        // End batch to flush rendering
        bufferSource.endBatch(renderType);
    }

    private static void addTexturedCubeFace(Matrix4f matrix, VertexConsumer buffer, float halfSize,
                                            Vector3d lightDir, Quaternionf rotation,
                                            float x1, float y1, float z1,
                                            float x2, float y2, float z2,
                                            float x3, float y3, float z3,
                                            float x4, float y4, float z4,
                                            float u1, float v1, float u2, float v2) {
        addTexturedVertexWithLighting(matrix, buffer, x1, y1, z1, u1, v2, lightDir, rotation);
        addTexturedVertexWithLighting(matrix, buffer, x2, y2, z2, u2, v2, lightDir, rotation);
        addTexturedVertexWithLighting(matrix, buffer, x3, y3, z3, u2, v1, lightDir, rotation);
        addTexturedVertexWithLighting(matrix, buffer, x4, y4, z4, u1, v1, lightDir, rotation);
    }

    private static void addTexturedVertexWithLighting(Matrix4f matrix, VertexConsumer buffer,
                                                      float x, float y, float z,
                                                      float u, float v,
                                                      Vector3d lightDir, Quaternionf rotation) {
        Vector3f vertexNormal = new Vector3f(x, y, z).normalize();
        rotation.transform(vertexNormal);

        Vector3d worldNormal = new Vector3d(vertexNormal.x, vertexNormal.y, vertexNormal.z);
        float lighting = (float) Math.max(0.05, worldNormal.dot(lightDir)); // Minimum ambient lighting

        int litValue = (int) (255 * lighting);

        buffer.vertex(matrix, x, y, z).color(litValue, litValue, litValue, 255).uv(u, v).endVertex();
    }
}
