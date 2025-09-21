package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11C;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.planets.PlanetData;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;

import static shipwrights.genesis.client.ShaderRegistry.SUN_SHADER;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlanetRenderer {


    private static LodestoneRenderType SUN_RENDER_TYPE;


    private static LodestoneRenderType getSunRenderType() {
        if (SUN_RENDER_TYPE == null) {
            SUN_RENDER_TYPE = LodestoneRenderTypeRegistry.createGenericRenderType("sun_render_type", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, LodestoneRenderTypeRegistry.builder()
                    .setShaderState(SUN_SHADER)
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("no_transparency", RenderSystem::disableBlend, () -> {}))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypeRegistry.CULL)
            );
        }
        return SUN_RENDER_TYPE;
    }

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
        VertexConsumer buffer = bufferSource.getBuffer(getSunRenderType());

        for (var planet: GenesisMod.planets) {
            renderPlanet(event, planet, buffer);
        }

        renderSun(event, buffer);

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

        addCubeFace(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize);
        addCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize);
        addCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize);
        addCubeFace(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize);
        addCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize);
        addCubeFace(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize);
    }

    private static void renderSun(RenderLevelStageEvent event, VertexConsumer buffer) {
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

        addCubeFace(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize);
        addCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize);
        addCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize);
        addCubeFace(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize);
        addCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize);
        addCubeFace(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize);
    }

    private static void addCubeFace(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2,
                                    float x3, float y3, float z3, float x4, float y4, float z4) {
        buffer.vertex(matrix, x1, y1, z1).uv(0, 0).endVertex();
        buffer.vertex(matrix, x2, y2, z2).uv(1, 0).endVertex();
        buffer.vertex(matrix, x3, y3, z3).uv(1, 1).endVertex();
        buffer.vertex(matrix, x4, y4, z4).uv(0, 1).endVertex();
    }
}
