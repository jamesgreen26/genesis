package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetTextures;
import shipwrights.genesis.mixin.FogRendererAccessor;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;

import java.lang.Math;

import static shipwrights.genesis.client.ShaderRegistry.getTexturedPlanetRenderType;

public class PlanetRenderer implements CelestialRenderer {

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @Nullable Celestial vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor)event.getLevelRenderer()).getLevel();
        long ticks = GenesisMod.getTicks(level);

        Vector3dc position = toRender.getPosition(ticks, event.getPartialTick());
        Quaterniondc rotation = toRender.getRotation(ticks, event.getPartialTick());
        double halfExtent = toRender.getActualSize() / 2;
        float alpha = 1f;

        // Special case: if rendering the vantage point itself, lock it at a fixed position in world space
        if (vantagePoint != null && vantagePoint.equals(toRender)) {
            var camera = event.getCamera();
            halfExtent = Minecraft.getInstance().gameRenderer.getRenderDistance();
            position = new Vector3d(
                0,
                - camera.getPosition().y - halfExtent - 100,
                0
            );
            rotation = new Quaterniond();
            int buildHeight = level.getMaxBuildHeight();
            float alphaInterpolateStart = (float) (halfExtent + buildHeight / 3f);
            float alphaInterpolateEnd = (float) (halfExtent + buildHeight);

            float cameraY = (float) camera.getPosition().y;
            alpha = Math.max(0f, Math.min(1f, (cameraY - alphaInterpolateStart) / (alphaInterpolateEnd - alphaInterpolateStart)));
        }
        // Transform by inverse of vantage point if present
        else if (vantagePoint != null) {
            Vector3dc vantagePos = vantagePoint.getPosition(ticks, event.getPartialTick());
            Quaterniondc vantageRot = vantagePoint.getRotation(ticks, event.getPartialTick());

            // Calculate relative position (subtract vantage point position)
            Vector3d relativePos = new Vector3d(
                position.x() - vantagePos.x(),
                position.y() - vantagePos.y(),
                position.z() - vantagePos.z()
            );

            // Apply inverse rotation of vantage point
            Quaterniond inverseVantageRot = new Quaterniond(vantageRot).conjugate();
            inverseVantageRot.transform(relativePos);
            position = relativePos;

            // Apply inverse rotation to the celestial's own rotation
            rotation = new Quaterniond(inverseVantageRot).mul(new Quaterniond(rotation));
        } else {
            position = position.sub(VectorConversionsMCKt.toJOML(event.getCamera().getPosition()), new Vector3d());
        }

        renderPlanetAt(toRender.getID(), event.getPoseStack(), position.x(), position.y(), position.z(), halfExtent, rotation, alpha);
    }

    private void renderPlanetAt(ResourceLocation planetID, PoseStack poseStack, double x, double y, double z, double halfExtent, Quaterniondc localRotation, float alpha) {
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

        Vector3d lightDir = null;

        // UV layout (3x2 grid):
        // | north (0,0)     | west (1/3,0)   | south (2/3,0)  |
        // | east (0,0.5)    | down (1/3,0.5) | up (2/3,0.5)   |
        float third = 1.0f / 3.0f;
        float twoThirds = 2.0f / 3.0f;

        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, twoThirds, 0.0f, 1.0f, 0.5f, alpha);        // South face (+Z)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, halfSize, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, 0.0f, 0.0f, third, 0.5f, alpha);        // North face (-Z)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, third, 0.0f, twoThirds, 0.5f, alpha);       // West face (-X)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, halfSize, -halfSize, halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, 0.0f, 0.5f, third, 1.0f, alpha);            // East face (+X)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, third, 0.5f, twoThirds, 1.0f, alpha);       // Down face (-Y)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, twoThirds, 0.5f, 1.0f, 1.0f, alpha);        // Up face (+Y)


        // End batch to flush rendering
        bufferSource.endBatch(renderType);
    }

    private static void addTexturedCubeFace(Matrix4f matrix, VertexConsumer buffer, float halfSize,
                                            Vector3d lightDir, Quaternionf rotation,
                                            float x1, float y1, float z1,
                                            float x2, float y2, float z2,
                                            float x3, float y3, float z3,
                                            float x4, float y4, float z4,
                                            float u1, float v1, float u2, float v2, float alpha) {
        addTexturedVertexWithLighting(matrix, buffer, x1, y1, z1, u1, v2, lightDir, rotation, alpha);
        addTexturedVertexWithLighting(matrix, buffer, x2, y2, z2, u2, v2, lightDir, rotation, alpha);
        addTexturedVertexWithLighting(matrix, buffer, x3, y3, z3, u2, v1, lightDir, rotation, alpha);
        addTexturedVertexWithLighting(matrix, buffer, x4, y4, z4, u1, v1, lightDir, rotation, alpha);
    }

    private static void addTexturedVertexWithLighting(Matrix4f matrix, VertexConsumer buffer,
                                                      float x, float y, float z,
                                                      float u, float v,
                                                      Vector3d lightDir, Quaternionf rotation, float alpha) {
        // Pass fog color in RGB and alpha in A for shader interpolation
        int fogRed = (int) (255 * FogRendererAccessor.getFogRed());
        int fogGreen = (int) (255 * FogRendererAccessor.getFogGreen());
        int fogBlue = (int) (255 * FogRendererAccessor.getFogBlue());

        buffer.vertex(matrix, x, y, z).color(fogRed, fogGreen, fogBlue, (int)(alpha * 255)).uv(u, v).endVertex();
    }
}
