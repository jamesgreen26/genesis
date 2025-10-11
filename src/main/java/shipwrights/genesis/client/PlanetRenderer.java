package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.planets.PlanetData;

import static shipwrights.genesis.client.ShaderRegistry.getPlanetRenderType;
import static shipwrights.genesis.client.ShaderRegistry.getSunRenderType;
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
        VertexConsumer planetBuffer = bufferSource.getBuffer(getPlanetRenderType());

        for (var planet: GenesisMod.planets) {
            renderPlanet(event, planet, planetBuffer, ticks);
        }

        bufferSource.endBatch(getPlanetRenderType());

        VertexConsumer sunBuffer = bufferSource.getBuffer(getSunRenderType());
        renderSun(event, sunBuffer);
        bufferSource.endBatch(getSunRenderType());

        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void renderPlanet(RenderLevelStageEvent event, PlanetData data, VertexConsumer buffer, long ticks) {
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

        matrix.rotate(new Quaternionf().rotationXYZ((float) data.rot.x, (float) data.rot.y, (float) data.rot.z));

        float halfSize = (float) (data.size / 2);

        int textureScale = data.hash % 256;

        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, textureScale, data.color, pos, data.rot);
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, textureScale, data.color, pos, data.rot);
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, textureScale, data.color, pos, data.rot);
        addCubeFacePlanet(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, textureScale, data.color, pos, data.rot);
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, textureScale, data.color, pos, data.rot);
        addCubeFacePlanet(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, textureScale, data.color, pos, data.rot);
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
}
