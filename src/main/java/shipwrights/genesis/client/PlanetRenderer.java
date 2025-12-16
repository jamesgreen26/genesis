package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.planets.PlanetData;

import java.util.ArrayList;
import java.util.List;

import static shipwrights.genesis.client.ShaderRegistry.*;
import static shipwrights.genesis.client.SunRenderer.renderSun;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlanetRenderer {




    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || !GenesisMod.isSpaceDimension(level)) {
            return;
        }

        long ticks = level.getGameTime();

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11C.GL_LEQUAL);
        RenderSystem.enableCull();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        // Separate planets into textured and non-textured
        List<PlanetData> proceduralPlanets = new ArrayList<>();
        List<PlanetData> texturedPlanets = new ArrayList<>();

        for (var planet : GenesisMod.planets) {
            if (PlanetTextures.hasTexture(planet.dimensionID)) {
                texturedPlanets.add(planet);
            } else {
                proceduralPlanets.add(planet);
            }
        }

        // Render non-textured planets with procedural shader
        if (!proceduralPlanets.isEmpty()) {
            // Force depth state with raw GL calls
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);

            VertexConsumer planetBuffer = bufferSource.getBuffer(getPlanetRenderType());
            for (var planet : proceduralPlanets) {
                renderProceduralPlanet(event, planet, planetBuffer, ticks);
            }
            bufferSource.endBatch(getPlanetRenderType());

            // Re-render with solid render type to write depth
            GL11.glColorMask(false, false, false, false); // Don't write color, only depth
            GL11.glDepthMask(true);
            VertexConsumer depthBuffer = bufferSource.getBuffer(RenderType.solid());
            for (var planet : proceduralPlanets) {
                renderDepthOnly(event, planet, depthBuffer, ticks);
            }
            bufferSource.endBatch(RenderType.solid());
            GL11.glColorMask(true, true, true, true); // Restore color write
        }

        // Render textured planets - each needs its own render type for the texture binding
        for (var planet : texturedPlanets) {
            ResourceLocation textureLocation = PlanetTextures.getTexture(planet.dimensionID);
            if (textureLocation != null) {
                // Force depth state with raw GL calls
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glDepthMask(true);

                var renderType = getTexturedPlanetRenderType(textureLocation);
                VertexConsumer texturedBuffer = bufferSource.getBuffer(renderType);
                renderTexturedPlanet(event, planet, texturedBuffer, ticks);
                bufferSource.endBatch(renderType);

                // Re-render with solid render type to write depth
                GL11.glColorMask(false, false, false, false);
                GL11.glDepthMask(true);
                VertexConsumer depthBuffer = bufferSource.getBuffer(RenderType.solid());
                renderDepthOnly(event, planet, depthBuffer, ticks);
                bufferSource.endBatch(RenderType.solid());
                GL11.glColorMask(true, true, true, true);
            }
        }

        // Force depth state before rendering sun with raw GL calls
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);

        VertexConsumer sunBuffer = bufferSource.getBuffer(getSunRenderType());
        renderSun(event, sunBuffer);
        bufferSource.endBatch(getSunRenderType());

        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void renderProceduralPlanet(RenderLevelStageEvent event, PlanetData data, VertexConsumer buffer, long ticks) {
        PoseStack poseStack = event.getPoseStack();

        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        Vector3d pos = data.getCurrentPos(ticks, event.getPartialTick());

        matrix.translate((float) (pos.x -event.getCamera().getPosition().x),
                (float) (pos.y -event.getCamera().getPosition().y),
                (float) (pos.z -event.getCamera().getPosition().z));

        matrix.rotate(new Quaternionf().rotationXYZ((float) data.rotation.x, (float) data.rotation.y, (float) data.rotation.z));

        float halfSize = (float) (data.getActualSize() / 2);

        int textureScale = data.hash % 256;

        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, textureScale, data.color, pos, data.rotation);
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, textureScale, data.color, pos, data.rotation);
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, textureScale, data.color, pos, data.rotation);
        addCubeFacePlanet(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, textureScale, data.color, pos, data.rotation);
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, textureScale, data.color, pos, data.rotation);
        addCubeFacePlanet(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, textureScale, data.color, pos, data.rotation);
    }

    private static void renderTexturedPlanet(RenderLevelStageEvent event, PlanetData data, VertexConsumer buffer, long ticks) {
        PoseStack poseStack = event.getPoseStack();

        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        Vector3d pos = data.getCurrentPos(ticks, event.getPartialTick());

        matrix.translate((float) (pos.x - event.getCamera().getPosition().x),
                (float) (pos.y - event.getCamera().getPosition().y),
                (float) (pos.z - event.getCamera().getPosition().z));

        matrix.rotate(new Quaternionf().rotationXYZ((float) data.rotation.x, (float) data.rotation.y, (float) data.rotation.z));

        float halfSize = (float) (data.getActualSize() / 2);

        // Calculate light direction (from planet toward sun at origin)
        Vector3d lightDir = new Vector3d(-pos.x, -pos.y, -pos.z).normalize();
        Quaternionf rotation = new Quaternionf().rotationXYZ((float) data.rotation.x, (float) data.rotation.y, (float) data.rotation.z);

        // UV layout (3x2 grid):
        // | north (0,0)     | west (1/3,0)   | south (2/3,0)  |
        // | east (0,0.5)    | down (1/3,0.5) | up (2/3,0.5)   |
        float third = 1.0f / 3.0f;
        float twoThirds = 2.0f / 3.0f;

        // South face (+Z)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation,
            -halfSize, -halfSize, halfSize,
             halfSize, -halfSize, halfSize,
             halfSize,  halfSize, halfSize,
            -halfSize,  halfSize, halfSize,
            twoThirds, 0.0f, 1.0f, 0.5f);

        // North face (-Z)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation,
             halfSize, -halfSize, -halfSize,
            -halfSize, -halfSize, -halfSize,
            -halfSize,  halfSize, -halfSize,
             halfSize,  halfSize, -halfSize,
            0.0f, 0.0f, third, 0.5f);

        // West face (-X)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation,
            -halfSize, -halfSize, -halfSize,
            -halfSize, -halfSize,  halfSize,
            -halfSize,  halfSize,  halfSize,
            -halfSize,  halfSize, -halfSize,
            third, 0.0f, twoThirds, 0.5f);

        // East face (+X)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation,
             halfSize, -halfSize,  halfSize,
             halfSize, -halfSize, -halfSize,
             halfSize,  halfSize, -halfSize,
             halfSize,  halfSize,  halfSize,
            0.0f, 0.5f, third, 1.0f);

        // Down face (-Y)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation,
            -halfSize, -halfSize, -halfSize,
             halfSize, -halfSize, -halfSize,
             halfSize, -halfSize,  halfSize,
            -halfSize, -halfSize,  halfSize,
            third, 0.5f, twoThirds, 1.0f);

        // Up face (+Y)
        addTexturedCubeFace(matrix, buffer, halfSize, lightDir, rotation,
            -halfSize,  halfSize,  halfSize,
             halfSize,  halfSize,  halfSize,
             halfSize,  halfSize, -halfSize,
            -halfSize,  halfSize, -halfSize,
            twoThirds, 0.5f, 1.0f, 1.0f);
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

    private static void addCubeFacePlanet(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int textureScale, float color, Vector3d planetPos, Vector3d planetRot) {
        int[] rgb = PlanetData.floatToRgb(color);
        int r = rgb[0], g = rgb[1], b = rgb[2];

        Vector3d lightDir = new Vector3d(-planetPos.x, -planetPos.y, -planetPos.z).normalize();

        Quaternionf rotation = new Quaternionf().rotationXYZ((float) planetRot.x, (float) planetRot.y, (float) planetRot.z);

        addVertexWithLighting(matrix, buffer, x1, y1, z1, r, g, b, textureScale, lightDir, rotation);
        addVertexWithLighting(matrix, buffer, x2, y2, z2, r, g, b, textureScale, lightDir, rotation);
        addVertexWithLighting(matrix, buffer, x3, y3, z3, r, g, b, textureScale, lightDir, rotation);
        addVertexWithLighting(matrix, buffer, x4, y4, z4, r, g, b, textureScale, lightDir, rotation);
    }

    private static void addVertexWithLighting(Matrix4f matrix, VertexConsumer buffer, float x, float y, float z, int r, int g, int b, int textureScale, Vector3d lightDir, Quaternionf rotation) {
        Vector3f vertexNormal = new Vector3f(x, y, z).normalize();

        rotation.transform(vertexNormal);

        Vector3d worldNormal = new Vector3d(vertexNormal.x, vertexNormal.y, vertexNormal.z);
        float lighting = (float) Math.max(0.0, worldNormal.dot(lightDir));

        int litR = (int) (r * lighting);
        int litG = (int) (g * lighting);
        int litB = (int) (b * lighting);

        buffer.vertex(matrix, x, y, z).color(litR, litG, litB, textureScale).uv((x < 0 ? 0 : 0.25f) + (y < 0 ? 0 : 0.5f), z < 0 ? 0 : 1).endVertex();
    }

    private static void renderDepthOnly(RenderLevelStageEvent event, PlanetData data, VertexConsumer buffer, long ticks) {
        PoseStack poseStack = event.getPoseStack();

        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        Vector3d pos = data.getCurrentPos(ticks, event.getPartialTick());

        matrix.translate((float) (pos.x - event.getCamera().getPosition().x),
                (float) (pos.y - event.getCamera().getPosition().y),
                (float) (pos.z - event.getCamera().getPosition().z));

        matrix.rotate(new Quaternionf().rotationXYZ((float) data.rotation.x, (float) data.rotation.y, (float) data.rotation.z));

        float halfSize = (float) (data.getActualSize() / 2);

        // Render a simple cube for depth using BLOCK vertex format (position, color, uv, uv2, normal)
        addDepthCubeFace(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize);
        addDepthCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize);
        addDepthCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize);
        addDepthCubeFace(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize);
        addDepthCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize);
        addDepthCubeFace(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize);
    }

    private static void addDepthCubeFace(Matrix4f matrix, VertexConsumer buffer,
                                         float x1, float y1, float z1,
                                         float x2, float y2, float z2,
                                         float x3, float y3, float z3,
                                         float x4, float y4, float z4) {
        // RenderType.solid() uses BLOCK format: position, color, uv, uv2 (lightmap), normal
        buffer.vertex(matrix, x1, y1, z1).color(255, 255, 255, 255).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(255, 255, 255, 255).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color(255, 255, 255, 255).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color(255, 255, 255, 255).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();
    }
}
