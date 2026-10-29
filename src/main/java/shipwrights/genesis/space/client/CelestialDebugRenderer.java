package shipwrights.genesis.space.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Registry;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.VantagePoint;

import java.util.OptionalDouble;

/// While entity hitboxes are shown (F3+B), outlines every celestial where it's drawn, in front of everything else.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public class CelestialDebugRenderer {

    private static final Quaternionf viewRotation = new Quaternionf();

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // AFTER_LEVEL is given the projection's pose stack rather than the camera's, so keep the camera's rotation from
        // earlier in the frame
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            event.getPoseStack().last().pose().getNormalizedRotation(viewRotation);
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()) return;

        ClientLevel level = minecraft.level;
        if (level == null) return;

        Registry<Celestial> celestials = level.registryAccess().registry(Celestial.REGISTRY_KEY).orElse(null);
        if (celestials == null) return;

        Camera camera = event.getCamera();
        Time time = Time.get(level, event.getPartialTick());
        Vec3 cameraPos = camera.getPosition();
        VantagePoint vantagePoint = VantagePoint.get(level, time, new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z));
        if (!vantagePoint.isReal()) return;

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(viewRotation);
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = bufferSource.getBuffer(DebugRenderTypes.CELESTIAL_OUTLINE);

        for (Celestial celestial : celestials) {
            CelestialPlacement placement = CelestialPlacement.of(new CelestialRenderContext(celestial, vantagePoint, time, poseStack, camera, level));
            double h = placement.halfExtent();

            poseStack.pushPose();
            poseStack.translate(placement.position().x(), placement.position().y(), placement.position().z());
            poseStack.mulPose(new Quaternionf(placement.rotation()));
            LevelRenderer.renderLineBox(poseStack, lines, -h, -h, -h, h, h, h, 1f, 0f, 1f, 1f);
            poseStack.popPose();
        }

        // Distant celestials would otherwise fade into the terrain fog
        float fogStart = RenderSystem.getShaderFogStart();
        FogRenderer.setupNoFog();
        bufferSource.endBatch(DebugRenderTypes.CELESTIAL_OUTLINE);
        RenderSystem.setShaderFogStart(fogStart);
    }

    private static final class DebugRenderTypes extends RenderType {
        private DebugRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                                 boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        // Like vanilla's lines, but drawn over everything
        static final RenderType CELESTIAL_OUTLINE = create("genesis_celestial_outline", DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES, 256, false, false, CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(2.0)))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));
    }
}
