package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(GenesisMod.SPACE_DIM)) {
            return;
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11C.GL_LEQUAL);
        RenderSystem.enableCull();



        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer planetBuffer = bufferSource.getBuffer(getPlanetRenderType());

        for (var planet: GenesisMod.planets) {
            renderPlanet(event, planet, planetBuffer);
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

    private static void renderPlanet(RenderLevelStageEvent event, PlanetData data, VertexConsumer buffer) {
        PoseStack poseStack = event.getPoseStack();

        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        matrix.translate((float) (data.pos.x -event.getCamera().getPosition().x),
                (float) (data.pos.y -event.getCamera().getPosition().y),
                (float) (data.pos.z -event.getCamera().getPosition().z));

        matrix.rotate(new Quaternionf().rotationXYZ((float) data.rot.x, (float) data.rot.y, (float) data.rot.z));

        float halfSize = (float) (data.size / 2);

        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize);
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize);
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize);
        addCubeFacePlanet(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize);
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize);
        addCubeFacePlanet(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize);
    }

    private static void addCubeFacePlanet(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2,
                                       float x3, float y3, float z3, float x4, float y4, float z4) {
        buffer.vertex(matrix, x1, y1, z1).color(x1 < 0 ? 0 : 255, y1 < 0 ? 0 : 255, z1 < 0 ? 0 : 255, 0).uv(0, 0).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(x2 < 0 ? 0 : 255, y2 < 0 ? 0 : 255, z2 < 0 ? 0 : 255, 0).uv(1, 0).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color(x3 < 0 ? 0 : 255, y3 < 0 ? 0 : 255, z3 < 0 ? 0 : 255, 0).uv(1, 1).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color(x4 < 0 ? 0 : 255, y4 < 0 ? 0 : 255, z4 < 0 ? 0 : 255, 0).uv(0, 1).endVertex();
    }
}
